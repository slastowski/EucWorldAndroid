package net.lastowski.eucworld;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.PictureInPictureParams;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.util.Rational;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.lastowski.common.Commons;
import net.lastowski.common.Value;
import net.lastowski.common.view.DashboardGauge;
import net.lastowski.common.view.StatusBar;
import net.lastowski.common.view.ValueView;
import net.lastowski.eucworld.utils.Constants;
import net.lastowski.eucworld.utils.Constants.WHEEL_TYPE;
import net.lastowski.eucworld.utils.HttpClient;
import net.lastowski.eucworld.utils.NotificationUtil;
import net.lastowski.eucworld.utils.SettingsUtil;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import id.zelory.compressor.Compressor;
import io.flic.lib.FlicBroadcastReceiverFlags;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

import static io.flic.lib.FlicManager.GRAB_BUTTON_REQUEST_CODE;
import static net.lastowski.eucworld.utils.Constants.REQUEST_IMAGE_CAPTURE;
import static net.lastowski.eucworld.utils.Constants.RESULT_DEVICE_SCAN_REQUEST;
import static net.lastowski.eucworld.utils.Constants.RESULT_REQUEST_ENABLE_BT;
import static net.lastowski.eucworld.utils.MathsUtil.kmToMiles;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements
        DashboardGauge.OnGestureListener {

    @BindView(R.id.gauge) DashboardGauge gauge;
    @BindView(R.id.footer) LinearLayout footer;
    @BindView(R.id.statusbar) StatusBar statusBar;
    @BindView(R.id.indicator) SpringDotsIndicator indicator;

    @BindView(R.id.chart) LineChart chart;
    @BindView(R.id.chartUnavailable) TextView chartUnavailable;

    @BindView(R.id.tourContent) RelativeLayout tourContent;
    @BindView(R.id.tourButtons) LinearLayout tourButtons;
    @BindView(R.id.tourTitle) TextView tourTitle;
    @BindView(R.id.tourStatus) TextView tourStatus;
    @BindView(R.id.tourStartFinish) ImageButton tourStartFinish;
    @BindView(R.id.tourPause) ImageButton tourPause;
    @BindView(R.id.tourShare) ImageButton tourShare;
    @BindView(R.id.tourAddPhoto) ImageButton tourAddPhoto;

    // Live data
    @BindView(R.id.livedataUnavailable) TextView livedataUnavailable;

    @BindView(R.id.livedataSpeed) ValueView livedataSpeed;
    @BindView(R.id.livedataBatteryLevel) ValueView livedataBatteryLevel;
    @BindView(R.id.livedataDistance) ValueView livedataDistance;
    @BindView(R.id.livedataDistanceVehicle) ValueView livedataDistanceVehicle;
    @BindView(R.id.livedataDistanceUser) ValueView livedataDistanceUser;
    @BindView(R.id.livedataDuration) ValueView livedataDuration;
    @BindView(R.id.livedataDurationRiding) ValueView livedataDurationRiding;
    @BindView(R.id.livedataVoltage) ValueView livedataVoltage;
    @BindView(R.id.livedataCurrent) ValueView livedataCurrent;
    @BindView(R.id.livedataPower) ValueView livedataPower;
    @BindView(R.id.livedataTemperature) ValueView livedataTemperature;
    @BindView(R.id.livedataTemperatureMotor) ValueView livedataTemperatureMotor;
    @BindView(R.id.livedataTilt) ValueView livedataTilt;
    @BindView(R.id.livedataRoll) ValueView livedataRoll;
    @BindView(R.id.livedataCoolingFanStatus) ValueView livedataCoolingFanStatus;
    @BindView(R.id.livedataControlSensitivity) ValueView livedataControlSensitivity;
    @BindView(R.id.livedataDistanceTotal) ValueView livedataDistanceTotal;
    @BindView(R.id.livedataName) ValueView livedataName;
    @BindView(R.id.livedataModel) ValueView livedataModel;
    @BindView(R.id.livedataFirmwareVersion) ValueView livedataFirmwareVersion;
    @BindView(R.id.livedataSerialNumber) ValueView livedataSerialNumber;
    // Statistics
    @BindView(R.id.statsUnavailable) TextView statsUnavailable;

    @BindView(R.id.statsSpeedAvg) ValueView statsSpeedAvg;
    @BindView(R.id.statsSpeedAvgRiding) ValueView statsSpeedAvgRiding;
    @BindView(R.id.statsSpeedMax) ValueView statsSpeedMax;
    @BindView(R.id.statsLoadMaxRegen) ValueView statsLoadMaxRegen;
    @BindView(R.id.statsLoadMax) ValueView statsLoadMax;
    @BindView(R.id.statsTemperatureMin) ValueView statsTemperatureMin;
    @BindView(R.id.statsTemperatureMax) ValueView statsTemperatureMax;
    @BindView(R.id.statsBatteryLevelMin) ValueView statsBatteryLevelMin;
    @BindView(R.id.statsBatteryLevelMax) ValueView statsBatteryLevelMax;
    @BindView(R.id.statsVoltageMin) ValueView statsVoltageMin;
    @BindView(R.id.statsVoltageMax) ValueView statsVoltageMax;
    @BindView(R.id.statsCurrentMin) ValueView statsCurrentMin;
    @BindView(R.id.statsCurrentAvg) ValueView statsCurrentAvg;
    @BindView(R.id.statsCurrentMax) ValueView statsCurrentMax;
    @BindView(R.id.statsPowerMin) ValueView statsPowerMin;
    @BindView(R.id.statsPowerAvg) ValueView statsPowerAvg;
    @BindView(R.id.statsPowerMax) ValueView statsPowerMax;

    Menu mMenu;
    MenuItem miSearch;
    MenuItem miWheel;
    MenuItem miWatch;
    MenuItem miLogging;
    MenuItem miSpeech;
    MenuItem miSettings;
    WebView eucWorldWebView;
    Timer timer;
    TimerTask timerTask;

    private NotificationUtil notificationUtil;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private String mDeviceAddress;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private boolean doubleBackToExitPressedOnce = false;
    int viewPagerPage = 0;
    private ArrayList<String> xAxis_labels = new ArrayList<>();
    private DrawerLayout mDrawer;
    private String mImagePath = "";
    private EucworldJSInterface jsInterface;
    private boolean useMi = false;

    public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;
        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();
            if (position < -1)
                view.setAlpha(0f);
            else
            if (position <= 1) {
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                view.setTranslationX(position < 0 ? horzMargin - vertMargin / 2 : -horzMargin + vertMargin / 2);
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
                view.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
            }
            else
                view.setAlpha(0f);
        }
    }

    @Override
    public void onSingleTap(MotionEvent e) {
        Timber.d("Gauge single tap");
        performAction(SettingsUtil.getGaugeActionSingle(this));
    }

    @Override
    public void onDoubleTap(MotionEvent e) {
        Timber.d("Gauge double tap");
        performAction(SettingsUtil.getGaugeActionDouble(this));
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Timber.d("Gauge tap and hold");
        performAction(SettingsUtil.getGaugeActionHold(this));
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }

    @Override
    public void onUserLeaveHint () {
        if (SettingsUtil.isEnabledPIP(this) && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            ViewPager pager = findViewById(R.id.pager);
            pager.setCurrentItem(0, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Rational rational = new Rational(100, 100);
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(rational)
                        .build();
                enterPictureInPictureMode(params);
            }
            else
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                enterPictureInPictureMode();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPIPMode) {
        View header  = findViewById(R.id.header);
        View footer = findViewById(R.id.footer);
        if (isInPIPMode) {
            gauge.setWatchMode(true);
            header.setVisibility(View.GONE);
            footer.setVisibility(View.GONE);
        }
        else {
            header.setVisibility(View.VISIBLE);
            footer.setVisibility(View.VISIBLE);
            gauge.setWatchMode(false);
        }
        super.onPictureInPictureModeChanged(isInPIPMode);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Timber.e(getResources().getString(R.string.error_bluetooth_not_initialised));
                Toast.makeText(MainActivity.this, R.string.error_bluetooth_not_initialised, Toast.LENGTH_SHORT).show();
                finish();
            }

            if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED &&
                    mDeviceAddress != null && !mDeviceAddress.isEmpty()) {
                mBluetoothLeService.setDeviceAddress(mDeviceAddress);
                toggleConnectToWheel();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            finish();
        }
    };

    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null)
                switch (action) {
                    case Constants.ACTION_BLUETOOTH_CONNECTION_STATE:
                        int connectionState = intent.getIntExtra(Constants.INTENT_EXTRA_CONNECTION_STATE, BluetoothLeService.STATE_DISCONNECTED);
                        Timber.d("Bluetooth state = %d", connectionState);
                        setConnectionState(connectionState);
                        break;
                    case Constants.ACTION_WHEEL_DATA_AVAILABLE:
                        if (WheelData.getInstance().getWheelType() == WHEEL_TYPE.KINGSONG) {
                            if (WheelData.getInstance().getName().isEmpty())
                                sendBroadcast(new Intent(Constants.ACTION_REQUEST_KINGSONG_NAME_DATA));
                            else
                            if (WheelData.getInstance().getSerial().isEmpty())
                                sendBroadcast(new Intent(Constants.ACTION_REQUEST_KINGSONG_SERIAL_DATA));
                        }
                        if (intent.hasExtra(Constants.INTENT_EXTRA_WHEEL_SETTINGS)) {
                            setWheelPreferences();
                        }
                        updateScreen(intent.hasExtra(Constants.INTENT_EXTRA_GRAPH_UPDATE_AVILABLE));
                        break;
                    case Constants.ACTION_PEBBLE_SERVICE_TOGGLED:
                        setMenuIconStates();
                        break;
                    case Constants.ACTION_SPEECH_SERVICE_TOGGLED:
                        setMenuIconStates();
                        break;
                    case Constants.ACTION_LOGGING_SERVICE_TOGGLED:
                        boolean running = intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_RUNNING, false);
                        if (intent.hasExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION)) {
                            String filepath = intent.getStringExtra(Constants.INTENT_EXTRA_LOGGING_FILE_LOCATION);
                            if (running)
                                statusBar.setMessage(getResources().getString(R.string.logging_started), 3000);
                        }
                        setMenuIconStates();
                        break;
                    case Constants.ACTION_PREFERENCE_CHANGED:
                        loadPreferences();
                        break;
                    case Constants.ACTION_WHEEL_TYPE_RECOGNIZED:
                        break;
                    case Constants.ACTION_ALARM_TRIGGERED:
                    case Constants.ACTION_ALARM_FINISHED:
                        Constants.ALARM_TYPE alarm = (Constants.ALARM_TYPE) intent.getSerializableExtra(Constants.INTENT_EXTRA_ALARM_TYPE);
                        if (action.equals(Constants.ACTION_ALARM_TRIGGERED)) {
                            Timber.d("Raised alarm (%s)", alarm.toString());
                        }
                        else {
                            Timber.d("Ceased alarm (%s)", alarm.toString());
                        }
                        break;
                    case Constants.ACTION_LIVEMAP_STATUS:
                        updateFooterBackground();
                        updateLivemapUI();
                        if (!LivemapService.getLivemapGPS())
                            Data.invalidateTourValues();
                        break;
                    case Constants.ACTION_LIVEMAP_LOCATION_UPDATED:
                        updateScreen(true);
                        break;
                    case Constants.ACTION_WHEEL_CONNECTED:
                        gauge.setVehicleMode(true);
                        updateFooterBackground();
                        break;
                    case Constants.ACTION_WHEEL_DISCONNECTED:
                    case Constants.ACTION_WHEEL_CONNECTION_LOST:
                        if (LivemapService.getLivemapGPS())
                            gauge.setVehicleMode(false);
                        updateFooterBackground();
                        Data.invalidateVehicleValues();
                        break;
                    case Constants.ACTION_SET_TOUR_HEADER_VISIBILITY:
                        float v = intent.getIntExtra("v", 100) / 100;
                        tourTitle.setAlpha(v);
                        tourStatus.setAlpha(v);
                        break;
                    case Constants.ACTION_TOUR_RELOAD_WEBVIEW:
                        eucWorldWebView.reload();
                        break;
                }
        }
    };

    private void setConnectionState(int connectionState) {
        switch (connectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                configureDisplay(WheelData.getInstance().getWheelType());
                if (mDeviceAddress != null && !mDeviceAddress.isEmpty())
                    SettingsUtil.setLastAddress(getApplicationContext(), mDeviceAddress);
                statusBar.setMessage(getString(R.string.bt_connected), 3000);
                break;
            case BluetoothLeService.STATE_CONNECTING:
                if (mConnectionState == BluetoothLeService.STATE_CONNECTING) {
                    statusBar.setMessage(getString(R.string.bt_connecting));
                }
                else {
                    if (mBluetoothLeService.getDisconnectTime() != null) {
                        statusBar.setMessage(getString(R.string.bt_connection_lost));
                    }
                }
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                break;
        }
        mConnectionState = connectionState;
        setMenuIconStates();
    }

    private void setWheelPreferences() {
        Timber.d("SetWheelPreferences");
        ((PreferencesFragment) getPreferencesFragment()).refreshWheelSettings(WheelData.getInstance().getWheelLight(),
                WheelData.getInstance().getWheelLed(),
                WheelData.getInstance().getWheelHandleButton(),
                WheelData.getInstance().getWheelMaxSpeed(),
                WheelData.getInstance().getSpeakerVolume(),
                WheelData.getInstance().getPedalsPosition());
    }

    private void setMenuIconStates() {
        if (mMenu == null)
            return;

        miWheel.setVisible(mBluetoothAdapter != null);
        if (mDeviceAddress == null || mDeviceAddress.isEmpty()) {
            miWheel.setEnabled(false);
        }
        else {
            miWheel.setEnabled(mBluetoothAdapter != null);
        }

        miSearch.setVisible(mBluetoothAdapter != null);
        switch (mConnectionState) {
            case BluetoothLeService.STATE_CONNECTED:
                miWheel.setIcon(R.drawable.ic_wheel_active_24px);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(0);
                break;
            case BluetoothLeService.STATE_CONNECTING:
                miWheel.setIcon(R.drawable.anim_wheel_icon);
                miWheel.setTitle(R.string.disconnect_from_wheel);
                ((AnimationDrawable) miWheel.getIcon()).start();
                miSearch.setEnabled(false);
                miSearch.getIcon().setAlpha(0);
                break;
            case BluetoothLeService.STATE_DISCONNECTED:
                miWheel.setIcon(R.drawable.ic_circle_24px);
                miWheel.setTitle(R.string.connect_to_wheel);
                miSearch.setEnabled(mBluetoothAdapter != null);
                miSearch.getIcon().setAlpha(255);
                break;
        }

        if (PebbleService.isInstanceCreated()) {
            miWatch.setIcon(R.drawable.ic_watch_active_24px);
        } else {
            miWatch.setIcon(R.drawable.ic_watch_24px);
        }

        miLogging.setVisible(mBluetoothAdapter != null);
        miLogging.setEnabled(mBluetoothAdapter != null);
        if (LoggingService.isInstanceCreated()) {
            miLogging.setTitle(R.string.stop_data_service);
            miLogging.setIcon(R.drawable.ic_csv_active_24px);
        } else {
            miLogging.setTitle(R.string.start_data_service);
            miLogging.setIcon(R.drawable.ic_csv_24px);
        }

        if (SpeechService.isInstanceCreated()) {
            miSpeech.setTitle(R.string.stop_speech_service);
            miSpeech.setIcon(R.drawable.ic_speech_active_24px);
        } else {
            miSpeech.setTitle(R.string.start_speech_service);
            miSpeech.setIcon(R.drawable.ic_speech_24px);
        }

    }

    private void configureDisplay(WHEEL_TYPE wheelType) {
        switch (wheelType) {
            case KINGSONG:
                livedataUnavailable.setVisibility(View.GONE);

                livedataSpeed.setVisibility(View.VISIBLE);
                livedataBatteryLevel.setVisibility(View.VISIBLE);
                livedataTemperature.setVisibility(View.VISIBLE);
                livedataTemperatureMotor.setVisibility(View.GONE);
                livedataDuration.setVisibility(View.VISIBLE);
                livedataDurationRiding.setVisibility(View.VISIBLE);
                livedataDistance.setVisibility(View.VISIBLE);
                livedataDistanceVehicle.setVisibility(View.GONE);
                livedataDistanceUser.setVisibility(View.VISIBLE);
                livedataDistanceTotal.setVisibility(View.VISIBLE);
                livedataVoltage.setVisibility(View.VISIBLE);
                livedataCurrent.setVisibility(View.VISIBLE);
                livedataPower.setVisibility(View.VISIBLE);
                livedataTilt.setVisibility(View.GONE);
                livedataRoll.setVisibility(View.GONE);
                livedataCoolingFanStatus.setVisibility(View.VISIBLE);
                livedataControlSensitivity.setVisibility(View.VISIBLE);
                livedataName.setVisibility(View.VISIBLE);
                livedataModel.setVisibility(View.VISIBLE);
                livedataFirmwareVersion.setVisibility(View.VISIBLE);
                livedataSerialNumber.setVisibility(View.VISIBLE);
                break;
            case GOTWAY:
                livedataUnavailable.setVisibility(View.GONE);

                livedataSpeed.setVisibility(View.VISIBLE);
                livedataBatteryLevel.setVisibility(View.VISIBLE);
                livedataTemperature.setVisibility(View.VISIBLE);
                livedataTemperatureMotor.setVisibility(View.GONE);
                livedataDuration.setVisibility(View.VISIBLE);
                livedataDurationRiding.setVisibility(View.VISIBLE);
                livedataDistance.setVisibility(View.VISIBLE);
                livedataDistanceVehicle.setVisibility(View.GONE);
                livedataDistanceUser.setVisibility(View.VISIBLE);
                livedataDistanceTotal.setVisibility(View.VISIBLE);
                livedataVoltage.setVisibility(View.VISIBLE);
                livedataCurrent.setVisibility(View.VISIBLE);
                livedataPower.setVisibility(View.VISIBLE);
                livedataTilt.setVisibility(View.GONE);
                livedataRoll.setVisibility(View.GONE);
                livedataCoolingFanStatus.setVisibility(View.GONE);
                livedataControlSensitivity.setVisibility(View.GONE);
                livedataName.setVisibility(View.GONE);
                livedataModel.setVisibility(View.GONE);
                livedataFirmwareVersion.setVisibility(View.GONE);
                livedataSerialNumber.setVisibility(View.GONE);
                break;
            case INMOTION:
                livedataUnavailable.setVisibility(View.GONE);

                livedataSpeed.setVisibility(View.VISIBLE);
                livedataBatteryLevel.setVisibility(View.VISIBLE);
                livedataTemperature.setVisibility(View.VISIBLE);
                livedataTemperatureMotor.setVisibility(View.GONE);
                livedataDuration.setVisibility(View.VISIBLE);
                livedataDurationRiding.setVisibility(View.VISIBLE);
                livedataDistance.setVisibility(View.VISIBLE);
                livedataDistanceVehicle.setVisibility(View.GONE);
                livedataDistanceUser.setVisibility(View.VISIBLE);
                livedataDistanceTotal.setVisibility(View.VISIBLE);
                livedataVoltage.setVisibility(View.VISIBLE);
                livedataCurrent.setVisibility(View.VISIBLE);
                livedataPower.setVisibility(View.VISIBLE);
                livedataTilt.setVisibility(View.VISIBLE);
                livedataRoll.setVisibility(View.VISIBLE);
                livedataCoolingFanStatus.setVisibility(View.GONE);
                livedataControlSensitivity.setVisibility(View.VISIBLE);
                livedataName.setVisibility(View.VISIBLE);
                livedataModel.setVisibility(View.VISIBLE);
                livedataFirmwareVersion.setVisibility(View.VISIBLE);
                livedataSerialNumber.setVisibility(View.VISIBLE);
                break;

            case NINEBOT:
                livedataUnavailable.setVisibility(View.GONE);

                livedataSpeed.setVisibility(View.VISIBLE);
                livedataBatteryLevel.setVisibility(View.VISIBLE);
                livedataDistance.setVisibility(View.VISIBLE);
                livedataDistanceUser.setVisibility(View.VISIBLE);
                livedataDuration.setVisibility(View.VISIBLE);
                livedataDurationRiding.setVisibility(View.VISIBLE);
                livedataVoltage.setVisibility(View.VISIBLE);
                livedataCurrent.setVisibility(View.VISIBLE);
                livedataPower.setVisibility(View.VISIBLE);
                livedataTemperature.setVisibility(View.VISIBLE);
                livedataTemperatureMotor.setVisibility(View.GONE);
                livedataControlSensitivity.setVisibility(View.GONE);
                livedataTilt.setVisibility(View.VISIBLE);
                livedataRoll.setVisibility(View.VISIBLE);
                livedataDistanceTotal.setVisibility(View.VISIBLE);
                livedataModel.setVisibility(View.VISIBLE);
                livedataFirmwareVersion.setVisibility(View.VISIBLE);
                livedataSerialNumber.setVisibility(View.VISIBLE);
                break;                
                
            case NINEBOT_Z:
                livedataUnavailable.setVisibility(View.GONE);

                livedataSpeed.setVisibility(View.VISIBLE);
                livedataBatteryLevel.setVisibility(View.VISIBLE);
                livedataTemperature.setVisibility(View.VISIBLE);
                livedataTemperatureMotor.setVisibility(View.GONE);
                livedataDuration.setVisibility(View.VISIBLE);
                livedataDurationRiding.setVisibility(View.VISIBLE);
                livedataDistance.setVisibility(View.VISIBLE);
                livedataDistanceVehicle.setVisibility(View.GONE);
                livedataDistanceUser.setVisibility(View.VISIBLE);
                livedataDistanceTotal.setVisibility(View.VISIBLE);
                livedataVoltage.setVisibility(View.VISIBLE);
                livedataCurrent.setVisibility(View.VISIBLE);
                livedataPower.setVisibility(View.VISIBLE);
                livedataTilt.setVisibility(View.GONE);
                livedataRoll.setVisibility(View.GONE);
                livedataCoolingFanStatus.setVisibility(View.GONE);
                livedataControlSensitivity.setVisibility(View.GONE);
                livedataName.setVisibility(View.VISIBLE);
                livedataModel.setVisibility(View.VISIBLE);
                livedataFirmwareVersion.setVisibility(View.VISIBLE);
                livedataSerialNumber.setVisibility(View.VISIBLE);
                break;

            default:
                livedataUnavailable.setVisibility(View.VISIBLE);

                livedataSpeed.setVisibility(View.GONE);
                livedataBatteryLevel.setVisibility(View.GONE);
                livedataTemperature.setVisibility(View.GONE);
                livedataTemperatureMotor.setVisibility(View.GONE);
                livedataDuration.setVisibility(View.GONE);
                livedataDurationRiding.setVisibility(View.GONE);
                livedataDistance.setVisibility(View.GONE);
                livedataDistanceVehicle.setVisibility(View.GONE);
                livedataDistanceUser.setVisibility(View.GONE);
                livedataDistanceTotal.setVisibility(View.GONE);
                livedataVoltage.setVisibility(View.GONE);
                livedataCurrent.setVisibility(View.GONE);
                livedataPower.setVisibility(View.GONE);
                livedataTilt.setVisibility(View.GONE);
                livedataRoll.setVisibility(View.GONE);
                livedataCoolingFanStatus.setVisibility(View.GONE);
                livedataControlSensitivity.setVisibility(View.GONE);
                livedataName.setVisibility(View.GONE);
                livedataModel.setVisibility(View.GONE);
                livedataFirmwareVersion.setVisibility(View.GONE);
                livedataSerialNumber.setVisibility(View.GONE);

                statsUnavailable.setVisibility(View.VISIBLE);
                
                statsSpeedAvg.setVisibility(View.GONE);
                statsSpeedAvgRiding.setVisibility(View.GONE);
                statsSpeedMax.setVisibility(View.GONE);
                statsLoadMaxRegen.setVisibility(View.GONE);
                statsLoadMax.setVisibility(View.GONE);
                statsTemperatureMin.setVisibility(View.GONE);
                statsTemperatureMax.setVisibility(View.GONE);
                statsBatteryLevelMin.setVisibility(View.GONE);
                statsBatteryLevelMax.setVisibility(View.GONE);
                statsVoltageMin.setVisibility(View.GONE);
                statsVoltageMax.setVisibility(View.GONE);
                statsCurrentMin.setVisibility(View.GONE);
                statsCurrentAvg.setVisibility(View.GONE);
                statsCurrentMax.setVisibility(View.GONE);
                statsPowerMin.setVisibility(View.GONE);
                statsPowerAvg.setVisibility(View.GONE);
                statsPowerMax.setVisibility(View.GONE);

                break;
        }
        // Stats
        if (wheelType != WHEEL_TYPE.Unknown) {
            statsUnavailable.setVisibility(View.GONE);

            statsSpeedAvg.setVisibility(View.VISIBLE);
            statsSpeedAvgRiding.setVisibility(View.VISIBLE);
            statsSpeedMax.setVisibility(View.VISIBLE);
            statsTemperatureMin.setVisibility(View.VISIBLE);
            statsTemperatureMax.setVisibility(View.VISIBLE);
            statsBatteryLevelMin.setVisibility(View.VISIBLE);
            statsBatteryLevelMax.setVisibility(View.VISIBLE);
            statsVoltageMin.setVisibility(View.VISIBLE);
            statsVoltageMax.setVisibility(View.VISIBLE);
            statsCurrentMin.setVisibility(View.VISIBLE);
            statsCurrentAvg.setVisibility(View.VISIBLE);
            statsCurrentMax.setVisibility(View.VISIBLE);
            statsPowerMin.setVisibility(View.VISIBLE);
            statsPowerAvg.setVisibility(View.VISIBLE);
            statsPowerMax.setVisibility(View.VISIBLE);
        }
    }

    private void updateScreen(boolean updateGraph) {
        switch (viewPagerPage) {
            case 0: // GUI View
                gauge.setSpeedAlarm(WheelData.getInstance().isSpeedAlarm1Active() || WheelData.getInstance().isSpeedAlarm2Active() || WheelData.getInstance().isSpeedAlarm3Active());
                gauge.setLoadAlarm(WheelData.getInstance().isPeakCurrentAlarmActive() || WheelData.getInstance().isSustainedCurrentAlarmActive());
                gauge.setTemperatureAlarm(WheelData.getInstance().isTemperatureAlarmActive());
                gauge.setBatteryAlarm(WheelData.getInstance().isVoltageAlarmActive());
                gauge.setBatteryWarning(!Data.vehicleBatteryLevelFiltered.isEmpty() && Data.vehicleBatteryLevelFiltered.getAsFloat() <= 20f);
                break;
            case 1: // Text View
                statsLoadMax.setVisibility(Data.vehicleLoadMax.isEmpty() ? View.GONE : View.VISIBLE);
                statsLoadMaxRegen.setVisibility(Data.vehicleLoadMaxRegen.isEmpty() ? View.GONE : View.VISIBLE);
                livedataTemperatureMotor.setVisibility(Data.vehicleTemperatureMotor.isEmpty() ? View.GONE : View.VISIBLE);
                break;
            case 2: // Graph  View
                if (updateGraph) {
                    xAxis_labels = WheelData.getInstance().getXAxis();

                    if (xAxis_labels.size() > 0) {

                        LineDataSet dataSetSpeed;
                        LineDataSet dataSetCurrent;

                        if (chart.getData() == null) {
                            dataSetSpeed = new LineDataSet(null, getString(R.string.speed));
                            dataSetCurrent = new LineDataSet(null, getString(R.string.current));

                            dataSetSpeed.setLineWidth(2);
                            dataSetCurrent.setLineWidth(2);
                            dataSetSpeed.setAxisDependency(YAxis.AxisDependency.LEFT);
                            dataSetCurrent.setAxisDependency(YAxis.AxisDependency.RIGHT);
                            dataSetSpeed.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                            dataSetCurrent.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                            dataSetSpeed.setColor(getResources().getColor(R.color.speed));
                            dataSetCurrent.setColor(getResources().getColor(R.color.current));
                            dataSetSpeed.setDrawCircles(false);
                            dataSetCurrent.setDrawCircles(false);
                            dataSetSpeed.setDrawValues(false);
                            dataSetCurrent.setDrawValues(false);
                            LineData chart_lineData = new LineData();
                            chart_lineData.addDataSet(dataSetCurrent);
                            chart_lineData.addDataSet(dataSetSpeed);
                            chart.setData(chart_lineData);
                            chart.setVisibility(View.VISIBLE);
                            chartUnavailable.setVisibility(View.GONE);
                        }
                        else {
                            dataSetSpeed = (LineDataSet) chart.getData().getDataSetByLabel(getString(R.string.speed), true);
                            dataSetCurrent = (LineDataSet) chart.getData().getDataSetByLabel(getString(R.string.current), true);
                        }

                        dataSetSpeed.clear();
                        dataSetCurrent.clear();

                        ArrayList<Float> currentAxis = new ArrayList<>(WheelData.getInstance().getCurrentAxis());
                        ArrayList<Float> speedAxis = new ArrayList<>(WheelData.getInstance().getSpeedAxis());

                        for (Float d : currentAxis) {
                            float value = 0;
                            if (d != null)
                                value = d;

                            dataSetCurrent.addEntry(new Entry(dataSetCurrent.getEntryCount(), value));
                        }

                        for (Float d : speedAxis) {
                            float value = 0;

                            if (d != null)
                                value = d;

                            if (useMi)
                                dataSetSpeed.addEntry(new Entry(dataSetSpeed.getEntryCount(), kmToMiles(value)));
                            else
                                dataSetSpeed.addEntry(new Entry(dataSetSpeed.getEntryCount(), value));
                        }

                        dataSetCurrent.notifyDataSetChanged();
                        dataSetSpeed.notifyDataSetChanged();
                        chart.getData().notifyDataChanged();
                        chart.notifyDataSetChanged();
                        chart.invalidate();
                        break;
                    }
                }
                break;
            case 3: // Livemap View
                //Data.refreshTourValues();
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        rearrangeScreen(newConfig);
        updateScreen(true);
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Data.initialize(this);
        WheelData.initiate(this);

        bindValuesToViews();

        notificationUtil = new NotificationUtil(this);

        FlicManager.setAppCredentials(Constants.flicKey(this), Constants.flicSecret(this), Constants.APP_NAME);

        getFragmentManager().beginTransaction()
                .replace(R.id.settings_frame, getPreferencesFragment(), Constants.PREFERENCES_FRAGMENT_TAG)
                .commit();

        gauge.setSpeedRange(SettingsUtil.getMaxSpeed(this));
        gauge.setOnGestureListener(this);
        gauge.setVehicleMode(true);

        ViewPageAdapter adapter = new ViewPageAdapter(this);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(4);
        pager.setPageTransformer(true, new ZoomOutPageTransformer());

        SpringDotsIndicator dotsIndicator = findViewById(R.id.indicator);
        dotsIndicator.setViewPager(pager);
        pager.addOnPageChangeListener(pageChangeListener);

        mDeviceAddress = SettingsUtil.getLastAddress(getApplicationContext());
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        mDrawer = findViewById(R.id.drawer_layout);
        tourStartFinish = findViewById(R.id.tourStartFinish);
        tourPause = findViewById(R.id.tourPause);
        tourAddPhoto = findViewById(R.id.tourAddPhoto);
        tourShare = findViewById(R.id.tourShare);

        jsInterface = new EucworldJSInterface(this);

        eucWorldWebView = findViewById(R.id.tourWebView);
        eucWorldWebView.getSettings().setJavaScriptEnabled(true);
        eucWorldWebView.addJavascriptInterface(jsInterface, "eucWorld");
        eucWorldWebView.loadUrl("file:///android_asset/bootstrap.html");

        mDrawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) { }

            @Override
            public void onDrawerOpened(View drawerView) {
                ((PreferencesFragment) getPreferencesFragment()).show_main_menu();
            }

            @Override
            public void onDrawerClosed(View drawerView) { }

            @Override
            public void onDrawerStateChanged(int newState) { }
        });

        tourStartFinish.setOnClickListener(v -> {
            Timber.d("Tour start/finish button clicked; disabling button and toggling service");
            tourStartFinish.setEnabled(false);
            tourStartFinish.getLayoutParams().width = tourStartFinish.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_large);
            tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_disabled));
            tourStartFinish.setImageResource(R.drawable.ic_stop_large);
            tourPause.setVisibility(View.GONE);
            tourAddPhoto.setVisibility(View.GONE);
            tourShare.setVisibility(View.GONE);
            if (LivemapService.getStatus() == Commons.TourTrackingServiceStatus.DISCONNECTED)
                tourStatus.setText(getString(R.string.livemap_connecting));
            else
                tourStatus.setText(getString(R.string.livemap_disconnecting));
            MainActivityPermissionsDispatcher.toggleLivemapServiceWithPermissionCheck(MainActivity.this);
        });

        tourPause.setOnClickListener(v -> {
            Timber.d("Tour pause button clicked; disabling button and sending broadcast to service");
            tourPause.setEnabled(false);
            tourPause.setBackground(getResources().getDrawable(R.drawable.round_button_disabled));
            tourPause.setImageResource(R.drawable.ic_pause);
            if (LivemapService.getStatus() == Commons.TourTrackingServiceStatus.STARTED)
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_PAUSE));
            else
                sendBroadcast(new Intent(Constants.ACTION_LIVEMAP_RESUME));
        });

        tourAddPhoto.setOnClickListener(v -> {
            Timber.d("Tour picture button clicked");
            MainActivityPermissionsDispatcher.imageCaptureWithPermissionCheck(MainActivity.this);
        });

        tourShare.setOnClickListener(v -> {
            Timber.d("Tour share button clicked");
            shareLivemapUrl();
        });

        Typeface tf = Typeface.create("sans-serif-condensed-light", Typeface.NORMAL);

        chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(false);
        chart.setHardwareAccelerationEnabled(true);
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);
        chart.getLegend().setTextColor(getResources().getColor(android.R.color.white));
        chart.setNoDataTextColor(getResources().getColor(android.R.color.white));
        chart.setNoDataText("");

        Legend legend = chart.getLegend();
        legend.setTypeface(tf);
        legend.setTextSize(16f);

        YAxis leftAxis = chart.getAxisLeft();
        YAxis rightAxis = chart.getAxisRight();
        leftAxis.setAxisMinValue(0f);
        //rightAxis.setAxisMinValue(0f);
        leftAxis.setTypeface(tf);
        rightAxis.setTypeface(tf);
        leftAxis.setDrawGridLines(false);
        rightAxis.setDrawGridLines(false);
        leftAxis.setTextColor(getResources().getColor(android.R.color.white));
        rightAxis.setTextColor(getResources().getColor(android.R.color.white));
        leftAxis.setValueFormatter(new SpeedValueFormatter());
        rightAxis.setValueFormatter(new CurrentValueFormatter());

        XAxis xAxis = chart.getXAxis();
        xAxis.setTypeface(tf);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(android.R.color.white));
        xAxis.setValueFormatter(chartAxisValueFormatter);

        loadPreferences();

        // Initializes a Bluetooth adapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!mBluetoothAdapter.isEnabled())
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RESULT_REQUEST_ENABLE_BT);
        }
        else {
            startBluetoothService();
        }

        // Enable watch on startup
        if (SettingsUtil.isWatchEnabledOnStartup(this)) {
            startPebbleService();
            startWearService();
        }

        // Enable voice messages on startup
        if (SettingsUtil.isSpeechEnabledOnStartup(this))
            startSpeechService();

        // Sign in to euc.world
        signInOnStartup();

        // Request external storage permission for some setting options (eg. custom horn sounds)
        MainActivityPermissionsDispatcher.acquireStoragePermissionWithPermissionCheck(this); // .acquireReadExternalStoragePermissionWithPermissionCheck(this);

        timerTask = new TimerTask() {
            @Override
            public void run() {
                int batt = ((BatteryManager)getSystemService(BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (batt > 0) Data.phoneBatteryLevel.setAsInt(batt);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBluetoothLeService != null &&
                mConnectionState != BluetoothLeService.getConnectionState())
            setConnectionState(BluetoothLeService.getConnectionState());
        if (WheelData.getInstance().getWheelType() != WHEEL_TYPE.Unknown)
            configureDisplay(WheelData.getInstance().getWheelType());
        registerReceiver(mBluetoothUpdateReceiver, makeIntentFilter());
        rearrangeScreen(null);
        updateScreen(true);
        updateLivemapUI();
        updateFooterBackground();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setMenuIconStates();
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode())
            unregisterReceiver(mBluetoothUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        stopLivemapService();
        stopWearService();
        stopPebbleService();
        stopLoggingService();
        stopSpeechService();
        WheelData.getInstance().full_reset(false);
        WheelData.getInstance().destroy();
        if (mBluetoothLeService != null) {
            unbindService(mServiceConnection);
            stopService(new Intent(getApplicationContext(), BluetoothLeService.class));
            mBluetoothLeService = null;
        }
        notificationUtil.unregisterReceiver(this);
        super.onDestroy();
        //
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Wait until livemap finish gracefully
                if (LivemapService.getStatus() == Commons.TourTrackingServiceStatus.DISCONNECTED)
                    android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 0, 100);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenu = menu;
        miSearch = mMenu.findItem(R.id.miSearch);
        miWheel = mMenu.findItem(R.id.miWheel);
        miWatch = mMenu.findItem(R.id.miWatch);
        miLogging = mMenu.findItem(R.id.miLogging);
        miSpeech = mMenu.findItem(R.id.miSpeech);
        miSettings = mMenu.findItem(R.id.miSettings);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.miSearch:
                MainActivityPermissionsDispatcher.startScanActivityWithPermissionCheck(this);
                return true;
            case R.id.miWheel:
                toggleConnectToWheel();
                return true;
            case R.id.miLogging:
                MainActivityPermissionsDispatcher.toggleLoggingServiceWithPermissionCheck(this);
                return true;
            case R.id.miWatch:
                togglePebbleService();
                toggleWearService();
                return true;
            case R.id.miSpeech:
                toggleSpeechService();
                return true;
            case R.id.miSettings:
                View settings_layout = findViewById(R.id.settings_layout);
                if (mDrawer.isDrawerOpen(settings_layout))
                    mDrawer.closeDrawers();
                else
                    mDrawer.openDrawer(GravityCompat.START, true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        View settings_layout = findViewById(R.id.settings_layout);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (mDrawer.isDrawerOpen(settings_layout)) {
                    mDrawer.closeDrawers();
                } else {
                    mDrawer.openDrawer(GravityCompat.START, true);
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (mDrawer.isDrawerOpen(settings_layout)) {
                    if (((PreferencesFragment) getPreferencesFragment()).is_main_menu())
                        mDrawer.closeDrawer(GravityCompat.START, true);
                    else if (((PreferencesFragment) getPreferencesFragment()).is_speech_messages_menu())
                        ((PreferencesFragment) getPreferencesFragment()).show_speech_menu();
                    else
                        ((PreferencesFragment) getPreferencesFragment()).show_main_menu();
                } else {
                    if (doubleBackToExitPressedOnce) {
                        finish();
                        return true;
                    }

                    doubleBackToExitPressedOnce = true;
                    showToast(R.string.back_to_exit, Toast.LENGTH_SHORT);
                    //statusBar.setMessage(getString(R.string.back_to_exit), 2000);

                    new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    ViewPager.SimpleOnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            viewPagerPage = position;
            updateScreen(true);
        }
    };

    private void loadPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        setMainActivityLockBehaviour();

        gauge.setSpeedRange(SettingsUtil.getMaxSpeed(this));
        Data.setUseMi(sharedPreferences.getBoolean(getString(R.string.use_mi), false));
        Data.setUseF(sharedPreferences.getBoolean(getString(R.string.use_f), false));
        Data.setUseFt(sharedPreferences.getBoolean(getString(R.string.use_ft), false));

        boolean auto_log = sharedPreferences.getBoolean(getString(R.string.auto_log), false);
        boolean log_location = sharedPreferences.getBoolean(getString(R.string.log_location_data), false);
        boolean alarms_enabled = sharedPreferences.getBoolean(getString(R.string.alarms_enabled), false);
        boolean use_ratio = sharedPreferences.getBoolean(getString(R.string.use_ratio), false);
        int gotway_voltage = Integer.parseInt(sharedPreferences.getString(getString(R.string.gotway_voltage), "0"));
        int alarm1Speed = sharedPreferences.getInt(getString(R.string.alarm_1_speed), 0);
        int alarm2Speed = sharedPreferences.getInt(getString(R.string.alarm_2_speed), 0);
        int alarm3Speed = sharedPreferences.getInt(getString(R.string.alarm_3_speed), 0);
        int alarm1Battery = sharedPreferences.getInt(getString(R.string.alarm_1_battery), 0);
        int alarm2Battery = sharedPreferences.getInt(getString(R.string.alarm_2_battery), 0);
        int alarm3Battery = sharedPreferences.getInt(getString(R.string.alarm_3_battery), 0);
        int current_peak_alarm = sharedPreferences.getInt(getString(R.string.alarm_current), 0);
        int current_sustained_alarm = sharedPreferences.getInt(getString(R.string.alarm_current_sustained), 0);
        int temperature_alarm = sharedPreferences.getInt(getString(R.string.alarm_temperature), 0);
        boolean enableVoltageAlarm = sharedPreferences.getBoolean(getString(R.string.alarm_voltage), false);
        boolean disablePhoneVibrate = sharedPreferences.getBoolean(getString(R.string.disable_phone_vibrate), false);

        WheelData.getInstance().setUseRatio(use_ratio);
        WheelData.getInstance().setGotwayVoltage(gotway_voltage);
        WheelData.getInstance().setAlarmsEnabled(alarms_enabled);
        WheelData.getInstance().setPreferences(
                alarm1Speed, alarm1Battery,
                alarm2Speed, alarm2Battery,
                alarm3Speed, alarm3Battery,
                current_peak_alarm,
                current_sustained_alarm,
                temperature_alarm,
                enableVoltageAlarm,
                disablePhoneVibrate);

        if (auto_log)
            MainActivityPermissionsDispatcher.acquireStoragePermissionWithPermissionCheck(this);

        if (log_location)
            MainActivityPermissionsDispatcher.acquireLocationPermissionWithPermissionCheck(this);

        WearService.setDataUpdateMinPeriod(SettingsUtil.isWatchPowerSavingMode(this) ? 1000 : 250);
        WearService.sendSync(this);

        updateScreen(true);
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void acquireStoragePermission() {
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void acquireLocationPermission() {
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void storagePermissionDenied() {
        SettingsUtil.setAutoLog(this, false);
        ((PreferencesFragment) getPreferencesFragment()).refreshVolatileSettings();
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void locationPermissionDenied() {
        SettingsUtil.setLogLocationEnabled(this, false);
        ((PreferencesFragment) getPreferencesFragment()).refreshVolatileSettings();
    }

    private void stopLoggingService() {
        if (LoggingService.isInstanceCreated())
            toggleLoggingService();
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void toggleLoggingService() {
        Intent dataLoggerServiceIntent = new Intent(getApplicationContext(), LoggingService.class);
        if (LoggingService.isInstanceCreated())
            stopService(dataLoggerServiceIntent);
        else
            startService(dataLoggerServiceIntent);
    }

    private void startWearService() {
        if (!WearService.isInstanceCreated())
            toggleWearService();
    }

    private void stopWearService() {
        if (WearService.isInstanceCreated())
            toggleWearService();
    }

    private void toggleWearService() {
        Intent intent = new Intent(getApplicationContext(), WearService.class);
        if (WearService.isInstanceCreated()) {
            WearService.sendMessage(this, Commons.PATH_CLOSE, "{}");
            stopService(intent);
        }
        else
            startService(intent);
    }

    private void startPebbleService() {
        if (!PebbleService.isInstanceCreated())
            togglePebbleService();
    }

    private void stopPebbleService() {
        if (PebbleService.isInstanceCreated())
            togglePebbleService();
    }

    private void togglePebbleService() {
        Intent intent = new Intent(getApplicationContext(), PebbleService.class);
        if (PebbleService.isInstanceCreated())
            stopService(intent);
        else
            startService(intent);
    }

    private void startSpeechService() {
        if (!SpeechService.isInstanceCreated())
            toggleSpeechService();
    }
    private void stopSpeechService() {
        if (SpeechService.isInstanceCreated())
            toggleSpeechService();
    }

    void toggleSpeechService() {
        Intent speechServiceIntent = new Intent(getApplicationContext(), SpeechService.class);
        if (SpeechService.isInstanceCreated())
            stopService(speechServiceIntent);
        else
            startService(speechServiceIntent);
    }

    private void stopLivemapService() {
        if (LivemapService.isInstanceCreated())
            toggleLivemapService();
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void toggleLivemapService() {
        Intent livemapServiceIntent = new Intent(getApplicationContext(), LivemapService.class);
        if (LivemapService.isInstanceCreated())
            stopService(livemapServiceIntent);
        else
            startService(livemapServiceIntent);
    }

    private void startBluetoothService() {
        Intent bluetoothServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        startService(bluetoothServiceIntent);
        bindService(bluetoothServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void toggleConnectToWheel() {
        sendBroadcast(new Intent(Constants.ACTION_REQUEST_CONNECTION_TOGGLE));
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void startScanActivity() {
        startActivityForResult(new Intent(MainActivity.this, ScanActivity.class), RESULT_DEVICE_SCAN_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.d("onActivityResult");
        switch (requestCode) {
            case GRAB_BUTTON_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    try {
                        FlicManager.getInstance(this, manager -> {
                            FlicButton button = manager.completeGrabButton(requestCode, resultCode, data);
                            if (button != null) {
                                button.registerListenForBroadcast(FlicBroadcastReceiverFlags.CLICK_OR_DOUBLE_CLICK_OR_HOLD);
                                Toast.makeText(MainActivity.this, R.string.flic_button_grabbed, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, R.string.flic_button_not_grabbed, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case RESULT_DEVICE_SCAN_REQUEST:
                if (resultCode == RESULT_OK) {
                    mDeviceAddress = data.getStringExtra("MAC");
                    Timber.d("Device selected = %s", mDeviceAddress);
                    String mDeviceName = data.getStringExtra("NAME");
                    Timber.d("Device selected = %s", mDeviceName);
                    mBluetoothLeService.setDeviceAddress(mDeviceAddress);
                    WheelData.getInstance().full_reset(SettingsUtil.getDontResetData(this));
                    WheelData.getInstance().setBtName(mDeviceName);
                    updateScreen(true);
                    setMenuIconStates();
                    mBluetoothLeService.close();
                    toggleConnectToWheel();
                }
                break;
            case RESULT_REQUEST_ENABLE_BT:
                if (mBluetoothAdapter.isEnabled())
                    startBluetoothService();
                else {
                    Toast.makeText(this, R.string.bluetooth_required, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case REQUEST_IMAGE_CAPTURE:
                if ((resultCode == RESULT_OK) && (!mImagePath.isEmpty()) && (LivemapService.isInstanceCreated())) {
                    uploadImageToEucWorld();
                }
                break;
        }
    }

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_BLUETOOTH_CONNECTION_STATE);
        intentFilter.addAction(Constants.ACTION_WHEEL_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_LOGGING_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_PEBBLE_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_SPEECH_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_LIVEMAP_SERVICE_TOGGLED);
        intentFilter.addAction(Constants.ACTION_LIVEMAP_STATUS);
        intentFilter.addAction(Constants.ACTION_LIVEMAP_LOCATION_UPDATED);
        intentFilter.addAction(Constants.ACTION_PREFERENCE_CHANGED);
		intentFilter.addAction(Constants.ACTION_WHEEL_SETTING_CHANGED);
		intentFilter.addAction(Constants.ACTION_WHEEL_TYPE_RECOGNIZED);	
		intentFilter.addAction(Constants.ACTION_ALARM_TRIGGERED);
        intentFilter.addAction(Constants.ACTION_ALARM_FINISHED);
        intentFilter.addAction(Constants.ACTION_WHEEL_CONNECTED);
        intentFilter.addAction(Constants.ACTION_WHEEL_CONNECTION_LOST);
        intentFilter.addAction(Constants.ACTION_WHEEL_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_SET_TOUR_HEADER_VISIBILITY);
        intentFilter.addAction(Constants.ACTION_TOUR_RELOAD_WEBVIEW);
        return intentFilter;
    }

    IAxisValueFormatter chartAxisValueFormatter = new IAxisValueFormatter() {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            if (value < xAxis_labels.size())
                return xAxis_labels.get((int) value);
            else
                return "";
        }
        // we don't draw numbers, so no decimal digits needed
        @Override
        public int getDecimalDigits() {  return 0; }
    };

    private Fragment getPreferencesFragment() {
        Fragment frag = getFragmentManager().findFragmentByTag(Constants.PREFERENCES_FRAGMENT_TAG);
        if (frag == null)
            frag = new PreferencesFragment();
        return frag;
    }

    private void shareLivemapUrl() {
        if (!LivemapService.getUrl().equals("")) {
            Intent share = new Intent(android.content.Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, R.string.link_livemap_subject);
            share.putExtra(Intent.EXTRA_TEXT, LivemapService.getUrl());

            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setData(Uri.parse(LivemapService.getUrl()));

            Intent chooserIntent = Intent.createChooser(share, getString(R.string.share_livemap));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{view});
            startActivity(chooserIntent);
        }
    }

    private File createImageFile() {
        String filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";
        File path = new File(Environment.getExternalStorageDirectory(), Constants.PICTURE_FOLDER_NAME);
        path.mkdirs();
        File image = new File(path, filename);
        mImagePath = image.getAbsolutePath();
        return image;
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void imageCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File imageFile = createImageFile();
            if (imageFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "net.lastowski.eucworld.fileprovider", imageFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                    intent.setClipData(ClipData.newRawUri("", photoURI));
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void setMainActivityLockBehaviour() {
        if (SettingsUtil.getShowWhenLocked(this))
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        else
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    public void updateLivemapUI() {
        Timber.d("Updating tour UI; service is in %d state", LivemapService.getStatus());
        switch (LivemapService.getStatus()) {
            case Commons.TourTrackingServiceStatus.DISCONNECTED:
                tourStartFinish.setImageResource(R.drawable.ic_play);
                if (LivemapService.getApiKey().isEmpty()) {
                    tourStartFinish.setEnabled(false);
                    tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_disabled));
                    tourStatus.setText(getString(R.string.livemap_signed_off));
                }
                else {
                    tourStartFinish.setEnabled(true);
                    tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_white));
                    tourStatus.setText(getString(R.string.livemap_offline));
                }
                tourStartFinish.getLayoutParams().width = tourStartFinish.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_large);

                tourPause.setEnabled(true);
                tourPause.setVisibility(View.GONE);

                tourAddPhoto.setVisibility(View.GONE);
                tourShare.setVisibility(View.GONE);
                break;
            case Commons.TourTrackingServiceStatus.CONNECTING:
                tourStatus.setText(getString(R.string.livemap_connecting));

                tourStartFinish.setEnabled(false);
                tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_disabled));
                tourStartFinish.setImageResource(R.drawable.ic_play);
                tourStartFinish.getLayoutParams().width = tourStartFinish.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_large);

                tourPause.setVisibility(View.GONE);

                tourAddPhoto.setVisibility(View.GONE);
                tourShare.setVisibility(View.GONE);
                break;
            case Commons.TourTrackingServiceStatus.WAITING_FOR_GPS:
                tourStatus.setText(getString(R.string.livemap_gps_wait));

                tourStartFinish.setEnabled(true);
                tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_down));
                tourStartFinish.setImageResource(R.drawable.ic_stop_large_white);
                tourStartFinish.getLayoutParams().width = tourStartFinish.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_large);

                tourPause.setVisibility(View.GONE);

                tourAddPhoto.setVisibility(View.GONE);
                tourShare.setVisibility(View.GONE);
                break;
            case Commons.TourTrackingServiceStatus.STARTED:
                tourStatus.setText(getString(R.string.livemap_live));

                tourStartFinish.setEnabled(true);
                tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_down));
                tourStartFinish.setImageResource(R.drawable.ic_stop);
                tourStartFinish.getLayoutParams().width = tourStartFinish.getLayoutParams().height = tourPause.getLayoutParams().height;

                tourPause.setEnabled(true);
                tourPause.setBackground(getResources().getDrawable(R.drawable.round_button_white));
                tourPause.setImageResource(R.drawable.ic_pause);
                tourPause.setVisibility(View.VISIBLE);

                tourAddPhoto.setVisibility(View.VISIBLE);
                tourShare.setVisibility(View.VISIBLE);
                break;
            case Commons.TourTrackingServiceStatus.PAUSING:
            case Commons.TourTrackingServiceStatus.RESUMING:
                tourStatus.setText(getString(R.string.livemap_live));

                tourStartFinish.setEnabled(true);
                tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_down));
                tourStartFinish.setImageResource(R.drawable.ic_stop);
                tourStartFinish.getLayoutParams().width = tourStartFinish.getLayoutParams().height = tourPause.getLayoutParams().height;

                tourPause.setEnabled(false);
                tourPause.setBackground(getResources().getDrawable(R.drawable.round_button_disabled));
                tourPause.setImageResource(R.drawable.ic_pause);
                tourPause.setVisibility(View.VISIBLE);

                tourAddPhoto.setVisibility(View.VISIBLE);
                tourShare.setVisibility(View.VISIBLE);
                break;
            case Commons.TourTrackingServiceStatus.PAUSED:
                tourStatus.setText(getString(R.string.livemap_live));

                tourStartFinish.setEnabled(true);
                tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_down));
                tourStartFinish.setImageResource(R.drawable.ic_stop);
                tourStartFinish.getLayoutParams().width = tourStartFinish.getLayoutParams().height = tourPause.getLayoutParams().height;

                tourPause.setEnabled(true);
                tourPause.setBackground(getResources().getDrawable(R.drawable.round_button_down));
                tourPause.setImageResource(R.drawable.ic_pause_white);
                tourPause.setVisibility(View.VISIBLE);

                tourAddPhoto.setVisibility(View.VISIBLE);
                tourShare.setVisibility(View.VISIBLE);
                break;
            case Commons.TourTrackingServiceStatus.DISCONNECTING:
                tourStatus.setText(getString(R.string.livemap_disconnecting));

                tourStartFinish.setEnabled(false);
                tourStartFinish.setBackground(getResources().getDrawable(R.drawable.round_button_disabled));
                tourStartFinish.setImageResource(R.drawable.ic_stop_large);
                tourStartFinish.getLayoutParams().width = tourStartFinish.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_large);

                tourPause.setVisibility(View.GONE);

                tourAddPhoto.setVisibility(View.GONE);
                tourShare.setVisibility(View.GONE);
                break;
        }
    }

    public void loadEucWorldApp() {
        String url = String.format(Locale.getDefault(), "%s/app.v2?appid=%s&appversion=%s&apikey=%s&locale=%s&tid=%d",
            Constants.getEucWorldUrl(),
            Constants.appId(this),
            BuildConfig.VERSION_NAME,
            LivemapService.getApiKey(),
            Locale.getDefault().toString(),
            System.currentTimeMillis());
        eucWorldWebView.loadUrl(url);
    }

    private void uploadImageToEucWorld() {
        final RequestParams requestParams = new RequestParams();

        // Set image location
        requestParams.put("llt", String.format(Locale.US, "%.7f", LivemapService.getInstance().getLatitude()));
        requestParams.put("lln", String.format(Locale.US, "%.7f", LivemapService.getInstance().getLongitude()));
        float[] latLon = new float[2];
        try {
            final ExifInterface exifInterface = new ExifInterface(mImagePath);
            if (exifInterface.getLatLong(latLon)) {
                // Update image location with EXIF values
                requestParams.put("llt", String.format(Locale.US, "%.7f", latLon[0]));
                requestParams.put("lln", String.format(Locale.US, "%.7f", latLon[1]));
            }
        }
        catch (IOException ignored) { }

        // Get source image size
        BitmapFactory.Options bopts = new BitmapFactory.Options();
        bopts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mImagePath, bopts);
        int width = bopts.outWidth;
        int height = bopts.outHeight;

        // Limit image size and quality
        final File img;
        try {
            // Compressor doesn't scale image if you define both setMaxWidth() and setMaxHeight()
            if (width > height)
                img = new Compressor(this)
                        .setMaxWidth(2000)
                        .setQuality(75)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .compressToFile(new File(mImagePath));
            else
                img = new Compressor(this)
                        .setMaxHeight(2000)
                        .setQuality(75)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .compressToFile(new File(mImagePath));
        }
        catch (IOException e) {
            Toast.makeText(this, R.string.livemap_image_preparation_error, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            requestParams.put("image", img);
            requestParams.put("a", SettingsUtil.getLivemapApiKey(this));
            requestParams.put("k", LivemapService.getInstance().getTourKey());
            requestParams.put("dt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date()));
            requestParams.put("ldt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date(LivemapService.getInstance().getLocationTime())));
            Toast.makeText(getApplicationContext(), R.string.livemap_image_uploading, Toast.LENGTH_LONG).show();
            HttpClient.post(Constants.getEucWorldUrl() + "/api/tour/upload", requestParams, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                    img.delete();
                    try {
                        int error = response.getInt("error");
                        if (error == 0)
                            Toast.makeText(getApplicationContext(), R.string.livemap_image_uploaded, Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getApplicationContext(), R.string.livemap_image_upload_error, Toast.LENGTH_LONG).show();
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    updateLivemapUI();
                }
                @Override
                public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    img.delete();
                    Toast.makeText(getApplicationContext(), R.string.livemap_image_upload_error, Toast.LENGTH_LONG).show();
                    updateLivemapUI();
                }
                @Override
                public void onFailure (int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                    img.delete();
                    Toast.makeText(getApplicationContext(), R.string.livemap_image_upload_error, Toast.LENGTH_LONG).show();
                    updateLivemapUI();
                }
            });
        }
        catch(FileNotFoundException e) { }
    }

    private void signInOnStartup() {
        Timber.i("Signing in with euc.world; sending request to server...");
        final String apikey = SettingsUtil.getLivemapApiKey(this);
        if (!apikey.isEmpty()) {
            final RequestParams requestParams = new RequestParams();
            requestParams.put("appid", Constants.appId(this));
            requestParams.put("appversion", BuildConfig.VERSION_NAME);
            requestParams.put("apikey", apikey);
            HttpClient.post(Constants.getEucWorldUrl() + "/api/signin", requestParams, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                    try {
                        int error = response.getInt("error");
                        switch (error) {
                            case 0:
                                Timber.i("Signed in with euc.world");
                                LivemapService.setApiKey(apikey);
                                LivemapService.setUserName(response.getJSONObject("data").getString("username"));
                                Toast.makeText(getApplicationContext(), String.format(Locale.US, getString(R.string.user_signed_in), LivemapService.getUserName()), Toast.LENGTH_LONG).show();
                                ((PreferencesFragment) getPreferencesFragment()).updateSigninSignupButtons();
                                updateLivemapUI();
                                break;
                            case 1:
                                Timber.e("Error signing in with euc.world; general server error (1)");
                                showToast(R.string.livemap_api_error_general, Toast.LENGTH_LONG);
                                break;
                            case 8:
                                Timber.e("Error signing in with euc.world; wrong credentials (8)");
                                showToast(R.string.livemap_api_error_invalid_api_key, Toast.LENGTH_LONG);
                                break;
                            case 9:
                                Timber.e("Error signing in with euc.world; account is inactive (9)");
                                showToast(R.string.livemap_api_error_accound_needs_activation, Toast.LENGTH_LONG);
                                break;
                            case 403:
                                Timber.e("Error signing in with euc.world; server access forbidden (403)");
                                showToast(R.string.livemap_api_error_forbidden, Toast.LENGTH_LONG);
                                break;
                            default:
                                Timber.e("Error signing in with euc.world; unknown error (%d)", error);
                                showToast(R.string.livemap_api_error_unknown, Toast.LENGTH_LONG);
                        }
                        loadEucWorldApp();
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    Timber.e("Error signing in with euc.world; connection failed: %s", errorResponse.toString());
                    loadEucWorldApp();
                }
                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                    Timber.e("Error signing in with euc.world; request timed out");
                    loadEucWorldApp();
                }
            });
        }
        else
            loadEucWorldApp();
    }

    private void showToast(int message_id, int duration) {
        Toast.makeText(this, message_id, duration).show();
    }

    private void updateFooterBackground() {
        Timber.d("Updating footer background");
        if (WheelData.getInstance().isConnected()) {
            footer.setBackgroundColor(getResources().getColor(R.color.primary));
            this.getWindow().setNavigationBarColor(getResources().getColor(R.color.primary));
        }
        else
        if (LivemapService.getLivemapGPS()) {
            footer.setBackgroundColor(getResources().getColor(R.color.gps));
            this.getWindow().setNavigationBarColor(getResources().getColor(R.color.gps));
        }
        else {
            footer.setBackgroundColor(getResources().getColor(R.color.disabled));
            this.getWindow().setNavigationBarColor(getResources().getColor(R.color.disabled));
        }
    }

    private int dpToPx(float dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    private void rearrangeScreen(Configuration config) {
        Timber.d("Rearranging screen");
        int newOrientation = config != null ? config.orientation : getResources().getConfiguration().orientation;
        RelativeLayout.LayoutParams buttons_lp = (RelativeLayout.LayoutParams) tourButtons.getLayoutParams();
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            tourButtons.setOrientation(LinearLayout.VERTICAL);
            tourButtons.setPadding(0, 0, dpToPx(24), 0);

            buttons_lp.removeRule(RelativeLayout.CENTER_HORIZONTAL);
            buttons_lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            buttons_lp.addRule(RelativeLayout.ALIGN_PARENT_END);
            buttons_lp.addRule(RelativeLayout.CENTER_VERTICAL);

            tourStartFinish.getLayoutParams().width = (tourPause.getVisibility() == View.GONE) ? getResources().getDimensionPixelSize(R.dimen.button_large) : getResources().getDimensionPixelSize(R.dimen.button_small);
            tourStartFinish.getLayoutParams().height = (tourPause.getVisibility() == View.GONE) ? getResources().getDimensionPixelSize(R.dimen.button_large) : getResources().getDimensionPixelSize(R.dimen.button_small);
            tourPause.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.button_small);
            tourPause.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_small);
            tourShare.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.button_small);
            tourShare.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_small);
            tourAddPhoto.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.button_small);
            tourAddPhoto.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_small);
        }
        else {
            tourButtons.setOrientation(LinearLayout.HORIZONTAL);
            tourButtons.setPadding(0, 0, 0, dpToPx(24));

            buttons_lp.removeRule(RelativeLayout.CENTER_VERTICAL);
            buttons_lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
            buttons_lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            buttons_lp.addRule(RelativeLayout.CENTER_HORIZONTAL);

            tourStartFinish.getLayoutParams().width = (tourPause.getVisibility() == View.GONE) ? getResources().getDimensionPixelSize(R.dimen.button_large) : getResources().getDimensionPixelSize(R.dimen.button_normal);
            tourStartFinish.getLayoutParams().height =(tourPause.getVisibility() == View.GONE) ? getResources().getDimensionPixelSize(R.dimen.button_large) : getResources().getDimensionPixelSize(R.dimen.button_normal);
            tourPause.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.button_normal);
            tourPause.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_normal);
            tourShare.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.button_normal);
            tourShare.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_normal);
            tourAddPhoto.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.button_normal);
            tourAddPhoto.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.button_normal);
        }
    }

    private class SpeedValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
           return String.format(Locale.getDefault(), useMi ? "%.0f\u2009mph" : "%.0f\u2009km/h", useMi ?  Value.toMiles(value) : value);
        }
        @Override
        public int getDecimalDigits() {
            return 0;
        };
    }

    private class CurrentValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return String.format(Locale.getDefault(), "%.1f\u2009A", value);
        }
        @Override
        public int getDecimalDigits() {
            return 1;
        };
    }

    private void performAction(int action) {
        Bundle options = new Bundle();
        switch (action) {
            case Commons.Action.HORN:
                int mode = SettingsUtil.getGaugeHornMode(this);
                options.putInt("mode", mode);
                if (mode == Commons.Action.HornMode.BLUETOOTH_AUDIO) {
                    if (SettingsUtil.getGaugeUseCustomHornSound(this))
                        options.putString("path", SettingsUtil.getGaugeCustomHornSoundPath(this));
                }
                gauge.setTapResultPositive(getString(R.string.action_1), R.drawable.ic_horn_24px);
                break;
            case Commons.Action.LIGHT:
                if (!BluetoothLeService.isDisconnected())
                    gauge.setTapResultPositive(getString(R.string.action_2), R.drawable.ic_wb_incandescent_24px);
                break;
            case Commons.Action.REQUEST_VOICE_MESSAGE:
                if (SpeechService.isInstanceCreated())
                    gauge.setTapResultPositive(getString(R.string.action_3), R.drawable.ic_speech_24px);
                break;
            case Commons.Action.DISMISS_VOICE_MESSAGE:
                if (SpeechService.isInstanceCreated())
                    gauge.setTapResultPositive(getString(R.string.action_4), R.drawable.ic_speech_24px);
                break;
        }
        Utilities.performAction(action, this, options);
    }

    private void bindValuesToViews() {
        livedataSpeed.setValue(Data.vehicleSpeed);
        livedataBatteryLevel.setValue(Data.vehicleBatteryLevel);
        livedataDistance.setValue(Data.vehicleDistance);
        livedataDistanceVehicle.setValue(Data.vehicleDistanceVehicle);
        livedataDistanceUser.setValue(Data.vehicleDistanceUser);
        livedataDuration.setValue(Data.vehicleDuration);
        livedataDurationRiding.setValue(Data.vehicleDurationRiding);
        livedataVoltage.setValue(Data.vehicleVoltage);
        livedataCurrent.setValue(Data.vehicleCurrent);
        livedataPower.setValue(Data.vehiclePower);
        livedataTemperature.setValue(Data.vehicleTemperature);
        livedataTemperatureMotor.setValue(Data.vehicleTemperatureMotor);
        livedataTilt.setValue(Data.vehicleTilt);
        livedataRoll.setValue(Data.vehicleRoll);
        livedataCoolingFanStatus.setValue(Data.vehicleCoolingFanStatus);
        livedataControlSensitivity.setValue(Data.vehicleControlSensitivity);
        livedataDistanceTotal.setValue(Data.vehicleDistanceTotal);
        livedataName.setValue(Data.vehicleName);
        livedataModel.setValue(Data.vehicleModel);
        livedataFirmwareVersion.setValue(Data.vehicleFirmwareVersion);
        livedataSerialNumber.setValue(Data.vehicleSerialNumber);

        statsSpeedAvg.setValue(Data.vehicleSpeedAvg);
        statsSpeedAvgRiding.setValue(Data.vehicleSpeedAvgRiding);
        statsSpeedMax.setValue(Data.vehicleSpeedMax);
        statsLoadMaxRegen.setValue(Data.vehicleLoadMaxRegen);
        statsLoadMax.setValue(Data.vehicleLoadMax);
        statsTemperatureMin.setValue(Data.vehicleTemperatureMin);
        statsTemperatureMax.setValue(Data.vehicleTemperatureMax);
        statsBatteryLevelMin.setValue(Data.vehicleBatteryLevelMin);
        statsBatteryLevelMax.setValue(Data.vehicleBatteryLevelMax);
        statsVoltageMin.setValue(Data.vehicleVoltageMin);
        statsVoltageMax.setValue(Data.vehicleVoltageMax);
        statsCurrentMin.setValue(Data.vehicleCurrentMin);
        statsCurrentAvg.setValue(Data.vehicleCurrentAvg);
        statsCurrentMax.setValue(Data.vehicleCurrentMax);
        statsPowerMin.setValue(Data.vehiclePowerMin);
        statsPowerAvg.setValue(Data.vehiclePowerAvg);
        statsPowerMax.setValue(Data.vehiclePowerMax);

        gauge.setBatteryLevel(Data.vehicleBatteryLevel);
        gauge.setBatteryLevelMax(Data.vehicleBatteryLevelMax);
        gauge.setBatteryLevelMin(Data.vehicleBatteryLevelMin);
        gauge.setCurrent(Data.vehicleCurrent);
        gauge.setDistance(Data.vehicleDistance);
        gauge.setDuration(Data.vehicleDuration);
        gauge.setDurationRiding(Data.vehicleDurationRiding);
        gauge.setLoad(Data.vehicleLoad);
        gauge.setLoadMax(Data.vehicleLoadMax);
        gauge.setLoadMaxRegen(Data.vehicleLoadMaxRegen);
        gauge.setSpeed(Data.vehicleSpeed);
        gauge.setSpeedAvg(Data.vehicleSpeedAvg);
        gauge.setSpeedAvgRiding(Data.vehicleSpeedAvgRiding);
        gauge.setSpeedMax(Data.vehicleSpeedMax);
        gauge.setTemperature(Data.vehicleTemperature);
        gauge.setTemperatureMax(Data.vehicleTemperatureMax);
        gauge.setTemperatureMin(Data.vehicleTemperatureMin);
        gauge.setVoltage(Data.vehicleVoltage);
        gauge.setGpsAltitude(Data.tourAltitude);
        gauge.setGpsBearing(Data.tourBearing);
        gauge.setGpsDistance(Data.tourDistance);
        gauge.setGpsDuration(Data.tourDuration);
        gauge.setGpsDurationRiding(Data.tourDurationRiding);
        gauge.setGpsSpeed(Data.tourSpeed);
        gauge.setGpsSpeedAvg(Data.tourSpeedAvg);
        gauge.setGpsSpeedAvgRiding(Data.tourSpeedAvgRiding);
        gauge.setGpsSpeedMax(Data.tourSpeedMax);

        gauge.setPhoneBatteryLevel(Data.phoneBatteryLevel);
        gauge.setWatchBatteryLevel(Data.watchBatteryLevel);
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void acquireReadExternalStoragePermission() { }

}