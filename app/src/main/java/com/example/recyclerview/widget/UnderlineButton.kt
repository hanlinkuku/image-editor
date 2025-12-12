package com.example.recyclerview.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.max

class UnderlineButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private val underlinePaint: Paint
    private var isSelectedState = false

    private fun dpToPx(dp: Float): Int {
        return (dp * getResources().getDisplayMetrics().density + 0.5f).toInt()
    }

    init {
        setBackgroundColor(Color.WHITE)
        setPadding(dpToPx(16f), dpToPx(12f), dpToPx(16f), dpToPx(12f))
        setGravity(Gravity.CENTER)
        setBackground(null)
        setClickable(true)
        setTypeface(null, Typeface.NORMAL)

        underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        underlinePaint.setStyle(Paint.Style.STROKE)
        underlinePaint.setStrokeWidth(dpToPx(2f).toFloat())
        underlinePaint.setColor(Color.parseColor("#A0E6B7"))
        underlinePaint.setStrokeCap(Paint.Cap.ROUND)
    }

    var isCustomSelected: Boolean
        get() = isSelectedState
        set(selected) {
            if (this.isSelectedState != selected) {
                this.isSelectedState = selected
                setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
                invalidate()
            }
        }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isSelectedState) return

        val text = getText().toString().trim { it <= ' ' }
        if (text.isEmpty()) return

        val paint: Paint = getPaint()
        val textWidth = paint.measureText(text)
        val fm = paint.getFontMetrics()

        val underlineY = getBaseline() + fm.descent + 0.7f * (fm.descent - fm.ascent)


        val avgCharWidth = textWidth / max(1, text.length)
        val underlineWidth = max(textWidth - avgCharWidth, dpToPx(8f).toFloat())


        val paddingLeft = getCompoundPaddingLeft()
        val paddingRight = getCompoundPaddingRight()
        val availableWidth = (getWidth() - paddingLeft - paddingRight).toFloat()
        val textCenterX = paddingLeft + availableWidth / 2f


        val startX = textCenterX - underlineWidth / 2f
        val endX = textCenterX + underlineWidth / 2f

        canvas.drawLine(startX, underlineY, endX, underlineY, underlinePaint)
    }
}