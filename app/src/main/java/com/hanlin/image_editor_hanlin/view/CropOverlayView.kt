// app/src/main/java/com/example/recyclerview/view/CropOverlayView.java
package com.hanlin.image_editor_hanlin.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hanlin.image_editor_hanlin.view.CropOverlayView.Mode.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class CropOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    // 归一化裁剪区域 [0,1]
    private var mCropLeft = 0.0f
    private var mCropTop = 0.0f
    private var mCropRight = 1.0f
    private var mCropBottom = 1.0f
    private var mEditorView: ImageEditorView? = null // 引用图片编辑器视图，用于获取图片显示区域
    private var mAspectRatio = 0.0f // 裁剪比例，0表示自由裁剪

    private val mHandleSizePx: Int
    private val mMaskPaint: Paint
    private val mBorderPaint: Paint
    private val mHandlePaint: Paint

    // 交互状态
    private enum class Mode {
        NONE, DRAG, RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR, RESIZE_L, RESIZE_R, RESIZE_T, RESIZE_B
    }

    private var mCurrentMode = Mode.NONE
    private var mLastX = 0f
    private var mLastY = 0f

    // 回调
    interface OnCropRegionChangedListener {
        fun onCropRegionChanged(left: Float, top: Float, right: Float, bottom: Float)
    }

    private var mListener: OnCropRegionChangedListener? = null

    init {
        mHandleSizePx = (HANDLE_SIZE * getResources().getDisplayMetrics().density).toInt()

        mMaskPaint = Paint()
        mMaskPaint.setColor(Color.argb(128, 0, 0, 0)) // 半透黑蒙层

        mBorderPaint = Paint()
        mBorderPaint.setStyle(Paint.Style.STROKE)
        mBorderPaint.setColor(Color.WHITE)
        mBorderPaint.setStrokeWidth(STROKE_WIDTH.toFloat())
        mBorderPaint.setAntiAlias(true)

        mHandlePaint = Paint()
        mHandlePaint.setColor(Color.WHITE)
        mHandlePaint.setStyle(Paint.Style.FILL_AND_STROKE)
        mHandlePaint.setStrokeWidth(2f)
        mHandlePaint.setAntiAlias(true)
    }

    // 设置图片编辑器视图引用
    fun setEditorView(editorView: ImageEditorView?) {
        mEditorView = editorView
    }

    // 外部设置初始裁剪区域（归一化）
    fun setCropRegion(left: Float, top: Float, right: Float, bottom: Float) {
        // 确保裁剪区域在有效范围内，使用浮点数边界
        // 注意：这里的0.0f~1.0f对应图片的实际显示区域，而不是整个屏幕
        mCropLeft = max(0.0f, min(left, 1.0f))
        mCropTop = max(0.0f, min(top, 1.0f))
        mCropRight = max(mCropLeft + 0.01f, min(right, 1.0f))
        mCropBottom = max(mCropTop + 0.01f, min(bottom, 1.0f))
        invalidate()
    }

    val cropRegion: FloatArray?
        // 获取当前裁剪区域（归一化）
        get() = floatArrayOf(mCropLeft, mCropTop, mCropRight, mCropBottom)

    fun setOnCropRegionChangedListener(listener: OnCropRegionChangedListener?) {
        mListener = listener
    }

    val isVisible: Boolean
        // 检查裁剪框是否可见
        get() = getVisibility() == VISIBLE

    // 设置裁剪比例，aspectRatio为0表示自由裁剪
    fun setAspectRatio(aspectRatio: Float) {
        mAspectRatio = aspectRatio
        // 如果设置了比例，需要调整当前裁剪框以符合该比例
        if (mAspectRatio > 0.0f) {
            adjustCropRegionToAspectRatio()
        }
    }

    // 调整当前裁剪区域以符合设置的宽高比
    private fun adjustCropRegionToAspectRatio() {
        val width = getWidth()
        val height = getHeight()
        if (width <= 0 || height <= 0) return

        // 获取当前裁剪框在屏幕上的实际位置和尺寸
        val currentRect = getRectF(width, height)
        val screenWidth = currentRect.width()
        val screenHeight = currentRect.height()
        val currentRatio = screenWidth / screenHeight

        if (abs(currentRatio - mAspectRatio) < 0.001f) {
            return  // 已经符合比例，不需要调整
        }

        // 计算屏幕上的中心点
        val screenCenterX = currentRect.centerX()
        val screenCenterY = currentRect.centerY()

        // 在屏幕坐标上调整尺寸以保持正确比例
        val newScreenWidth: Float
        val newScreenHeight: Float
        if (currentRatio > mAspectRatio) {
            // 当前比例更宽，需要调整宽度
            newScreenHeight = screenHeight
            newScreenWidth = mAspectRatio * newScreenHeight
        } else {
            // 当前比例更高，需要调整高度
            newScreenWidth = screenWidth
            newScreenHeight = newScreenWidth / mAspectRatio
        }

        // 计算新的屏幕坐标
        val newScreenLeft = screenCenterX - newScreenWidth / 2.0f
        val newScreenTop = screenCenterY - newScreenHeight / 2.0f
        val newScreenRight = screenCenterX + newScreenWidth / 2.0f
        val newScreenBottom = screenCenterY + newScreenHeight / 2.0f

        // 将屏幕坐标转换回归一化坐标
        val newNormalizedCoords = screenToNormalized(
            newScreenLeft,
            newScreenTop,
            newScreenRight,
            newScreenBottom,
            width,
            height
        )
        mCropLeft = newNormalizedCoords[0]
        mCropTop = newNormalizedCoords[1]
        mCropRight = newNormalizedCoords[2]
        mCropBottom = newNormalizedCoords[3]

        // 确保裁剪区域在有效范围内
        mCropLeft = max(0.0f, min(mCropLeft, mCropRight - 0.01f))
        mCropTop = max(0.0f, min(mCropTop, mCropBottom - 0.01f))
        mCropRight = min(1.0f, max(mCropRight, mCropLeft + 0.01f))
        mCropBottom = min(1.0f, max(mCropBottom, mCropTop + 0.01f))

        invalidate()
        if (mListener != null) {
            mListener!!.onCropRegionChanged(mCropLeft, mCropTop, mCropRight, mCropBottom)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val width = getWidth()
        val height = getHeight()
        if (width <= 0 || height <= 0) return

        // 计算图像中心点（屏幕坐标）
        val centerX = width / 2.0f
        val centerY = height / 2.0f

        // 获取图像旋转角度
        var rotation = 0.0f
        if (mEditorView != null) {
            rotation = mEditorView!!.getRenderer().getRotation()
        }

        // 获取旋转后的裁剪区域
        val cropRect = getRotatedRectF(width, height, rotation, centerX, centerY)

        // 如果有旋转角度，先保存画布状态并旋转
        if (rotation != 0.0f) {
            canvas.save()
            canvas.rotate(rotation, centerX, centerY)
        }

        // 绘制蒙层（四区域）
        // 计算未旋转的裁剪区域
        val originalCropRect = getRectF(width, height)


        // 改为绘制四个遮罩矩形，正确计算每个区域的位置
        // 上遮罩
        canvas.drawRect(0f, 0f, width.toFloat(), originalCropRect.top, mMaskPaint)
        // 左遮罩
        canvas.drawRect(
            0f,
            originalCropRect.top,
            originalCropRect.left,
            originalCropRect.bottom,
            mMaskPaint
        )
        // 右遮罩
        canvas.drawRect(
            originalCropRect.right,
            originalCropRect.top,
            width.toFloat(),
            originalCropRect.bottom,
            mMaskPaint
        )
        // 下遮罩
        canvas.drawRect(0f, originalCropRect.bottom, width.toFloat(), height.toFloat(), mMaskPaint)

        // 恢复画布状态（移除旋转），以便绘制旋转后的编辑框
        if (rotation != 0.0f) {
            canvas.restore()
        }

        // 绘制裁剪框边框
        canvas.drawRect(cropRect, mBorderPaint)

        // 绘制控制点（8 个）
        drawHandle(canvas, cropRect.left, cropRect.top) // TL
        drawHandle(canvas, cropRect.right, cropRect.top) // TR
        drawHandle(canvas, cropRect.left, cropRect.bottom) // BL
        drawHandle(canvas, cropRect.right, cropRect.bottom) // BR
        drawHandle(canvas, cropRect.centerX(), cropRect.top) // T
        drawHandle(canvas, cropRect.centerX(), cropRect.bottom) // B
        drawHandle(canvas, cropRect.left, cropRect.centerY()) // L
        drawHandle(canvas, cropRect.right, cropRect.centerY()) // R
    }

    private fun getRectF(width: Int, height: Int): RectF {
        // 将 NDC [-1,1] → 归一化 [0,1] 
        // NDC: -1=bottom, +1=top
        // 屏幕归一化: 0=top, 1=bottom

        var imageLeft = 0.0f
        var imageRight = 1.0f
        var imageTop = 0.0f
        var imageBottom = 1.0f

        if (mEditorView != null) {
            val scale = mEditorView!!.getRenderer().getScale()
            val translateX = mEditorView!!.getRenderer().getTranslateX()
            val translateY = mEditorView!!.getRenderer().getTranslateY()
            val rotation = mEditorView!!.getRenderer().getRotation()

            // 获取图片在NDC空间的实际边界
            val ndcLeft = mEditorView!!.getImageLeft() // NDC (-1 ~ 1)
            val ndcRight = mEditorView!!.getImageRight()
            val ndcTop = mEditorView!!.getImageTop()
            val ndcBottom = mEditorView!!.getImageBottom()

            // 计算图像在NDC空间的原始宽高
            val ndcWidth = ndcRight - ndcLeft
            val ndcHeight = ndcTop - ndcBottom // 注意：NDC空间中top > bottom


            // 考虑旋转角度对宽高的影响
            val rotationQuadrant = abs(rotation % 360).toInt() / 90
            var effectiveNdcWidth = ndcWidth
            var effectiveNdcHeight = ndcHeight


            // 当旋转90度或270度时，宽高互换
            if (rotationQuadrant == 1 || rotationQuadrant == 3) {
                effectiveNdcWidth = ndcHeight
                effectiveNdcHeight = ndcWidth
            }

            // 计算缩放后的NDC宽高
            val scaledNdcWidth = effectiveNdcWidth * scale
            val scaledNdcHeight = effectiveNdcHeight * scale

            // 计算图像在NDC空间的中心位置
            val ndcCenterX = (ndcLeft + ndcRight) / 2.0f
            val ndcCenterY = (ndcTop + ndcBottom) / 2.0f

            // 计算缩放和平移后的NDC边界
            val finalNdcLeft = ndcCenterX - scaledNdcWidth / 2.0f + translateX
            val finalNdcRight = ndcCenterX + scaledNdcWidth / 2.0f + translateX
            val finalNdcTop = ndcCenterY + scaledNdcHeight / 2.0f + translateY
            val finalNdcBottom = ndcCenterY - scaledNdcHeight / 2.0f + translateY

            // 将最终的NDC边界转换为屏幕归一化坐标 [0,1]
            imageLeft = (finalNdcLeft + 1.0f) / 2.0f
            imageRight = (finalNdcRight + 1.0f) / 2.0f
            imageTop = (1.0f - finalNdcTop) / 2.0f // NDC到屏幕Y轴翻转
            imageBottom = (1.0f - finalNdcBottom) / 2.0f // NDC到屏幕Y轴翻转
        }

        // 将归一化裁剪坐标 [0,1] 映射到图像显示区域
        val left = imageLeft + mCropLeft * (imageRight - imageLeft)
        val top = imageTop + mCropTop * (imageBottom - imageTop)
        val right = imageLeft + mCropRight * (imageRight - imageLeft)
        val bottom = imageTop + mCropBottom * (imageBottom - imageTop)
        return RectF(
            left * width,
            top * height,
            right * width,
            bottom * height
        )
    }

    // 计算旋转后的裁剪区域
    private fun getRotatedRectF(
        width: Int,
        height: Int,
        rotation: Float,
        centerX: Float,
        centerY: Float
    ): RectF {
        // 获取原始的未旋转裁剪区域
        val originalRect = getRectF(width, height)


        // 如果旋转角度为0，直接返回原始区域
        if (rotation == 0.0f) {
            return originalRect
        }


        // 创建旋转矩阵
        val matrix = Matrix()


        // 设置旋转中心和旋转角度
        matrix.setRotate(rotation, centerX, centerY)


        // 创建一个数组来存储原始矩形的四个顶点
        val points = floatArrayOf(
            originalRect.left, originalRect.top,
            originalRect.right, originalRect.top,
            originalRect.left, originalRect.bottom,
            originalRect.right, originalRect.bottom
        )


        // 应用旋转变换
        matrix.mapPoints(points)


        // 计算旋转后矩形的边界
        val left = min(min(points[0], points[2]), min(points[4], points[6]))
        val top = min(min(points[1], points[3]), min(points[5], points[7]))
        val right = max(max(points[0], points[2]), max(points[4], points[6]))
        val bottom = max(max(points[1], points[3]), max(points[5], points[7]))


        // 返回旋转后的矩形
        return RectF(left, top, right, bottom)
    }

    // 将屏幕坐标转换为归一化坐标
    private fun screenToNormalized(
        screenLeft: Float,
        screenTop: Float,
        screenRight: Float,
        screenBottom: Float,
        width: Int,
        height: Int
    ): FloatArray {
        // 获取图片在屏幕上的实际显示区域
        var imageLeft = 0.0f
        var imageRight = 1.0f
        var imageTop = 0.0f
        var imageBottom = 1.0f

        if (mEditorView != null) {
            val scale = mEditorView!!.getRenderer().getScale()
            val translateX = mEditorView!!.getRenderer().getTranslateX()
            val translateY = mEditorView!!.getRenderer().getTranslateY()

            // 获取图片在NDC空间的实际边界
            val ndcLeft = mEditorView!!.getImageLeft() // NDC (-1 ~ 1)
            val ndcRight = mEditorView!!.getImageRight()
            val ndcTop = mEditorView!!.getImageTop()
            val ndcBottom = mEditorView!!.getImageBottom()

            // 计算图像在NDC空间的原始宽高
            val ndcWidth = ndcRight - ndcLeft
            val ndcHeight = ndcTop - ndcBottom // 注意：NDC空间中top > bottom

            // 计算缩放后的NDC宽高
            val scaledNdcWidth = ndcWidth * scale
            val scaledNdcHeight = ndcHeight * scale

            // 计算图像在NDC空间的中心位置
            val ndcCenterX = (ndcLeft + ndcRight) / 2.0f
            val ndcCenterY = (ndcTop + ndcBottom) / 2.0f

            // 计算缩放和平移后的NDC边界
            val finalNdcLeft = ndcCenterX - scaledNdcWidth / 2.0f + translateX
            val finalNdcRight = ndcCenterX + scaledNdcWidth / 2.0f + translateX
            val finalNdcTop = ndcCenterY + scaledNdcHeight / 2.0f + translateY
            val finalNdcBottom = ndcCenterY - scaledNdcHeight / 2.0f + translateY

            // 将最终的NDC边界转换为屏幕归一化坐标 [0,1]
            imageLeft = (finalNdcLeft + 1.0f) / 2.0f
            imageRight = (finalNdcRight + 1.0f) / 2.0f
            imageTop = (1.0f - finalNdcTop) / 2.0f // NDC到屏幕Y轴翻转
            imageBottom = (1.0f - finalNdcBottom) / 2.0f // NDC到屏幕Y轴翻转
        }

        // 将屏幕像素坐标转换为屏幕归一化坐标 [0,1]
        val normalizedScreenLeft = screenLeft / width
        val normalizedScreenTop = screenTop / height
        val normalizedScreenRight = screenRight / width
        val normalizedScreenBottom = screenBottom / height

        // 将屏幕归一化坐标转换为相对于图片显示区域的归一化坐标 [0,1]
        val imageWidth = imageRight - imageLeft
        val imageHeight = imageBottom - imageTop

        val newCropLeft = (normalizedScreenLeft - imageLeft) / imageWidth
        val newCropTop = (normalizedScreenTop - imageTop) / imageHeight
        val newCropRight = (normalizedScreenRight - imageLeft) / imageWidth
        val newCropBottom = (normalizedScreenBottom - imageTop) / imageHeight

        return floatArrayOf(newCropLeft, newCropTop, newCropRight, newCropBottom)
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        canvas.drawRect(
            x - mHandleSizePx / 2f,
            y - mHandleSizePx / 2f,
            x + mHandleSizePx / 2f,
            y + mHandleSizePx / 2f,
            mHandlePaint
        )
    }

    //手势交互核心
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.getActionMasked()
        val x = event.getX()
        val y = event.getY()

        // 获取图像旋转角度
        var rotation = 0.0f
        if (mEditorView != null) {
            rotation = mEditorView!!.getRenderer().getRotation()
        }

        // 计算图像中心点（屏幕坐标）
        val centerX = getWidth() / 2.0f
        val centerY = getHeight() / 2.0f

        // 如果有旋转角度，将触摸坐标旋转回原始坐标系
        val transformedPoint = FloatArray(2)
        if (rotation != 0.0f) {
            // 计算触摸点相对于中心点的偏移
            val dx = x - centerX
            val dy = y - centerY


            // 计算旋转后的坐标（将旋转角度取反，将触摸点旋转回未旋转状态）
            val angleRad = -Math.toRadians(rotation.toDouble())
            transformedPoint[0] = (dx * cos(angleRad) - dy * sin(angleRad)).toFloat() + centerX
            transformedPoint[1] = (dx * sin(angleRad) + dy * cos(angleRad)).toFloat() + centerY
        } else {
            // 没有旋转，使用原始坐标
            transformedPoint[0] = x
            transformedPoint[1] = y
        }

        // 使用转换后的坐标进行触摸事件处理
        val transformedX = transformedPoint[0]
        val transformedY = transformedPoint[1]

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mCurrentMode = getTouchedMode(transformedX, transformedY)
                mLastX = transformedX
                mLastY = transformedY
                return mCurrentMode != Mode.NONE
            }

            MotionEvent.ACTION_MOVE -> if (mCurrentMode != Mode.NONE) {
                handleMove(transformedX, transformedY)
                mLastX = transformedX
                mLastY = transformedY
                invalidate()
                if (mListener != null) {
                    mListener!!.onCropRegionChanged(mCropLeft, mCropTop, mCropRight, mCropBottom)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mCurrentMode = Mode.NONE
        }
        return false
    }

    // 根据点击位置判断操作模式
    private fun getTouchedMode(x: Float, y: Float): Mode {
        val width = getWidth()
        val height = getHeight()
        val rect = getRectF(width, height)
        val half = mHandleSizePx / 2f

        // 角
        if (abs(x - rect.left) < half && abs(y - rect.top) < half) return Mode.RESIZE_TL
        if (abs(x - rect.right) < half && abs(y - rect.top) < half) return Mode.RESIZE_TR
        if (abs(x - rect.left) < half && abs(y - rect.bottom) < half) return Mode.RESIZE_BL
        if (abs(x - rect.right) < half && abs(y - rect.bottom) < half) return Mode.RESIZE_BR

        // 边
        if (abs(y - rect.top) < half && x > rect.left && x < rect.right) return Mode.RESIZE_T
        if (abs(y - rect.bottom) < half && x > rect.left && x < rect.right) return Mode.RESIZE_B
        if (abs(x - rect.left) < half && y > rect.top && y < rect.bottom) return Mode.RESIZE_L
        if (abs(x - rect.right) < half && y > rect.top && y < rect.bottom) return Mode.RESIZE_R

        // 中心区域 → 拖拽整个框
        if (rect.contains(x, y)) return Mode.DRAG

        return Mode.NONE
    }

    // 处理移动逻辑
    private fun handleMove(x: Float, y: Float) {
        val width = getWidth()
        val height = getHeight()
        val dx = (x - mLastX) / width
        val dy = (y - mLastY) / height

        val oldWidth = mCropRight - mCropLeft
        val oldHeight = mCropBottom - mCropTop

        when (mCurrentMode) {
            DRAG -> moveCropRegion(dx, dy)
            RESIZE_TL -> {
                mCropLeft += dx
                mCropTop += dy
            }

            RESIZE_TR -> {
                mCropRight += dx
                mCropTop += dy
            }

            RESIZE_BL -> {
                mCropLeft += dx
                mCropBottom += dy
            }

            RESIZE_BR -> {
                mCropRight += dx
                mCropBottom += dy
            }

            RESIZE_L -> mCropLeft += dx
            RESIZE_R -> mCropRight += dx
            RESIZE_T -> mCropTop += dy
            RESIZE_B -> mCropBottom += dy
            NONE -> TODO()
        }

        // 如果设置了宽高比，需要保持比例
        if (mAspectRatio > 0.0f) {
            // 获取当前裁剪框在屏幕上的实际位置和尺寸
            val currentRect = getRectF(width, height)
            val screenWidth = currentRect.width()
            val screenHeight = currentRect.height()
            val currentRatio = screenWidth / screenHeight

            // 计算屏幕上的中心点
            val screenCenterX = currentRect.centerX()
            val screenCenterY = currentRect.centerY()

            // 在屏幕坐标上调整尺寸以保持正确比例
            val newScreenWidth: Float
            val newScreenHeight: Float
            if (currentRatio > mAspectRatio) {
                // 当前比例更宽，需要调整宽度
                newScreenHeight = screenHeight
                newScreenWidth = mAspectRatio * newScreenHeight
            } else {
                // 当前比例更高，需要调整高度
                newScreenWidth = screenWidth
                newScreenHeight = newScreenWidth / mAspectRatio
            }

            // 计算新的屏幕坐标
            val newScreenLeft = screenCenterX - newScreenWidth / 2.0f
            val newScreenTop = screenCenterY - newScreenHeight / 2.0f
            val newScreenRight = screenCenterX + newScreenWidth / 2.0f
            val newScreenBottom = screenCenterY + newScreenHeight / 2.0f

            // 将屏幕坐标转换回归一化坐标
            val newNormalizedCoords = screenToNormalized(
                newScreenLeft,
                newScreenTop,
                newScreenRight,
                newScreenBottom,
                width,
                height
            )
            mCropLeft = newNormalizedCoords[0]
            mCropTop = newNormalizedCoords[1]
            mCropRight = newNormalizedCoords[2]
            mCropBottom = newNormalizedCoords[3]
        }

        // 边界与有效性约束 - 使用浮点数边界
        mCropLeft = max(0.0f, min(mCropLeft, mCropRight - 0.01f))
        mCropTop = max(0.0f, min(mCropTop, mCropBottom - 0.01f))
        mCropRight = min(1.0f, max(mCropRight, mCropLeft + 0.01f))
        mCropBottom = min(1.0f, max(mCropBottom, mCropTop + 0.01f))
    }

    // 拖拽整个裁剪框（保持宽高比）
    private fun moveCropRegion(dx: Float, dy: Float) {
        val w = mCropRight - mCropLeft
        val h = mCropBottom - mCropTop

        mCropLeft += dx
        mCropTop += dy
        mCropRight = mCropLeft + w
        mCropBottom = mCropTop + h

        // 边界限制 - 使用浮点数边界
        if (mCropLeft < 0.0f) {
            val shift = -mCropLeft
            mCropLeft += shift
            mCropRight += shift
        }
        if (mCropTop < 0.0f) {
            val shift = -mCropTop
            mCropTop += shift
            mCropBottom += shift
        }
        if (mCropRight > 1.0f) {
            val shift = mCropRight - 1.0f
            mCropLeft -= shift
            mCropRight -= shift
        }
        if (mCropBottom > 1.0f) {
            val shift = mCropBottom - 1.0f
            mCropTop -= shift
            mCropBottom -= shift
        }
    }

    companion object {
        // UI 参数
        private const val HANDLE_SIZE = 32 // dp
        private const val STROKE_WIDTH = 2 // px
    }
}