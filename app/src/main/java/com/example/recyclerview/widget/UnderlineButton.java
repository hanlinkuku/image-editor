package com.example.recyclerview.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class UnderlineButton extends AppCompatTextView {

    private final Paint underlinePaint;
    private boolean isSelectedState = false;

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    public UnderlineButton(Context context) {
        this(context, null);
    }

    public UnderlineButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UnderlineButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackgroundColor(Color.WHITE);
        setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12)); 
        setGravity(android.view.Gravity.CENTER);
        setBackground(null);
        setClickable(true);
        setTypeface(null, Typeface.NORMAL);

        underlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        underlinePaint.setStyle(Paint.Style.STROKE);
        underlinePaint.setStrokeWidth(dpToPx(2)); 
        underlinePaint.setColor(Color.parseColor("#A0E6B7"));
        underlinePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setCustomSelected(boolean selected) {
        if (this.isSelectedState != selected) {
            this.isSelectedState = selected;
            setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            invalidate();
        }
    }

    public boolean isCustomSelected() {
        return isSelectedState;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isSelectedState) return;

        String text = getText().toString().trim();
        if (text.isEmpty()) return;

        Paint paint = getPaint();
        float textWidth = paint.measureText(text);
        Paint.FontMetrics fm = paint.getFontMetrics();

        float underlineY = getBaseline() + fm.descent + 0.7f * (fm.descent - fm.ascent);


        float avgCharWidth = textWidth / Math.max(1, text.length());
        float underlineWidth = Math.max(textWidth - avgCharWidth, dpToPx(8)); 


        int paddingLeft = getCompoundPaddingLeft();
        int paddingRight = getCompoundPaddingRight();
        float availableWidth = getWidth() - paddingLeft - paddingRight;
        float textCenterX = paddingLeft + availableWidth / 2f; 


        float startX = textCenterX - underlineWidth / 2f;
        float endX   = textCenterX + underlineWidth / 2f;

        canvas.drawLine(startX, underlineY, endX, underlineY, underlinePaint);
    }
}