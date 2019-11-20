package net.lastowski.common;

public class Commons {

    public static final String GOOGLE_MARKET_APP_URI = "market://details?id=net.lastowski.eucworld";

    public static final String PATH_START = "/start";
    public static final String PATH_CLOSE = "/close";
    public static final String PATH_SYNC = "/sync";
    public static final String PATH_ALARM = "/alarm";
    public static final String PATH_DATA = "/data";
    public static final String PATH_PAUSE = "/pause";
    public static final String PATH_RESUME = "/resume";
    public static final String PATH_SINGLETAP = "/singletap";
    public static final String PATH_DOUBLETAP = "/doubletap";
    public static final String PATH_LONGPRESS = "/longpress";
    public static final String PATH_TAPRESULT = "/tapresult";
    public static final String PATH_TOUR_START = "/tour-start";
    public static final String PATH_TOUR_PAUSE = "/tour-pause";
    public static final String PATH_TOUR_RESUME = "/tour-resume";
    public static final String PATH_TOUR_FINISH = "/tour-finish";
    public static final String PATH_PHONE = "/phone";
    public static final String PATH_WATCH = "/watch";

    // Capabilities
    public static final String CAPABILITY_PHONE = "eucworld_phone";
    public static final String CAPABILITY_WATCH = "eucworld_watch";

    public interface Action {
        int NONE = 0;
        int HORN = 1;
        int LIGHT = 2;
        int REQUEST_VOICE_MESSAGE = 3;
        int DISMISS_VOICE_MESSAGE = 4;
        public interface HornMode {
            int NONE = 0;
            int BUILT_IN = 1;
            int BLUETOOTH_AUDIO = 2;
        }
    }

    public interface TourTrackingServiceStatus {
        int DISCONNECTED = 0;
        int CONNECTING = 1;
        int WAITING_FOR_GPS = 2;
        int STARTED = 3;
        int PAUSING = 4;
        int PAUSED = 5;
        int RESUMING = 6;
        int DISCONNECTING = 7;
    }

}
