package net.lastowski.eucworld;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import net.lastowski.common.Commons;
import net.lastowski.common.EucWorldApi;
import net.lastowski.common.Value;
import net.lastowski.eucworld.utils.Constants;
import net.lastowski.eucworld.utils.SettingsUtil;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

public class WearService extends WearableListenerService {

    private static WearService instance = null;
    private static String wearNodeId = "";
    private static int dataUpdateMinPeriod = 500;
    private static int phoneUpdateMinPeriod = 10000;

    private int wearPageIndex = 0;
    private boolean resumed = false;
    private long lastData = 0;
    private long lastPhone = 0;

    private Timer timer;
    private boolean alarm = false;
    private long lastAlarm = 0;

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            long now = SystemClock.elapsedRealtime();
            if (alarm && lastAlarm + 2000 < now) {
                lastAlarm = now;
                sendAlarm();
            }
            sendPhone();
        }
    };

    public static boolean isInstanceCreated() { return instance != null; }
    public static void setDataUpdateMinPeriod(int minPeriod) { dataUpdateMinPeriod = minPeriod; }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action != null)
                switch (action) {
                    case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                        break;
                    case Constants.ACTION_LIVEMAP_LOCATION_UPDATED:
                    case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                        sendData();
                        break;
                    case Constants.ACTION_ALARM_TRIGGERED:
                        alarm = true;
                        break;
                    case Constants.ACTION_ALARM_FINISHED:
                        alarm = false;
                        sendAlarm();
                        break;
                    case Constants.ACTION_WHEEL_CONNECTED:
                        getWearNodeId();
                    case Constants.ACTION_WHEEL_DISCONNECTED:
                    case Constants.ACTION_WHEEL_CONNECTION_LOST:
                    case Constants.ACTION_LIVEMAP_STATUS:
                        sendSync(getApplicationContext());
                        break;
                }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        wearNodeId = "";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_LIVEMAP_LOCATION_UPDATED);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_ALARM_TRIGGERED);
        intentFilter.addAction(Constants.ACTION_ALARM_FINISHED);
        intentFilter.addAction(Constants.ACTION_LIVEMAP_STATUS);
        intentFilter.addAction(Constants.ACTION_WHEEL_CONNECTED);
        intentFilter.addAction(Constants.ACTION_WHEEL_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_WHEEL_CONNECTION_LOST);
        registerReceiver(broadcastReceiver, intentFilter);
        getWearNodeId();

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 100);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
        instance = null;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        wearNodeId = messageEvent.getSourceNodeId();
        try {
            String path = messageEvent.getPath();
            String data = new String(messageEvent.getData());
            Timber.d("RX< %s %s", path, data);
            JSONObject json = new JSONObject(data.isEmpty() ? "{}" : data);
            switch (path) {
                case Commons.PATH_START:
                    Intent startIntent = new Intent(this, MainActivity.class);
                    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startIntent);
                    break;
                case Commons.PATH_WATCH:
                    if (json.has(EucWorldApi.WKI.Watch.Battery_Level)) Data.watchBatteryLevel.setAsInt(json.getInt(EucWorldApi.WKI.Watch.Battery_Level));
                    break;
                case Commons.PATH_SYNC:
                    if (json.has("r")) resumed = json.getBoolean("r");
                    if (json.has("p")) wearPageIndex = json.getInt("p");
                    sendSync(this);
                    break;
                case Commons.PATH_SINGLETAP:
                    performAction(SettingsUtil.getWatchActionSingle(this));
                    break;
                case Commons.PATH_DOUBLETAP:
                    performAction(SettingsUtil.getWatchActionDouble(this));
                    break;
                case Commons.PATH_LONGPRESS:
                    performAction(SettingsUtil.getWatchActionHold(this));
                    break;
                case Commons.PATH_TOUR_START:
                    boolean fineLocation = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                    if (fineLocation) {
                        if (!LivemapService.isInstanceCreated())
                            startService(new Intent(getApplicationContext(), LivemapService.class));
                    }
                    break;
                case Commons.PATH_TOUR_PAUSE:
                    sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_PAUSE));
                    break;
                case Commons.PATH_TOUR_RESUME:
                    sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_RESUME));
                    break;
                case Commons.PATH_TOUR_FINISH:
                    if (LivemapService.isInstanceCreated())
                            stopService(new Intent(getApplicationContext(), LivemapService.class));
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(Context context, String path, String data) {
        if (wearNodeId != null) {
            Timber.d("TX> %s %s", path, data);
            Task<Integer> sendMessageTask = Wearable.getMessageClient(context).sendMessage(wearNodeId, path, data.getBytes());
            sendMessageTask.addOnCompleteListener(
                    task -> {
                        if (task.isSuccessful()) {
                            //
                        }
                        else {
                            //
                        }
                    });
        }
    }

    private void getWearNodeId() {
        Task<CapabilityInfo> capabilityInfoTask = Wearable.getCapabilityClient(this)
                .getCapability(Commons.CAPABILITY_WATCH, CapabilityClient.FILTER_ALL);
        capabilityInfoTask.addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
            @Override
            public void onComplete(@NonNull Task<CapabilityInfo> task) {
                if (task.isSuccessful()) {
                    CapabilityInfo capabilityInfo = task.getResult();
                    for (Node node : capabilityInfo.getNodes()) {
                        wearNodeId = node.getId();
                        Timber.d("  * found wear node %s (%s)", wearNodeId, node.getDisplayName());
                    }
                    sendMessage(getApplicationContext(), Commons.PATH_START, "{}");
                }
            }
        });
    }

    private void sendData() {
        long now = SystemClock.elapsedRealtime();
        if (wearNodeId != null) {
            if (!resumed && lastPhone + phoneUpdateMinPeriod < now) {
                lastPhone = now;
                sendMessage(this, Commons.PATH_DATA, "{}");
            }
            else
            if (WheelData.getInstance() != null && resumed && lastData + dataUpdateMinPeriod < now) {
                lastData = now;
                JSONObject json = new JSONObject();
                try {
                    switch (wearPageIndex) {
                        case 0: // Dashboard
                            jsonPutValue(json, Data.vehicleBatteryLevelFiltered);
                            jsonPutValue(json, Data.vehicleBatteryLevel);
                            jsonPutValue(json, Data.vehicleBatteryLevelMax);
                            jsonPutValue(json, Data.vehicleBatteryLevelMin);
                            jsonPutValue(json, Data.vehicleDistance);
                            jsonPutValue(json, Data.vehicleLoad);
                            jsonPutValue(json, Data.vehicleLoadMaxRegen);
                            jsonPutValue(json, Data.vehicleLoadMax);
                            jsonPutValue(json, Data.vehicleSpeed);
                            jsonPutValue(json, Data.vehicleSpeedAvg);
                            jsonPutValue(json, Data.vehicleSpeedMax);
                            jsonPutValue(json, Data.vehicleSpeedAvgRiding);
                            jsonPutValue(json, Data.vehicleTemperature);
                            jsonPutValue(json, Data.vehicleTemperatureMax);
                            jsonPutValue(json, Data.vehicleTemperatureMin);

                            jsonPutValue(json, Data.tourDistance);
                            jsonPutValue(json, Data.tourSpeed);
                            jsonPutValue(json, Data.tourSpeedAvg);
                            jsonPutValue(json, Data.tourSpeedMax);
                            jsonPutValue(json, Data.tourSpeedAvgRiding);
                            break;
                        case 1: // Live data
                            jsonPutValue(json, Data.vehicleBatteryLevel);
                            jsonPutValue(json, Data.vehicleCurrent);
                            jsonPutValue(json, Data.vehicleDistance);
                            jsonPutValue(json, Data.vehicleDistanceTotal);
                            jsonPutValue(json, Data.vehicleDistanceUser);
                            jsonPutValue(json, Data.vehicleDistanceVehicle);
                            jsonPutValue(json, Data.vehicleDuration);
                            jsonPutValue(json, Data.vehicleDurationRiding);
                            jsonPutValue(json, Data.vehiclePower);
                            jsonPutValue(json, Data.vehicleSpeed);
                            jsonPutValue(json, Data.vehicleTemperature);
                            jsonPutValue(json, Data.vehicleTemperatureMotor);
                            jsonPutValue(json, Data.vehicleVoltage);
                            break;
                        case 2: // Statistics
                            jsonPutValue(json, Data.vehicleBatteryLevelMax);
                            jsonPutValue(json, Data.vehicleBatteryLevelMin);
                            jsonPutValue(json, Data.vehicleCurrentAvg);
                            jsonPutValue(json, Data.vehicleCurrentMax);
                            jsonPutValue(json, Data.vehicleCurrentMin);
                            jsonPutValue(json, Data.vehicleLoadMaxRegen);
                            jsonPutValue(json, Data.vehicleLoadMax);
                            jsonPutValue(json, Data.vehiclePowerAvg);
                            jsonPutValue(json, Data.vehiclePowerMax);
                            jsonPutValue(json, Data.vehiclePowerMin);
                            jsonPutValue(json, Data.vehicleSpeedAvg);
                            jsonPutValue(json, Data.vehicleSpeedMax);
                            jsonPutValue(json, Data.vehicleSpeedAvgRiding);
                            jsonPutValue(json, Data.vehicleTemperatureMax);
                            jsonPutValue(json, Data.vehicleTemperatureMin);
                            jsonPutValue(json, Data.vehicleVoltageMax);
                            jsonPutValue(json, Data.vehicleVoltageMin);
                            break;
                        case 3: // Tour tracking
                            jsonPutValue(json, Data.tourDistance);
                            jsonPutValue(json, Data.tourDuration);
                            break;
                    }
                    sendMessage(this, Commons.PATH_DATA, json.toString());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendPhone() {
        long now = SystemClock.elapsedRealtime();
        if (wearNodeId != null) {
            if (lastPhone + phoneUpdateMinPeriod < now) {
                lastPhone = now;
                JSONObject json = new JSONObject();
                try {
                    int batt = ((BatteryManager)getSystemService(BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    if (batt > 0) {
                        lastPhone = now;
                        json.put(EucWorldApi.WKI.Phone.Battery_Level, batt);
                        sendMessage(this, Commons.PATH_PHONE, json.toString());
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendAlarm() {
        if (wearNodeId != null) {
            JSONObject json = new JSONObject();
            try {
                json.put("s1", WheelData.getInstance().isSpeedAlarm1Active());
                json.put("s2", WheelData.getInstance().isSpeedAlarm2Active());
                json.put("s3", WheelData.getInstance().isSpeedAlarm3Active());
                json.put("ocp", WheelData.getInstance().isPeakCurrentAlarmActive());
                json.put("ocs", WheelData.getInstance().isSustainedCurrentAlarmActive());
                json.put("ot", WheelData.getInstance().isTemperatureAlarmActive());
                json.put("ov", WheelData.getInstance().isVoltageAlarmActive());

                String data = json.toString();
                sendMessage(this, Commons.PATH_ALARM, data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendSync(Context context) {
        if (wearNodeId != null) {
            JSONObject json = new JSONObject();
            try {
                json.put("signedIn", !LivemapService.getApiKey().isEmpty());
                json.put("euc", WheelData.getInstance().isConnected());
                json.put("gps", LivemapService.getLivemapGPS());
                json.put("tour", LivemapService.getStatus());
                json.put("useF", SettingsUtil.isUseF(context));
                json.put("useMi", SettingsUtil.isUseMi(context));
                json.put("powerSave", SettingsUtil.isWatchPowerSavingMode(context));
                json.put("maxSpeed", SettingsUtil.getMaxSpeed(context));

                String data = json.toString();
                sendMessage(context, Commons.PATH_SYNC, data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendTapResult(Context context, String msg, int action) {
        if (wearNodeId != null) {
            JSONObject json = new JSONObject();
            try {
                json.put("msg", msg);
                json.put("action", action);

                String data = json.toString();
                sendMessage(context, Commons.PATH_TAPRESULT, data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void jsonPutValue(JSONObject json, Value value) {
        if (json != null && value != null && value.isValid()) {
            String wki = value.getWKI();
            Value.ValueType type = value.getType();
            if (!wki.isEmpty() && type != null) {
                try {
                    switch (type) {
                        case STRING:
                            json.put(wki, value.getAsString());
                            break;
                        case INT:
                            json.put(wki, value.getAsInt());
                            break;
                        case LONG:
                        case DATE:
                            json.put(wki, value.getAsLong());
                            break;
                        case DISTANCE:
                            json.put(wki, value.getAsDecimal(2));
                            break;
                        case FLOAT:
                        case SPEED:
                        case DOUBLE:
                            json.put(wki, value.getAsDecimal(1));
                            break;
                        case TEMPERATURE:
                        case ALTITUDE:
                            json.put(wki, value.getAsDecimal(0));
                            break;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void performAction(int action) {
        Bundle options = new Bundle();
        switch (action) {
            case Commons.Action.HORN:
                int mode = SettingsUtil.getWatchHornMode(this);
                options.putInt("mode", mode);
                if (mode == Commons.Action.HornMode.BLUETOOTH_AUDIO) {
                    if (SettingsUtil.getWatchUseCustomHornSound(this))
                        options.putString("path", SettingsUtil.getWatchCustomHornSoundPath(this));
                }
                sendTapResult(this, getString(R.string.action_1), action);
                break;
            case Commons.Action.LIGHT:
                if (!BluetoothLeService.isDisconnected())
                    sendTapResult(this, getString(R.string.action_2), action);
                break;
            case Commons.Action.REQUEST_VOICE_MESSAGE:
                if (SpeechService.isInstanceCreated())
                    sendTapResult(this, getString(R.string.action_3), action);
                break;
            case Commons.Action.DISMISS_VOICE_MESSAGE:
                if (SpeechService.isInstanceCreated())
                    sendTapResult(this, getString(R.string.action_4), action);
                break;
        }
        Utilities.performAction(action, this, options);
    }

}
