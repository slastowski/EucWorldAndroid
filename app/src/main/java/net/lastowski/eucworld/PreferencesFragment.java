package net.lastowski.eucworld;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.widget.*;
import android.text.InputType;

import net.lastowski.eucworld.utils.Constants;
import net.lastowski.eucworld.utils.Constants.WHEEL_TYPE;
import net.lastowski.eucworld.utils.HttpClient;
import net.lastowski.eucworld.utils.SettingsUtil;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pavelsikun.seekbarpreference.SeekBarPreference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import io.flic.lib.FlicAppNotInstalledException;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;

import static android.app.Activity.RESULT_OK;
import static net.lastowski.eucworld.utils.Constants.REQUEST_GAUGE_CUSTOM_HORN;
import static net.lastowski.eucworld.utils.Constants.REQUEST_FLIC_CUSTOM_HORN;
import static net.lastowski.eucworld.utils.Constants.REQUEST_WATCH_CUSTOM_HORN;

public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    enum SettingsScreen {
        Main,
        General,
        Gauge,
        Logs,
        Livemap,
        Alarms,
        Speech,
        SpeechMessages,
        Watch,
        Flic,
        Wheel
    }

    WHEEL_TYPE mWheelType = WHEEL_TYPE.Unknown;

    private SettingsScreen currentScreen = SettingsScreen.Main;
    private String lang;
    private boolean ligthEnabled;
    private boolean ledEnabled;
    private boolean handleButtonDisabled;
    private int wheelMaxSpeed;
    private int speakerVolume;
    private int pedalsAdjustment;
    private int pedalsMode;
    private int lightMode;
    private int alarmMode;
    private int strobeMode;
    private int ledMode;

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_WATCH_CUSTOM_HORN:
            case REQUEST_GAUGE_CUSTOM_HORN:
            case REQUEST_FLIC_CUSTOM_HORN:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String path = getAudioPath(getActivity(), uri);
                    String title = getAudioTitle(getActivity(), uri);
                    switch (requestCode) {
                        case REQUEST_WATCH_CUSTOM_HORN:
                            SettingsUtil.setWatchCustomHornSound(getActivity(), path, title);
                            findPreference(getString(R.string.watch_custom_horn_sound)).setSummary(title);
                            break;
                        case REQUEST_GAUGE_CUSTOM_HORN:
                            SettingsUtil.setGaugeCustomHornSound(getActivity(), path, title);
                            findPreference(getString(R.string.gauge_custom_horn_sound)).setSummary(title);
                            break;
                        case REQUEST_FLIC_CUSTOM_HORN:
                            SettingsUtil.setFlicCustomHornSound(getActivity(), path, title);
                            findPreference(getString(R.string.flic_custom_horn_sound)).setSummary(title);
                            break;
                    }
                }
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Context context = preferenceScreen.getContext();
        context.setTheme(R.style.PreferenceScreen);

        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        lang = sharedPreferences.getString(getString(R.string.lang), "default");
        ligthEnabled = sharedPreferences.getBoolean(getString(R.string.light_enabled), false);
        ledEnabled = sharedPreferences.getBoolean(getString(R.string.led_enabled), false);
        handleButtonDisabled = sharedPreferences.getBoolean(getString(R.string.handle_button_disabled), false);
        wheelMaxSpeed = sharedPreferences.getInt(getString(R.string.wheel_max_speed), 0);
        speakerVolume = sharedPreferences.getInt(getString(R.string.speaker_volume), 0);
        pedalsAdjustment = sharedPreferences.getInt(getString(R.string.pedals_adjustment), 0);
        pedalsMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.pedals_mode), "0"));
        lightMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.light_mode), "0"));
        alarmMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.alarm_mode), "0"));
        strobeMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.strobe_mode), "0"));
        ledMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.led_mode), "0"));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        updateSigninSignupButtons();
        setup_screen();
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "lang":
                if (!lang.equals(SettingsUtil.getLang(getActivity()))) {
                    lang = SettingsUtil.getLang(getActivity());
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.lang_restart_needed_title))
                            .setMessage(getString(R.string.lang_restart_needed))
                            .setPositiveButton(android.R.string.ok, null)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                    lang = SettingsUtil.getLang(getActivity());
                }
                break;
            case "alarms_enabled":
                break;
            case "use_mi":
            case "max_speed":
                getActivity().sendBroadcast(new Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED));
                break;
            case "light_enabled":
                if (ligthEnabled != sharedPreferences.getBoolean(getString(R.string.light_enabled), false)) {
                    ligthEnabled = sharedPreferences.getBoolean(getString(R.string.light_enabled), false);
                    WheelData.getInstance().updateLight(ligthEnabled);
                }
                break;
            case "led_enabled":
                if (ledEnabled != sharedPreferences.getBoolean(getString(R.string.led_enabled), false)) {
                    ledEnabled = sharedPreferences.getBoolean(getString(R.string.led_enabled), false);
                    WheelData.getInstance().updateLed(ledEnabled);
                }
                break;
            case "handle_button_disabled":
                if (handleButtonDisabled != sharedPreferences.getBoolean(getString(R.string.handle_button_disabled), false)) {
                    handleButtonDisabled = sharedPreferences.getBoolean(getString(R.string.handle_button_disabled), false);
                    WheelData.getInstance().updateHandleButton(handleButtonDisabled);
                }
                break;
            case "wheel_max_speed":
                if (wheelMaxSpeed != sharedPreferences.getInt(getString(R.string.wheel_max_speed), 0)) {
                    wheelMaxSpeed = sharedPreferences.getInt(getString(R.string.wheel_max_speed), 0);
                    WheelData.getInstance().updateMaxSpeed(wheelMaxSpeed);
                }
                break;
            case "speaker_volume":
                if (speakerVolume != sharedPreferences.getInt(getString(R.string.speaker_volume), 0)) {
                    speakerVolume = sharedPreferences.getInt(getString(R.string.speaker_volume), 0);
                    WheelData.getInstance().updateSpeakerVolume(speakerVolume);
                }
                break;
            case "pedals_adjustment":
                if (pedalsAdjustment != sharedPreferences.getInt(getString(R.string.pedals_adjustment), 0)) {
                    pedalsAdjustment = sharedPreferences.getInt(getString(R.string.pedals_adjustment), 0);
                    WheelData.getInstance().updatePedals(pedalsAdjustment);
                }
                break;
            case "pedals_mode":
                if (pedalsMode != Integer.parseInt(sharedPreferences.getString(getString(R.string.pedals_mode), "0"))) {
                    pedalsMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.pedals_mode), "0"));
                    WheelData.getInstance().updatePedalsMode(pedalsMode);
                }
                break;
            case "light_mode":
                if (lightMode != Integer.parseInt(sharedPreferences.getString(getString(R.string.light_mode), "0"))) {
                    lightMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.light_mode), "0"));
                    WheelData.getInstance().updateLightMode(lightMode);
                }
                break;
            case "alarm_mode":
                if (alarmMode != Integer.parseInt(sharedPreferences.getString(getString(R.string.alarm_mode), "0"))) {
                    alarmMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.alarm_mode), "0"));
                    WheelData.getInstance().updateAlarmMode(alarmMode);
                }
                break;
            case "strobe_mode":
                if (strobeMode != Integer.parseInt(sharedPreferences.getString(getString(R.string.strobe_mode), "0"))) {
                    strobeMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.strobe_mode), "0"));
                    WheelData.getInstance().updateStrobe(strobeMode);
                }
                break;
            case "led_mode":
                if (ledMode != Integer.parseInt(sharedPreferences.getString(getString(R.string.led_mode), "0"))) {
                    ledMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.led_mode), "0"));
                    WheelData.getInstance().updateLedMode(ledMode);
                }
                break;
        }
        getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
    }

    private void setup_screen() {
        boolean readExternalStorage = (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        Toolbar tb = getActivity().findViewById(R.id.preference_toolbar);
        if (currentScreen == SettingsScreen.Main) {
            tb.setNavigationIcon(null);
        }
        else if (currentScreen == SettingsScreen.SpeechMessages) {
            tb.setNavigationIcon(R.drawable.ic_arrow_back_24px);
            tb.setNavigationOnClickListener(view -> show_speech_menu());
        } else {
            tb.setNavigationIcon(R.drawable.ic_arrow_back_24px);
            tb.setNavigationOnClickListener(view -> show_main_menu());
        }
        switch (currentScreen) {
            case Main:
                tb.setTitle(R.string.settings_title);
                Preference speed_button = findPreference(getString(R.string.general_preferences));
                Preference gauge_button = findPreference(getString(R.string.gauge_preferences));
                Preference logs_button = findPreference(getString(R.string.log_preferences));
                Preference livemap_button = findPreference(getString(R.string.livemap_preferences));
                Preference alarm_button = findPreference(getString(R.string.alarm_preferences));
                Preference speech_button = findPreference(getString(R.string.speech_preferences));
                Preference watch_button = findPreference(getString(R.string.watch_preferences));
                Preference flic_button = findPreference(getString(R.string.flic_preferences));
                Preference wheel_button = findPreference(getString(R.string.wheel_settings));
                Preference about_button = findPreference(getString(R.string.about));
                Preference signinout_button = findPreference(getString(R.string.sign_in_out));
                Preference signup_button = findPreference(getString(R.string.sign_up));

                if (speed_button != null) {
                    speed_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.General;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_general);
                        setup_screen();
                        return true;
                    });
                }
                if (gauge_button != null) {
                    gauge_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Gauge;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_gauge);

                        SeekBarPreference max_speed = (SeekBarPreference) findPreference(getString(R.string.max_speed));
                        if (max_speed != null)
                            max_speed.setImperial(SettingsUtil.isUseMi(getActivity()));

                        setup_screen();
                        return true;
                    });
                }
                if (logs_button != null) {
                    logs_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Logs;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_logs);
                        setup_screen();
                        return true;
                    });
                }
                if (livemap_button != null) {
                    livemap_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Livemap;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_livemap);
                        setup_screen();
                        return true;
                    });
                }
                if (alarm_button != null) {
                    alarm_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Alarms;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_alarms);

                        SeekBarPreference alarm_1_speed = (SeekBarPreference) findPreference(getString(R.string.alarm_1_speed));
                        if (alarm_1_speed != null)
                            alarm_1_speed.setImperial(SettingsUtil.isUseMi(getActivity()));

                        SeekBarPreference alarm_2_speed = (SeekBarPreference) findPreference(getString(R.string.alarm_2_speed));
                        if (alarm_2_speed != null)
                            alarm_2_speed.setImperial(SettingsUtil.isUseMi(getActivity()));

                        SeekBarPreference alarm_3_speed = (SeekBarPreference) findPreference(getString(R.string.alarm_3_speed));
                        if (alarm_3_speed != null)
                            alarm_3_speed.setImperial(SettingsUtil.isUseMi(getActivity()));

                        SeekBarPreference alarm_temperature = (SeekBarPreference) findPreference(getString(R.string.alarm_temperature));
                        if (alarm_temperature != null)
                            alarm_temperature.setImperial(SettingsUtil.isUseF(getActivity()));

                        setup_screen();
                        return true;
                    });
                }
                if (speech_button != null) {
                    speech_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Speech;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_speech);
                        setup_screen();
                        return true;
                    });
                }
                if (watch_button != null) {
                    watch_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Watch;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_watch);
                        setup_screen();
                        return true;
                    });
                }
                if (flic_button != null) {
                    flic_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Flic;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_flic);
                        setup_screen();
                        return true;
                    });
                }
                if (wheel_button != null) {
                    wheel_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.Wheel;
                        getPreferenceScreen().removeAll();
                        if (mWheelType == WHEEL_TYPE.NINEBOT_Z)
                            addPreferencesFromResource(R.xml.preferences_ninebot_z);
                        if (mWheelType == WHEEL_TYPE.INMOTION) {
                            addPreferencesFromResource(R.xml.preferences_inmotion);

                            SeekBarPreference wheel_max_speed = (SeekBarPreference) findPreference(getString(R.string.wheel_max_speed));
                            if (wheel_max_speed != null)
                                wheel_max_speed.setImperial(SettingsUtil.isUseMi(getActivity()));
                        }
                        if (mWheelType == WHEEL_TYPE.KINGSONG) {
                            addPreferencesFromResource(R.xml.preferences_kingsong);

                            SeekBarPreference wheel_max_speed = (SeekBarPreference) findPreference(getString(R.string.wheel_max_speed));
                            if (wheel_max_speed != null)
                                wheel_max_speed.setImperial(SettingsUtil.isUseMi(getActivity()));
                        }
                        if (mWheelType == WHEEL_TYPE.GOTWAY) {
                            addPreferencesFromResource(R.xml.preferences_gotway);

                            SeekBarPreference wheel_max_speed = (SeekBarPreference) findPreference(getString(R.string.wheel_max_speed));
                            if (wheel_max_speed != null)
                                wheel_max_speed.setImperial(SettingsUtil.isUseMi(getActivity()));

                            Preference start_calibration_button = findPreference(getString(R.string.start_calibration));
                            if (start_calibration_button != null) {
                                start_calibration_button.setOnPreferenceClickListener(preference1 -> {
                                    WheelData.getInstance().updateCalibration();
                                    return true;
                                });
                            }
                        }
                        setup_screen();
                        return true;
                    });
                }
                if (about_button != null) {
                    about_button.setOnPreferenceClickListener(preference -> {
                        showAboutDialog();
                        return true;
                    });
                }

                if (signinout_button != null) {
                    signinout_button.setOnPreferenceClickListener(preference -> {
                        if (LivemapService.getApiKey().isEmpty())
                            showSignInDialog();
                        else {
                            LivemapService.setUserName("");
                            LivemapService.setApiKey("");
                            SettingsUtil.setLivemapApiKey(getActivity(), "");
                            updateSigninSignupButtons();
                            try {
                                ((MainActivity) getActivity()).loadEucWorldApp();
                                ((MainActivity) getActivity()).updateLivemapUI();
                            }
                            catch (Exception ignored) { }
                        }
                        return true;
                    });
                }

                if (signup_button != null) {
                    signup_button.setOnPreferenceClickListener(preference -> {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.getEucWorldUrl() + "/signup")));
                        return true;
                    });
                }
                break;
            case General:
                tb.setTitle(R.string.general_preferences_title);
                Preference reset_top_button = findPreference(getString(R.string.reset_top_speed));
                Preference edit_user_distance_button = findPreference(getString(R.string.edit_user_distance));
                Preference last_mac_button = findPreference(getString(R.string.last_mac));
                if (reset_top_button != null) {
                    reset_top_button.setOnPreferenceClickListener(preference -> {
                        WheelData.getInstance().resetTopSpeed();
                        return true;
                    });
                }
                if (edit_user_distance_button != null) {
                    edit_user_distance_button.setOnPreferenceClickListener(preference -> {
                        WheelData.getInstance().resetUserDistance();
                        //showEditUserDistanceDialog();
                        return true;
                    });
                }
                if (last_mac_button != null) {
                    last_mac_button.setOnPreferenceClickListener(preference -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.mac_edit_type);

                        final EditText input = new EditText(getActivity());
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        input.setText(SettingsUtil.getLastAddress(getActivity()));
                        builder.setView(input);
                        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                            final String deviceAddress = input.getText().toString();
                            SettingsUtil.setLastAddress(getActivity(), deviceAddress);
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
                            builder1.setTitle(R.string.wheel_password);

                            final EditText input1 = new EditText(getActivity());
                            input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            builder1.setView(input1);
                            builder1.setPositiveButton(R.string.ok, (dialog12, which12) -> {
                                String password = input1.getText().toString();
                                SettingsUtil.setPasswordForWheel(getActivity(), deviceAddress, password);
                            });
                            builder1.setNegativeButton(R.string.cancel, (dialog1, which1) -> dialog1.cancel());
                            builder1.show();
                        });
                        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
                        builder.show();
                        return true;
                    });
                }
                break;
            case Gauge:
                tb.setTitle(R.string.gauge_preferences_title);
                Preference gauge_use_custom_horn_sound = findPreference(getString(R.string.gauge_use_custom_horn_sound));
                Preference gauge_custom_horn_sound = findPreference(getString(R.string.gauge_custom_horn_sound));
                if (gauge_use_custom_horn_sound != null)
                    gauge_use_custom_horn_sound.setEnabled(readExternalStorage);
                if (gauge_custom_horn_sound != null) {
                    gauge_custom_horn_sound.setEnabled(readExternalStorage);
                    gauge_custom_horn_sound.setSummary(SettingsUtil.getGaugeCustomHornSoundTitle(getActivity()));
                    gauge_custom_horn_sound.setOnPreferenceClickListener(preference -> {
                        selectSound(Constants.REQUEST_GAUGE_CUSTOM_HORN);
                        return true;
                    });
                }
                break;
            case Logs:
                tb.setTitle(R.string.log_preferences_title);
                break;
            case Livemap:
                tb.setTitle(R.string.livemap_preferences_title);
                break;
            case Alarms:
                tb.setTitle(R.string.alarm_preferences_title);
                break;
            case Speech:
                tb.setTitle(R.string.speech_preferences_title);
                Preference speech_messages_button = findPreference(getString(R.string.speech_messages_preferences));
                if (speech_messages_button != null) {
                    speech_messages_button.setOnPreferenceClickListener(preference -> {
                        currentScreen = SettingsScreen.SpeechMessages;
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences_speech_messages);
                        setup_screen();
                        return true;
                    });
                }
                break;
            case SpeechMessages:
                tb.setTitle(R.string.speech_messages_preferences_title);
                break;
            case Watch:
                tb.setTitle(R.string.watch_preferences_title);
                Preference watch_use_custom_horn_sound = findPreference(getString(R.string.watch_use_custom_horn_sound));
                Preference watch_custom_horn_sound = findPreference(getString(R.string.watch_custom_horn_sound));
                if (watch_use_custom_horn_sound != null)
                    watch_use_custom_horn_sound.setEnabled(readExternalStorage);
                if (watch_custom_horn_sound != null) {
                    watch_custom_horn_sound.setEnabled(readExternalStorage);
                    watch_custom_horn_sound.setSummary(SettingsUtil.getWatchCustomHornSoundTitle(getActivity()));
                    watch_custom_horn_sound.setOnPreferenceClickListener(preference -> {
                        selectSound(Constants.REQUEST_WATCH_CUSTOM_HORN);
                        return true;
                    });
                }
                break;
            case Flic:
                tb.setTitle(R.string.flic_preferences_title);
                Preference flic_button_setup = findPreference(getString(R.string.flic_button_setup));
                if (flic_button_setup != null) {
                    flic_button_setup.setOnPreferenceClickListener(preference -> {
                        try {
                            FlicManager.getInstance(getActivity(), manager -> manager.initiateGrabButton(getActivity()));
                        } catch (FlicAppNotInstalledException err) {
                            Toast.makeText(getActivity(), getString(R.string.flic_app_not_installed), Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    });
                }
                Preference flic_use_custom_horn_sound = findPreference(getString(R.string.flic_use_custom_horn_sound));
                Preference flic_custom_horn_sound = findPreference(getString(R.string.flic_custom_horn_sound));
                if (flic_use_custom_horn_sound != null)
                    flic_use_custom_horn_sound.setEnabled(readExternalStorage);
                if (flic_custom_horn_sound != null) {
                    flic_custom_horn_sound.setEnabled(readExternalStorage);
                    flic_custom_horn_sound.setSummary(SettingsUtil.getFlicCustomHornSoundTitle(getActivity()));
                    flic_custom_horn_sound.setOnPreferenceClickListener(preference -> {
                        selectSound(Constants.REQUEST_FLIC_CUSTOM_HORN);
                        return true;
                    });
                }
                break;
            case Wheel:
                tb.setTitle(R.string.wheel_settings_title);
                break;
        }
    }

    public void refreshVolatileSettings() {
        if (currentScreen == SettingsScreen.Logs) {
            correctCheckState(getString(R.string.auto_log));
            correctCheckState(getString(R.string.log_location_data));
            correctCheckState(getString(R.string.auto_upload));
        }
    }

    public void refreshWheelSettings(boolean isLight, boolean isLed, boolean isButton, int maxSpeed, int speakerVolume, int pedals) {
        correctWheelCheckState(getString(R.string.light_enabled), isLight);
        correctWheelCheckState(getString(R.string.led_enabled), isLed);
        correctWheelCheckState(getString(R.string.handle_button_disabled), isButton);

        correctWheelBarState(getString(R.string.wheel_max_speed), maxSpeed);
        correctWheelBarState(getString(R.string.speaker_volume), speakerVolume);
        correctWheelBarState(getString(R.string.pedals_adjustment), pedals);
    }

    private void correctWheelCheckState(String preference, boolean state) {
        CheckBoxPreference cb_preference = (CheckBoxPreference) findPreference(preference);
        if (cb_preference == null)
            return;

        boolean check_state = cb_preference.isChecked();

        if (state != check_state)
            cb_preference.setChecked(state);

    }

    private void correctWheelBarState(String preference, int stateInt) {
        SeekBarPreference sb_preference = (SeekBarPreference) findPreference(preference);
        if (sb_preference == null)
            return;
        int sb_value = sb_preference.getCurrentValue();
        if (stateInt != sb_value) {
            sb_preference.setCurrentValue(stateInt);
            /// Workaround, seekbar doesn't want to update view
            getPreferenceScreen().removeAll();
            if (mWheelType == WHEEL_TYPE.NINEBOT_Z)
                addPreferencesFromResource(R.xml.preferences_ninebot_z);
            if (mWheelType == WHEEL_TYPE.INMOTION)
                addPreferencesFromResource(R.xml.preferences_inmotion);
            if (mWheelType == WHEEL_TYPE.KINGSONG)
                addPreferencesFromResource(R.xml.preferences_kingsong);
            if (mWheelType == WHEEL_TYPE.GOTWAY)
                addPreferencesFromResource(R.xml.preferences_gotway);
            setup_screen();
        }
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(preference, stateInt);
        editor.apply();
    }

    private void correctCheckState(String preference) {
        boolean setting_state = SettingsUtil.getBoolean(getActivity(), preference);
        CheckBoxPreference cb_preference = (CheckBoxPreference) findPreference(preference);
        if (cb_preference == null)
            return;

        boolean check_state = cb_preference.isChecked();

        if (setting_state != check_state)
            cb_preference.setChecked(setting_state);
    }

    public boolean is_main_menu() {
        if (currentScreen == SettingsScreen.Main)
            return true;
        else return false;
    }

    public boolean show_main_menu() {
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);
        Preference wheel_button = findPreference(getString(R.string.wheel_settings));
        mWheelType = WheelData.getInstance().getWheelType();
        if ((mWheelType == WHEEL_TYPE.INMOTION) | (mWheelType == WHEEL_TYPE.KINGSONG) | (mWheelType == WHEEL_TYPE.GOTWAY)) {
            wheel_button.setEnabled(true);
        }
        currentScreen = SettingsScreen.Main;
        setup_screen();
        return true;
    }

    public boolean is_speech_messages_menu() {
        if (currentScreen == SettingsScreen.SpeechMessages)
            return true;
        else return false;
    }

    public boolean show_speech_menu() {
        currentScreen = SettingsScreen.Speech;
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences_speech);
        setup_screen();
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showAboutDialog() {
        WebView webView = new WebView(getActivity());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new EucworldJSInterface(getActivity()), "eucWorld");
        webView.loadUrl("file:///android_asset/about.html");
        new AlertDialog.Builder(getActivity())
                .setView(webView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .setNegativeButton(R.string.visit_eucworld, (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.getEucWorldUrl()))))
                .show();
    }

    private void showSignInDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.signin, null);
        builder.setView(view);
        final EditText email = view.findViewById(R.id.etEmail);
        final EditText password = view.findViewById(R.id.etPassword);
        builder.setPositiveButton(R.string.sign_in_title, (dialog, id) -> {
            final RequestParams requestParams = new RequestParams();
            requestParams.put("appid", Constants.appId(getActivity()));
            requestParams.put("email", email.getText());
            requestParams.put("password", password.getText());
            HttpClient.post(Constants.getEucWorldUrl() + "/api/signin", requestParams, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                    try {
                        int error = response.getInt("error");
                        switch (error) {
                            case 0:
                                LivemapService.setUserName(response.getJSONObject("data").getString("username"));
                                LivemapService.setApiKey(response.getJSONObject("data").getString("apikey"));
                                SettingsUtil.setLivemapApiKey(getActivity(), response.getJSONObject("data").getString("apikey"));
                                updateSigninSignupButtons();
                                Toast.makeText(getActivity(), String.format(Locale.US, getString(R.string.user_signed_in), LivemapService.getUserName()), Toast.LENGTH_LONG).show();
                                try {
                                    ((MainActivity) getActivity()).loadEucWorldApp();
                                    ((MainActivity) getActivity()).updateLivemapUI();
                                }
                                catch (Exception ignored) { }
                                break;
                            case 1:
                                showToast(R.string.livemap_api_error_general, Toast.LENGTH_LONG);
                                break;
                            case 8:
                                showToast(R.string.livemap_api_error_invalid_api_key, Toast.LENGTH_LONG);
                                break;
                            case 9:
                                showToast(R.string.livemap_api_error_accound_needs_activation, Toast.LENGTH_LONG);
                                break;
                            case 403:
                                showToast(R.string.livemap_api_error_forbidden, Toast.LENGTH_LONG);
                                break;
                            default:
                                showToast(R.string.livemap_api_error_unknown, Toast.LENGTH_LONG);
                        }
                    } catch (JSONException e) {
                        showToast(R.string.livemap_api_error_unknown, Toast.LENGTH_LONG);
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    showToast(R.string.livemap_api_error_unknown, Toast.LENGTH_LONG);
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                    showToast(R.string.livemap_api_error_no_connection, Toast.LENGTH_LONG);
                }
            });
        })
        .setNegativeButton(R.string.cancel, (dialog, id) -> { })
        .show();
    }

    public void updateSigninSignupButtons() {
        Preference signinout_button = findPreference(getString(R.string.sign_in_out));
        Preference signup_button = findPreference(getString(R.string.sign_up));
        if (signinout_button != null) {
            signinout_button.setTitle(LivemapService.getApiKey().isEmpty() ? getString(R.string.sign_in_title) : getString(R.string.sign_out_title));
            signinout_button.setEnabled(!LivemapService.isConnected());
        }
        if (signup_button != null)
            signup_button.setEnabled(LivemapService.getApiKey().isEmpty());
    }

    private void showToast(int message_id, int duration) {
        Toast.makeText(getActivity(), message_id, duration).show();
    }

    private void selectSound(int requestCode) {
        startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI), requestCode);
    }

    public static String getAudioPath(Context context, Uri uri) {
        String[] proj = { MediaStore.Audio.Media.DATA };
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
        cursor.close();
        return path;
    }

    public static String getAudioTitle(Context context, Uri uri) {
        String[] proj = { MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME };
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        cursor.moveToFirst();
        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
        String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
        cursor.close();
        return String.format("%s - %s\n%s", artist, title, name);
    }

}
