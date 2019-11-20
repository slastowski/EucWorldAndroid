package net.lastowski.common.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import net.lastowski.common.R;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

@SuppressLint("AppCompatCustomView")
public class StatusBar extends TextView {

    private String msg = "";
    private int pageCount = 0;
    private int pageIndex;

    Context context;
    String time = "";
    Paint paint;
    Rect rect;
    Timer timer;
    Handler handler;
    int timerCounter;

    public void setPageCount(int pageCount) {
        if (this.pageCount != pageCount && pageCount >= 0) {
            this.pageCount = pageCount;
            invalidate();
        }
    }

    public void setPageIndex(int pageIndex) {
        if (this.pageIndex != pageIndex) {
            this.pageIndex = pageIndex;
            invalidate();
        }
    }

    public StatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new Rect();
        handler = new Handler();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String t = DateFormat.getTimeFormat(getContext()).format(Calendar.getInstance().getTime());
                        if (!time.equals(t)) {
                            time = t;
                            if (timerCounter == 0) invalidate();
                        }
                        if (timerCounter > 0) {
                            if (--timerCounter == 0) invalidate();
                        }
                    }
                });
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 100);
        if (isInEditMode()) {
            pageCount = 0;
            pageIndex = -1;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final ColorStateList csl = getTextColors();
        final int color = csl.getDefaultColor();
        final TextPaint textPaint = getPaint();
        final int topPadding = getPaddingTop();

        // Time or message
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(color);

        if (pageCount > 0) {
            textPaint.setTextSize(rect.height() / 2);
            canvas.drawText(timerCounter != 0 ? msg : time, rect.centerX(), rect.centerY(), textPaint);

            paint.setStrokeWidth(1);
            paint.setColor(color);
            float w = 10 * (pageCount - 1);
            float x;
            for (int i = 0; i < pageCount; ++i) {
                x = rect.centerX() - (w / 2) + i * 10;
                paint.setStyle((i == pageIndex) ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
                canvas.drawCircle(x, rect.centerY() * 1.5f, 3, paint);
            }
        }
        else
            canvas.drawText(timerCounter != 0 ? msg : time, rect.centerX(), getTextSize() + topPadding, textPaint);
    }

    public void setMessage(String message, int duration) {
        timerCounter = duration / 100;
        msg = message;
        invalidate();
    }

    public void setMessage(String message) {
        this.setMessage(message , -1);
    }

    public void clearMessage() {
        timerCounter = 0;
        msg = "";
        invalidate();
    }

}
