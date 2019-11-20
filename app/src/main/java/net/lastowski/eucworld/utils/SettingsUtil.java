package net.lastowski.eucworld.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import net.lastowski.eucworld.R;


public class SettingsUtil {

    private static final String key = "Eucworld";

    public static String getLastAddress(Context context) {
        SharedPreferences pref = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        if (pref.contains("last_mac")) {
            return pref.getString("last_mac", "");
        }
        return "";
    }

    public static void setLastAddress(Context context, String address) {
        SharedPreferences.Editor editor = context.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        editor.putString("last_mac", address);
        editor.apply();
    }
	
	public static void setUserDistance(Context context, String id, long distance) {
        SharedPreferences.Editor editor = context.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        editor.putLong("user_distance_"+id, distance);
        editor.apply();
    }
	
	public static long getUserDistance(Context context, String id) {
        SharedPreferences pref = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        if (pref.contains("user_distance_"+id)) {
            return pref.getLong("user_distance_"+id, 0);
        }
        return 0;
    }

    public static boolean isFirstRun(Context context) {
        SharedPreferences pref = context.getSharedPreferences(key, Context.MODE_PRIVATE);

        if (pref.contains("first_run"))
            return false;

        pref.edit().putBoolean("first_run", false).apply();
        return true;
    }

    public static boolean getBoolean(Context context, String preference) {
        return getSharedPreferences(context).getBoolean(preference, false);
    }

    public static boolean isAutoLogEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.auto_log), false);
    }

    public static void setAutoLog(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.auto_log), enabled).apply();
    }

    public static boolean isLogLocationEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.log_location_data), false);
    }

    public static void setLogLocationEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.log_location_data), enabled).apply();
    }

    public static boolean isUseGPSEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.use_gps), false);
    }

    public static boolean isAutoUploadEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.auto_upload), false);
    }

    public static void setAutoUploadEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(context.getString(R.string.auto_upload), enabled).apply();
    }
    
    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isUseMi(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.use_mi), false);
    }

    public static boolean isWatchPowerSavingMode(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.watch_power_saving_mode), false);
    }

    public static boolean isUseF(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.use_f), false);
    }

    public static boolean isUseFt(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.use_ft), false);
    }

    public static int getMaxSpeed(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.max_speed), 30);
    }

    public static int getHornMode(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.horn_mode), "0"));
    }

    //Inmotion Specific, but can be the same for other wheels

    public static boolean hasPasswordForWheel(Context context, String id) {
        return getSharedPreferences(context).contains("wheel_password_"+id);
    }

    public static String getPasswordForWheel(Context context, String id) {
        return getSharedPreferences(context).getString("wheel_password_"+id, "000000");
    }

    public static void setPasswordForWheel(Context context, String id, String password) {
        while (password.length() < 6) {
            password = "0" + password;
        }
        getSharedPreferences(context).edit().putString("wheel_password_"+id, password).apply();
    }

    public static boolean isSpeechEnabledOnStartup(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_enable_on_startup), false);
    }

    public static int getSpeechRate(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.speech_rate), "1"));
    }

    public static int getSpeechPitch(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.speech_pitch), "1"));
    }

    public static int getSpeechMsgInterval(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.speech_msg_interval), "60"));
    }

    public static int getSpeechMsgMode(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.speech_msg_mode), "0"));
    }

    public static boolean getSpeechOnlyInMotion(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_only_when_moving), true);
    }

    public static boolean getSpeechGPSBTStatus(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_gps_bt_status), true);
    }

    public static boolean getSpeechMessagesSpeed(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_speed), true);
    }

    public static boolean getSpeechMessagesAvgSpeed(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_avg_speed), false);
    }

    public static boolean getSpeechMessagesAvgRidingSpeed(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_avg_riding_speed), false);
    }

    public static boolean getSpeechMessagesTopSpeed(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_top_speed), false);
    }

    public static boolean getSpeechMessagesDistance(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_distance), true);
    }

    public static boolean getSpeechMessagesBattery(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_battery), true);
    }

    public static boolean getSpeechMessagesPhoneBattery(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_phone_battery), true);
    }

    public static boolean getSpeechMessagesVoltage(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_voltage), false);
    }

    public static boolean getSpeechMessagesCurrent(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_current), false);
    }

    public static boolean getSpeechMessagesPower(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_power), false);
    }

    public static boolean getSpeechMessagesTemperature(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_temperature), false);
    }

    public static boolean getSpeechMessagesTime(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_time), false);
    }

    public static boolean getSpeechMessagesTimeFromStart(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_time_from_start), true);
    }

    public static boolean getSpeechMessagesTimeInMotion(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_time_in_motion), false);
    }

    public static int getSpeechMessagesBatteryLowLevel(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.speech_messages_battery_low_level), 20);
    }

    public static int getSpeechMessagesPhoneBatteryLowLevel(Context context) {
        return getSharedPreferences(context).getInt(context.getString(R.string.speech_messages_phone_battery_low_level), 20);
    }

    public static boolean getSpeechMessagesWeather(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_messages_weather), false);
    }

    public static String getLivemapApiKey(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.livemap_api_key), "");
    }

    public static void setLivemapApiKey(Context context, String v) {
        getSharedPreferences(context).edit().putString(context.getString(R.string.livemap_api_key), v).apply();
    }

    public static int getLivemapPublish(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.livemap_publish), "0"));
    }

    public static boolean getLivemapStartNewSegment(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.livemap_start_new_segment), false);
    }

    public static boolean getShowWhenLocked(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.show_when_locked), false);
    }

    public static boolean getLivemapAutoStart(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.livemap_auto_start), false);
    }

    public static boolean getLivemapAutoFinish(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.livemap_auto_finish), false);
    }

    public static int getLivemapAutoFinishDelay(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.livemap_auto_finish_delay), "180"));
    }

    public static double getDistCorrectionFactor(Context context) {
        int v = getSharedPreferences(context).getInt(context.getString(R.string.dist_correction), 0);
        return ((double)v + 100.0) / 100.0;
    }

    public static double getSpeedCorrectionFactor(Context context) {
        int v = getSharedPreferences(context).getInt(context.getString(R.string.speed_correction), 0);
        return ((double)v + 100.0) / 100.0;
    }

    public static boolean getSpeechUseA2DPOnly(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.speech_use_a2dp_only), false);
    }

    public static boolean getOptimizedBatteryLevel(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.optimized_battery_level), true);
    }

    public static boolean getDontResetData(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.dont_reset_data), false);
    }

    public static int getSpeechFocus(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.speech_focus), "1"));
    }

    public static int getFlicHornMode(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.flic_horn_mode), "0"));
    }

    public static int getFlicActionSingle(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.flic_action_single), "0"));
    }

    public static int getFlicActionDouble(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.flic_action_double), "0"));
    }

    public static int getFlicActionHold(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.flic_action_hold), "0"));
    }

    public static boolean isWatchEnabledOnStartup(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.watch_enable_on_startup), false);
    }

    public static boolean isEnabledPIP(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.enable_pip), false);
    }

    public static String getLang(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.lang), "default");
    }

    public static void setLang(Context context, String lang) {
        getSharedPreferences(context).edit().putString(context.getString(R.string.lang), lang).apply();
    }

    public static boolean getFlicUseCustomHornSound(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.flic_use_custom_horn_sound), false);
    }

    public static String getFlicCustomHornSoundPath(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.flic_custom_horn_sound_path), "");
    }

    public static String getFlicCustomHornSoundTitle(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.flic_custom_horn_sound_title), "");
    }

    public static void setFlicCustomHornSound(Context context, String path, String title) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.flic_custom_horn_sound_path), path);
        editor.putString(context.getString(R.string.flic_custom_horn_sound_title), title);
        editor.apply();
    }

    public static int getGaugeHornMode(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.gauge_horn_mode), "0"));
    }

    public static boolean getGaugeUseCustomHornSound(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.gauge_use_custom_horn_sound), false);
    }

    public static String getGaugeCustomHornSoundPath(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.gauge_custom_horn_sound_path), "");
    }

    public static String getGaugeCustomHornSoundTitle(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.gauge_custom_horn_sound_title), "");
    }

    public static void setGaugeCustomHornSound(Context context, String path, String title) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.gauge_custom_horn_sound_path), path);
        editor.putString(context.getString(R.string.gauge_custom_horn_sound_title), title);
        editor.apply();
    }

    public static int getGaugeActionSingle(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.gauge_action_single), "0"));
    }

    public static int getGaugeActionDouble(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.gauge_action_double), "0"));
    }

    public static int getGaugeActionHold(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.gauge_action_hold), "0"));
    }

    public static int getWatchHornMode(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.watch_horn_mode), "0"));
    }

    public static boolean getWatchUseCustomHornSound(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.watch_use_custom_horn_sound), false);
    }

    public static String getWatchCustomHornSoundPath(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.watch_custom_horn_sound_path), "");
    }

    public static String getWatchCustomHornSoundTitle(Context context) {
        return getSharedPreferences(context).getString(context.getString(R.string.watch_custom_horn_sound_title), "");
    }

    public static void setWatchCustomHornSound(Context context, String path, String title) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.watch_custom_horn_sound_path), path);
        editor.putString(context.getString(R.string.watch_custom_horn_sound_title), title);
        editor.apply();
    }

    public static int getWatchActionSingle(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.watch_action_single), "0"));
    }

    public static int getWatchActionDouble(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.watch_action_double), "0"));
    }

    public static int getWatchActionHold(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.watch_action_hold), "0"));
    }

    public static int getWatchActionButton(Context context) {
        return Integer.parseInt(getSharedPreferences(context).getString(context.getString(R.string.watch_action_button), "0"));
    }

    public static boolean getVoltageDependentAlarms(Context context) {
        return getSharedPreferences(context).getBoolean(context.getString(R.string.voltage_dependent_alarms), false);
    }

}
