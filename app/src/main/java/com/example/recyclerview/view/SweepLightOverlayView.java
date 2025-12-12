package com.example.recyclerview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class SweepLightOverlayView extends View {
    private Paint sweepPaint;
    private LinearGradient sweepGradient;


    public SweepLightOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setBackground(null); // 透明底


    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 尺寸变化时，重建底部渐变（适配旋转/折叠屏）
        if (h > 0) {
            // 创建：从底部（y=h）向上到 y=h*0.4 处的 黑→透明 渐变
            sweepGradient = new LinearGradient(
                    0, h,           // 起点：底部
                    0, h * 0.4f,    // 终点：向上 60% 高度（控制渐变范围）
                    new int[]{Color.WHITE, Color.TRANSPARENT},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            );
            sweepPaint.setShader(sweepGradient);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 只绘制渐变遮罩层
        if (sweepGradient != null) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), sweepPaint);
        }
    }

}