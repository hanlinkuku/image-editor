package com.example.recyclerview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class SweepLightOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var sweepPaint: Paint? = null
    private var sweepGradient: LinearGradient? = null


    init {
        init()
    }

    private fun init() {
        sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        setBackground(null) // 透明底
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 尺寸变化时，重建底部渐变（适配旋转/折叠屏）
        if (h > 0) {
            // 创建：从底部（y=h）向上到 y=h*0.4 处的 黑→透明 渐变
            sweepGradient = LinearGradient(
                0f, h.toFloat(),  // 起点：底部
                0f, h * 0.4f,  // 终点：向上 60% 高度（控制渐变范围）
                intArrayOf(Color.WHITE, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            sweepPaint!!.setShader(sweepGradient)
        }
    }

    override fun onDraw(canvas: Canvas) {
        // 只绘制渐变遮罩层
        if (sweepGradient != null) {
            canvas.drawRect(0f, 0f, getWidth().toFloat(), getHeight().toFloat(), sweepPaint!!)
        }
    }
}