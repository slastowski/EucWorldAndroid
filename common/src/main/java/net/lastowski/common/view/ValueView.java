package net.lastowski.common.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

import net.lastowski.common.R;
import net.lastowski.common.Value;

public class ValueView extends AppCompatTextView {

    int valueColor = 0xFFFFFFFF;
    int valueColorInvalid = 0xFF555555;
    float columnRatio = 0.5f;
    float titleValueHeightRatio = 0.8f;
    int titleValueMargin;
    boolean vertical = false;

    Value value;
    TextPaint titlePaint, valuePaint;
    Rect rect;

    public ValueView(Context context) {
        super(context);
        init(context, null);
    }

    public ValueView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ValueView(Context context,
                             AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        titlePaint = new TextPaint();
        valuePaint = new TextPaint();
        rect = new Rect();

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs, R.styleable.ValueView, 0, 0);
            try {
                valueColor = a.getColor(R.styleable.ValueView_valueColor, valueColor);
                valueColorInvalid = a.getColor(R.styleable.ValueView_valueColorInvalid, valueColorInvalid);
                titleValueMargin = a.getDimensionPixelSize(R.styleable.ValueView_titleValueMargin, 8);
                titleValueHeightRatio = a.getFloat(R.styleable.ValueView_titleValueHeightRatio, titleValueHeightRatio);
                columnRatio = a.getFloat(R.styleable.ValueView_columnRatio, columnRatio);
                vertical = a.getBoolean(R.styleable.ValueView_vertical, vertical);
            }
            finally {
                a.recycle();
            }
        }

    }

    public void setValue(Value value) {
        if (this.value != value) {
            if (this.value != null) this.value.removeView(this);

            this.value = value;
            this.value.addView(this);
            if (this.value != null) setText(this.value.getTitle());
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics fm = getPaint().getFontMetrics();
        int h = (int)(fm.bottom - fm.top);
        setMeasuredDimension(widthMeasureSpec, (vertical ? (int)(h * titleValueHeightRatio) + h + titleValueMargin : h) + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String valueText;
        if (value != null && (valueText = value.getAsString()) != null) {
            String titleText = value.getTitle();

            canvas.getClipBounds(rect);
            titlePaint.set(getPaint());
            titlePaint.setColor(getCurrentTextColor());
            titlePaint.setTextSize(getTextSize() * titleValueHeightRatio);

            valuePaint.set(getPaint());
            valuePaint.setColor(value.isValid() ? valueColor : valueColorInvalid);

            if (vertical) {
                titlePaint.setTextAlign(Paint.Align.CENTER);
                valuePaint.setTextAlign(Paint.Align.CENTER);

                if (titlePaint.measureText(titleText) > rect.width())
                    titleText = TextUtils.ellipsize(titleText, titlePaint, rect.width(), TextUtils.TruncateAt.END).toString();
                if (valuePaint.measureText(valueText) > rect.width())
                    valueText = TextUtils.ellipsize(valueText, valuePaint, rect.width(), TextUtils.TruncateAt.END).toString();

                canvas.drawText(titleText, rect.centerX(), getTextSize() * titleValueHeightRatio + getPaddingTop(), titlePaint);
                canvas.drawText(valueText, rect.centerX(), getTextSize() * (1 + titleValueHeightRatio) + titleValueMargin + getPaddingTop(), valuePaint);
            }
            else {
                float titleMaxWidth = rect.width() * columnRatio - (float)titleValueMargin / 2;
                float valueMaxWidth = rect.width() - titleMaxWidth - (float)titleValueMargin / 2;

                if (titleMaxWidth > 0 && titlePaint.measureText(titleText) > titleMaxWidth)
                    titleText = TextUtils.ellipsize(titleText, titlePaint, titleMaxWidth, TextUtils.TruncateAt.END).toString();
                if (valueMaxWidth > 0 && valuePaint.measureText(valueText) > valueMaxWidth)
                    valueText = TextUtils.ellipsize(valueText, valuePaint, valueMaxWidth, TextUtils.TruncateAt.END).toString();

                titlePaint.setTextAlign(Paint.Align.RIGHT);
                valuePaint.setTextAlign(Paint.Align.LEFT);

                canvas.drawText(titleText, titleMaxWidth, getTextSize() + getPaddingTop(), titlePaint);
                canvas.drawText(valueText, titleMaxWidth + titleValueMargin, getTextSize() + getPaddingTop(), valuePaint);
            }
        }
    }

    private int dpToPx(Context context, float dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }

}
