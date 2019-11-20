package net.lastowski.eucworld.utils;

import android.content.Context;
import android.os.Build;

import java.util.UUID;

public class Constants {

    public static final double LIVEMAP_API_VERSION = 2;

    public static final double MIN_RIDING_SPEED_EUC = 0.5d;     // km/h
    public static final double MIN_RIDING_SPEED_GPS = 2.0d;     // km/h
    public static final int LIVEMAP_UPDATE_TIMEOUT = 60;        // Seconds
    public static final int WHEEL_DATA_VALIDITY = 5000;         // Milliseconds
    public static final int GPS_DATA_VALIDITY = 10000;          // Milliseconds
    public static final String APP_NAME = "EUC World";
    public static final String SUPPORT_FOLDER_NAME = "EUC World/Support Files";
    public static final String LOG_FOLDER_NAME = "EUC World/Logs";
    public static final String PICTURE_FOLDER_NAME = "EUC World/Pictures";
    public static final String EMPTY_HTML = "<html><body style=\"background: #000000;\"></body></html>";

    public static final String ACTION_BLUETOOTH_CONNECT = "net.lastowski.eucworld.bluetoothConnect";
    public static final String ACTION_BLUETOOTH_DISCONNECT = "net.lastowski.eucworld.bluetoothDisconnect";
    public static final String ACTION_BLUETOOTH_CONNECTION_STATE = "net.lastowski.eucworld.bluetoothConnectionState";
    public static final String ACTION_WHEEL_DATA_AVAILABLE = "net.lastowski.eucworld.wheelDataAvailable";
	public static final String ACTION_WHEEL_SETTING_CHANGED = "net.lastowski.eucworld.wheelSettingChanged";
    public static final String ACTION_WHEEL_CONNECTED = "net.lastowski.eucworld.wheelConnected";
    public static final String ACTION_WHEEL_CONNECTION_LOST = "net.lastowski.eucworld.wheelConnectionLost";
    public static final String ACTION_WHEEL_DISCONNECTED = "net.lastowski.eucworld.wheelDisconnected";
    public static final String ACTION_REQUEST_KINGSONG_SERIAL_DATA = "net.lastowski.eucworld.requestSerialData";
    public static final String ACTION_REQUEST_KINGSONG_NAME_DATA = "net.lastowski.eucworld.requestNameData";
    public static final String ACTION_REQUEST_HORN = "net.lastowski.eucworld.requestHorn";
    public static final String ACTION_REQUEST_LIGHT_TOGGLE = "net.lastowski.eucworld.requestLightToggle";
    public static final String ACTION_REQUEST_VOICE_REPORT = "net.lastowski.eucworld.requestVoiceReport";
    public static final String ACTION_REQUEST_VOICE_DISMISS = "net.lastowski.eucworld.requestVoiceDismiss";

    public static final String ACTION_PEBBLE_SERVICE_TOGGLED = "net.lastowski.eucworld.pebbleServiceToggled";
    public static final String ACTION_LOGGING_SERVICE_TOGGLED = "net.lastowski.eucworld.loggingServiceToggled";
    public static final String ACTION_SPEECH_SERVICE_TOGGLED = "net.lastowski.eucworld.speechServiceToggled";
    public static final String ACTION_LIVEMAP_SERVICE_TOGGLED = "net.lastowski.eucworld.livemapServiceToggled";
    public static final String ACTION_LIVEMAP_STATUS = "net.lastowski.eucworld.livemapStatus";
    public static final String ACTION_LIVEMAP_PAUSE = "net.lastowski.eucworld.livemapPause";
    public static final String ACTION_LIVEMAP_RESUME = "net.lastowski.eucworld.livemapResume";
    public static final String ACTION_LIVEMAP_LOCATION_UPDATED = "net.lastowski.eucworld.livemapLocationUpdated";
    public static final String ACTION_REQUEST_CONNECTION_TOGGLE = "net.lastowski.eucworld.requestConnectionToggle";
    public static final String ACTION_PREFERENCE_CHANGED = "net.lastowski.eucworld.preferenceChanged";
    public static final String ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED = "net.lastowski.eucworld.pebblePreferenceChanged";
    public static final String ACTION_ALARM_TRIGGERED = "net.lastowski.eucworld.alarmTriggered";
    public static final String ACTION_ALARM_FINISHED = "net.lastowski.eucworld.alarmFinished";
    public static final String ACTION_PEBBLE_APP_READY = "net.lastowski.eucworld.pebbleAppReady";
    public static final String ACTION_PEBBLE_APP_SCREEN = "net.lastowski.eucworld.pebbleAppScreen";
	public static final String ACTION_WHEEL_TYPE_RECOGNIZED = "net.lastowski.eucworld.wheelTypeRecognized";
    public static final String ACTION_SPEECH_SAY = "net.lastowski.eucworld.speechSay";
    public static final String ACTION_SET_TOUR_HEADER_VISIBILITY = "net.lastowski.eucworld.changeTourHeaderVisibility";
    public static final String ACTION_TOUR_RELOAD_WEBVIEW = "net.lastowski.eucworld.tourReloadWebView";
    static final String NOTIFICATION_BUTTON_CONNECTION = "net.lastowski.eucworld.notificationConnectionButton";
    static final String NOTIFICATION_BUTTON_LOGGING = "net.lastowski.eucworld.notificationLoggingButton";
    static final String NOTIFICATION_BUTTON_WATCH = "net.lastowski.eucworld.notificationWatchButton";
    static final String NOTIFICATION_BUTTON_SPEECH = "net.lastowski.eucworld.notificationSpeechButton";
    static final String NOTIFICATION_BUTTON_LIVEMAP = "net.lastowski.eucworld.notificationLivemapButton";

    public static final String KINGSONG_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String KINGSONG_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String KINGSONG_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";

    public static final String GOTWAY_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String GOTWAY_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";

    public static final String INMOTION_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String INMOTION_READ_CHARACTER_UUID = "0000ffe4-0000-1000-8000-00805f9b34fb";
    public static final String INMOTION_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String INMOTION_WRITE_CHARACTER_UUID = "0000ffe9-0000-1000-8000-00805f9b34fb";
    public static final String INMOTION_WRITE_SERVICE_UUID = "0000ffe5-0000-1000-8000-00805f9b34fb";

    public static final String NINEBOT_Z_SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NINEBOT_Z_WRITE_CHARACTER_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NINEBOT_Z_READ_CHARACTER_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String NINEBOT_Z_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String NINEBOT_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String NINEBOT_WRITE_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String NINEBOT_READ_CHARACTER_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String NINEBOT_DESCRIPTER_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static final UUID PEBBLE_APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d");
    public static final int PEBBLE_KEY_READY = 11;
    public static final int PEBBLE_KEY_LAUNCH_APP = 10012;
    public static final int PEBBLE_KEY_PLAY_HORN = 10013;
    public static final int PEBBLE_KEY_DISPLAYED_SCREEN = 10014;
    public static final int PEBBLE_APP_VERSION = 104;

    public static final String INTENT_EXTRA_LAUNCHED_FROM_PEBBLE = "launched_from_pebble";
    public static final String INTENT_EXTRA_PEBBLE_APP_VERSION = "pebble_app_version";
    public static final String INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN = "pebble_displayed_Screen";
    public static final String INTENT_EXTRA_BLE_AUTO_CONNECT = "ble_auto_connect";
    public static final String INTENT_EXTRA_LOGGING_FILE_LOCATION = "logging_file_location";
    public static final String INTENT_EXTRA_IS_RUNNING = "is_running";
    public static final String INTENT_EXTRA_GRAPH_UPDATE_AVILABLE = "graph_update_available";
    public static final String INTENT_EXTRA_CONNECTION_STATE = "connection_state";
    public static final String INTENT_EXTRA_ALARM_TYPE = "alarm_type";
    public static final String INTENT_EXTRA_WHEEL_SETTINGS = "wheel_settings";
	public static final String INTENT_EXTRA_WHEEL_LIGHT = "wheel_light";
	public static final String INTENT_EXTRA_WHEEL_LED = "wheel_led";
	public static final String INTENT_EXTRA_WHEEL_BUTTON = "wheel_button";
	public static final String INTENT_EXTRA_WHEEL_MAX_SPEED= "wheel_max_speed";
	public static final String INTENT_EXTRA_WHEEL_SPEAKER_VOLUME = "wheel_speaker_volume";
	public static final String INTENT_EXTRA_WHEEL_REFRESH = "wheel_refresh";
	public static final String INTENT_EXTRA_WHEEL_PEDALS_ADJUSTMENT = "pedals_adjustment";
	public static final String INTENT_EXTRA_WHEEL_TYPE = "wheel_type";

    public static final String INTENT_EXTRA_SPEECH_TEXT = "speech_text";
    public static final String INTENT_EXTRA_SPEECH_EARCON = "speech_earcon";
    public static final String INTENT_EXTRA_SPEECH_PRIORITY = "speech_now";
    public static final String INTENT_EXTRA_SPEECH_NOW_OR_NEVER = "speech_noqueue";

    public static final String PREFERENCES_FRAGMENT_TAG = "tagPrefs";


    public static final int RESULT_DEVICE_SCAN_REQUEST = 20;
    public static final int RESULT_REQUEST_ENABLE_BT = 30;
    public static final int REQUEST_IMAGE_CAPTURE = 50;
    public static final int REQUEST_FLIC_CUSTOM_HORN = 60;
    public static final int REQUEST_WATCH_CUSTOM_HORN = 70;
    public static final int REQUEST_GAUGE_CUSTOM_HORN = 80;

    public enum WHEEL_TYPE {
        Unknown,
        KINGSONG,
        GOTWAY,
        NINEBOT,
        NINEBOT_Z,
        INMOTION;
    }

    public enum PEBBLE_APP_SCREEN {
        GUI(0),
        DETAILS(1);

        private final int value;

        PEBBLE_APP_SCREEN(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        }

    public enum ALARM_TYPE {
        SPEED_1ST(0),
        SPEED_2ND(1),
        SPEED_3RD(2),
        CURRENT_PEAK(3),
        CURRENT_SUSTAINED(4),
		TEMPERATURE(5),
        VOLTAGE(6);

        private final int value;

        ALARM_TYPE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    public static boolean isEmulatedDevice() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static String getEucWorldUrl() {
        return (android.os.Debug.isDebuggerConnected()) ? "https://dev.euc.world" : "https://euc.world";
    }

    public static String appId(Context context) {
        int id =  context.getResources().getIdentifier("appid", "string", context.getPackageName());
        return (id > 0) ? context.getResources().getString(id) : "";
    }

    public static String flicKey(Context context) {
        int id =  context.getResources().getIdentifier("flickey", "string", context.getPackageName());
        return (id > 0) ? context.getResources().getString(id) : "";
    }

    public static String flicSecret(Context context) {
        int id =  context.getResources().getIdentifier("flicsecret", "string", context.getPackageName());
        return (id > 0) ? context.getResources().getString(id) : "";
    }

}