package com.pavelsikun.seekbarpreference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Pavel Sikun on 28.05.16.
 */

class PreferenceControllerDelegate implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private final String TAG = getClass().getSimpleName();

    private static final int DEFAULT_CURRENT_VALUE = 50;
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_INTERVAL = 1;
    private static final boolean DEFAULT_IMPERIAL = false;
    private static final boolean DEFAULT_DIALOG_ENABLED = true;
    private static final boolean DEFAULT_IS_ENABLED = true;

    private static final int DEFAULT_DIALOG_STYLE = R.style.MSB_Dialog_Default;

    private int maxValue;
    private int minValue;
    private int interval;
    private int currentValue;
    private String type;
    private String measurementUnit;
    private String measurementUnitImperial;
    private String title;
    private boolean dialogEnabled;
    private boolean imperial;

    private int dialogStyle;

    private TextView valueView;
    private SeekBar seekBarView;
    private TextView measurementView;
    private LinearLayout valueHolderView;
    private FrameLayout bottomLineView;

    //view stuff
    private TextView titleView, summaryView;
    private String summary;
    private boolean isEnabled;

    //controller stuff
    private boolean isView = false;
    private Context context;
    private ViewStateListener viewStateListener;
    private PersistValueListener persistValueListener;
    private ChangeValueListener changeValueListener;

    interface ViewStateListener {
        boolean isEnabled();
        void setEnabled(boolean enabled);
    }

    PreferenceControllerDelegate(Context context, Boolean isView, String title) {
        this.context = context;
        this.isView = isView;
        if (!isView)
            this.title = title;
    }

    void setPersistValueListener(PersistValueListener persistValueListener) {
        this.persistValueListener = persistValueListener;
    }

    void setViewStateListener(ViewStateListener viewStateListener) {
        this.viewStateListener = viewStateListener;
    }

    void setChangeValueListener(ChangeValueListener changeValueListener) {
        this.changeValueListener = changeValueListener;
    }

    void loadValuesFromXml(AttributeSet attrs) {
        if(attrs == null) {
            currentValue = DEFAULT_CURRENT_VALUE;
            minValue = DEFAULT_MIN_VALUE;
            maxValue = DEFAULT_MAX_VALUE;
            interval = DEFAULT_INTERVAL;
            dialogEnabled = DEFAULT_DIALOG_ENABLED;
            isEnabled = DEFAULT_IS_ENABLED;
        }
        else {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
            try {
                minValue = a.getInt(R.styleable.SeekBarPreference_msbp_minValue, DEFAULT_MIN_VALUE);
                interval = a.getInt(R.styleable.SeekBarPreference_msbp_interval, DEFAULT_INTERVAL);
                maxValue = a.getInt(R.styleable.SeekBarPreference_msbp_maxValue, DEFAULT_MAX_VALUE);
                //int saved_maxValue = a.getInt(R.styleable.SeekBarPreference_msbp_maxValue, DEFAULT_MAX_VALUE);
                //maxValue = (saved_maxValue - minValue) / interval;
                //maxValue = saved_maxValue;
                dialogEnabled = a.getBoolean(R.styleable.SeekBarPreference_msbp_dialogEnabled, DEFAULT_DIALOG_ENABLED);
                imperial = a.getBoolean(R.styleable.SeekBarPreference_msbp_imperial, DEFAULT_IMPERIAL);
                type = a.getString(R.styleable.SeekBarPreference_msbp_type);
                measurementUnit = a.getString(R.styleable.SeekBarPreference_msbp_measurementUnit);
                measurementUnitImperial = a.getString(R.styleable.SeekBarPreference_msbp_measurementUnitImperial);
                currentValue = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "defaultValue", DEFAULT_CURRENT_VALUE);

//                TODO make it work:
//                dialogStyle = a.getInt(R.styleable.SeekBarPreference_msbp_interval, DEFAULT_DIALOG_STYLE);

                dialogStyle = DEFAULT_DIALOG_STYLE;

                if(isView) {
                    title = a.getString(R.styleable.SeekBarPreference_msbp_view_title);
                    summary = a.getString(R.styleable.SeekBarPreference_msbp_view_summary);
                    currentValue = a.getInt(R.styleable.SeekBarPreference_msbp_view_defaultValue, DEFAULT_CURRENT_VALUE);
                    isEnabled = a.getBoolean(R.styleable.SeekBarPreference_msbp_view_enabled, DEFAULT_IS_ENABLED);
                }
            }
            finally {
                a.recycle();
            }
        }
    }


    void onBind(View view) {

        if(isView) {
            titleView = (TextView) view.findViewById(android.R.id.title);
            summaryView = (TextView) view.findViewById(android.R.id.summary);

            titleView.setText(title);
            summaryView.setText(summary);
        }

        view.setClickable(false);

        seekBarView = (SeekBar) view.findViewById(R.id.seekbar);
        measurementView = (TextView) view.findViewById(R.id.measurement_unit);
        valueView = (TextView) view.findViewById(R.id.seekbar_value);

        setMaxValue(maxValue);
        seekBarView.setOnSeekBarChangeListener(this);

        measurementView.setText(imperial ? measurementUnitImperial : measurementUnit);

        setCurrentValue(currentValue);
        valueView.setText(valueToString(currentValue));

        bottomLineView = (FrameLayout) view.findViewById(R.id.bottom_line);
        valueHolderView = (LinearLayout) view.findViewById(R.id.value_holder);

        setDialogEnabled(dialogEnabled);
        setEnabled(isEnabled(), true);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = minValue + (progress * interval);

        if (changeValueListener != null) {
            if (!changeValueListener.onChange(newValue)) {
                return;
            }
        }
        currentValue = newValue;
        valueView.setText(valueToString(currentValue));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setCurrentValue(currentValue);
    }

    @Override
    public void onClick(final View v) {
        new CustomValueDialog(context, dialogStyle, minValue, maxValue, currentValue, title, type, imperial)
                .setPersistValueListener(new PersistValueListener() {
                    @Override
                    public boolean persistInt(int value) {
                        setCurrentValue(value);
                        seekBarView.setOnSeekBarChangeListener(null);
                        seekBarView.setProgress(currentValue - minValue);
                        seekBarView.setOnSeekBarChangeListener(PreferenceControllerDelegate.this);
                        valueView.setText(valueToString(currentValue));
                        return true;
                    }
                })
                .show();
    }


    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
        if(titleView != null) {
            titleView.setText(title);
        }
    }

    String getSummary() {
        return summary;
    }

    void setSummary(String summary) {
        this.summary = summary;
        if(seekBarView != null) {
            summaryView.setText(summary);
        }
    }

    boolean isEnabled() {
        if(!isView && viewStateListener != null) {
            return viewStateListener.isEnabled();
        }
        else return isEnabled;
    }

    void setEnabled(boolean enabled, boolean viewsOnly) {
        Log.d(TAG, "setEnabled = " + enabled);
        isEnabled = enabled;

        if(viewStateListener != null && !viewsOnly) {
            viewStateListener.setEnabled(enabled);
        }

        if(seekBarView != null) { //theoretically might not always work
            Log.d(TAG, "view is disabled!");
            seekBarView.setEnabled(enabled);
            valueView.setEnabled(enabled);
            valueHolderView.setClickable(enabled);
            valueHolderView.setEnabled(enabled);

            measurementView.setEnabled(enabled);
            bottomLineView.setEnabled(enabled);

            if(isView) {
                titleView.setEnabled(enabled);
                summaryView.setEnabled(enabled);
            }
        }

    }

    void setEnabled(boolean enabled) {
        setEnabled(enabled, false);
    }

    int getMaxValue() {
        return maxValue;
    }

    void setMaxValue(int maxValue) {
        this.maxValue = maxValue;

        if (seekBarView != null) {
            seekBarView.setMax(maxValue - minValue);
            /*
            if (minValue <= 0 && maxValue >= 0) {
                seekBarView.setMax(maxValue - minValue);
            }
            else {
                seekBarView.setMax(maxValue);
            }
             */
            seekBarView.setProgress(currentValue - minValue);
        }
    }

    int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
        setMaxValue(maxValue);
    }

    boolean getImperial() { return imperial; }

    void setImperial(boolean imperial) {
        this.imperial = imperial;
        if(measurementView != null)
            measurementView.setText(imperial ? measurementUnitImperial :  measurementUnit);
        if (valueView != null)
            valueView.setText(valueToString(currentValue));
    }

    int getInterval() {
        return interval;
    }

    void setInterval(int interval) {
        this.interval = interval;
    }

    int getCurrentValue() {
        return currentValue;
    }

    void setCurrentValue(int value) {
        if(value < minValue) value = minValue;
        if(value > maxValue) value = maxValue;

        if (changeValueListener != null) {
            if (!changeValueListener.onChange(value)) {
                return;
            }
        }
        currentValue = value;
        if(seekBarView != null)
            seekBarView.setProgress(currentValue - minValue);

        if(persistValueListener != null) {
            persistValueListener.persistInt(value);
        }
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public String getMeasurementUnitImperial() {
        return measurementUnitImperial;
    }

    void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
        if(measurementView != null)
            measurementView.setText(imperial ? measurementUnitImperial :  measurementUnit);
    }

    void setMeasurementUnitImperial(String measurementUnit) {
        this.measurementUnitImperial = measurementUnit;
        if(measurementView != null)
            measurementView.setText(imperial ? measurementUnitImperial :  measurementUnit);
    }

    boolean isDialogEnabled() {
        return dialogEnabled;
    }

    void setDialogEnabled(boolean dialogEnabled) {
        this.dialogEnabled = dialogEnabled;

        if(valueHolderView != null && bottomLineView != null) {
            valueHolderView.setOnClickListener(dialogEnabled ? this : null);
            valueHolderView.setClickable(dialogEnabled);
            bottomLineView.setVisibility(dialogEnabled ? View.VISIBLE : View.INVISIBLE);
        }
    }

    void setDialogStyle(int dialogStyle) {
        this.dialogStyle = dialogStyle;
    }

    private String valueToString(int value) {
        if (type != null) {
            switch (type) {
                case "speed":
                    return String.valueOf(Math.round(imperial ? toMiles(value) : value));
                case "temperature":
                    return String.valueOf(Math.round(imperial ? toFahrenheit(value) : value));
            }
        }
        return String.valueOf(value);
    }

    public static float toMiles(float km) { return km * 0.621371192f; }
    public static float fromMiles(float mi) { return mi * 1.609344f; }
    public static float toFahrenheit(float c) { return c * 1.8f + 32; }
    public static float fromFahrenheit(float f) { return (f - 32) / 1.8f; }

}
