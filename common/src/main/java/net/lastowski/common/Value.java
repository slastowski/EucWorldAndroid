package net.lastowski.common;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.view.View;

import net.lastowski.common.view.ValueView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Value {

    public static final int INFINITE_VALIDITY = 0;
    public static final int DEFAULT_VALIDITY = 5000;
    public enum ValueType {
        STRING,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        TEMPERATURE,
        SPEED,
        DISTANCE,
        DATE,
        ALTITUDE;
    }

    private String suffixImperial;
    private String suffixMetric;
    private String timeZone;
    private String emptyText = "—";
    private String invalidText;
    private String title;
    private String wki;
    private String format;
    private String asString;
    private long asLong;
    private double asDouble;
    private double multiplier = 1;
    private long age;
    private long validity;
    private boolean imperial;
    private boolean valid;
    private ValueType type;
    private ArrayList<View> views;

    public Value(long validity) { this(validity, null, null, null); }
    public Value(long validity, ValueType type) { this(validity, type, null, null); }
    public Value(long validity, ValueType type, String format) { this(validity, type, format, null); }

    public Value(long validity, ValueType type, String format, String wki) {
        views = new ArrayList<>();
        this.validity = validity;
        this.type = type;
        this.wki = wki;
        this.format = format;
        age = 0;
        valid = false;
    }

    public Value(String value, long validity) {
        this.validity = validity;
        asString = value;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.STRING;
    }

    public Value(int value, long validity) {
        this.validity = validity;
        asLong = value;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.INT;
    }

    public Value(long value, long validity) {
        this.validity = validity;
        asLong = value;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.LONG;
    }

    public Value(float value, long validity) {
        this.validity = validity;
        asDouble = value;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.FLOAT;
    }

    public Value(double value, long validity) {
        this.validity = validity;
        asDouble = value;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.DOUBLE;
    }

    public Value setAsString(String v) {
        this.asString = v;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.STRING;
        invalidateViews();
        return this;
    }

    public Value setAsInt(int v) {
        this.asLong = v;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.INT;
        invalidateViews();
        return this;
    }

    public Value setAsLong(long v) {
        this.asLong = v;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.LONG;
        invalidateViews();
        return this;
    }

    public Value setAsDate(long v) {
        this.asLong = v;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.DATE;
        invalidateViews();
        return this;
    }

    public Value setAsFloat(float v) {
        this.asDouble = v;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.FLOAT;
        invalidateViews();
        return this;
    }

    public Value setAsDouble(double v) {
        this.asDouble = v;
        age = System.currentTimeMillis();
        valid = true;
        type = ValueType.DOUBLE;
        invalidateViews();
        return this;
    }

    public Value setAsSpeed(float v) {
        return setAsDoubleType(v, ValueType.SPEED);
    }

    public Value setAsSpeed(double v) {
        return setAsDoubleType(v, ValueType.SPEED);
    }

    public Value setAsDistance(float v) {
        return setAsDoubleType(v, ValueType.DISTANCE);
    }

    public Value setAsDistance(double v) {
        return setAsDoubleType(v, ValueType.DISTANCE);
    }

    public Value setAsTemperature(float v) {
        return setAsDoubleType(v, ValueType.TEMPERATURE);
    }

    public Value setAsTemperature(double v) {
        return setAsDoubleType(v, ValueType.TEMPERATURE);
    }

    public Value setAsAltitude(double v) {
        return setAsDoubleType(v, ValueType.ALTITUDE);
    }

    private Value setAsDoubleType(double v, ValueType t) {
        this.asDouble = v;
        age = System.currentTimeMillis();
        valid = true;
        type = t;
        invalidateViews();
        return this;
    }

    public Value set(Value v) {
        this.type = v.type;
        this.asDouble = v.asDouble;
        this.asLong = v.asLong;
        this.asString = v.asString;
        this.age = v.age;
        this.valid = v.valid;
        this.validity = v.validity;
        invalidateViews();
        return this;
    }

    public String getAsString() {
        if (isEmpty() && emptyText != null) return emptyText;
        if (!isValid() && invalidText != null) return invalidText;

        if (type != null) {
            switch (type) {
                case STRING:        return asString;
                case INT:
                case LONG:          return String.format(Locale.getDefault(), (format != null && !format.isEmpty()) ? format : "%d", asLong * (long)multiplier);
                case FLOAT:
                case DOUBLE:        return String.format(Locale.getDefault(), (format != null && !format.isEmpty()) ? format : "%f", asDouble  * multiplier);
                case SPEED:         return getAsSpeedString();
                case DISTANCE:      return getAsDistanceString();
                case TEMPERATURE:   return getAsTemperatureString();
                case DATE:          return getAsDateString();
                case ALTITUDE:      return getAsAltitudeString();
            }
        }
        return "";
    }

    public int getAsInt() {
        if (type != null) {
            switch (type) {
                case STRING:        return Integer.parseInt(asString);
                case INT:
                case LONG:
                case DATE:          return (int)asLong * (int)multiplier;
                case FLOAT:
                case DOUBLE:
                case SPEED:
                case DISTANCE:
                case TEMPERATURE:
                case ALTITUDE:      return (int)(asDouble *  multiplier);
            }
        }
        return 0;
    }

    public long getAsLong() {
        if (type != null) {
            switch (type) {
                case STRING:        return Long.parseLong(asString);
                case INT:
                case LONG:
                case DATE:          return asLong * (long)multiplier;
                case FLOAT:
                case DOUBLE:
                case SPEED:
                case DISTANCE:
                case TEMPERATURE:
                case ALTITUDE:      return (long)(asDouble * multiplier);
            }
        }
        return 0;
    }

    public float getAsFloat() {
        if (type != null) {
            switch (type) {
                case STRING:        return Float.parseFloat(asString);
                case INT:
                case LONG:
                case DATE:          return (float)(asLong * multiplier);
                case FLOAT:
                case DOUBLE:
                case SPEED:
                case DISTANCE:
                case TEMPERATURE:
                case ALTITUDE:      return (float)(asDouble * multiplier);
            }
        }
        return 0;
    }

    public double getAsDouble() {
        if (type != null) {
            switch (type) {
                case STRING:        return Double.parseDouble(asString);
                case INT:
                case LONG:
                case DATE:          return (double)asLong * multiplier;
                case FLOAT:
                case DOUBLE:
                case SPEED:
                case DISTANCE:
                case TEMPERATURE:
                case ALTITUDE:      return asDouble * multiplier;
            }
        }
        return 0;
    }

    public double getAsDecimal(int precision) {
        BigDecimal d = new BigDecimal(getAsFloat());
        BigDecimal s = d.setScale(precision, BigDecimal.ROUND_HALF_DOWN);
        return s.doubleValue();
    }

    public float getAsSpeed() {
        return imperial ? toMiles(getAsFloat()) : getAsFloat();
    }

    public float getAsDistance() {
        return getAsSpeed();
    }

    public float getAsTemperature() {
        return imperial ? toFahrenheit(getAsFloat()) : getAsFloat();
    }

    public float getAsAltitude() {
        return imperial ? toFeet(getAsFloat()) : getAsFloat();
    }

    public String getAsDateString() {
        if (isEmpty() && emptyText != null) return emptyText;
        if (!isValid() && invalidText != null) return invalidText;

        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = format != null && !format.isEmpty() ? new SimpleDateFormat(format, Locale.getDefault()) : new SimpleDateFormat();
        if (timeZone != null) formatter.setTimeZone(TimeZone.getTimeZone(timeZone));
        return formatter.format(new Date(getAsLong()));
    }

    public String getAsSpeedString() {
        if (isEmpty() && emptyText != null) return emptyText;
        if (!isValid() && invalidText != null) return invalidText;

        return imperial ? String.format(format+(suffixImperial != null ? suffixImperial : ""), toMiles(getAsFloat())) : String.format(format+(suffixMetric != null ? suffixMetric : ""), getAsFloat());
    }

    public String getAsDistanceString() {
        if (isEmpty() && emptyText != null) return emptyText;
        if (!isValid() && invalidText != null) return invalidText;

        return imperial ? String.format(format+(suffixImperial != null ? suffixImperial : ""), toMiles(getAsFloat())) : String.format(format+(suffixMetric != null ? suffixMetric : ""), getAsFloat());
    }

    public String getAsTemperatureString() {
        if (isEmpty() && emptyText != null) return emptyText;
        if (!isValid() && invalidText != null) return invalidText;

        return imperial ? String.format(format+(suffixImperial != null ? suffixImperial : ""), toFahrenheit(getAsFloat())) : String.format(format+(suffixMetric != null ? suffixMetric : ""), getAsFloat());
    }

    public String getAsAltitudeString() {
        if (isEmpty() && emptyText != null) return emptyText;
        if (!isValid() && invalidText != null) return invalidText;

        return imperial ? String.format(format+(suffixImperial != null ? suffixImperial : ""), toFeet(getAsFloat())) : String.format(format+(suffixMetric != null ? suffixMetric : ""), getAsFloat());
    }

    public Value touch() {
        if (age > 0) {
            age = System.currentTimeMillis();
            valid = true;
            invalidateViews();
        }
        return this;
    }

    public ValueType getType() {
        return type;
    }

    public long getAge() {
        return age;
    }

    public Value invalidate() {
        valid = false;
        age = 1;
        invalidateViews();
        return this;
    }

    public boolean isValid() {
        if (valid) {
            valid = validity <= 0 || (age + validity) >= System.currentTimeMillis();
            if (!valid) invalidateViews();
            return valid;
        }
        else
            return false;
    }

    public boolean isEmpty() {
        return (type == null || age == 0);
    }

    public String getEmptyText() {
        return emptyText;
    }

    public Value setEmptyText(String text) {
        if (emptyText != text) {
            emptyText = text;
            invalidateViews();
        }
        return this;
    }

    public boolean getImperial() {
        return imperial;
    }

    public Value setImperial(boolean imperial) {
        if (this.imperial != imperial) {
            this.imperial = imperial;
            invalidateViews();
        }
        return this;
    }

    public String getInvalidText() {
        return invalidText;
    }

    public Value setInvalidText(String text) {
        if (invalidText != text) {
            invalidText = text;
            invalidateViews();
        }
        return this;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public Value setMultiplier(double multiplier) {
        if (this.multiplier != multiplier) {
            this.multiplier = multiplier;
            invalidateViews();
        }
        return this;
    }

    public String getFormat() {
        return format;
    }

    public Value setFormat(String format) {
        if (this.format != format) {
            this.format = format;
            invalidateViews();
        }
        return this;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public Value setTitle(String title) {
        if (this.title != title) {
            this.title = title;
            invalidateViews();
        }
        return this;
    }
    
    public String getTimeZone() {
        return format;
    }

    public Value setTimeZone(String timeZone) {
        if (this.timeZone != timeZone) {
            this.timeZone = timeZone;
            invalidateViews();
        }
        return this;
    }

    public String getSuffixImperial() {
        return suffixImperial;
    }

    public Value setSuffixImperial(String suffix) {
        if (this.suffixImperial != suffix) {
            this.suffixImperial = suffix;
            invalidateViews();
        }
        return this;
    }

    public String getSuffixMetric() {
        return suffixMetric;
    }

    public Value setSuffixMetric(String suffix) {
        if (this.suffixMetric != suffix) {
            this.suffixMetric = suffix;
            invalidateViews();
        }
        return this;
    }

    public long getValidity() {
        return validity;
    }

    public Value setValidity(long validity) {
        this.validity = validity;
        isValid();
        return this;
    }

    public String getWKI() {
        return wki;
    }

    public Value setWKI(String wki) {
        this.wki = wki;
        return this;
    }

    public Value addView(View view) {
        if (view != null) {
            if (!views.contains(view)) views.add(view);
            if (view.getWindowToken() != null) view.postInvalidate();
            view.invalidate();
        }
        return this;
    }

    public Value removeView(View view) {
        if (view != null)
            views.remove(view);
        return this;
    }

    public void invalidateViews() {
        for (View view : views) {
            if (view.getWindowToken() != null) view.postInvalidate();
        }
    }

    public void setValueViewsVisibility(int empty, int notEmpty) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            for (View view : views) {
                if (view instanceof ValueView)
                    view.setVisibility(age == 0 ? empty : notEmpty);
            }
        }
    }

    public static float toFeet(float m) { return m * 0.3048f; }
    public static float fromFeet(float ft) { return ft * 3.28084f; }
    public static float toMiles(float km) { return km * 0.621371192f; }
    public static float fromMiles(float mi) { return mi * 1.609344f; }
    public static float toFahrenheit(float c) { return c * 1.8f + 32; }
    public static float fromFahrenheit(float f) { return (f - 32) / 1.8f; }

}
