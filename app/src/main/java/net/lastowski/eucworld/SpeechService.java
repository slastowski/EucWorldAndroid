package net.lastowski.eucworld;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import net.lastowski.common.Commons;
import net.lastowski.eucworld.utils.Constants;
import net.lastowski.eucworld.utils.NotificationUtil;
import net.lastowski.eucworld.utils.SettingsUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SpeechService extends Service implements TextToSpeech.OnInitListener {

    private static SpeechService instance = null;
    private static boolean ttsInitialized = false;

    private int sayCount = 0;
    private int sayPriority = -1;
    private TextToSpeech tts;
    private AudioManager am;
    private BatteryManager bm;
    private BluetoothAdapter btAdapter;
    private long ttsLastPeriodicMessageTime = 0;
    private double ttsLastPeriodicMessageDistance = -999;
    private long ttsLastDisconnectedMessageTime = 0;
    private HashMap<String, String> ttsMap;
    private Timer timer;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private void startTimer() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                long now = SystemClock.elapsedRealtime();
                // Periodic message
                if ((isBTAudioConnected() || !SettingsUtil.getSpeechUseA2DPOnly(getApplicationContext())) && (LivemapService.getStatus() != Commons.TourTrackingServiceStatus.PAUSED || getSpeed() >= Constants.MIN_RIDING_SPEED_GPS))
                    sayPeriodicMessage(false);
                // Wheel connection stale warning
                if (!WheelData.getInstance().isConnected() &&
                        !BluetoothLeService.isDisconnected() &&
                        now - ttsLastDisconnectedMessageTime >= 5000 &&
                        (LivemapService.getStatus() != Commons.TourTrackingServiceStatus.PAUSED || getSpeed() >= Constants.MIN_RIDING_SPEED_GPS)) {
                    ttsLastDisconnectedMessageTime = now;
                    say("", "warning1", 0, true);
                }
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 15000, 100);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            switch (action) {
                case Constants.ACTION_BLUETOOTH_CONNECT:
                    ttsLastDisconnectedMessageTime = SystemClock.elapsedRealtime();
                    break;
                case Constants.ACTION_WHEEL_CONNECTED:
                    if ((isBTAudioConnected() || !SettingsUtil.getSpeechUseA2DPOnly(getApplicationContext()) && SettingsUtil.getSpeechGPSBTStatus(getApplicationContext())))
                        say(getString(R.string.speech_text_connected), "info");
                    break;
                case Constants.ACTION_WHEEL_CONNECTION_LOST:
                    ttsLastDisconnectedMessageTime = SystemClock.elapsedRealtime();
                    if ((isBTAudioConnected() || !SettingsUtil.getSpeechUseA2DPOnly(getApplicationContext()) && SettingsUtil.getSpeechGPSBTStatus(getApplicationContext())))
                        say(getString(R.string.speech_text_connection_lost), "warning1");
                    break;
                case Constants.ACTION_WHEEL_DISCONNECTED:
                    ttsLastPeriodicMessageTime = 0;
                    ttsLastPeriodicMessageDistance = -999;
                    if ((isBTAudioConnected() || !SettingsUtil.getSpeechUseA2DPOnly(getApplicationContext()) && SettingsUtil.getSpeechGPSBTStatus(getApplicationContext())))
                            say(getString(R.string.speech_text_disconnected), "info");
                    break;
                case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                    if (WheelData.getInstance().isVoltageAlarmActive())
                        say(getString(R.string.speech_text_voltage_too_high), "alarm", 5, true);
                    else
                    if (WheelData.getInstance().isPeakCurrentAlarmActive() || WheelData.getInstance().isSustainedCurrentAlarmActive())
                        say(getString(R.string.speech_text_current_too_high), "alarm", 5, true);
                    else
                    if (WheelData.getInstance().isTemperatureAlarmActive())
                        say(getString(R.string.speech_text_temp_too_high), "alarm", 5, true);
                    else
                    if (WheelData.getInstance().isSpeedAlarm1Active())
                        say(getString(R.string.speech_text_slow_down_3), "warning3", 4, true);
                    else
                    if (WheelData.getInstance().isSpeedAlarm2Active())
                        say(getString(R.string.speech_text_slow_down_2), "warning2", 3, true);
                    else
                    if (WheelData.getInstance().isSpeedAlarm3Active())
                        say(getString(R.string.speech_text_slow_down_1), "warning1", 2, true);
                    break;
                case Constants.ACTION_SPEECH_SAY:
                    int priority = intent.getIntExtra(Constants.INTENT_EXTRA_SPEECH_PRIORITY, 1);
                    if (isBTAudioConnected() || !SettingsUtil.getSpeechUseA2DPOnly(getApplicationContext()) || priority > 1) {
                        String text = intent.getStringExtra(Constants.INTENT_EXTRA_SPEECH_TEXT);
                        String earcon = intent.getStringExtra(Constants.INTENT_EXTRA_SPEECH_EARCON);
                        boolean nowOrNever = intent.getBooleanExtra(Constants.INTENT_EXTRA_SPEECH_NOW_OR_NEVER, false);
                        say(text, earcon, priority, nowOrNever);
                    }
                    break;
                case Constants.ACTION_REQUEST_VOICE_REPORT:
                    sayPeriodicMessage(true);
                    break;
                case Constants.ACTION_REQUEST_VOICE_DISMISS:
                    say(" ", "", 2, true);
                    break;
            }
        }
    };

    private final AudioManager.OnAudioFocusChangeListener afl = focusChange -> { };

    @Override
    public void onCreate() {
        ttsLastPeriodicMessageTime = 0;
        ttsLastPeriodicMessageDistance = -999;
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        tts = new TextToSpeech(this, this);
        ttsMap = new HashMap<>();
        ttsMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "EucworldSpeech");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECT);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_WHEEL_CONNECTED);
        intentFilter.addAction(Constants.ACTION_WHEEL_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_WHEEL_CONNECTION_LOST);
        intentFilter.addAction(Constants.ACTION_REQUEST_VOICE_DISMISS);
        intentFilter.addAction(Constants.ACTION_REQUEST_VOICE_REPORT);
        intentFilter.addAction(Constants.ACTION_SPEECH_SAY);
        registerReceiver(receiver, intentFilter);
        Intent serviceStartedIntent = new Intent(Constants.ACTION_SPEECH_SERVICE_TOGGLED).putExtra(Constants.INTENT_EXTRA_IS_RUNNING, true);
        sendBroadcast(serviceStartedIntent);

        startForeground(NotificationUtil.getNotificationId(), NotificationUtil.getNotification(this));

        Configuration conf = getResources().getConfiguration();
        conf.setLocale(Locale.getDefault());
        getResources().updateConfiguration(conf, null);

        return START_STICKY;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) { }
                @Override
                public void onError(String s) { }
                @Override
                public void onDone(String utterance_id) {
                    --sayCount;
                    if (sayCount <= 0) {
                        sayCount = 0;
                        if (sayPriority > 0) am.abandonAudioFocus(afl);
                        sayPriority = -1;
                    }
                }
            });
            int result;
            if (Locale.getDefault().getLanguage().equals("ca"))
                result = tts.setLanguage(new Locale("es"));
            else
                result = tts.setLanguage(Locale.getDefault());
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.addEarcon("alarm", getPackageName(), R.raw.alarm);
                tts.addEarcon("info", getPackageName(), R.raw.info);
                tts.addEarcon("infogps", getPackageName(), R.raw.infogps);
                tts.addEarcon("time", getPackageName(), R.raw.time);
                tts.addEarcon("warning1", getPackageName(), R.raw.warning_1);
                tts.addEarcon("warning2", getPackageName(), R.raw.warning_2);
                tts.addEarcon("warning3", getPackageName(), R.raw.warning_3);
                ttsInitialized = true;

                if (isBTAudioConnected() || !SettingsUtil.getSpeechUseA2DPOnly(getApplicationContext()))
                    say(getString(R.string.speech_text_welcome_on_board), "info");
                startTimer();
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        if (timer != null)
            timer.cancel();
        ttsInitialized = false;
        instance = null;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        Intent serviceStartedIntent = new Intent(Constants.ACTION_SPEECH_SERVICE_TOGGLED).putExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
        sendBroadcast(serviceStartedIntent);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void say(String text, String earcon, int priority, boolean nowOrNever) {
        if (priority <= sayPriority && nowOrNever) return;
        if (ttsInitialized && (priority >= sayPriority)) {
            int res = AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            switch (SettingsUtil.getSpeechFocus(this)) {
                case 1:
                    res = (priority > 0) ? am.requestAudioFocus(afl, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) : AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
                    break;
                case 2:
                    res = (priority > 0) ? am.requestAudioFocus(afl, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE) : AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
                    break;
            }
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                int count = 0;
                if (priority > sayPriority) sayCount = 0;
                setPitch(SettingsUtil.getSpeechPitch(this));
                setRate(SettingsUtil.getSpeechRate(this));
                if (!earcon.isEmpty()) {
                    if (tts.playEarcon(earcon, (priority > sayPriority) ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, ttsMap) == TextToSpeech.SUCCESS) count += 1;
                    if (!text.equals("") && tts.speak(text, TextToSpeech.QUEUE_ADD, ttsMap) == TextToSpeech.SUCCESS) count += 1;
                }
                else
                if (!text.equals("") && tts.speak(text, (priority > sayPriority) ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, ttsMap) == TextToSpeech.SUCCESS) count += 1;
                if (count > 0) {
                    sayPriority = priority;
                    sayCount += count;
                }
            }
        }
    }

    private void say(String text, String earcon) {
        say(text, earcon, 1, false);
    }

    private void setPitch(int pitch) {
        if (ttsInitialized) {
            if (pitch == 0)
                tts.setPitch(0.75f);
            else if (pitch == 1)
                tts.setPitch(1.0f);
            else if (pitch == 2)
                tts.setPitch(1.5f);
        }
    }

    private void setRate(int rate) {
        if (ttsInitialized) {
            if (rate == 0)
                tts.setSpeechRate(0.75f);
            else if (rate == 1)
                tts.setSpeechRate(1.0f);
            else if (rate == 2)
                tts.setSpeechRate(1.5f);
        }
    }

    @TargetApi(21)
    private int getPhoneBatteryLevel() {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && (bm != null)) {
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        else
            return -1;
    }

    private void sayPeriodicMessage(boolean force) {
        if (sayPriority != -1) return;  // Wait for other messages to finish playing
        long now = SystemClock.elapsedRealtime();
        boolean useWheelData = WheelData.getInstance().isConnected();
        boolean useLivemapData = LivemapService.isConnected();

        // Distance
        double dist = 0;
        if (useWheelData)
            dist = WheelData.getInstance().getDistanceDouble();
        else if (useLivemapData)
            dist = LivemapService.getDistance();

        // Speed
        double speed = 0;
        if (useWheelData) {
            speed = WheelData.getInstance().getSpeedDouble();
        }
        else if (useLivemapData)
            speed = LivemapService.getSpeed();

        // Speed avg
        double speed_avg = 0;
        if (useWheelData) {
            speed_avg = WheelData.getInstance().getAverageSpeedDouble();
        }
        else if (useLivemapData)
            speed_avg = LivemapService.getAverageSpeed();

        // Speed avg riding
        double speed_avg_riding = 0;
        if (useWheelData) {
            speed_avg_riding = WheelData.getInstance().getAverageRidingSpeedDouble();
        }
        else if (useLivemapData)
            speed_avg_riding = LivemapService.getAverageRidingSpeed();

        // Speed top
        double speed_top = 0;
        if (useWheelData) {
            speed_top = WheelData.getInstance().getTopSpeedDouble();
        }
        else if (useLivemapData)
            speed_top = LivemapService.getTopSpeed();

        // Ride time
        int ridetime = 0;
        if (useWheelData)
            ridetime = WheelData.getInstance().getRideTime();
        else if (useLivemapData)
            ridetime = LivemapService.getRideTime();

        // Riding time
        int ridingtime = 0;
        if (useWheelData)
            ridingtime = WheelData.getInstance().getRidingTime();
        else if (useLivemapData)
            ridingtime = LivemapService.getRidingTime();

        if (!force) {
            if (SettingsUtil.getSpeechOnlyInMotion(this) && speed < Constants.MIN_RIDING_SPEED_GPS)
                return; // If activated in settings, inhibit periodic messages when speed < 2 km/h
            if (SettingsUtil.getSpeechMsgMode(this) == 0) {
                if (now - ttsLastPeriodicMessageTime < SettingsUtil.getSpeechMsgInterval(this) * 1000)
                    return;  // Time interval not passed
            } else {
                double d = (double) SettingsUtil.getSpeechMsgInterval(this) / 60;
                if (dist < ttsLastPeriodicMessageDistance + d)
                    return; // Distance interval not passed
            }
        }

        String text = "";
        ttsLastPeriodicMessageTime = now;
        ttsLastPeriodicMessageDistance = dist;

        // Speed
        if (SettingsUtil.getSpeechMessagesSpeed(this) && speed >= Constants.MIN_RIDING_SPEED_GPS)
            text += " " + String.format(Locale.US, getString(R.string.speech_text_speed), formatSpeed(speed));

        // Average speed
        if (SettingsUtil.getSpeechMessagesAvgSpeed(this) && speed_avg >= Constants.MIN_RIDING_SPEED_GPS)
            text += " " + String.format(Locale.US, getString(R.string.speech_text_avg_speed), formatSpeed(speed_avg));

        // Average riding speed
        if (SettingsUtil.getSpeechMessagesAvgRidingSpeed(this) && speed_avg_riding >= Constants.MIN_RIDING_SPEED_GPS)
            text += " " + String.format(Locale.US, getString(R.string.speech_text_avg_riding_speed), formatSpeed(speed_avg_riding));

        // Top speed
        if (SettingsUtil.getSpeechMessagesTopSpeed(this) && speed_top >= Constants.MIN_RIDING_SPEED_GPS)
            text += " " + String.format(Locale.US, getString(R.string.speech_text_top_speed), formatSpeed(speed_top));

        // Distance
        if (SettingsUtil.getSpeechMessagesDistance(this)) {
            String distance = formatDistance(dist);
            if (!distance.equals(""))
                text += " " + String.format(Locale.US, getString(R.string.speech_text_distance), distance);
        }

        // Battery level
        if (SettingsUtil.getSpeechMessagesBattery(this) && useWheelData)
            text += " " + String.format(Locale.US, getString(R.string.speech_text_battery), WheelData.getInstance().getAverageBatteryLevelDouble());

        // Phone battery level
        if (SettingsUtil.getSpeechMessagesPhoneBattery(this)) {
            int bl = getPhoneBatteryLevel();
            if (bl >= 0)
                text += " " + String.format(Locale.US, getString(R.string.speech_phone_battery), bl);
        }

        // Voltage
        if (SettingsUtil.getSpeechMessagesVoltage(this) && useWheelData)
            text += " " + String.format(Locale.US, getString(R.string.speech_text_voltage), WheelData.getInstance().getVoltageDouble());

        // Current
        if (SettingsUtil.getSpeechMessagesCurrent(this) && useWheelData) {
            double current = Math.round(WheelData.getInstance().getCurrentDouble());
            if (Math.abs(current) > 0)
                text += " " + String.format(Locale.US, getString(R.string.speech_text_current), current);
        }

        // Power
        if (SettingsUtil.getSpeechMessagesPower(this) && useWheelData) {
            double power = WheelData.getInstance().getPowerDouble();
            if (power > 50)
                text += " " + String.format(Locale.US, getString(R.string.speech_text_power), power);
        }

        // Temperature
        if (SettingsUtil.getSpeechMessagesTemperature(this) && useWheelData)
            text += " " + String.format(Locale.US, getString(R.string.speech_text_temperature), formatTemperature(WheelData.getInstance().getTemperature()));

        // Time
        if (SettingsUtil.getSpeechMessagesTime(this)) {
            SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.US);
            String time = df.format(new Date());
            text += " " + String.format(Locale.US, getString(R.string.speech_text_time), time);
        }

        // Time from start
        if (SettingsUtil.getSpeechMessagesTimeFromStart(this)) {
            String timefromstart = formatDuration(ridetime);
            if (!timefromstart.equals(""))
                text += " " + String.format(Locale.US, getString(R.string.speech_text_time_from_start), timefromstart);
        }

        // Time in motion
        if (SettingsUtil.getSpeechMessagesTimeInMotion(this)) {
            String timeinmotion = formatDuration(ridingtime);
            if (!timeinmotion.equals(""))
                text += " " + String.format(Locale.US, getString(R.string.speech_text_time_in_motion), timeinmotion);
        }

        // Weather
        if (SettingsUtil.getSpeechMessagesWeather(this) && LivemapService.isInstanceCreated()) {
            if (LivemapService.getInstance().getWeatherTimestamp() > 0) {
                String temp = formatTemperature(LivemapService.getInstance().getWeatherTemperature());
                String tempf = formatTemperature(LivemapService.getInstance().getWeatherTemperatureFeels());
                text += " " + getString(R.string.speech_text_weather) + " " + getConditionText(LivemapService.getInstance().getWeatherConditionCode());
                if (temp.equals(tempf))
                    text += " " + String.format(Locale.US, getString(R.string.speech_text_weather_temp), temp);
                else
                    text += " " + String.format(Locale.US, getString(R.string.speech_text_weather_temp_feels), temp, tempf);
            }
        }

        if (!text.equals(""))
            say(text, (useWheelData) ? "info" : "infogps");

        if (SettingsUtil.getSpeechMessagesBatteryLowLevel(this) > 0 && WheelData.getInstance().getAverageBatteryLevelDouble() < SettingsUtil.getSpeechMessagesBatteryLowLevel(this) && useWheelData)
            say(getString(R.string.speech_text_low_battery), "warning1");

        if (SettingsUtil.getSpeechMessagesPhoneBatteryLowLevel(this) > 0 && getPhoneBatteryLevel() < SettingsUtil.getSpeechMessagesPhoneBatteryLowLevel(this))
            say(getString(R.string.speech_text_low_phone_battery), "warning1");
    }

    private String formatSpeed(double speed) {
        return (SettingsUtil.isUseMi(this)) ? String.format(Locale.US, "%.0f mi/h", speed / 1.609) : String.format(Locale.US, "%.0f km/h", speed);
    }

    private String formatTemperature(double temperature) {
        return (SettingsUtil.isUseF(this)) ? String.format(Locale.US, "%.0f °F", temperature * 1.8 + 32) : String.format(Locale.US, "%.0f °C", temperature);
    }

    private String formatDistance(double distance) {
        if (SettingsUtil.isUseMi(this)) {
            int miles = (int) (distance / 1.609);
            int yds100 = (int) (17.6 * ((distance / 1.609) - miles));
            if (miles > 0 || yds100 > 0) {
                String res = "";
                if (miles == 1)
                    res = "1 mi";
                else
                if (miles > 1)
                    res = String.format(Locale.US, "%d mi", miles);
                if (yds100 > 0) {
                    if (!res.equals(""))
                        res += ", ";
                    int yds = 100 * yds100;
                    res += String.format(Locale.US,"%d yd", yds);
                }
                return res;
            }
            else
                return "";
        }
        else {
            int kms = (int) distance;
            int mtrs100 = (int) (10 * (distance - kms));
            if (kms > 0 || mtrs100 > 0) {
                String res = "";
                if (kms == 1)
                    res = "1 km";
                else if (kms > 1)
                    res = String.format(Locale.US, "%d km", kms);
                if (mtrs100 > 0) {
                    if (!res.equals(""))
                        res += ", ";
                    int mtrs = 100 * mtrs100;
                    res += String.format(Locale.US, "%d m", mtrs);
                }
                return res;
            } else
                return "";
        }
    }

    private String formatDuration(long duration) {
        String text = "";
        if (duration > 0) {
            long hours = TimeUnit.SECONDS.toHours(duration);
            long minutes = TimeUnit.SECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(duration));
            if ((hours > 0) || (minutes > 0)) {
                if (hours == 0)
                    text = String.format(Locale.US, getString(R.string.duration_fmt_min), minutes);
                else
                if (minutes == 0)
                    text = String.format(Locale.US, getString(R.string.duration_fmt_hr), hours);
                else
                    text = String.format(Locale.US, getString(R.string.duration_fmt_hr_and_min), hours, minutes);
            }
        }
        return text;
    }

    private String getConditionText(int code) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String name = (hour > 5 && hour < 18) ? "d"+code : "n"+code;
        int id = getResources().getIdentifier(name, "string", getPackageName());
        return (id > 0) ? getResources().getString(id)+"." : "";
    }

    private double getSpeed() {
        if (WheelData.getInstance().isConnected())
            return WheelData.getInstance().getSpeedDouble();
        else
        if (LivemapService.isConnected())
            return LivemapService.getSpeed();
        else
            return 0;
    }

    private boolean isBTAudioConnected() {
        if (btAdapter != null)
            return (btAdapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED);
        else
            return false;
    }

}
