package net.lastowski.eucworld;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import android.widget.Toast;

import net.lastowski.common.Commons;
import net.lastowski.eucworld.utils.Constants;
import net.lastowski.eucworld.utils.HttpClient;
import net.lastowski.eucworld.utils.NotificationUtil;
import net.lastowski.eucworld.utils.PermissionsUtil;
import net.lastowski.eucworld.utils.SettingsUtil;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

public class LivemapService extends Service {

    private static LivemapService instance = null;
    private static int status = Commons.TourTrackingServiceStatus.DISCONNECTED;
    private static String url = "";
    private static String apiKey = "";
    private static String userName = "";
    private static boolean autoStarted = false;
    private static int autoStartedPublish = 1;
    private static int livemapError = -1;
    private static boolean livemapGPS = false;
    private static double currentDistance;
    private static double currentSpeed;
    private static double topSpeed;
    private static int ridingTime;
    private static int rideTime;
    private static double speedAvg;
    private static long speedAvgCount;
    private static double speedAvgRiding;
    private static long speedAvgRidingCount;

    private Timer timer;
    private String updateDateTime = "";
    private String tourKey;
    private long lastUpdated;
    private long lastGPS;
    private Location lastLocation;
    private double lastLatitude;
    private double lastLongitude;
    private long lastLocationTime;
    private Location currentLocation;
    private long tourStartInitiated = 0;
    private LocationManager locationManager;
    private BatteryManager batteryManager;
    private SimpleDateFormat df;
    private int updateTimer = 0;

    private long weatherTimestamp = 0;
    private double weatherTemperature;
    private double weatherTemperatureFeels;
    private double weatherWindSpeed;
    private double weatherWindDir;
    private double weatherHumidity;
    private double weatherPressure;
    private double weatherPrecipitation;
    private double weatherVisibility;
    private double weatherCloudCoverage;
    private int weatherConditionCode;

    public static boolean isInstanceCreated() {
        return instance != null;
    }
    public static LivemapService getInstance() { return instance; }
    public static double getDistance() { return currentDistance / 1000; }
    public static double getAverageSpeed() { return speedAvg; }
    public static double getAverageRidingSpeed() { return speedAvgRiding; }
    public static double getSpeed() { return (livemapGPS) ? currentSpeed : 0; }
    public static double getTopSpeed() { return topSpeed; }
    public static int getRideTime() { return rideTime; }
    public static int getRidingTime() { return ridingTime; }
    public static int getStatus() { return status; }
    public static String getUrl() { return url; }
    public static boolean getAutoStarted() { return autoStarted; }
    public static void setAutoStarted(boolean b) { autoStarted = b; }
    public static int getLivemapError() { return livemapError; }
    public static boolean getLivemapGPS() { return livemapGPS; }
    public static boolean isConnected() { return status >= Commons.TourTrackingServiceStatus.CONNECTING && status < Commons.TourTrackingServiceStatus.DISCONNECTING; }
    public static String getApiKey() { return apiKey; }
    public static void setApiKey(String k) { apiKey = k; }
    public static String getUserName() { return userName; }
    public static void setUserName(String k) { userName = k; }

    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (!livemapGPS) {
                livemapGPS = true;
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                Timber.d("GPS signal regained");
                if (lastLocation != null && status != Commons.TourTrackingServiceStatus.PAUSED && SettingsUtil.getSpeechGPSBTStatus(getApplicationContext()))
                    say(getString(R.string.livemap_speech_gps_signal_regained), "info", 1);
            }
            lastGPS = SystemClock.elapsedRealtime();
            currentLocation = location;
            currentSpeed = location.getSpeed() * 3.6f;
            lastLocationTime = location.getTime();
            lastLatitude = location.getLatitude();
            lastLongitude = location.getLongitude();

            if (status != Commons.TourTrackingServiceStatus.PAUSED) {
                if (topSpeed < currentSpeed) topSpeed = currentSpeed;
                speedAvg = ((speedAvg * speedAvgCount) + currentSpeed) / ++speedAvgCount;
                if (currentSpeed >= Constants.MIN_RIDING_SPEED_GPS)
                    speedAvgRiding = ((speedAvgRiding * speedAvgRidingCount) + currentSpeed) / ++speedAvgRidingCount;

                if (lastLocation != null) {
                    if (currentLocation.getSpeed() * 3.6f >= Constants.MIN_RIDING_SPEED_GPS || lastLocation.distanceTo(currentLocation) >= location.getAccuracy()) {
                        currentDistance += lastLocation.distanceTo(currentLocation);
                        lastLocation = currentLocation;
                    }
                }
                else
                    lastLocation = currentLocation;
            }

            if (location.hasAltitude()) Data.tourAltitude.setAsAltitude(location.getAltitude());
            if (location.hasBearing()) Data.tourBearing.setAsFloat(location.getBearing());
            Data.tourDistance.setAsDistance(currentDistance / 1000);
            Data.tourSpeed.setAsSpeed(currentSpeed);
            Data.tourSpeedAvg.setAsSpeed(speedAvg);
            Data.tourSpeedAvgRiding.setAsSpeed(speedAvgRiding);
            Data.tourSpeedMax.setAsSpeed(topSpeed);

            sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_LOCATION_UPDATED));
            updateLivemap();
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            switch (action) {
                case Constants.ACTION_LIVEMAP_PAUSE:
                    pauseLivemap();
                    break;
                case Constants.ACTION_LIVEMAP_RESUME:
                    resumeLivemap();
                    break;
            }
        }
    };

    private void startTimer() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                long now = SystemClock.elapsedRealtime();
                // Decrement API update timer
                if (updateTimer > 0) --updateTimer;
                // Toggle GPS signal lost
                if (livemapGPS && now - lastGPS > Constants.GPS_DATA_VALIDITY) {
                    livemapGPS = false;
                    sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                    Timber.d("GPS signal lost");
                    if (status != Commons.TourTrackingServiceStatus.PAUSED && SettingsUtil.getSpeechGPSBTStatus(getApplicationContext()))
                        say(getString(R.string.livemap_speech_gps_signal_lost), "warning1", 1);
                }
                // If enabled in settings, finish unpaused tour after wheel connection is lost
                if (status != Commons.TourTrackingServiceStatus.DISCONNECTED &&
                        status != Commons.TourTrackingServiceStatus.PAUSED &&
                        SettingsUtil.getLivemapAutoFinish(getApplicationContext()) &&
                        WheelData.getInstance().getDataAge() > SettingsUtil.getLivemapAutoFinishDelay(getApplicationContext()) * 1000 &&
                        tourStartInitiated + SettingsUtil.getLivemapAutoFinishDelay(getApplicationContext()) * 1000 < now)
                    stopSelf();
                // Ride & riding time
                if (status != Commons.TourTrackingServiceStatus.WAITING_FOR_GPS) {
                    Data.tourDuration.setAsDate(++rideTime);
                    if (livemapGPS & currentSpeed >= Constants.MIN_RIDING_SPEED_GPS)
                        Data.tourDurationRiding.setAsDate(++ridingTime);
                }
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 1000, 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_LIVEMAP_PAUSE);
        intentFilter.addAction(Constants.ACTION_LIVEMAP_RESUME);
        registerReceiver(receiver, intentFilter);
        if (!PermissionsUtil.checkLocationPermission(this)) {
            showToast(R.string.livemap_error_no_location_permission, Toast.LENGTH_LONG);
            stopSelf();
            return START_STICKY;
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if ((locationManager == null) || (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
            locationManager = null;
            showToast(R.string.livemap_error_no_gps_provider, Toast.LENGTH_LONG);
            stopSelf();
            return START_STICKY;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_SERVICE_TOGGLED).putExtra(Constants.INTENT_EXTRA_IS_RUNNING, true));
        startLivemap();
        startTimer();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        instance = this;
        livemapGPS = false;
        livemapError = 0;
        currentDistance = 0;
        currentSpeed = 0;
        speedAvg = 0;
        speedAvgCount = 0;
        speedAvgRiding = 0;
        speedAvgRidingCount = 0;
        topSpeed = 0;
        ridingTime = 0;
        rideTime = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) batteryManager = (BatteryManager)getSystemService(BATTERY_SERVICE);
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        startForeground(NotificationUtil.getNotificationId(), NotificationUtil.getNotification(this));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        if (timer != null)
            timer.cancel();
        stopLivemap();
        autoStarted = false;
        if (locationManager != null)
            locationManager.removeUpdates(locationListener);
        livemapGPS = false;
        Intent serviceStartedIntent = new Intent(Constants.ACTION_LIVEMAP_SERVICE_TOGGLED).putExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
        sendBroadcast(serviceStartedIntent);
        super.onDestroy();
        currentSpeed = 0;
        instance = null;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void updateLivemap() {
        long now = SystemClock.elapsedRealtime();
        if (isConnected() && updateTimer == 0 && tourKey != null && now - lastUpdated >= 1000) {
            Timber.d("Update tour; sending request to server...");
            updateTimer = Constants.LIVEMAP_UPDATE_TIMEOUT;
            lastUpdated = now;

            final RequestParams requestParams = new RequestParams();
            requestParams.put("a", apiKey);
            requestParams.put("k", tourKey);
            requestParams.put("p", (autoStarted) ? autoStartedPublish : SettingsUtil.getLivemapPublish(this));
            requestParams.put("i", 2);
            requestParams.put("dt", df.format(new Date()));
            // Weather timestamp
            requestParams.put("xts", weatherTimestamp);
            // Location data
            requestParams.put("ldt", df.format(new Date(currentLocation.getTime())));
            requestParams.put("llt", String.format(Locale.US, "%.7f", currentLocation.getLatitude()));
            requestParams.put("lln", String.format(Locale.US, "%.7f", currentLocation.getLongitude()));
            requestParams.put("lds", String.format(Locale.US, "%.3f", currentDistance / 1000.0));
            requestParams.put("lsa", String.format(Locale.US, "%.1f", getAverageSpeed()));
            requestParams.put("lsr", String.format(Locale.US, "%.1f", getAverageRidingSpeed()));
            requestParams.put("lst", String.format(Locale.US, "%.1f", getTopSpeed()));
            requestParams.put("lrt", String.format(Locale.US, "%d", getRideTime()));
            requestParams.put("lrr", String.format(Locale.US, "%d", getRidingTime()));
            requestParams.put("lsp", String.format(Locale.US, "%.1f", currentLocation.getSpeed() * 3.6f));
            requestParams.put("lac", String.format(Locale.US, "%.1f", currentLocation.getAccuracy()));
            requestParams.put("lat", String.format(Locale.US, "%.1f", currentLocation.getAltitude()));
            requestParams.put("lbg", String.format(Locale.US, "%.1f", currentLocation.getBearing()));
            // Device battery
            int deviceBattery = getDeviceBattery();
            if (deviceBattery > -1) requestParams.put("dbl", String.format(Locale.US, "%d", deviceBattery));
            // Wheel data
            if (WheelData.getInstance().isConnected()) {
                requestParams.put("was", String.format(Locale.US, "%.1f", WheelData.getInstance().getAverageSpeedDouble()));
                requestParams.put("wbl", String.format(Locale.US, "%.1f", WheelData.getInstance().getAverageBatteryLevelDouble()));
                requestParams.put("wcu", String.format(Locale.US, "%.1f", WheelData.getInstance().getAverageCurrentDouble()));
                requestParams.put("wds", String.format(Locale.US, "%.3f", WheelData.getInstance().getDistanceDouble()));
                requestParams.put("wpw", String.format(Locale.US, "%.1f", WheelData.getInstance().getAveragePowerDouble()));
                requestParams.put("wsp", String.format(Locale.US, "%.1f", WheelData.getInstance().getSpeedDouble()));
                requestParams.put("wsa", String.format(Locale.US, "%.1f", WheelData.getInstance().getAverageSpeedDouble()));
                requestParams.put("wsr", String.format(Locale.US, "%.1f", WheelData.getInstance().getAverageRidingSpeedDouble()));
                requestParams.put("wst", String.format(Locale.US, "%.1f", WheelData.getInstance().getTopSpeedDouble()));
                requestParams.put("wtm", String.format(Locale.US, "%.1f", WheelData.getInstance().getTemperatureDouble()));
                requestParams.put("wvt", String.format(Locale.US, "%.1f", WheelData.getInstance().getAverageVoltageDouble()));
                requestParams.put("wrt", String.format(Locale.US, "%d", WheelData.getInstance().getRideTime()));
                requestParams.put("wrr", String.format(Locale.US, "%d", WheelData.getInstance().getRidingTime()));
            }
            final long started = SystemClock.elapsedRealtime();
            HttpClient.post(Constants.getEucWorldUrl() + "/api/tour/update", requestParams, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                    livemapError = 2;
                    if (status == Commons.TourTrackingServiceStatus.WAITING_FOR_GPS) {
                        say(getString(R.string.livemap_speech_tour_started), "info", 1);
                        status = Commons.TourTrackingServiceStatus.STARTED;
                        sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                        Timber.d("Tour started");
                    }
                    try {
                        int error = response.getInt("error");
                        if (error == 0) {
                            livemapError = 0;
                            updateDateTime = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                            if (response.has("data") && response.getJSONObject("data").has("xtm")) {
                                weatherTemperature = response.getJSONObject("data").getDouble("xtm");
                                weatherTemperatureFeels = response.getJSONObject("data").getDouble("xtf");
                                weatherWindSpeed = response.getJSONObject("data").getDouble("xws");
                                weatherWindDir = response.getJSONObject("data").getDouble("xwd");
                                weatherHumidity = response.getJSONObject("data").getDouble("xhu");
                                weatherPressure = response.getJSONObject("data").getDouble("xpr");
                                weatherPrecipitation = response.getJSONObject("data").getDouble("xpc");
                                weatherVisibility = response.getJSONObject("data").getDouble("xvi");
                                weatherCloudCoverage = response.getJSONObject("data").getDouble("xcl");
                                weatherConditionCode = response.getJSONObject("data").getInt("xco");
                                weatherTimestamp = response.getJSONObject("data").getInt("xts");
                            }
                        }
                        Timber.d("Update tour; done (%d) in %d ms", error, SystemClock.elapsedRealtime() - started);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    updateTimer = 0;
                }
                @Override
                public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    Timber.e("Update tour; request failed (%d) in %d ms with error response: %s", statusCode, SystemClock.elapsedRealtime() - started, errorResponse != null ? errorResponse.toString() : "(null)");
                    livemapError = 1;
                    updateTimer = 0;
                }
                @Override
                public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                    Timber.e("Update tour; request failed (%d) in %d ms", statusCode, SystemClock.elapsedRealtime() - started);
                    livemapError = 2;
                    updateTimer = 0;
                }
            });
        }
    }

    private void startLivemap() {
        Timber.d("Start tour; sending request to server...");
        tourStartInitiated = SystemClock.elapsedRealtime();
        status = Commons.TourTrackingServiceStatus.CONNECTING;
        sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
        JSONObject info = new JSONObject();
        try {
            /*

                Basic device, EUC & app data collected
                for diagnostic, support and further development

             */
            // Device data
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("brand", Build.BRAND);
            info.put("device", Build.DEVICE);
            info.put("display", Build.DISPLAY);
            info.put("id", Build.ID);
            info.put("model", Build.MODEL);
            info.put("product", Build.PRODUCT);
            info.put("sdk", Build.VERSION.SDK_INT);
            // Application data
            info.put("appVersionName", BuildConfig.VERSION_NAME);
            info.put("appVersionCode", BuildConfig.VERSION_CODE);
            // EUC data
            info.put("eucDistance", WheelData.getInstance().getWheelDistanceDouble());
            info.put("eucUserDistance", WheelData.getInstance().getUserDistanceDouble());
            info.put("eucSerial", WheelData.getInstance().getSerial());
            info.put("eucModel", WheelData.getInstance().getModel());
            info.put("eucType", WheelData.getInstance().getWheelType().toString());
            if (BluetoothLeService.isInstanceCreated()) {
                // EUC Bluetooth data
                info.put("eucBluetoothAddress", BluetoothLeService.getInstance().getBluetoothDeviceAddress());
                info.put("eucBluetoothName", BluetoothLeService.getInstance().getBluetoothDeviceName());
                if (BluetoothLeService.getInstance().getSupportedGattServices() != null) {
                    JSONArray array = new JSONArray();
                    for (BluetoothGattService service: BluetoothLeService.getInstance().getSupportedGattServices())
                        array.put(service.getUuid().toString());
                    info.put("eucBluetoothServices", array);
                }
            }
        }
        catch (Exception ignored) {}
        String i = info.toString();

        final RequestParams requestParams = new RequestParams();
        requestParams.put("api", Constants.LIVEMAP_API_VERSION);
        requestParams.put("a", apiKey);
        requestParams.put("p", (autoStarted) ? autoStartedPublish : SettingsUtil.getLivemapPublish(this));
        requestParams.put("i", 2);
        requestParams.put("m", SettingsUtil.getLivemapStartNewSegment(this));
        requestParams.put("t", TimeZone.getDefault().getID());
        requestParams.put("l", String.valueOf(Locale.getDefault()));
        requestParams.put("ci", i);
        final long started = SystemClock.elapsedRealtime();
        HttpClient.post(Constants.getEucWorldUrl() + "/api/tour/start", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                int error = -1;
                try {
                    error = response.getInt("error");
                    switch (error) {
                        case 0:
                            livemapError = 0;
                            status = Commons.TourTrackingServiceStatus.WAITING_FOR_GPS;
                            //notificationManager.notify(NOTIFY_ID, getNotification(getString(R.string.notification_livemap_title), getString(R.string.livemap_gps_wait)));
                            //showToast(R.string.livemap_api_connected, Toast.LENGTH_LONG);
                            tourKey = response.getJSONObject("data").getString("k");
                            url = Constants.getEucWorldUrl() + "/tour/" + tourKey;
                            Timber.d("Start tour; done (%d) in %d ms", error, SystemClock.elapsedRealtime() - started);
                            break;
                        case 1:
                            Timber.e("Start tour; general server error (1) in %d ms", SystemClock.elapsedRealtime() - started);
                            showToast(R.string.livemap_api_error_general, Toast.LENGTH_LONG);
                            break;
                        case 8:
                            Timber.e("Start tour; wrong credentials (8) in %d ms", SystemClock.elapsedRealtime() - started);
                            showToast(R.string.livemap_api_error_invalid_api_key, Toast.LENGTH_LONG);
                            break;
                        case 9:
                            Timber.e("Start tour; account is inactive (9) in %d ms", SystemClock.elapsedRealtime() - started);
                            showToast(R.string.livemap_api_error_accound_needs_activation, Toast.LENGTH_LONG);
                            break;
                        case 403:
                            Timber.e("Start tour; server access forbidden (403) in %d ms", SystemClock.elapsedRealtime() - started);
                            showToast(R.string.livemap_api_error_forbidden, Toast.LENGTH_LONG);
                            break;
                        default:
                            showToast(R.string.livemap_api_error_unknown, Toast.LENGTH_LONG);
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    stopSelf();
                }
                if (error != 0) {
                    livemapError = 2;
                    status = Commons.TourTrackingServiceStatus.DISCONNECTED;
                    sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                    stopSelf();
                }
                else
                    sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
            }
            @Override
            public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Timber.e("Start tour; request failed (%d) in %d ms with error response: %s", statusCode, SystemClock.elapsedRealtime() - started, errorResponse != null ? errorResponse.toString() : "(null)");
                livemapError = 2;
                status = Commons.TourTrackingServiceStatus.DISCONNECTED;
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                showToast(R.string.livemap_api_error_no_connection, Toast.LENGTH_LONG);
                stopSelf();
            }
            @Override
            public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                Timber.e("Start tour; request failed (%d) in %d ms", statusCode, SystemClock.elapsedRealtime() - started);
                livemapError = 2;
                status = Commons.TourTrackingServiceStatus.DISCONNECTED;
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                showToast(R.string.livemap_api_error_server, Toast.LENGTH_LONG);
                stopSelf();
            }
        });
    }

    private void stopLivemap() {
        if (status != Commons.TourTrackingServiceStatus.DISCONNECTED) {
            status = Commons.TourTrackingServiceStatus.DISCONNECTING;
            sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
            Timber.d("Finish tour; sending request to server...");
            final RequestParams requestParams = new RequestParams();
            requestParams.put("a", apiKey);
            requestParams.put("k", tourKey);
            requestParams.put("p", (autoStarted) ? autoStartedPublish : SettingsUtil.getLivemapPublish(this));
            requestParams.put("i", 2);
            final long started = SystemClock.elapsedRealtime();
            HttpClient.post(Constants.getEucWorldUrl() + "/api/tour/finish", requestParams, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                    Timber.i("Finish tour; done");
                    if (status > Commons.TourTrackingServiceStatus.WAITING_FOR_GPS)
                        say(getString(R.string.livemap_speech_tour_finished), "info", 1);
                    livemapError = -1;
                    status = Commons.TourTrackingServiceStatus.DISCONNECTED;
                    sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                }
                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    //livemapError = 1;
                    Timber.e("Finish tour; request failed (%d) in %d ms with error response: %s", statusCode, SystemClock.elapsedRealtime() - started, errorResponse != null ? errorResponse.toString() : "(null)");
                    livemapError = -1;
                    status = Commons.TourTrackingServiceStatus.DISCONNECTED;
                    sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                }
                @Override
                public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                    //livemapError = 1;
                    Timber.e("Finish tour; request failed (%d) in %d ms", statusCode, SystemClock.elapsedRealtime() - started);
                    livemapError = -1;
                    status = Commons.TourTrackingServiceStatus.DISCONNECTED;
                    sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
                }
            });
        }
    }

    private void pauseLivemap() {
        Timber.d("Pause tour; sending request to server...");
        status = Commons.TourTrackingServiceStatus.PAUSING;
        final RequestParams requestParams = new RequestParams();
        requestParams.put("a", apiKey);
        requestParams.put("k", tourKey);
        requestParams.put("p", (autoStarted) ? autoStartedPublish : SettingsUtil.getLivemapPublish(this));
        requestParams.put("i", 2);
        final long started = SystemClock.elapsedRealtime();
        HttpClient.post(Constants.getEucWorldUrl() + "/api/tour/pause", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                livemapError = 1;
                try {
                    int error = response.getInt("error");
                    if (error == 0) {
                        livemapError = 0;
                        status = Commons.TourTrackingServiceStatus.PAUSED;
                    }
                    else
                        status = Commons.TourTrackingServiceStatus.STARTED;
                    Timber.d("Pause tour; done (%d) in %d ms", error, SystemClock.elapsedRealtime() - started);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
            }
            @Override
            public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Timber.e("Pause tour; request failed (%d) in %d ms with error response: %s", statusCode, SystemClock.elapsedRealtime() - started, errorResponse != null ? errorResponse.toString() : "(null)");
                livemapError = 1;
                status = Commons.TourTrackingServiceStatus.STARTED;
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
            }
            @Override
            public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                Timber.e("Pause tour; request failed (%d) in %d ms", statusCode, SystemClock.elapsedRealtime() - started);
                livemapError = 1;
                status = Commons.TourTrackingServiceStatus.STARTED;
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
            }
        });
    }

    private void resumeLivemap() {
        Timber.d("Resume tour; sending request to server...");
        status = Commons.TourTrackingServiceStatus.RESUMING;
        final RequestParams requestParams = new RequestParams();
        requestParams.put("a", apiKey);
        requestParams.put("k", tourKey);
        requestParams.put("p", (autoStarted) ? autoStartedPublish : SettingsUtil.getLivemapPublish(this));
        requestParams.put("i", 1 /*SettingsUtil.getLivemapUpdateInterval(this)*/);
        final long started = SystemClock.elapsedRealtime();
        HttpClient.post(Constants.getEucWorldUrl() + "/api/tour/resume", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                livemapError = 1;
                try {
                    int error = response.getInt("error");
                    if (error == 0) {
                        livemapError = 0;
                        status = Commons.TourTrackingServiceStatus.STARTED;
                    }
                    else
                        status = Commons.TourTrackingServiceStatus.PAUSED;
                    Timber.d("Resume tour; done (%d) in %d ms", error, SystemClock.elapsedRealtime() - started);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Timber.e("Resume tour; request failed (%d) in %d ms with error response: %s", statusCode, SystemClock.elapsedRealtime() - started, errorResponse != null ? errorResponse.toString() : "(null)");
                livemapError = 1;
                status = Commons.TourTrackingServiceStatus.PAUSED;
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
            }
            @Override
            public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                Timber.e("Resume tour; request failed (%d) in %d ms", statusCode, SystemClock.elapsedRealtime() - started);
                livemapError = 1;
                status = Commons.TourTrackingServiceStatus.PAUSED;
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_STATUS));
            }
        });
    }

    @TargetApi(21)
    private int getDeviceBattery() {
        if (batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        else
            return -1;
    }

    private void showToast(int message_id, int duration) {
        Toast.makeText(this, message_id, duration).show();
    }

    public long getWeatherTimestamp() { return weatherTimestamp; }
    public double getWeatherTemperature() { return weatherTemperature; }
    public double getWeatherTemperatureFeels() { return weatherTemperatureFeels; }
    public int getWeatherConditionCode() { return weatherConditionCode; }
    public String getUpdateDateTime() { return updateDateTime; }
    public String getTourKey() { return tourKey; }
    public double getLatitude() { return lastLatitude; }
    public double getLongitude() { return lastLongitude; }
    public long getLocationTime() { return lastLocationTime; }

    private void say(String text, String earcon, int priority) {
        sendBroadcast(new Intent(Constants.ACTION_SPEECH_SAY)
                .putExtra(Constants.INTENT_EXTRA_SPEECH_TEXT, text)
                .putExtra(Constants.INTENT_EXTRA_SPEECH_EARCON, earcon)
                .putExtra(Constants.INTENT_EXTRA_SPEECH_PRIORITY, priority));
    }

}
