package net.lastowski.common.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import net.lastowski.common.R;
import net.lastowski.common.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

public class DashboardGauge extends View {

    float gaugeSpeedAngle                   = 240f;
    float gaugeBattTempAngle                = 80f;
    float gaugeLoadAngle                    = 50f;
    float gaugeSpeedWidth                   = 6f;
    float gaugeBattTempLoadWidth            = 3f;
    float gaugeSpeedMargin                  = 3f;
    float gaugeBattTempLoadMargin           = 1f;

    float tickSpeedLength                   = 1f;
    float tickBattTempLoadLength            = 0.75f;
    float tickSpeedWidth                    = 1.5f;
    float tickBattTempLoadWidth             = 1f;
    float tickSpeedMargin                   = 0f;
    float tickBattTempLoadMargin            = 1.5f;

    float subtickSpeedLength                = 0.1f;
    float subtickBattTempLoadLength         = 0.1f;
    float subtickSpeedWidth                 = 1f;
    float subtickBattTempLoadWidth          = 0.75f;

    float textDistanceHeight                = 7f;
    float textSpeedHeight                   = 35f;
    float textSpeedUnitHeight               = 7f;
    float textBattTempHeight                = 11f;
    float textTapActionHeight               = 10f;
    float drawableTapActionHeight           = 25f;

    float iconsHeight                       = 6f;
    float iconsY                            = 36f;
    float iconsBatteryX                     = 30f;
    float iconsBatteryY                     = 30f;

    float textTitleHeight                   = 12f;
    float textValueHeight                   = 25f;

    float textDistanceY                     = 29f;
    float textSpeedY                        = 60f;
    float textSpeedUnitY                    = 69f;
    float textBattTempX                     = 18f;
    float textBattTempY                     = 81f;
    float drawableTapActionY                = 60f;
    float textTapActionY                    = 65f;

    float gaugeMarginTopPortrait            = 0f;
    float gaugeMarginTopLandscape           = 7f;
    float infoMarginTopPortrait             = -7f;
    float infoCellMargin                    = 8f;
    float infoCellTitleValueMargin          = 4f;
    float infoTitleToValueHeightMaxRatio    = 0.8f;

    int tapActionNegativeColor              = 0xFFFFFFFF;
    int tapActionPositiveColor              = 0xFFFFFFFF;
    int tapActionNegativeBackgroundColor    = 0xFF770000;
    int tapActionPositiveBackgroundColor    = 0x00000000;
    int gaugeDotStrokeColor                 = 0xFF000000;
    int gaugeBackgroundColor                = 0xFF333333;
    int gaugeSpeedColor                     = 0xFFFF5722;
    int gaugeBatteryColor                   = 0xFF00cc00;
    int gaugeLoadColor                      = 0xFFcccc00;
    int gaugeLoadRegenColor                 = 0xFF3333ff;
    int gaugeTemperatureColor               = 0xFF33ccff;
    int textInactiveColor                   = 0xFF555555;
    int textTitleColor                      = 0xFF999999;
    int textValueColor                      = 0xFFFFFFFF;
    int textDistanceColor                   = 0xFFFFFFFF;
    int textSpeedColor                      = 0xFFFFFFFF;
    int textSpeedUnitColor                  = 0xFF999999;
    int textBattTempColor                   = 0xFFFFFFFF;
    int tickColor                           = 0xFF777777;
    int subtickColor                        = 0xFF777777;
    int warningColor                        = 0xFFFF9900;
    int alarmColor                          = 0xFFFF0000;

    private int speedRange;
    private boolean vehicleMode;
    private boolean watchMode;
    private boolean batterySaving;

    private Value speed;
    private Value speedMax;
    private Value speedAvg;
    private Value speedAvgRiding;
    private Value batteryLevel;
    private Value batteryLevelMin;
    private Value batteryLevelMax;
    private Value distance;
    private Value load;
    private Value loadMax;
    private Value loadMaxRegen;
    private Value temperature;
    private Value temperatureMax;
    private Value temperatureMin;
    private Value current;
    private Value voltage;
    private Value duration;
    private Value durationRiding;

    private Value gpsAltitude;
    private Value gpsBearing;
    private Value gpsDistance;
    private Value gpsDuration;
    private Value gpsDurationRiding;
    private Value gpsSpeed;
    private Value gpsSpeedAvg;
    private Value gpsSpeedAvgRiding;
    private Value gpsSpeedMax;

    private Value phoneBatteryLevel;
    private Value watchBatteryLevel;

    private boolean loadAlarm;
    private boolean speedAlarm;
    private boolean temperatureAlarm;
    private boolean batteryAlarm;
    private boolean batteryWarning;

    private OnGestureListener gestureListener;

    Context context;
    Paint paint;
    TextPaint textPaint;
    Typeface tfSpeed;
    Typeface tfBT;
    Timer timer;
    GestureDetector detector;
    RectF rect;
    Rect clipRect;
    Rect gaugeClipRect;
    Rect infoLeftClipRect;
    Rect infoRightClipRect;
    Handler handler;
    Drawable gpsIcon;
    Drawable gpsIconInactive;
    Drawable vehicleIcon;
    Drawable vehicleIconInactive;
    Drawable batteryWatch;
    Drawable batteryPhone;
    Drawable batteryWatchEmpty;
    Drawable batteryPhoneEmpty;
    Drawable batteryWatchInactive;
    Drawable batteryPhoneInactive;
    Drawable[] batteryBars = new Drawable[5];

    int tapAction;
    Drawable tapDrawable;
    String tapTitle;
    int tapTimer;
    float ratio = 1;

    float cellMargin;
    float cellWidth;
    float cellHeight;
    float cellTitleValueMargin;
    int cellTitleTextSize;
    int cellValueTextSize;
    boolean portrait;

    public interface OnGestureListener {
        void onSingleTap(MotionEvent e);
        void onDoubleTap(MotionEvent e);
        void onLongPress(MotionEvent e);
    }

    class GestureTap extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (gestureListener != null)
                gestureListener.onSingleTap(e);
            return true;
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (gestureListener != null)
                gestureListener.onDoubleTap(e);
            return true;
        }
        @Override
        public void onLongPress(MotionEvent e) {
            if (gestureListener != null)
                gestureListener.onLongPress(e);
        }
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distX = e2.getX() - e1.getX();
            float distY = -(e2.getY() - e1.getY());
            double angle = Math.toDegrees(Math.atan2(distY, distX));
            Timber.d("onFling: distX=%f, distY=%f, angle=%f", distX, distY, angle);
            if (angle > -120 && angle < -60)
                setVehicleMode(true);
            else
            if (angle > 60 && angle < 120)
                setVehicleMode(false);
            return true;
        }
    }

    public DashboardGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.DashboardGauge, 0, 0);
        try {
            gaugeSpeedAngle = a.getFloat(R.styleable.DashboardGauge_gaugeSpeedAngle, gaugeSpeedAngle);
            gaugeBattTempAngle = a.getFloat(R.styleable.DashboardGauge_gaugeBattTempAngle, gaugeBattTempAngle);
            gaugeLoadAngle = a.getFloat(R.styleable.DashboardGauge_gaugeLoadAngle, gaugeLoadAngle);
            gaugeSpeedWidth = a.getFloat(R.styleable.DashboardGauge_gaugeSpeedWidth, gaugeSpeedWidth);
            gaugeBattTempLoadWidth = a.getFloat(R.styleable.DashboardGauge_gaugeBattTempLoadWidth, gaugeBattTempLoadWidth);
            gaugeSpeedMargin = a.getFloat(R.styleable.DashboardGauge_gaugeSpeedMargin, gaugeSpeedMargin);
            gaugeBattTempLoadMargin = a.getFloat(R.styleable.DashboardGauge_gaugeBattTempLoadMargin, gaugeBattTempLoadMargin);

            tickSpeedLength = a.getFloat(R.styleable.DashboardGauge_tickSpeedLength, tickSpeedLength);
            tickBattTempLoadLength = a.getFloat(R.styleable.DashboardGauge_tickBattTempLoadLength, tickBattTempLoadLength);
            tickSpeedWidth = a.getFloat(R.styleable.DashboardGauge_tickSpeedWidth, tickSpeedWidth);
            tickBattTempLoadWidth = a.getFloat(R.styleable.DashboardGauge_tickBattTempLoadWidth, tickBattTempLoadWidth);
            tickSpeedMargin = a.getFloat(R.styleable.DashboardGauge_tickSpeedMargin, tickSpeedMargin);
            tickBattTempLoadMargin = a.getFloat(R.styleable.DashboardGauge_tickBattTempLoadMargin, tickBattTempLoadMargin);

            subtickSpeedLength = a.getFloat(R.styleable.DashboardGauge_subtickSpeedLength, subtickSpeedLength);
            subtickBattTempLoadLength = a.getFloat(R.styleable.DashboardGauge_subtickBattTempLoadLength, subtickBattTempLoadLength);
            subtickSpeedWidth = a.getFloat(R.styleable.DashboardGauge_subtickSpeedWidth, subtickSpeedWidth);
            subtickBattTempLoadWidth = a.getFloat(R.styleable.DashboardGauge_subtickBattTempLoadWidth, subtickBattTempLoadWidth);
            textDistanceHeight = a.getFloat(R.styleable.DashboardGauge_textDistanceHeight, textDistanceHeight);
            textSpeedHeight = a.getFloat(R.styleable.DashboardGauge_textSpeedHeight, textSpeedHeight);
            textTitleHeight = a.getFloat(R.styleable.DashboardGauge_textTitleHeight, textTitleHeight);
            textValueHeight = a.getFloat(R.styleable.DashboardGauge_textValueHeight, textValueHeight);
            textSpeedUnitHeight = a.getFloat(R.styleable.DashboardGauge_textSpeedUnitHeight, textSpeedUnitHeight);
            textBattTempHeight = a.getFloat(R.styleable.DashboardGauge_textBattTempHeight, textBattTempHeight);
            textTapActionHeight = a.getFloat(R.styleable.DashboardGauge_textTapActionHeight, textTapActionHeight);
            textDistanceY = a.getFloat(R.styleable.DashboardGauge_textDistanceY, textDistanceY);
            textSpeedY = a.getFloat(R.styleable.DashboardGauge_textSpeedY, textSpeedY);
            textSpeedUnitY = a.getFloat(R.styleable.DashboardGauge_textSpeedUnitY, textSpeedUnitY);
            textBattTempX = a.getFloat(R.styleable.DashboardGauge_textBattTempX, textBattTempX);
            textBattTempY = a.getFloat(R.styleable.DashboardGauge_textBattTempY, textBattTempY);
            textTapActionY = a.getFloat(R.styleable.DashboardGauge_textTapActionY, textTapActionY);

            drawableTapActionHeight = a.getFloat(R.styleable.DashboardGauge_drawableTapActionHeight, drawableTapActionHeight);
            drawableTapActionY = a.getFloat(R.styleable.DashboardGauge_drawableTapActionY, drawableTapActionY);

            iconsY = a.getFloat(R.styleable.DashboardGauge_iconsY, iconsY);
            iconsBatteryX = a.getFloat(R.styleable.DashboardGauge_iconsBatteryX, iconsBatteryX);
            iconsBatteryY = a.getFloat(R.styleable.DashboardGauge_iconsBatteryY, iconsBatteryY);
            iconsHeight = a.getFloat(R.styleable.DashboardGauge_iconsHeight, iconsHeight);

            gaugeMarginTopLandscape = a.getFloat(R.styleable.DashboardGauge_gaugeMarginTopLandscape, gaugeMarginTopLandscape);
            gaugeMarginTopPortrait = a.getFloat(R.styleable.DashboardGauge_gaugeMarginTopPortrait, gaugeMarginTopPortrait);
            infoMarginTopPortrait = a.getFloat(R.styleable.DashboardGauge_infoMarginTopPortrait, infoMarginTopPortrait);
            infoCellMargin = a.getFloat(R.styleable.DashboardGauge_infoCellMargin, infoCellMargin);
            infoCellTitleValueMargin = a.getFloat(R.styleable.DashboardGauge_infoCellTitleValueMargin, infoCellTitleValueMargin);
            infoTitleToValueHeightMaxRatio = a.getFloat(R.styleable.DashboardGauge_infoTitleToValueHeightMaxRatio, infoTitleToValueHeightMaxRatio);

            tapActionNegativeColor = a.getColor(R.styleable.DashboardGauge_tapActionNegativeColor, tapActionNegativeColor);
            tapActionPositiveColor = a.getColor(R.styleable.DashboardGauge_tapActionPositiveColor, tapActionPositiveColor);
            tapActionNegativeBackgroundColor = a.getColor(R.styleable.DashboardGauge_tapActionNegativeBackgroundColor, tapActionNegativeBackgroundColor);
            tapActionPositiveBackgroundColor = a.getColor(R.styleable.DashboardGauge_tapActionPositiveBackgroundColor, tapActionPositiveBackgroundColor);
            gaugeBackgroundColor = a.getColor(R.styleable.DashboardGauge_gaugeBackgroundColor, gaugeBackgroundColor);
            gaugeSpeedColor = a.getColor(R.styleable.DashboardGauge_gaugeSpeedColor, gaugeSpeedColor);
            gaugeBatteryColor = a.getColor(R.styleable.DashboardGauge_gaugeBatteryColor, gaugeBatteryColor);
            gaugeLoadColor = a.getColor(R.styleable.DashboardGauge_gaugeLoadColor, gaugeLoadColor);
            gaugeLoadRegenColor = a.getColor(R.styleable.DashboardGauge_gaugeLoadRegenColor, gaugeLoadRegenColor);
            gaugeTemperatureColor = a.getColor(R.styleable.DashboardGauge_gaugeTemperatureColor, gaugeTemperatureColor);
            textInactiveColor = a.getColor(R.styleable.DashboardGauge_textInactiveColor, textInactiveColor);
            textDistanceColor = a.getColor(R.styleable.DashboardGauge_textDistanceColor, textDistanceColor);
            textTitleColor = a.getColor(R.styleable.DashboardGauge_textTitleColor, textTitleColor);
            textValueColor = a.getColor(R.styleable.DashboardGauge_textValueColor, textValueColor);
            textSpeedColor = a.getColor(R.styleable.DashboardGauge_textSpeedColor, textSpeedColor);
            textSpeedUnitColor = a.getColor(R.styleable.DashboardGauge_textSpeedUnitColor, textSpeedUnitColor);
            textBattTempColor = a.getColor(R.styleable.DashboardGauge_textBattTempColor, textBattTempColor);
            tickColor = a.getColor(R.styleable.DashboardGauge_tickColor, tickColor);
            subtickColor = a.getColor(R.styleable.DashboardGauge_subtickColor, subtickColor);
            //warningColor = a.getColor(R.styleable.AbstractView_warningColor, warningColor);
            //alarmColor = a.getColor(R.styleable.AbstractView_alarmColor, alarmColor);
        }
        finally {
            a.recycle();
        }

        handler = new Handler();
        gestureListener = null;
        detector = new GestureDetector(context, new GestureTap());
        speedRange = 50;
        rect = new RectF();
        clipRect = new Rect();
        gaugeClipRect = new Rect();
        infoLeftClipRect = new Rect();
        infoRightClipRect = new Rect();

        textPaint = new TextPaint();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tfSpeed = Typeface.create("sans-serif-condensed-light", Typeface.NORMAL);
        tfBT = Typeface.create("sans-serif-condensed-light", Typeface.NORMAL);

        gpsIcon = getResources().getDrawable(R.drawable.ic_gps_24_36px, null);
        gpsIconInactive = getResources().getDrawable(R.drawable.ic_gps_inactive_24_36px, null);
        vehicleIcon = getResources().getDrawable(R.drawable.ic_euc_24_36px, null);
        vehicleIconInactive = getResources().getDrawable(R.drawable.ic_euc_inactive_24_36px, null);
        batteryWatch = getResources().getDrawable(R.drawable.ic_battery_watch_24_36px, null);
        batteryPhone = getResources().getDrawable(R.drawable.ic_battery_phone_24_36px, null);
        batteryWatchEmpty = getResources().getDrawable(R.drawable.ic_battery_watch_empty_24_36px, null);
        batteryPhoneEmpty = getResources().getDrawable(R.drawable.ic_battery_phone_empty_24_36px, null);
        batteryWatchInactive = getResources().getDrawable(R.drawable.ic_battery_watch_inactive_24_36px, null);
        batteryPhoneInactive = getResources().getDrawable(R.drawable.ic_battery_phone_inactive_24_36px, null);
        batteryBars[0] = getResources().getDrawable(R.drawable.ic_battery_bar_20_24_36px);
        batteryBars[1] = getResources().getDrawable(R.drawable.ic_battery_bar_40_24_36px);
        batteryBars[2] = getResources().getDrawable(R.drawable.ic_battery_bar_60_24_36px);
        batteryBars[3] = getResources().getDrawable(R.drawable.ic_battery_bar_80_24_36px);
        batteryBars[4] = getResources().getDrawable(R.drawable.ic_battery_bar_100_24_36px);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (tapTimer > 0) {
                            --tapTimer;
                            invalidate();
                        }
                    }
                });
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 40);

        if (isInEditMode()) {
            vehicleMode = true;
            batteryWarning = true;
            batteryAlarm = true;

            speed = new Value(Value.DEFAULT_VALIDITY).setAsSpeed(20f);
            batteryLevel = new Value(Value.DEFAULT_VALIDITY).setAsFloat(75f).setFormat("%.0f\u2009%%");
            batteryLevelMin = new Value(Value.DEFAULT_VALIDITY).setAsFloat(45f);
            batteryLevelMax = new Value(Value.DEFAULT_VALIDITY).setAsFloat(80f);
            load = new Value(Value.DEFAULT_VALIDITY).setAsFloat(33f);
            loadMax = new Value(Value.DEFAULT_VALIDITY).setAsFloat(80f);
            loadMaxRegen = new Value(Value.DEFAULT_VALIDITY).setAsFloat(20f);
            distance = new Value(Value.DEFAULT_VALIDITY).setAsDistance(36.9f).setFormat("%.2f").setSuffixImperial("\u2009mi").setSuffixMetric("\u2009km");
            temperature = new Value(Value.DEFAULT_VALIDITY).setAsTemperature(45f).setFormat("%.0f").setSuffixImperial("\u2009°F").setSuffixMetric("\u2009°C");
            temperatureMax = new Value(Value.DEFAULT_VALIDITY).setAsTemperature(60f);
            temperatureMin = new Value(Value.DEFAULT_VALIDITY).setAsTemperature(25f);
            speedMax = new Value(Value.DEFAULT_VALIDITY).setAsSpeed(45f).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h").setTitle("Top Speed");
            speedAvg = new Value(Value.DEFAULT_VALIDITY).setAsSpeed(12.5f).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h").setTitle("Avg Speed");
            speedAvgRiding = new Value(Value.DEFAULT_VALIDITY).setAsSpeed(15f).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h").setTitle("Avg Riding Speed");
            current = new Value(Value.DEFAULT_VALIDITY).setAsFloat(13.5f).setFormat("%.1f\u2009A").setTitle("Current");
            voltage = new Value(Value.DEFAULT_VALIDITY).setAsFloat(78.2f).setFormat("%.1f\u2009V").setTitle("Voltage");
            duration = new Value(Value.DEFAULT_VALIDITY).setAsDate(1357000).setFormat("HH:mm:ss").setTitle("Journey Time");
            durationRiding = new Value(Value.DEFAULT_VALIDITY).setAsDate(1234000).setFormat("HH:mm:ss").setTitle("Riding Time");

            gpsAltitude = new Value(Value.DEFAULT_VALIDITY).setAsAltitude(84f).setFormat("%.0f").setSuffixImperial("\u2009ft").setSuffixMetric("\u2009m").setTitle("Altitude");
            gpsBearing = new Value(Value.DEFAULT_VALIDITY).setAsFloat(265f).setFormat("%.0f\u2009°").setTitle("Bearing");
            gpsDistance = new Value(Value.DEFAULT_VALIDITY).setAsDistance(36.9f).setFormat("%.2f").setSuffixImperial("\u2009mi").setSuffixMetric("\u2009km");
            gpsDuration = new Value(Value.DEFAULT_VALIDITY).setAsDate(2357000).setFormat("HH:mm:ss").setTitle("Journey Time");
            gpsDurationRiding = new Value(Value.DEFAULT_VALIDITY).setAsDate(2234000).setFormat("HH:mm:ss").setTitle("Riding Time");
            gpsSpeed = new Value(Value.DEFAULT_VALIDITY).setAsSpeed(24f);
            gpsSpeedMax = new Value(Value.DEFAULT_VALIDITY).setAsSpeed(42f);
            gpsSpeedAvg = new Value(Value.DEFAULT_VALIDITY).setAsSpeed(16.5f).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h").setTitle("Avg Speed");
            gpsSpeedAvgRiding = new Value(Value.DEFAULT_VALIDITY).setAsSpeed(20f).setFormat("%.1f").setSuffixImperial("\u2009mph").setSuffixMetric("\u2009km/h").setTitle("Avg Riding Speed");

            phoneBatteryLevel = new Value(Value.DEFAULT_VALIDITY).setAsFloat(25f);
            watchBatteryLevel = new Value(Value.DEFAULT_VALIDITY).setAsFloat(100f);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        cellMargin = dpToPx(context, infoCellMargin);
        cellTitleValueMargin = dpToPx(context, infoCellTitleValueMargin);
        clipRect.set(0, 0, w - 1, h - 1);
        ratio = (h != 0) ? (float)w / h : 1;
        if (watchMode) {
            gaugeClipRect.set(0, 0, w, w);
        }
        else {
            if (h >= w) {
                // Portrait
                portrait = true;
                int y = (int) (h * (gaugeMarginTopPortrait / 100));
                gaugeClipRect.set(0, y, w - 1, w + y - 1);
                y += (int) r(infoMarginTopPortrait);
                infoLeftClipRect.set(0, w + y, w / 2 - 1, h - 1);
                infoRightClipRect.set(w / 2, w + y, w - 1, h - 1);
                cellWidth = infoLeftClipRect.width() - cellMargin * 1.5f;
            } else {
                // Landscape
                portrait = false;
                int x = (w - h) / 2;
                int y = (int) (h * (gaugeMarginTopLandscape / 100));
                gaugeClipRect.set(x, y, h + x - 1, h + y - 1);
                infoLeftClipRect.set(0, 0, gaugeClipRect.left - 1, h - 1);
                infoRightClipRect.set(gaugeClipRect.right + 1, 0, w - 1, h - 1);
                cellWidth = infoLeftClipRect.width() - cellMargin * 2f;
            }
            cellHeight = (infoLeftClipRect.height() - cellMargin * 4) / 3;

            ArrayList<Integer> titleHeights = new ArrayList<>();
            if (duration != null && !duration.getTitle().isEmpty())                     titleHeights.add(getTextSize(duration.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (speedAvg != null && !speedAvg.getTitle().isEmpty())                     titleHeights.add(getTextSize(speedAvg.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (voltage != null && !voltage.getTitle().isEmpty())                       titleHeights.add(getTextSize(voltage.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (speedMax != null && !speedMax.getTitle().isEmpty())                     titleHeights.add(getTextSize(speedMax.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (speedAvgRiding != null && !speedAvgRiding.getTitle().isEmpty())         titleHeights.add(getTextSize(speedAvgRiding.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (current != null && !current.getTitle().isEmpty())                       titleHeights.add(getTextSize(current.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (gpsAltitude != null && !gpsAltitude.getTitle().isEmpty())               titleHeights.add(getTextSize(gpsAltitude.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (gpsBearing != null && !gpsBearing.getTitle().isEmpty())                 titleHeights.add(getTextSize(gpsBearing.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (gpsDuration != null && !gpsDuration.getTitle().isEmpty())               titleHeights.add(getTextSize(gpsDuration.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (gpsSpeedAvg != null && !gpsSpeedAvg.getTitle().isEmpty())               titleHeights.add(getTextSize(gpsSpeedAvg.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (gpsDurationRiding != null && !gpsDurationRiding.getTitle().isEmpty())   titleHeights.add(getTextSize(gpsDurationRiding.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));
            if (gpsSpeedAvgRiding != null && !gpsSpeedAvgRiding.getTitle().isEmpty())   titleHeights.add(getTextSize(gpsSpeedAvgRiding.getTitle(), cellWidth, (cellHeight - cellTitleValueMargin) / 2, 100));

            cellTitleTextSize = Collections.min(titleHeights);
            cellValueTextSize = getTextSize("000000000", cellWidth, (cellHeight - cellTitleValueMargin) / 2, 200);

            if ((float)cellTitleTextSize / cellValueTextSize > infoTitleToValueHeightMaxRatio)
                cellTitleTextSize = (int) (cellValueTextSize * infoTitleToValueHeightMaxRatio);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        drawIcons(canvas);

        drawSpeedGauge(canvas);
        drawBatteryGauge(canvas);
        drawLoadGauge(canvas);
        drawTemperatureGauge(canvas);

        drawSpeedTicks(canvas);
        drawBatteryTicks(canvas);
        drawLoadTicks(canvas);
        drawTemperatureTicks(canvas);

        drawGaugeValues(canvas);
        if (!watchMode && (ratio < 0.9 || ratio > 1.1))
            drawInfo(canvas);

        drawTapAction(canvas);

    }

    public void setSpeedRange(int speedRange) {
        if (this.speedRange != speedRange) {
            this.speedRange = forceInRange(10, 80, speedRange);
            invalidate();
        }
    }

    public void setVehicleMode(boolean vehicleMode) {
        if (this.vehicleMode != vehicleMode) {
            this.vehicleMode = vehicleMode;
            invalidate();
        }
    }

    public void setWatchMode(boolean watchMode) {
        if (this.watchMode != watchMode) {
            this.watchMode = watchMode;
            invalidate();
        }
    }

    public void setBatterySaving(boolean batterySaving) {
        if (this.batterySaving != batterySaving) {
            this.batterySaving = batterySaving;
            invalidate();
        }
    }

    public void setSpeed(Value v) {
        if (speed != v) {
            if (speed != null)
                speed.removeView(this);
            speed = v;
            speed.addView(this);
            invalidate();
        }
    }

    public void setSpeedMax(Value v) {
        if (speedMax != v) {
            if (speedMax != null)
                speedMax.removeView(this);
            speedMax = v;
            speedMax.addView(this);
            invalidate();
        }
    }

    public void setSpeedAvg(Value v) {
        if (speedAvg != v) {
            if (speedAvg != null)
                speedAvg.removeView(this);
            speedAvg = v;
            speedAvg.addView(this);
            invalidate();
        }
    }

    public void setSpeedAvgRiding(Value v) {
        if (speedAvgRiding != v) {
            if (speedAvgRiding != null)
                speedAvgRiding.removeView(this);
            speedAvgRiding = v;
            speedAvgRiding.addView(this);
            invalidate();
        }
    }

    public void setBatteryLevel(Value v) {
        if (batteryLevel != v) {
            if (batteryLevel != null)
                batteryLevel.removeView(this);
            batteryLevel = v;
            batteryLevel.addView(this);
            invalidate();
        }
    }

    public void setBatteryLevelMin(Value v) {
        if (batteryLevelMin != v) {
            if (batteryLevelMin != null)
                batteryLevelMin.removeView(this);
            batteryLevelMin = v;
            batteryLevelMin.addView(this);
            invalidate();
        }
    }

    public void setBatteryLevelMax(Value v) {
        if (batteryLevelMax != v) {
            if (batteryLevelMax != null)
                batteryLevelMax.removeView(this);
            batteryLevelMax = v;
            batteryLevelMax.addView(this);
            invalidate();
        }
    }

    public void setDistance(Value v) {
        if (distance != v) {
            if (distance != null)
                distance.removeView(this);
            distance = v;
            distance.addView(this);
            invalidate();
        }
    }

    public void setLoad(Value v) {
        if (load != v) {
            if (load != null)
                load.removeView(this);
            load = v;
            load.addView(this);
            invalidate();
        }
    }

    public void setLoadMax(Value v) {
        if (loadMax != v) {
            if (loadMax != null)
                loadMax.removeView(this);
            loadMax = v;
            loadMax.addView(this);
            invalidate();
        }
    }

    public void setLoadMaxRegen(Value v) {
        if (loadMaxRegen != v) {
            if (loadMaxRegen != null)
                loadMaxRegen.removeView(this);
            loadMaxRegen = v;
            loadMaxRegen.addView(this);
            invalidate();
        }
    }

    public void setTemperature(Value v) {
        if (temperature != v) {
            if (temperature != null)
                temperature.removeView(this);
            temperature = v;
            temperature.addView(this);
            invalidate();
        }
    }

    public void setTemperatureMin(Value v) {
        if (temperatureMin != v) {
            if (temperatureMin != null)
                temperatureMin.removeView(this);
            temperatureMin = v;
            temperatureMin.addView(this);
            invalidate();
        }
    }

    public void setTemperatureMax(Value v) {
        if (temperatureMax != v) {
            if (temperatureMax != null)
                temperatureMax.removeView(this);
            temperatureMax = v;
            temperatureMax.addView(this);
            invalidate();
        }
    }

    public void setCurrent(Value v) {
        if (current != v) {
            if (current != null)
                current.removeView(this);
            current = v;
            current.addView(this);
            invalidate();
        }
    }

    public void setVoltage(Value v) {
        if (voltage != v) {
            if (voltage != null)
                voltage.removeView(this);
            voltage = v;
            voltage.addView(this);
            invalidate();
        }
    }

    public void setDuration(Value v) {
        if (duration != v) {
            if (duration != null)
                duration.removeView(this);
            duration = v;
            duration.addView(this);
            invalidate();
        }
    }

    public void setDurationRiding(Value v) {
        if (durationRiding != v) {
            if (durationRiding != null)
                durationRiding.removeView(this);
            durationRiding = v;
            durationRiding.addView(this);
            invalidate();
        }
    }

    public void setGpsAltitude(Value v) {
        if (gpsAltitude != v) {
            if (gpsAltitude != null)
                gpsAltitude.removeView(this);
            gpsAltitude = v;
            gpsAltitude.addView(this);
            invalidate();
        }
    }

    public void setGpsBearing(Value v) {
        if (gpsBearing != v) {
            if (gpsBearing != null)
                gpsBearing.removeView(this);
            gpsBearing = v;
            gpsBearing.addView(this);
            invalidate();
        }
    }

    public void setGpsDistance(Value v) {
        if (gpsDistance != v) {
            if (gpsDistance != null)
                gpsDistance.removeView(this);
            gpsDistance = v;
            gpsDistance.addView(this);
            invalidate();
        }
    }

    public void setGpsDuration(Value v) {
        if (gpsDuration != v) {
            if (gpsDuration != null)
                gpsDuration.removeView(this);
            gpsDuration = v;
            gpsDuration.addView(this);
            invalidate();
        }
    }

    public void setGpsDurationRiding(Value v) {
        if (gpsDurationRiding != v) {
            if (gpsDurationRiding != null)
                gpsDurationRiding.removeView(this);
            gpsDurationRiding = v;
            gpsDurationRiding.addView(this);
            invalidate();
        }
    }

    public void setGpsSpeed(Value v) {
        if (gpsSpeed != v) {
            if (gpsSpeed != null)
                gpsSpeed.removeView(this);
            gpsSpeed = v;
            gpsSpeed.addView(this);
            invalidate();
        }
    }

    public void setGpsSpeedAvg(Value v) {
        if (gpsSpeedAvg != v) {
            if (gpsSpeedAvg != null)
                gpsSpeedAvg.removeView(this);
            gpsSpeedAvg = v;
            gpsSpeedAvg.addView(this);
            invalidate();
        }
    }

    public void setGpsSpeedAvgRiding(Value v) {
        if (gpsSpeedAvgRiding != v) {
            if (gpsSpeedAvgRiding != null)
                gpsSpeedAvgRiding.removeView(this);
            gpsSpeedAvgRiding = v;
            gpsSpeedAvgRiding.addView(this);
            invalidate();
        }
    }

    public void setGpsSpeedMax(Value v) {
        if (gpsSpeedMax != v) {
            if (gpsSpeedMax != null)
                gpsSpeedMax.removeView(this);
            gpsSpeedMax = v;
            gpsSpeedMax.addView(this);
            invalidate();
        }
    }

    public void setPhoneBatteryLevel(Value v) {
        if (phoneBatteryLevel != v) {
            if (phoneBatteryLevel != null)
                phoneBatteryLevel.removeView(this);
            phoneBatteryLevel = v;
            phoneBatteryLevel.addView(this);
            invalidate();
        }
    }

    public void setWatchBatteryLevel(Value v) {
        if (watchBatteryLevel != v) {
            if (watchBatteryLevel != null)
                watchBatteryLevel.removeView(this);
            watchBatteryLevel = v;
            watchBatteryLevel.addView(this);
            invalidate();
        }
    }

    public void setLoadAlarm(boolean alarm) {
        if (loadAlarm != alarm) {
            loadAlarm = alarm;
            invalidate();
        }
    }

    public void setSpeedAlarm(boolean alarm) {
        if (speedAlarm != alarm) {
            speedAlarm = alarm;
            invalidate();
        }
    }

    public void setTemperatureAlarm(boolean alarm) {
        if (temperatureAlarm != alarm) {
            temperatureAlarm = alarm;
            invalidate();
        }
    }

    public void setBatteryAlarm(boolean alarm) {
        if (batteryAlarm != alarm) {
            batteryAlarm = alarm;
            invalidate();
        }
    }

    public void setBatteryWarning(boolean warning) {
        if (batteryWarning != warning) {
            batteryWarning = warning;
            invalidate();
        }
    }

    public void setTapResultPositive(String title, int resId) {
        try {
            tapDrawable = getResources().getDrawable(resId, null);
        }
        catch (Exception e) {
            tapDrawable = null;
            e.printStackTrace();
        }
        tapTitle = title;
        tapAction = 1;
        tapTimer = 50;
    }

    public void setTapResultNegative(String title, int resId) {
        try {
            tapDrawable = getResources().getDrawable(resId, null);
        }
        catch (Exception e) {
            tapDrawable = null;
            e.printStackTrace();
        }
        tapTitle = title;
        tapAction = 0;
        tapTimer = 50;
    }

    public void setOnGestureListener(OnGestureListener listener) { gestureListener = listener; };

    private void drawIcons(Canvas canvas) {
        Drawable icon;
        if (vehicleMode)
            icon = (speed != null && speed.isValid()) ? vehicleIcon : vehicleIconInactive;
        else
            icon = (gpsSpeed != null && gpsSpeed.isValid()) ? gpsIcon : gpsIconInactive;

        int w = icon.getIntrinsicWidth();
        int h = icon.getIntrinsicHeight();
        float ratio = (float)w / h;
        float marginTop;
        if (watchMode)
            marginTop = 0;
        else
            marginTop = (portrait ? gaugeMarginTopPortrait : gaugeMarginTopLandscape);

        scaleGaugeRect(0);
        icon.setBounds(
                (int) (rect.centerX() - r(iconsHeight * ratio / 2)),
                (int) r(marginTop + iconsY - iconsHeight / 2),
                (int) (rect.centerX() + r(iconsHeight * ratio / 2)),
                (int) r(marginTop + iconsY + iconsHeight / 2));
        icon.draw(canvas);

        if (phoneBatteryLevel != null && phoneBatteryLevel.isValid())
            drawBatteryIcon(canvas, batteryPhone, batteryPhoneEmpty, iconsBatteryX, phoneBatteryLevel.getAsInt());
        else
            drawBatteryIcon(canvas, batteryPhone, batteryPhoneInactive, iconsBatteryX, 0);

        if (watchBatteryLevel != null && watchBatteryLevel.isValid())
            drawBatteryIcon(canvas, batteryWatch, batteryWatchEmpty,100 - iconsBatteryX, watchBatteryLevel.getAsInt());
        else
            drawBatteryIcon(canvas, batteryWatch, batteryWatchInactive,100 - iconsBatteryX, 0);
    }

    private void drawBatteryIcon(Canvas canvas, Drawable icon, Drawable iconEmpty, float x, int level) {
        int w = icon.getIntrinsicWidth();
        int h = icon.getIntrinsicHeight();
        float ratio = (float)w / h;
        float marginTop;
        if (watchMode)
            marginTop = 0;
        else
            marginTop = (portrait ? gaugeMarginTopPortrait : gaugeMarginTopLandscape);

        scaleGaugeRect(0);

        int i = (forceInRange(0, 100, level) + 10) / 20 - 1;
        if (i > -1) {
            icon.setBounds(
                    (int) (rect.left + r(x) - r(iconsHeight * ratio / 2)),
                    (int) r(marginTop + iconsBatteryY - iconsHeight / 2),
                    (int) (rect.left + r(x) + r(iconsHeight * ratio / 2)),
                    (int) r(marginTop + iconsBatteryY + iconsHeight / 2));
            icon.draw(canvas);

            batteryBars[i].setBounds(
                    (int) (rect.left + r(x) - r(iconsHeight * ratio / 2)),
                    (int) r(marginTop + iconsBatteryY - iconsHeight / 2),
                    (int) (rect.left + r(x) + r(iconsHeight * ratio / 2)),
                    (int) r(marginTop + iconsBatteryY + iconsHeight / 2));
            batteryBars[i].draw(canvas);
        }
        else {
            iconEmpty.setBounds(
                    (int) (rect.left + r(x) - r(iconsHeight * ratio / 2)),
                    (int) r(marginTop + iconsBatteryY - iconsHeight / 2),
                    (int) (rect.left + r(x) + r(iconsHeight * ratio / 2)),
                    (int) r(marginTop + iconsBatteryY + iconsHeight / 2));
            iconEmpty.draw(canvas);
        }
    }

    private void drawSpeedTicks(Canvas canvas) {
        float d = gaugeSpeedAngle / speedRange;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(speedAlarm ? alarmColor : tickColor);
        canvas.save();
        scaleGaugeRect(r(tickSpeedMargin));
        canvas.rotate((360 - gaugeSpeedAngle) / 2 - 90, rect.centerX(), rect.centerY());
        for (int i = 0; i <= speedRange; ++i) {
            if ((i % 10) == 0) {
                paint.setStrokeWidth(r(tickSpeedWidth));
                canvas.drawLine(rect.left, rect.centerY(), rect.left + r(tickSpeedLength), rect.centerY(), paint);
            }
            else {
                paint.setStrokeWidth(r(subtickSpeedWidth));
                canvas.drawLine(rect.left + r(tickSpeedLength - subtickSpeedLength), rect.centerY(), rect.left + r(tickSpeedLength), rect.centerY(), paint);
            }
            canvas.rotate(d, rect.centerX(), rect.centerY());
        }
        canvas.restore();
    }

    private void drawBatteryTicks(Canvas canvas) {
        if (vehicleMode && ((batteryLevel != null && !batteryLevel.isEmpty()) || (batteryLevelMin != null && !batteryLevelMin.isEmpty()) || (batteryLevelMax != null && !batteryLevelMax.isEmpty()))) {
            float d = gaugeBattTempAngle / 100;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.save();
            scaleGaugeRect(r(tickSpeedMargin + tickSpeedLength + gaugeSpeedMargin + gaugeSpeedWidth + gaugeBattTempLoadMargin + gaugeBattTempLoadWidth + tickBattTempLoadMargin));
            canvas.rotate((360 - gaugeSpeedAngle) / 2 - 90, rect.centerX(), rect.centerY());
            for (int i = 0; i <= 100; ++i) {
                if ((i % 5) == 0) {
                    paint.setColor(batteryAlarm ? alarmColor : tickColor);
                    if ((i % 25) == 0) {
                        paint.setStrokeWidth(r(tickBattTempLoadWidth));
                        canvas.drawLine(rect.left, rect.centerY(), rect.left + r(tickBattTempLoadLength), rect.centerY(), paint);
                    }
                    else {
                        paint.setStrokeWidth(r(subtickBattTempLoadWidth));
                        canvas.drawLine(rect.left, rect.centerY(), rect.left + r(subtickBattTempLoadLength), rect.centerY(), paint);
                    }
                }
                canvas.rotate(d, rect.centerX(), rect.centerY());
            }
            canvas.restore();
        }
    }

    private void drawLoadTicks(Canvas canvas) {
        if (vehicleMode && ((load != null && !load.isEmpty()) || (loadMax != null && !loadMaxRegen.isEmpty()) || (loadMaxRegen != null && !loadMaxRegen.isEmpty()))) {
            float d = gaugeLoadAngle / 100;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.save();
            scaleGaugeRect(r(tickSpeedMargin + tickSpeedLength + gaugeSpeedMargin + gaugeSpeedWidth + gaugeBattTempLoadMargin + gaugeBattTempLoadWidth + tickBattTempLoadMargin));
            canvas.rotate(-gaugeLoadAngle / 2, rect.centerX(), rect.centerY());
            for (int i = 0; i <= 100; ++i) {
                if ((i % 10) == 0) {
                    paint.setColor(loadAlarm ? alarmColor : tickColor);
                    if ((i % 50) == 0) {
                        paint.setStrokeWidth(r(tickBattTempLoadWidth));
                        canvas.drawLine(rect.centerX(), rect.top, rect.centerX(), rect.top + r(tickBattTempLoadLength), paint);
                    }
                    else {
                        paint.setStrokeWidth(r(subtickBattTempLoadWidth));
                        canvas.drawLine(rect.centerX(), rect.top + r(tickBattTempLoadLength - subtickBattTempLoadLength), rect.centerX(), rect.top + r(tickBattTempLoadLength), paint);
                    }
                }
                canvas.rotate(d, rect.centerX(), rect.centerY());
            }
            canvas.restore();
        }
    }

    private void drawTemperatureTicks(Canvas canvas) {
        if (vehicleMode && ((temperature != null && !temperature.isEmpty()) || (temperatureMin != null && !temperatureMin.isEmpty()) || (temperatureMax != null && !temperatureMax.isEmpty()))) {
            float d = gaugeBattTempAngle / 60;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.save();
            scaleGaugeRect(r(tickSpeedMargin + tickSpeedLength + gaugeSpeedMargin + gaugeSpeedWidth + gaugeBattTempLoadMargin + gaugeBattTempLoadWidth + tickBattTempLoadMargin));
            canvas.rotate(90 - (360 - gaugeSpeedAngle) / 2, rect.centerX(), rect.centerY());
            for (int i = 0; i <= 60; ++i) {
                if ((i % 5) == 0) {
                    paint.setColor(temperatureAlarm ? alarmColor : tickColor);
                    if ((i % 20) == 0) {
                        paint.setStrokeWidth(r(tickBattTempLoadWidth));
                        canvas.drawLine(rect.right, rect.centerY(), rect.right - r(tickBattTempLoadLength), rect.centerY(), paint);
                    }
                    else {
                        paint.setStrokeWidth(r(subtickBattTempLoadWidth));
                        canvas.drawLine(rect.right, rect.centerY(), rect.right - r(subtickBattTempLoadLength), rect.centerY(), paint);
                    }
                }
                canvas.rotate(-d, rect.centerX(), rect.centerY());
            }
            canvas.restore();
        }
    }

    private void drawSpeedGauge(Canvas canvas) {
        Value s = vehicleMode ? speed : gpsSpeed;
        if (s != null) {
            scaleGaugeRect(r(tickSpeedMargin + tickSpeedLength + gaugeSpeedMargin + gaugeSpeedWidth / 2));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeWidth(r(gaugeSpeedWidth));
            paint.setColor(gaugeBackgroundColor);
            canvas.drawArc(rect, (360 - gaugeSpeedAngle) / 2 + 90, gaugeSpeedAngle, false, paint);
            float x = gaugeSpeedAngle * (forceInRange(0, speedRange, s.getAsFloat() / (float) speedRange));
            if (x >= 0.1f) {
                paint.setColor(speedAlarm ? alarmColor : setSaturation(gaugeSpeedColor, s.isValid() ? 1 : 0));
                canvas.drawArc(rect, (360 - gaugeSpeedAngle) / 2 + 90, x, false, paint);
            }
            drawSpeedDot(canvas, vehicleMode ? speedAvg : gpsSpeedAvg);
            drawSpeedDot(canvas, vehicleMode ? speedAvgRiding : gpsSpeedAvgRiding);
            drawSpeedDot(canvas, vehicleMode ? speedMax : gpsSpeedMax);
         }
    }

    private void drawSpeedDot(Canvas canvas, Value value) {
        if (value != null && !value.isEmpty() && value.getAsFloat() >= 5) {
            canvas.save();
            float v = gaugeSpeedAngle * (forceInRange(0, speedRange, value.getAsFloat()) / (float) speedRange);
            canvas.rotate((360 - gaugeSpeedAngle) / 2 - 90 + v, rect.centerX(), rect.centerY());
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(gaugeDotStrokeColor);
            canvas.drawCircle(rect.left - r(gaugeSpeedWidth * 0.5f), rect.centerY(), r(gaugeSpeedWidth * 0.66f), paint);
            paint.setColor(speedAlarm ? alarmColor : setSaturation(gaugeSpeedColor, value.isValid() ? 1 : 0));
            canvas.drawCircle(rect.left - r(gaugeSpeedWidth * 0.5f), rect.centerY(), r(gaugeSpeedWidth * 0.33f), paint);
            canvas.restore();
        }
    }

    private void drawBatteryGauge(Canvas canvas) {
        if (vehicleMode && ((batteryLevel != null && !batteryLevel.isEmpty()) || (batteryLevelMin != null && !batteryLevelMin.isEmpty()) || (batteryLevelMax != null && !batteryLevelMax.isEmpty()))) {
            scaleGaugeRect(r(tickSpeedMargin + tickSpeedLength + gaugeSpeedMargin + gaugeSpeedWidth + gaugeBattTempLoadMargin + gaugeBattTempLoadWidth / 2));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeWidth(r(gaugeBattTempLoadWidth));
            paint.setColor(gaugeBackgroundColor);
            canvas.drawArc(rect, (360 - gaugeSpeedAngle) / 2 + 90, gaugeBattTempAngle, false, paint);
            // Battery Level
            if (batteryLevel != null && !batteryLevel.isEmpty()) {
                float v = gaugeBattTempAngle * (forceInRange(0, 100, batteryLevel.getAsFloat()) / 100);
                if (v >= 0.1f) {
                    if (batteryAlarm)
                        paint.setColor(alarmColor);
                    else
                        paint.setColor(batteryWarning ? warningColor : setSaturation(gaugeBatteryColor, batteryLevel.isValid() ? 1 : 0));
                    canvas.drawArc(rect, (360 - gaugeSpeedAngle) / 2 + 90, v, false, paint);
                }
            }
            drawBatteryDot(canvas, batteryLevelMin);
            drawBatteryDot(canvas, batteryLevelMax);
        }
    }

    private void drawBatteryDot(Canvas canvas, Value value) {
        if (value != null && !value.isEmpty()) {
            canvas.save();
            float v = gaugeBattTempAngle * (forceInRange(0, 100, value.getAsFloat()) / 100);
            canvas.rotate((360 - gaugeSpeedAngle) / 2 - 90 + v, rect.centerX(), rect.centerY());
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(gaugeDotStrokeColor);
            canvas.drawCircle(rect.left + r(gaugeBattTempLoadWidth * 0.5f), rect.centerY(), r(gaugeBattTempLoadWidth * 0.66f), paint);
            paint.setColor(batteryAlarm ? alarmColor : setSaturation(gaugeBatteryColor, value.isValid() ? 1 : 0));
            canvas.drawCircle(rect.left + r(gaugeBattTempLoadWidth * 0.5f), rect.centerY(), r(gaugeBattTempLoadWidth * 0.33f), paint);
            canvas.restore();
        }
    }

    private void drawLoadGauge(Canvas canvas) {
        if (vehicleMode && ((load != null && !load.isEmpty()) || (loadMax != null && !loadMax.isEmpty()) || (loadMaxRegen != null && !loadMaxRegen.isEmpty()))) {
            scaleGaugeRect(r(tickSpeedMargin + tickSpeedLength + gaugeSpeedMargin + gaugeSpeedWidth + gaugeBattTempLoadMargin + gaugeBattTempLoadWidth / 2));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeWidth(r(gaugeBattTempLoadWidth));
            paint.setColor(gaugeBackgroundColor);
            canvas.drawArc(rect, 270 - gaugeLoadAngle / 2, gaugeLoadAngle, false, paint);
            //Load
            if (load != null && !load.isEmpty()) {
                float v = gaugeLoadAngle * (forceInRange(0, 100, Math.abs(load.getAsFloat())) / 100);
                if (v >= 0.1f) {
                    if (loadAlarm)
                        paint.setColor(alarmColor);
                    else
                        paint.setColor(setSaturation(load.getAsFloat() < 0 ? gaugeLoadRegenColor : gaugeLoadColor, load.isValid() ? 1 : 0));
                    canvas.drawArc(rect, 270 - gaugeLoadAngle / 2, v, false, paint);
                }
            }
            drawLoadDot(canvas, loadMaxRegen, gaugeLoadRegenColor);
            drawLoadDot(canvas, loadMax, gaugeLoadColor);
        }
    }

    private void drawLoadDot(Canvas canvas, Value value, int color) {
        if (value != null && !value.isEmpty() && value.getAsFloat() >= 5) {
            canvas.save();
            float v = gaugeLoadAngle * (forceInRange(0, 100, Math.abs(value.getAsFloat())) / 100);
            canvas.rotate(-gaugeLoadAngle / 2 + v, rect.centerX(), rect.centerY());
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(gaugeDotStrokeColor);
            canvas.drawCircle(rect.centerX(), rect.top + r(gaugeBattTempLoadWidth * 0.5f), r(gaugeBattTempLoadWidth * 0.66f), paint);
            paint.setColor(loadAlarm ? alarmColor : setSaturation(color, value.isValid() ? 1: 0));
            canvas.drawCircle(rect.centerX(), rect.top + r(gaugeBattTempLoadWidth * 0.5f), r(gaugeBattTempLoadWidth * 0.33f), paint);
            canvas.restore();
        }
    }

    private void drawTemperatureGauge(Canvas canvas) {
        if (vehicleMode && ((temperature != null && !temperature.isEmpty()) || (temperatureMin != null && !temperatureMin.isEmpty()) || (temperatureMax != null && !temperatureMax.isEmpty()))) {
            scaleGaugeRect(r(tickSpeedMargin + tickSpeedLength + gaugeSpeedMargin + gaugeSpeedWidth + gaugeBattTempLoadMargin + gaugeBattTempLoadWidth / 2));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeWidth(r(gaugeBattTempLoadWidth));
            paint.setColor(gaugeBackgroundColor);
            canvas.drawArc(rect, 90 - (360 - gaugeSpeedAngle) / 2, -gaugeBattTempAngle, false, paint);
            // Temperature
            if (temperature != null && !temperature.isEmpty()) {
                float v = gaugeBattTempAngle * ((forceInRange(20, 80, temperature.getAsFloat()) - 20) / 60);
                if (v >= 0.1f) {
                    paint.setColor(temperatureAlarm ? alarmColor : setSaturation(gaugeTemperatureColor, temperature.isValid() ? 1 : 0));
                    canvas.drawArc(rect, 90 - (360 - gaugeSpeedAngle) / 2, -v, false, paint);
                }
            }
            drawTemperatureDot(canvas, temperatureMin);
            drawTemperatureDot(canvas, temperatureMax);
        }
    }

    private void drawTemperatureDot(Canvas canvas, Value value) {
        if (value != null && !value.isEmpty() && value.getAsFloat() >= 20) {
            canvas.save();
            float v = gaugeBattTempAngle * ((forceInRange(20, 80, value.getAsFloat()) - 20) / 60);
            canvas.rotate(90 - (360 - gaugeSpeedAngle) / 2 - v, rect.centerX(), rect.centerY());
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(gaugeDotStrokeColor);
            canvas.drawCircle(rect.right - r(gaugeBattTempLoadWidth * 0.5f), rect.centerY(), r(gaugeBattTempLoadWidth * 0.66f), paint);
            paint.setColor(temperatureAlarm ? alarmColor : setSaturation(gaugeTemperatureColor, value.isValid() ? 1 : 0));
            canvas.drawCircle(rect.right - r(gaugeBattTempLoadWidth * 0.5f), rect.centerY(), r(gaugeBattTempLoadWidth * 0.33f), paint);
            canvas.restore();
        }
    }

    private void drawGaugeValues(Canvas canvas) {
        scaleGaugeRect(0);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        // Speed
        if (speed != null || (!vehicleMode && gpsSpeed != null)) {
            paint.setTypeface(tfSpeed);
            paint.setTextSize(r(textSpeedHeight));
            if (speedAlarm)
                paint.setColor(alarmColor);
            else {
                if (!vehicleMode && gpsSpeed != null)
                    paint.setColor(gpsSpeed.isValid() ? textSpeedColor : textInactiveColor);
                else
                    paint.setColor(speed.isValid() ? textSpeedColor : textInactiveColor);
            }
            canvas.drawText(String.valueOf((!vehicleMode && gpsSpeed != null) ? gpsSpeed.getAsInt() : speed.getAsInt()), rect.centerX(), rect.top + r(textSpeedY), paint);
            // Speed unit
            paint.setColor(speedAlarm ? alarmColor : textSpeedUnitColor);
            paint.setTextSize(r(textSpeedUnitHeight));
            if (!vehicleMode && gpsSpeed != null)
                canvas.drawText(gpsSpeed.getImperial() ? "mph" : "km/h", rect.centerX(), rect.top + r(textSpeedUnitY), paint);
            else
                canvas.drawText(speed.getImperial() ? "mph" : "km/h", rect.centerX(), rect.top + r(textSpeedUnitY), paint);
        }
        // Distance
        if (distance != null || (!vehicleMode && gpsDistance != null)) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(r(textDistanceHeight));
            if (!vehicleMode && gpsDistance != null)
                paint.setColor(gpsDistance.isValid() ? textSpeedColor : textInactiveColor);
            else
                paint.setColor(distance.isValid() ? textSpeedColor : textInactiveColor);
            canvas.drawText((!vehicleMode && gpsDistance != null) ? gpsDistance.getAsDistanceString() : distance.getAsDistanceString(), rect.centerX(), rect.top + r(textDistanceY), paint);
        }
        if (vehicleMode) {
            paint.setTextSize(r(textBattTempHeight));
            paint.setTypeface(tfBT);
            // Battery
            if (batteryLevel != null) {
                paint.setTextAlign(Paint.Align.LEFT);
                if (batteryAlarm)
                    paint.setColor(alarmColor);
                else if (batteryWarning)
                    paint.setColor(warningColor);
                else
                    paint.setColor(batteryLevel.isValid() ? textBattTempColor : textInactiveColor);
                canvas.drawText(batteryLevel.getAsString(), rect.left + r(textBattTempX), rect.top + r(textBattTempY), paint);
            }
            // Temperature
            if (temperature != null) {
                paint.setTextAlign(Paint.Align.RIGHT);
                if (temperatureAlarm)
                    paint.setColor(alarmColor);
                else
                    paint.setColor(temperature.isValid() ? textBattTempColor : textInactiveColor);
                canvas.drawText(temperature.getAsTemperatureString(), rect.right - r(textBattTempX), rect.top + r(textBattTempY), paint);
            }
        }
    }

    private void drawCell(Canvas canvas, Rect colRect, int col, int row, Value value) {
        if (value != null) {
            float cellTop = (cellHeight + cellMargin) * row;

            rect.set(colRect.left, colRect.top + cellMargin + cellTop, colRect.left + cellWidth, colRect.top + cellMargin + cellTop + cellHeight);
            rect.offset(col == 0 ? cellMargin : cellMargin / 2, 0);

            textPaint.setTypeface(tfBT);

            textPaint.setTextSize(cellTitleTextSize);
            textPaint.setColor(textTitleColor);
            drawText(canvas, textPaint, value.getTitle(), rect.width(), rect.left + rect.width() / 2, rect.top + cellHeight / 2 - cellTitleValueMargin / 2);

            textPaint.setTextSize(cellValueTextSize);
            textPaint.setColor(value.isValid() ? textValueColor : textInactiveColor);
            drawText(canvas, textPaint, value.getAsString(), rect.width(), rect.left + rect.width() / 2, rect.top + cellHeight / 2 + cellValueTextSize + cellTitleValueMargin / 2);
        }
    }

    private int getTextSize(String text, float maxWidth, float maxHeight, int maxTextSize) {
        TextPaint p = new TextPaint();
        p.setTypeface(tfBT);
        for (int i = 5; i < maxTextSize; i += 2) {
            p.setTextSize(i);
            StaticLayout l = new StaticLayout(text, p, (int)maxWidth, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
            int h = l.getHeight();
            if (h > maxHeight) return i - 2;
        }
        return maxTextSize;
    }

    private void drawText(Canvas canvas, TextPaint paint, String text, float maxWidth, float cx, float b) {
        StaticLayout l = new StaticLayout(text, paint, (int)maxWidth, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        canvas.save();
        canvas.translate(cx - maxWidth / 2, b - l.getHeight());
        l.draw(canvas);
        canvas.restore();
    }

    private void drawInfo(Canvas canvas) {
        if (vehicleMode) {
            drawCell(canvas, infoLeftClipRect, 0, 0, duration);        drawCell(canvas, infoRightClipRect, 1, 0, speedMax);
            drawCell(canvas, infoLeftClipRect, 0, 1, speedAvg);        drawCell(canvas, infoRightClipRect, 1, 1, speedAvgRiding);
            drawCell(canvas, infoLeftClipRect, 0, 2, voltage);         drawCell(canvas, infoRightClipRect, 1, 2, current);
        }
        else {
            drawCell(canvas, infoLeftClipRect, 0, 0, gpsDuration);     drawCell(canvas, infoRightClipRect, 1, 0, gpsDurationRiding);
            drawCell(canvas, infoLeftClipRect, 0, 1, gpsSpeedAvg);     drawCell(canvas, infoRightClipRect, 1, 1, gpsSpeedAvgRiding);
            drawCell(canvas, infoLeftClipRect, 0, 2, gpsAltitude);     drawCell(canvas, infoRightClipRect, 1, 2, gpsBearing);
        }
    }

    private void drawTapAction(Canvas canvas) {
        if (tapTimer > 0) {
            int a;
            if (tapTimer > 46) {
                a = (50 - tapTimer) * 50;
            }
            else
            if (tapTimer > 26) {
                a = 255;
            }
            else {
                a = (tapTimer - 1) * 10;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(tapAction > 0 ? tapActionPositiveBackgroundColor : tapActionNegativeBackgroundColor);
            paint.setAlpha(a);
            canvas.drawRect(clipRect, paint);

            // Ikona

            scaleGaugeRect(0);
            if (tapDrawable != null) {
                int w = tapDrawable.getIntrinsicWidth();
                int h = tapDrawable.getIntrinsicHeight();
                float ratio = (float)w / h;

                tapDrawable.setBounds((int) (rect.centerX() - r(drawableTapActionHeight * ratio / 2)), (int) r(drawableTapActionY - drawableTapActionHeight), (int) (rect.centerX() + r(drawableTapActionHeight * ratio / 2)), (int) r(drawableTapActionY));
                tapDrawable.setAlpha(a);
                tapDrawable.draw(canvas);
            }
            if (tapTitle != null && !tapTitle.isEmpty()) {
                textPaint.setTypeface(tfSpeed);
                textPaint.setAntiAlias(true);
                textPaint.setColor(tapAction > 0 ? tapActionPositiveColor : tapActionNegativeColor);
                textPaint.setAlpha(a);
                textPaint.setTextSize(r(textTapActionHeight));

                StaticLayout tl;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder builder = StaticLayout.Builder.obtain(tapTitle, 0, tapTitle.length(), textPaint, canvas.getWidth())
                            .setAlignment(Layout.Alignment.ALIGN_CENTER)
                            .setLineSpacing(0, 1f)
                            .setIncludePad(false);
                    tl = builder.build();
                }
                else
                    tl = new StaticLayout(tapTitle, textPaint, canvas.getWidth(), Layout.Alignment.ALIGN_CENTER, 1f, 0, false);
                canvas.save();
                canvas.translate(0, r(textTapActionY));
                tl.draw(canvas);
                canvas.restore();
            }
        }
    }

    private float r(float ref, float size) {
        return ref * (size / 100);
    }

    private float r(float size) {
        return gaugeClipRect.width() * (size / 100);
    }

    private void scaleGaugeRect(float v) {
        rect.set(gaugeClipRect.left + v, gaugeClipRect.top + v, gaugeClipRect.right - v, gaugeClipRect.bottom - v);
    }

    private float forceInRange(float min, float max, float v) {
        if (v > max) return max;
        else
        if (v < min) return min;
        else         return v;
    }

    private int forceInRange(int min, int max, int v) {
        if (v > max) return max;
        else
        if (v < min) return min;
        else         return v;
    }

    private int dpToPx(Context context, float dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }

    private int setSaturation(int color, float saturation) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = saturation;
        return Color.HSVToColor(hsv);
    }

}
