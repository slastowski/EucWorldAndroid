package com.pavelsikun.seekbarpreference;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Pavel Sikun on 21.05.16.
 */
class CustomValueDialog {

    private final String TAG = getClass().getSimpleName();

    private Dialog dialog;
    private EditText customValueView;
    private TextView titleView;

    private int minValue, maxValue, currentValue;
    private String type;
    private boolean imperial;
    private PersistValueListener persistValueListener;

    CustomValueDialog(Context context, int theme, int minValue, int maxValue, int currentValue, String title, String type, boolean imperial) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = currentValue;
        this.type = type;
        this.imperial = imperial;

        init(new AlertDialog.Builder(context, theme), title);
    }

    private void init(AlertDialog.Builder dialogBuilder, String title) {
        View dialogView = LayoutInflater.from(dialogBuilder.getContext()).inflate(R.layout.value_selector_dialog, null);
        dialog = dialogBuilder.setView(dialogView).create();

        TextView minValueView = (TextView) dialogView.findViewById(R.id.minValue);
        TextView maxValueView = (TextView) dialogView.findViewById(R.id.maxValue);
        customValueView = (EditText) dialogView.findViewById(R.id.customValue);

        titleView = dialogView.findViewById(R.id.dialog_title);
        titleView.setText(title);

        minValueView.setText(valueToString(minValue));
        maxValueView.setText(valueToString(maxValue));
        customValueView.setHint(valueToString(currentValue));

        //LinearLayout colorView = (LinearLayout) dialogView.findViewById(R.id.dialog_color_area);
        //colorView.setBackgroundColor(fetchAccentColor(dialogBuilder.getContext()));

        Button applyButton = (Button) dialogView.findViewById(R.id.btn_apply);
        Button cancelButton = (Button) dialogView.findViewById(R.id.btn_cancel);

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryApply();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private int fetchAccentColor(Context context) {
        TypedValue typedValue = new TypedValue();

        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] { R.attr.colorAccent });
        int color = a.getColor(0, 0);
        a.recycle();

        return color;
    }

    CustomValueDialog setPersistValueListener(PersistValueListener listener) {
        persistValueListener = listener;
        return this;
    }

    void show() {
        dialog.show();
    }

    private void tryApply() {
        int value;
        try {
            value = forceInRange(minValue, maxValue, inputToInt(customValueView.getText().toString()));
        }
        catch (Exception e) {
            Log.e(TAG, "worng input(non-integer): " + customValueView.getText().toString());
            notifyWrongInput();
            return;
        }

        if(persistValueListener != null) {
            persistValueListener.persistInt(value);
            dialog.dismiss();
        }
    }

    private void notifyWrongInput() {
        customValueView.setText("");
        //customValueView.setHint("Wrong Input!");
    }

    private String valueToString(int value) {
        if (type != null) {
            switch (type) {
                case "speed":
                    return String.valueOf(Math.round(imperial ? PreferenceControllerDelegate.toMiles(value) : value));
                case "temperature":
                    return String.valueOf(Math.round(imperial ? PreferenceControllerDelegate.toFahrenheit(value) : value));
            }
        }
        return String.valueOf(value);
    }

    private int inputToInt(String input) {
        int v = Integer.parseInt(input);
        if (type != null) {
            switch (type) {
                case "speed":
                    return imperial ? Math.round(PreferenceControllerDelegate.fromMiles(v)) : v;
                case "temperature":
                    return imperial ? Math.round(PreferenceControllerDelegate.fromFahrenheit(v)) : v;
            }
        }
        return v;
    }

    private int forceInRange(int min, int max, int v) {
        if (v > max) return max;
        else
        if (v < min) return min;
        else         return v;
    }

}

