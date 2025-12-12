// app/src/main/java/com/example/recyclerview/view/ImageEditorView.java
package com.example.recyclerview.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.Nullable;

public class ImageEditorView extends GLSurfaceView {

    private ImageEditorRenderer mRenderer;

    // 手势相关
    private ScaleGestureDetector mScaleDetector;
    private float mLastTouchX, mLastTouchY;
    private boolean mIsScaling = false;
    private int mActivePointerId = -1;

    // 可调参数
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 10.0f;
    private GestureDetector mGestureDetector;
    public ImageEditorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageEditorView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        // 配置透明背景支持
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);

        mRenderer = new ImageEditorRenderer(this);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // 初始化缩放检测器
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (mRenderer == null) return false;
                float scaleFactor = detector.getScaleFactor();
                // 以手势中心为锚点缩放
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                applyScaleAround(scaleFactor, focusX, focusY);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mIsScaling = true;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                mIsScaling = false;
            }
        });

        // 🔹 新增：双击检测器
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mRenderer != null) {
                    mRenderer.resetTransformToFit(); 
                    requestRender();                  // 立即刷新
                }
                return true;
            }
        });
    }

    // 🔧 核心：以屏幕坐标 (focusX, focusY) 为中心缩放
    private void applyScaleAround(float scaleFactor, float focusX, float focusY) {
        if (mRenderer == null) return;

        // 1️获取当前 viewport 尺寸（用于坐标归一化）
        int[] viewport = new int[4];
        mRenderer.getViewport(viewport);
        int viewW = viewport[2];
        int viewH = viewport[3];
        if (viewW <= 0 || viewH <= 0) return;

        // 2将屏幕坐标 → NDC 坐标 [-1,1]
        float ndcFocusX = 2.0f * focusX / viewW - 1.0f;
        float ndcFocusY = 1.0f - 2.0f * focusY / viewH; // y 翻转

        // 3️计算缩放后的新中心偏移
        // 要求: focus 在缩放前后位置不变 → 解出 T'
        float oldScale = mRenderer.getScale(); 
        float newScale = oldScale * scaleFactor;
        newScale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));

        float deltaScale = newScale - oldScale;
        float deltaTx = -deltaScale * ndcFocusX / newScale;
        float deltaTy = -deltaScale * ndcFocusY / newScale;

        // 4️更新变换矩阵
        mRenderer.setScale(newScale);
        mRenderer.setTranslate(deltaTx, deltaTy); // 注意：setTranslate 是增量接口
    }

    //重写触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mRenderer == null) return false;

        // 先让 ScaleGestureDetector 处理（双指）
        mGestureDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);

        // 再处理单指平移
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mActivePointerId = event.getPointerId(0);
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                // 双指开始 → 暂停平移
                mActivePointerId = -1;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (!mIsScaling && mActivePointerId != -1) {
                    // 单指拖拽
                    int pointerIndex = event.findPointerIndex(mActivePointerId);
                    if (pointerIndex != -1) {
                        float currX = event.getX(pointerIndex);
                        float currY = event.getY(pointerIndex);

                        // 计算位移（屏幕坐标）
                        float dx = currX - mLastTouchX;
                        float dy = currY - mLastTouchY;

                        // 转为 NDC 位移
                        int[] viewport = new int[4];
                        mRenderer.getViewport(viewport);
                        int viewW = viewport[2], viewH = viewport[3];
                        if (viewW > 0 && viewH > 0) {
                            float ndcDx = 2.0f * dx / viewW;
                            float ndcDy = -2.0f * dy / viewH; // y 反向

                            mRenderer.setTranslate(ndcDx, ndcDy);
                        }

                        mLastTouchX = currX;
                        mLastTouchY = currY;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                // 单指抬起时恢复 active pointer
                if (event.getPointerCount() == 1) {
                    mActivePointerId = event.getPointerId(0);
                    mLastTouchX = event.getX();
                    mLastTouchY = event.getY();
                } else {
                    mActivePointerId = -1;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = -1;
                break;
            }
        }

   
        requestRender();  

        return true;  
    }

    //暴露给 Activity 的 loadImage 接口
    public void loadImageFromUriString(String uriString) {
        if (mRenderer != null) {
            mRenderer.loadImageFromUriString(uriString);
        }
    }

    // 调整参数接口
    public void setBrightness(float brightness) {
        if (mRenderer != null) {
            mRenderer.setBrightness(brightness);
        }
    }

    public void setContrast(float contrast) {
        if (mRenderer != null) {
            mRenderer.setContrast(contrast);
        }
    }

    public void setSaturation(float saturation) {
        if (mRenderer != null) {
            mRenderer.setSaturation(saturation);
        }
    }

    public void setSharpness(float sharpness) {
        if (mRenderer != null) {
            mRenderer.setSharpness(sharpness);
        }
    }
    
    // 批量设置调整参数，减少render调用次数，提高性能
    public void setAdjustments(float brightness, float contrast, float saturation, float sharpness) {
        if (mRenderer != null) {
            mRenderer.setAdjustments(brightness, contrast, saturation, sharpness);
        }
    }

    public float getBrightness() {
        if (mRenderer != null) {
            return mRenderer.getBrightness();
        }
        return 0.0f;
    }

    public float getContrast() {
        if (mRenderer != null) {
            return mRenderer.getContrast();
        }
        return 1.0f;
    }

    public float getSaturation() {
        if (mRenderer != null) {
            return mRenderer.getSaturation();
        }
        return 1.0f;
    }

    public float getSharpness() {
        if (mRenderer != null) {
            return mRenderer.getSharpness();
        }
        return 0.0f;
    }

    public void resetAdjustments() {
        if (mRenderer != null) {
            mRenderer.resetAdjustments();
        }
    }

    // 导出相关
    public void setOnExportListener(ImageEditorRenderer.OnExportListener listener) {
        if (mRenderer != null) {
            mRenderer.setOnExportListener(listener);
        }
    }

    public void export() {
        if (mRenderer != null) {
            mRenderer.export();
        }
    }

    // 暴露控制接口
    public void setScale(float scale) {
        if (mRenderer != null) mRenderer.setScale(scale);
    }

    public void setTranslate(float dx, float dy) {
        if (mRenderer != null) mRenderer.setTranslate(dx, dy);
    }

    public void setCropRegion(float left, float top, float right, float bottom) {
        if (mRenderer != null) mRenderer.setCropRegion(left, top, right, bottom);
    }
    
    // 设置滤镜类型
    public void setFilterType(int filterType) {
        if (mRenderer != null) {
            mRenderer.setFilterType(filterType);
        }
    }
    
    // 获取当前滤镜类型
    public int getFilterType() {
        if (mRenderer != null) {
            return mRenderer.getFilterType();
        }
        return ImageEditorRenderer.FILTER_NONE;
    }

    // 暴露图片的实际显示区域
    public float getImageLeft() {
        if (mRenderer != null) return mRenderer.getImageLeft();
        return -1.0f;
    }

    public float getImageRight() {
        if (mRenderer != null) return mRenderer.getImageRight();
        return 1.0f;
    }

    public float getImageBottom() {
        if (mRenderer != null) return mRenderer.getImageBottom();
        return -1.0f;
    }

    public float getImageTop() {
        if (mRenderer != null) return mRenderer.getImageTop();
        return 1.0f;
    }

    // 暴露渲染器实例
    public ImageEditorRenderer getRenderer() {
        return mRenderer;
    }

    public void applyCrop() {
        if (mRenderer != null) mRenderer.applyCrop();
    }


    public void setOnApplyCropListener(ImageEditorRenderer.OnApplyCropListener listener) {
        if (mRenderer != null) {
            mRenderer.setOnApplyCropListener(listener);
        }
    }

    // 旋转控制接口
    public void rotate(float degrees) {
        if (mRenderer != null) mRenderer.rotate(degrees);
    }

    public void setRotation(float degrees) {
        if (mRenderer != null) mRenderer.setRotation(degrees);
    }

    public float getRotation() {
        if (mRenderer != null) return mRenderer.getRotation();
        return 0.0f;
    }

    // 翻转控制接口
    public void setFlipHorizontal(boolean flip) {
        if (mRenderer != null) mRenderer.setFlipHorizontal(flip);
    }

    public void setFlipVertical(boolean flip) {
        if (mRenderer != null) mRenderer.setFlipVertical(flip);
    }

    public boolean isFlipHorizontal() {
        if (mRenderer != null) return mRenderer.isFlipHorizontal();
        return false;
    }

    public boolean isFlipVertical() {
        if (mRenderer != null) return mRenderer.isFlipVertical();
        return false;
    }



}