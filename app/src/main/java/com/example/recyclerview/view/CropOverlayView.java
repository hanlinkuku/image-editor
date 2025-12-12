// app/src/main/java/com/example/recyclerview/view/CropOverlayView.java
package com.example.recyclerview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class CropOverlayView extends View {

    // 归一化裁剪区域 [0,1]
    private float mCropLeft = 0.0f, mCropTop = 0.0f;
    private float mCropRight = 1.0f, mCropBottom = 1.0f;
    private ImageEditorView mEditorView; // 引用图片编辑器视图，用于获取图片显示区域
    private float mAspectRatio = 0.0f; // 裁剪比例，0表示自由裁剪

    // UI 参数
    private static final int HANDLE_SIZE = 32; // dp
    private static final int STROKE_WIDTH = 2; // px
    private final int mHandleSizePx;
    private final Paint mMaskPaint;
    private final Paint mBorderPaint;
    private final Paint mHandlePaint;

    // 交互状态
    private enum Mode { NONE, DRAG, RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR, RESIZE_L, RESIZE_R, RESIZE_T, RESIZE_B }
    private Mode mCurrentMode = Mode.NONE;
    private float mLastX, mLastY;

    // 回调
    public interface OnCropRegionChangedListener {
        void onCropRegionChanged(float left, float top, float right, float bottom);
    }
    private OnCropRegionChangedListener mListener;

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandleSizePx = (int) (HANDLE_SIZE * getResources().getDisplayMetrics().density);

        mMaskPaint = new Paint();
        mMaskPaint.setColor(Color.argb(128, 0, 0, 0)); // 半透黑蒙层

        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(Color.WHITE);
        mBorderPaint.setStrokeWidth(STROKE_WIDTH);
        mBorderPaint.setAntiAlias(true);

        mHandlePaint = new Paint();
        mHandlePaint.setColor(Color.WHITE);
        mHandlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mHandlePaint.setStrokeWidth(2);
        mHandlePaint.setAntiAlias(true);
    }

    // 设置图片编辑器视图引用
    public void setEditorView(ImageEditorView editorView) {
        mEditorView = editorView;
    }

    // 外部设置初始裁剪区域（归一化）
    public void setCropRegion(float left, float top, float right, float bottom) {
        // 确保裁剪区域在有效范围内，使用浮点数边界
        // 注意：这里的0.0f~1.0f对应图片的实际显示区域，而不是整个屏幕
        mCropLeft = Math.max(0.0f, Math.min(left, 1.0f));
        mCropTop = Math.max(0.0f, Math.min(top, 1.0f));
        mCropRight = Math.max(mCropLeft + 0.01f, Math.min(right, 1.0f));
        mCropBottom = Math.max(mCropTop + 0.01f, Math.min(bottom, 1.0f));
        invalidate();
    }

    // 获取当前裁剪区域（归一化）
    public float[] getCropRegion() {
        return new float[]{mCropLeft, mCropTop, mCropRight, mCropBottom};
    }

    public void setOnCropRegionChangedListener(OnCropRegionChangedListener listener) {
        mListener = listener;
    }

    // 检查裁剪框是否可见
    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }

    // 设置裁剪比例，aspectRatio为0表示自由裁剪
    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
        // 如果设置了比例，需要调整当前裁剪框以符合该比例
        if (mAspectRatio > 0.0f) {
            adjustCropRegionToAspectRatio();
        }
    }

    // 调整当前裁剪区域以符合设置的宽高比
    private void adjustCropRegionToAspectRatio() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // 获取当前裁剪框在屏幕上的实际位置和尺寸
        RectF currentRect = getRectF(width, height);
        float screenWidth = currentRect.width();
        float screenHeight = currentRect.height();
        float currentRatio = screenWidth / screenHeight;

        if (Math.abs(currentRatio - mAspectRatio) < 0.001f) {
            return; // 已经符合比例，不需要调整
        }

        // 计算屏幕上的中心点
        float screenCenterX = currentRect.centerX();
        float screenCenterY = currentRect.centerY();

        // 在屏幕坐标上调整尺寸以保持正确比例
        float newScreenWidth, newScreenHeight;
        if (currentRatio > mAspectRatio) {
            // 当前比例更宽，需要调整宽度
            newScreenHeight = screenHeight;
            newScreenWidth = mAspectRatio * newScreenHeight;
        } else {
            // 当前比例更高，需要调整高度
            newScreenWidth = screenWidth;
            newScreenHeight = newScreenWidth / mAspectRatio;
        }

        // 计算新的屏幕坐标
        float newScreenLeft = screenCenterX - newScreenWidth / 2.0f;
        float newScreenTop = screenCenterY - newScreenHeight / 2.0f;
        float newScreenRight = screenCenterX + newScreenWidth / 2.0f;
        float newScreenBottom = screenCenterY + newScreenHeight / 2.0f;

        // 将屏幕坐标转换回归一化坐标
        float[] newNormalizedCoords = screenToNormalized(newScreenLeft, newScreenTop, newScreenRight, newScreenBottom, width, height);
        mCropLeft = newNormalizedCoords[0];
        mCropTop = newNormalizedCoords[1];
        mCropRight = newNormalizedCoords[2];
        mCropBottom = newNormalizedCoords[3];

        // 确保裁剪区域在有效范围内
        mCropLeft = Math.max(0.0f, Math.min(mCropLeft, mCropRight - 0.01f));
        mCropTop = Math.max(0.0f, Math.min(mCropTop, mCropBottom - 0.01f));
        mCropRight = Math.min(1.0f, Math.max(mCropRight, mCropLeft + 0.01f));
        mCropBottom = Math.min(1.0f, Math.max(mCropBottom, mCropTop + 0.01f));

        invalidate();
        if (mListener != null) {
            mListener.onCropRegionChanged(mCropLeft, mCropTop, mCropRight, mCropBottom);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // 计算图像中心点（屏幕坐标）
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        // 获取图像旋转角度
        float rotation = 0.0f;
        if (mEditorView != null) {
            rotation = mEditorView.getRenderer().getRotation();
        }

        // 获取旋转后的裁剪区域
        RectF cropRect = getRotatedRectF(width, height, rotation, centerX, centerY);

        // 如果有旋转角度，先保存画布状态并旋转
        if (rotation != 0.0f) {
            canvas.save();
            canvas.rotate(rotation, centerX, centerY);
        }

        // 绘制蒙层（四区域）
        // 计算未旋转的裁剪区域
        RectF originalCropRect = getRectF(width, height);
        
        // 改为绘制四个遮罩矩形，正确计算每个区域的位置
        // 上遮罩
        canvas.drawRect(0, 0, width, originalCropRect.top, mMaskPaint);
        // 左遮罩
        canvas.drawRect(0, originalCropRect.top, originalCropRect.left, originalCropRect.bottom, mMaskPaint);
        // 右遮罩
        canvas.drawRect(originalCropRect.right, originalCropRect.top, width, originalCropRect.bottom, mMaskPaint);
        // 下遮罩
        canvas.drawRect(0, originalCropRect.bottom, width, height, mMaskPaint);

        // 恢复画布状态（移除旋转），以便绘制旋转后的编辑框
        if (rotation != 0.0f) {
            canvas.restore();
        }

        // 绘制裁剪框边框
        canvas.drawRect(cropRect, mBorderPaint);

        // 绘制控制点（8 个）
        drawHandle(canvas, cropRect.left, cropRect.top);        // TL
        drawHandle(canvas, cropRect.right, cropRect.top);       // TR
        drawHandle(canvas, cropRect.left, cropRect.bottom);     // BL
        drawHandle(canvas, cropRect.right, cropRect.bottom);    // BR
        drawHandle(canvas, cropRect.centerX(), cropRect.top);   // T
        drawHandle(canvas, cropRect.centerX(), cropRect.bottom);// B
        drawHandle(canvas, cropRect.left, cropRect.centerY());  // L
        drawHandle(canvas, cropRect.right, cropRect.centerY()); // R
    }

    private RectF getRectF(int width, int height) {
        // 将 NDC [-1,1] → 归一化 [0,1] 
        // NDC: -1=bottom, +1=top
        // 屏幕归一化: 0=top, 1=bottom

        float imageLeft = 0.0f, imageRight = 1.0f, imageTop = 0.0f, imageBottom = 1.0f;

        if (mEditorView != null) {
            float scale = mEditorView.getRenderer().getScale();
            float translateX = mEditorView.getRenderer().getTranslateX();
            float translateY = mEditorView.getRenderer().getTranslateY();
            float rotation = mEditorView.getRenderer().getRotation();

            // 获取图片在NDC空间的实际边界
            float ndcLeft = mEditorView.getImageLeft();   // NDC (-1 ~ 1)
            float ndcRight = mEditorView.getImageRight();
            float ndcTop = mEditorView.getImageTop();
            float ndcBottom = mEditorView.getImageBottom();

            // 计算图像在NDC空间的原始宽高
            float ndcWidth = ndcRight - ndcLeft;
            float ndcHeight = ndcTop - ndcBottom; // 注意：NDC空间中top > bottom
            
            // 考虑旋转角度对宽高的影响
            int rotationQuadrant = (int) Math.abs(rotation % 360) / 90;
            float effectiveNdcWidth = ndcWidth;
            float effectiveNdcHeight = ndcHeight;
            
            // 当旋转90度或270度时，宽高互换
            if (rotationQuadrant == 1 || rotationQuadrant == 3) {
                effectiveNdcWidth = ndcHeight;
                effectiveNdcHeight = ndcWidth;
            }

            // 计算缩放后的NDC宽高
            float scaledNdcWidth = effectiveNdcWidth * scale;
            float scaledNdcHeight = effectiveNdcHeight * scale;

            // 计算图像在NDC空间的中心位置
            float ndcCenterX = (ndcLeft + ndcRight) / 2.0f;
            float ndcCenterY = (ndcTop + ndcBottom) / 2.0f;

            // 计算缩放和平移后的NDC边界
            float finalNdcLeft = ndcCenterX - scaledNdcWidth / 2.0f + translateX;
            float finalNdcRight = ndcCenterX + scaledNdcWidth / 2.0f + translateX;
            float finalNdcTop = ndcCenterY + scaledNdcHeight / 2.0f + translateY;
            float finalNdcBottom = ndcCenterY - scaledNdcHeight / 2.0f + translateY;

            // 将最终的NDC边界转换为屏幕归一化坐标 [0,1]
            imageLeft = (finalNdcLeft + 1.0f) / 2.0f;
            imageRight = (finalNdcRight + 1.0f) / 2.0f;
            imageTop = (1.0f - finalNdcTop) / 2.0f;     // NDC到屏幕Y轴翻转
            imageBottom = (1.0f - finalNdcBottom) / 2.0f; // NDC到屏幕Y轴翻转
        }

        // 将归一化裁剪坐标 [0,1] 映射到图像显示区域
        float left = imageLeft + mCropLeft * (imageRight - imageLeft);
        float top = imageTop + mCropTop * (imageBottom - imageTop);
        float right = imageLeft + mCropRight * (imageRight - imageLeft);
        float bottom = imageTop + mCropBottom * (imageBottom - imageTop);
        return new RectF(
                left * width,
                top * height,
                right * width,
                bottom * height
        );
    }
    
    // 计算旋转后的裁剪区域
    private RectF getRotatedRectF(int width, int height, float rotation, float centerX, float centerY) {
        // 获取原始的未旋转裁剪区域
        RectF originalRect = getRectF(width, height);
        
        // 如果旋转角度为0，直接返回原始区域
        if (rotation == 0.0f) {
            return originalRect;
        }
        
        // 创建旋转矩阵
        Matrix matrix = new Matrix();
        
        // 设置旋转中心和旋转角度
        matrix.setRotate(rotation, centerX, centerY);
        
        // 创建一个数组来存储原始矩形的四个顶点
        float[] points = {
            originalRect.left, originalRect.top,
            originalRect.right, originalRect.top,
            originalRect.left, originalRect.bottom,
            originalRect.right, originalRect.bottom
        };
        
        // 应用旋转变换
        matrix.mapPoints(points);
        
        // 计算旋转后矩形的边界
        float left = Math.min(Math.min(points[0], points[2]), Math.min(points[4], points[6]));
        float top = Math.min(Math.min(points[1], points[3]), Math.min(points[5], points[7]));
        float right = Math.max(Math.max(points[0], points[2]), Math.max(points[4], points[6]));
        float bottom = Math.max(Math.max(points[1], points[3]), Math.max(points[5], points[7]));
        
        // 返回旋转后的矩形
        return new RectF(left, top, right, bottom);
    }

    // 将屏幕坐标转换为归一化坐标
    private float[] screenToNormalized(float screenLeft, float screenTop, float screenRight, float screenBottom, int width, int height) {
        // 获取图片在屏幕上的实际显示区域
        float imageLeft = 0.0f, imageRight = 1.0f, imageTop = 0.0f, imageBottom = 1.0f;

        if (mEditorView != null) {
            float scale = mEditorView.getRenderer().getScale();
            float translateX = mEditorView.getRenderer().getTranslateX();
            float translateY = mEditorView.getRenderer().getTranslateY();

            // 获取图片在NDC空间的实际边界
            float ndcLeft = mEditorView.getImageLeft();   // NDC (-1 ~ 1)
            float ndcRight = mEditorView.getImageRight();
            float ndcTop = mEditorView.getImageTop();
            float ndcBottom = mEditorView.getImageBottom();

            // 计算图像在NDC空间的原始宽高
            float ndcWidth = ndcRight - ndcLeft;
            float ndcHeight = ndcTop - ndcBottom; // 注意：NDC空间中top > bottom

            // 计算缩放后的NDC宽高
            float scaledNdcWidth = ndcWidth * scale;
            float scaledNdcHeight = ndcHeight * scale;

            // 计算图像在NDC空间的中心位置
            float ndcCenterX = (ndcLeft + ndcRight) / 2.0f;
            float ndcCenterY = (ndcTop + ndcBottom) / 2.0f;

            // 计算缩放和平移后的NDC边界
            float finalNdcLeft = ndcCenterX - scaledNdcWidth / 2.0f + translateX;
            float finalNdcRight = ndcCenterX + scaledNdcWidth / 2.0f + translateX;
            float finalNdcTop = ndcCenterY + scaledNdcHeight / 2.0f + translateY;
            float finalNdcBottom = ndcCenterY - scaledNdcHeight / 2.0f + translateY;

            // 将最终的NDC边界转换为屏幕归一化坐标 [0,1]
            imageLeft = (finalNdcLeft + 1.0f) / 2.0f;
            imageRight = (finalNdcRight + 1.0f) / 2.0f;
            imageTop = (1.0f - finalNdcTop) / 2.0f;     // NDC到屏幕Y轴翻转
            imageBottom = (1.0f - finalNdcBottom) / 2.0f; // NDC到屏幕Y轴翻转
        }

        // 将屏幕像素坐标转换为屏幕归一化坐标 [0,1]
        float normalizedScreenLeft = screenLeft / width;
        float normalizedScreenTop = screenTop / height;
        float normalizedScreenRight = screenRight / width;
        float normalizedScreenBottom = screenBottom / height;

        // 将屏幕归一化坐标转换为相对于图片显示区域的归一化坐标 [0,1]
        float imageWidth = imageRight - imageLeft;
        float imageHeight = imageBottom - imageTop;

        float newCropLeft = (normalizedScreenLeft - imageLeft) / imageWidth;
        float newCropTop = (normalizedScreenTop - imageTop) / imageHeight;
        float newCropRight = (normalizedScreenRight - imageLeft) / imageWidth;
        float newCropBottom = (normalizedScreenBottom - imageTop) / imageHeight;

        return new float[]{newCropLeft, newCropTop, newCropRight, newCropBottom};
    }

    private void drawHandle(Canvas canvas, float x, float y) {
        canvas.drawRect(
                x - mHandleSizePx / 2f,
                y - mHandleSizePx / 2f,
                x + mHandleSizePx / 2f,
                y + mHandleSizePx / 2f,
                mHandlePaint
        );
    }

    //手势交互核心
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        // 获取图像旋转角度
        float rotation = 0.0f;
        if (mEditorView != null) {
            rotation = mEditorView.getRenderer().getRotation();
        }

        // 计算图像中心点（屏幕坐标）
        float centerX = getWidth() / 2.0f;
        float centerY = getHeight() / 2.0f;

        // 如果有旋转角度，将触摸坐标旋转回原始坐标系
        float[] transformedPoint = new float[2];
        if (rotation != 0.0f) {
            // 计算触摸点相对于中心点的偏移
            float dx = x - centerX;
            float dy = y - centerY;
            
            // 计算旋转后的坐标（将旋转角度取反，将触摸点旋转回未旋转状态）
            double angleRad = -Math.toRadians(rotation);
            transformedPoint[0] = (float)(dx * Math.cos(angleRad) - dy * Math.sin(angleRad)) + centerX;
            transformedPoint[1] = (float)(dx * Math.sin(angleRad) + dy * Math.cos(angleRad)) + centerY;
        } else {
            // 没有旋转，使用原始坐标
            transformedPoint[0] = x;
            transformedPoint[1] = y;
        }

        // 使用转换后的坐标进行触摸事件处理
        float transformedX = transformedPoint[0];
        float transformedY = transformedPoint[1];

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCurrentMode = getTouchedMode(transformedX, transformedY);
                mLastX = transformedX;
                mLastY = transformedY;
                return mCurrentMode != Mode.NONE;

            case MotionEvent.ACTION_MOVE:
                if (mCurrentMode != Mode.NONE) {
                    handleMove(transformedX, transformedY);
                    mLastX = transformedX;
                    mLastY = transformedY;
                    invalidate();
                    if (mListener != null) {
                        mListener.onCropRegionChanged(mCropLeft, mCropTop, mCropRight, mCropBottom);
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mCurrentMode = Mode.NONE;
                break;
        }
        return false;
    }

    // 根据点击位置判断操作模式
    private Mode getTouchedMode(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        RectF rect = getRectF(width, height);
        float half = mHandleSizePx / 2f;

        // 角
        if (Math.abs(x - rect.left) < half && Math.abs(y - rect.top) < half) return Mode.RESIZE_TL;
        if (Math.abs(x - rect.right) < half && Math.abs(y - rect.top) < half) return Mode.RESIZE_TR;
        if (Math.abs(x - rect.left) < half && Math.abs(y - rect.bottom) < half) return Mode.RESIZE_BL;
        if (Math.abs(x - rect.right) < half && Math.abs(y - rect.bottom) < half) return Mode.RESIZE_BR;

        // 边
        if (Math.abs(y - rect.top) < half && x > rect.left && x < rect.right) return Mode.RESIZE_T;
        if (Math.abs(y - rect.bottom) < half && x > rect.left && x < rect.right) return Mode.RESIZE_B;
        if (Math.abs(x - rect.left) < half && y > rect.top && y < rect.bottom) return Mode.RESIZE_L;
        if (Math.abs(x - rect.right) < half && y > rect.top && y < rect.bottom) return Mode.RESIZE_R;

        // 中心区域 → 拖拽整个框
        if (rect.contains(x, y)) return Mode.DRAG;

        return Mode.NONE;
    }

    // 处理移动逻辑
    private void handleMove(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        float dx = (x - mLastX) / width;
        float dy = (y - mLastY) / height;

        float oldWidth = mCropRight - mCropLeft;
        float oldHeight = mCropBottom - mCropTop;

        switch (mCurrentMode) {
            case DRAG:
                moveCropRegion(dx, dy);
                break;
            case RESIZE_TL:
                mCropLeft += dx;
                mCropTop += dy;
                break;
            case RESIZE_TR:
                mCropRight += dx;
                mCropTop += dy;
                break;
            case RESIZE_BL:
                mCropLeft += dx;
                mCropBottom += dy;
                break;
            case RESIZE_BR:
                mCropRight += dx;
                mCropBottom += dy;
                break;
            case RESIZE_L:
                mCropLeft += dx;
                break;
            case RESIZE_R:
                mCropRight += dx;
                break;
            case RESIZE_T:
                mCropTop += dy;
                break;
            case RESIZE_B:
                mCropBottom += dy;
                break;
        }

        // 如果设置了宽高比，需要保持比例
        if (mAspectRatio > 0.0f) {
            // 获取当前裁剪框在屏幕上的实际位置和尺寸
            RectF currentRect = getRectF(width, height);
            float screenWidth = currentRect.width();
            float screenHeight = currentRect.height();
            float currentRatio = screenWidth / screenHeight;

            // 计算屏幕上的中心点
            float screenCenterX = currentRect.centerX();
            float screenCenterY = currentRect.centerY();

            // 在屏幕坐标上调整尺寸以保持正确比例
            float newScreenWidth, newScreenHeight;
            if (currentRatio > mAspectRatio) {
                // 当前比例更宽，需要调整宽度
                newScreenHeight = screenHeight;
                newScreenWidth = mAspectRatio * newScreenHeight;
            } else {
                // 当前比例更高，需要调整高度
                newScreenWidth = screenWidth;
                newScreenHeight = newScreenWidth / mAspectRatio;
            }

            // 计算新的屏幕坐标
            float newScreenLeft = screenCenterX - newScreenWidth / 2.0f;
            float newScreenTop = screenCenterY - newScreenHeight / 2.0f;
            float newScreenRight = screenCenterX + newScreenWidth / 2.0f;
            float newScreenBottom = screenCenterY + newScreenHeight / 2.0f;

            // 将屏幕坐标转换回归一化坐标
            float[] newNormalizedCoords = screenToNormalized(newScreenLeft, newScreenTop, newScreenRight, newScreenBottom, width, height);
            mCropLeft = newNormalizedCoords[0];
            mCropTop = newNormalizedCoords[1];
            mCropRight = newNormalizedCoords[2];
            mCropBottom = newNormalizedCoords[3];
        }

        // 边界与有效性约束 - 使用浮点数边界
        mCropLeft = Math.max(0.0f, Math.min(mCropLeft, mCropRight - 0.01f));
        mCropTop = Math.max(0.0f, Math.min(mCropTop, mCropBottom - 0.01f));
        mCropRight = Math.min(1.0f, Math.max(mCropRight, mCropLeft + 0.01f));
        mCropBottom = Math.min(1.0f, Math.max(mCropBottom, mCropTop + 0.01f));
    }

    // 拖拽整个裁剪框（保持宽高比）
    private void moveCropRegion(float dx, float dy) {
        float w = mCropRight - mCropLeft;
        float h = mCropBottom - mCropTop;

        mCropLeft += dx;
        mCropTop += dy;
        mCropRight = mCropLeft + w;
        mCropBottom = mCropTop + h;

        // 边界限制 - 使用浮点数边界
        if (mCropLeft < 0.0f) {
            float shift = -mCropLeft;
            mCropLeft += shift;
            mCropRight += shift;
        }
        if (mCropTop < 0.0f) {
            float shift = -mCropTop;
            mCropTop += shift;
            mCropBottom += shift;
        }
        if (mCropRight > 1.0f) {
            float shift = mCropRight - 1.0f;
            mCropLeft -= shift;
            mCropRight -= shift;
        }
        if (mCropBottom > 1.0f) {
            float shift = mCropBottom - 1.0f;
            mCropTop -= shift;
            mCropBottom -= shift;
        }
    }
}