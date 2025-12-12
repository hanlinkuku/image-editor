package com.example.recyclerview.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ImageEditorRenderer implements GLSurfaceView.Renderer {
    private volatile String mPendingUriString = null;
    // 线程池，用于处理图片解码和处理操作
    private final ExecutorService mImageProcessingExecutor = Executors.newSingleThreadExecutor();
    // 主线程Handler，用于防抖机制
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    // 防抖间隔时间（毫秒）
    private static final long DEBOUNCE_INTERVAL_MS = 16; // 约60fps
    // 防抖Runnable
    private final Runnable mRenderRunnable = this::requestRenderDebounced;

    private volatile int mTextureId = 0;
    private volatile int mImageWidth = 0;
    private volatile int mImageHeight = 0;
    private static final String TAG = "ImageEditorApp";
    private final GLSurfaceView mSurfaceView;

    // 防抖渲染请求
    private void requestRenderDebounced() {
        mSurfaceView.requestRender();
    }
    
    // 防抖请求渲染方法
    private void requestRender() {
        mMainHandler.removeCallbacks(mRenderRunnable);
        mMainHandler.postDelayed(mRenderRunnable, DEBOUNCE_INTERVAL_MS);
    }

    // 在 ImageEditorRenderer 中新增字段：
    private final int[] mCachedViewport = new int[4];
    // 着色器
    private int mProgram;
    private int maPositionHandle;
    private int maTexCoordHandle;
    private int muTransformHandle;
    private int muTextureHandle;
    // ImageEditorRenderer.java 新增字段
    private boolean mIsCroppingMode = false; // false=正常编辑，true=裁剪选择预览

    // 新增方法：进入/退出裁剪预览
    public void setCroppingMode(final boolean isCropping) {
        mSurfaceView.queueEvent(() -> {
            mIsCroppingMode = isCropping;
            if (isCropping) {
                // 进入裁剪模式时，重置变换
                resetTransformToFit();
            }
            mSurfaceView.requestRender();
        });
    }

    // 设置滤镜类型
    public void setFilterType(final int filterType) {
        mSurfaceView.queueEvent(() -> {
            mCurrentFilter = filterType;
            mSurfaceView.requestRender();
        });
    }

    // 获取当前滤镜类型
    public int getFilterType() {
        return mCurrentFilter;
    }

    // 调整参数的setter方法
    public void setBrightness(final float brightness) {
        mSurfaceView.queueEvent(() -> {
            mBrightness = Math.max(-1.0f, Math.min(1.0f, brightness));
            requestRender();
        });
    }

    public void setContrast(final float contrast) {
        mSurfaceView.queueEvent(() -> {
            mContrast = Math.max(0.0f, Math.min(3.0f, contrast));
            requestRender();
        });
    }

    public void setSaturation(final float saturation) {
        mSurfaceView.queueEvent(() -> {
            mSaturation = Math.max(0.0f, Math.min(3.0f, saturation));
            requestRender();
        });
    }

    public void setSharpness(final float sharpness) {
        mSurfaceView.queueEvent(() -> {
            mSharpness = Math.max(-1.0f, Math.min(1.0f, sharpness));
            requestRender();
        });
    }

    // 批量设置调整参数，减少render调用次数
    public void setAdjustments(float brightness, float contrast, float saturation, float sharpness) {
        mSurfaceView.queueEvent(() -> {
            mBrightness = Math.max(-1.0f, Math.min(1.0f, brightness));
            mContrast = Math.max(0.0f, Math.min(3.0f, contrast));
            mSaturation = Math.max(0.0f, Math.min(3.0f, saturation));
            mSharpness = Math.max(-1.0f, Math.min(1.0f, sharpness));
            requestRender();
        });
    }

    // 获取当前调整参数
    public float getBrightness() {
        return mBrightness;
    }

    public float getContrast() {
        return mContrast;
    }

    public float getSaturation() {
        return mSaturation;
    }

    public float getSharpness() {
        return mSharpness;
    }
    // 移除原固定 VERTICES/TEX_COORDS —— 改为动态计算
    // 旋转角度（度数）
    private float mRotation = 0.0f;
    // 翻转标志
    private boolean mFlipHorizontal = false;
    private boolean mFlipVertical = false;

    // 滤镜类型常量
    public static final int FILTER_NONE = 0;
    public static final int FILTER_GRAYSCALE = 1;
    public static final int FILTER_COLD = 2;
    public static final int FILTER_WARM = 3;

    // 当前滤镜类型
    private int mCurrentFilter = FILTER_NONE;

    // 调整参数
    private float mBrightness = 0.0f;  // -1.0 到 1.0
    private float mContrast = 1.0f;    // 0.0 到 3.0
    private float mSaturation = 1.0f;  // 0.0 到 3.0
    private float mSharpness = 0.0f;   // -1.0 到 1.0

    // 滤镜uniform位置
    private int muFilterTypeHandle;
    // 调整参数uniform位置
    private int muBrightnessHandle;
    private int muContrastHandle;
    private int muSaturationHandle;
    private int muSharpnessHandle;

    private void initShaders() {
        String vertexShader =
                "attribute vec2 vPosition;\n" +
                        "attribute vec2 aCoordinate;\n" +
                        "uniform mat4 vMatrix;\n" +
                        "varying vec2 vCoord;\n" +
                        "void main() {\n" +
                        "  gl_Position = vMatrix * vec4(vPosition, 0.0, 1.0);\n" +
                        "  vCoord = aCoordinate;\n" +
                        "}";

        String fragmentShader =
                "precision mediump float;\n" +
                        "uniform sampler2D vTexture;\n" +
                        "uniform int uFilterType;\n" +
                        "uniform float uBrightness;\n" +
                        "uniform float uContrast;\n" +
                        "uniform float uSaturation;\n" +
                        "uniform float uSharpness;\n" +
                        "varying vec2 vCoord;\n" +
                        "\n" +
                        "// 锐化卷积核\n" +
                        "float sharpen(vec2 texCoord, sampler2D texture) {\n" +
                        "  float dx = 1.0 / 512.0;\n" +
                        "  float dy = 1.0 / 512.0;\n" +
                        "  float sum = 0.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(-dx, -dy)).r * -1.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(-dx, 0.0)).r * -1.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(-dx, dy)).r * -1.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(0.0, -dy)).r * -1.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(0.0, 0.0)).r * 9.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(0.0, dy)).r * -1.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(dx, -dy)).r * -1.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(dx, 0.0)).r * -1.0;\n" +
                        "  sum += texture2D(texture, texCoord + vec2(dx, dy)).r * -1.0;\n" +
                        "  return sum;\n" +
                        "}\n" +
                        "\n" +
                        "void main() {\n" +
                        "  vec4 color = texture2D(vTexture, vCoord);\n" +
                        "  vec4 result = color;\n" +
                        "\n" +
                        "  // 应用滤镜\n" +
                        "  if (uFilterType == 1) { // 黑白滤镜\n" +
                        "    float gray = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;\n" +
                        "    result = vec4(gray, gray, gray, color.a);\n" +
                        "  } else if (uFilterType == 2) { // 冷色调滤镜\n" +
                        "    result.r = color.r * 0.8;\n" +
                        "    result.b = color.b * 1.2;\n" +
                        "  } else if (uFilterType == 3) { // 暖色调滤镜\n" +
                        "    result.r = color.r * 1.2;\n" +
                        "    result.b = color.b * 0.8;\n" +
                        "  }\n" +
                        "\n" +
                        "  // 应用亮度调整\n" +
                        "  result.rgb += uBrightness;\n" +
                        "\n" +
                        "  // 应用对比度调整\n" +
                        "  result.rgb = (result.rgb - 0.5) * uContrast + 0.5;\n" +
                        "\n" +
                        "  // 应用饱和度调整\n" +
                        "  float gray = 0.299 * result.r + 0.587 * result.g + 0.114 * result.b;\n" +
                        "  result.rgb = mix(vec3(gray), result.rgb, uSaturation);\n" +
                        "\n" +
                        "  // 应用锐度调整\n" +
                        "  if (uSharpness != 0.0) {\n" +
                        "    float sharpenedR = sharpen(vCoord, vTexture);\n" +
                        "    float sharpenedG = sharpen(vCoord, vTexture);\n" +
                        "    float sharpenedB = sharpen(vCoord, vTexture);\n" +
                        "    vec3 sharpened = vec3(sharpenedR, sharpenedG, sharpenedB);\n" +
                        "    result.rgb = mix(result.rgb, sharpened, uSharpness);\n" +
                        "  }\n" +
                        "\n" +
                        "  // 确保颜色值在有效范围内\n" +
                        "  result.rgb = clamp(result.rgb, 0.0, 1.0);\n" +
                        "\n" +
                        "  gl_FragColor = result;\n" +
                        "}\n";

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vs);
        GLES20.glAttachShader(mProgram, fs);
        GLES20.glLinkProgram(mProgram);

        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aCoordinate");
        muTransformHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        muTextureHandle = GLES20.glGetUniformLocation(mProgram, "vTexture");
        // 获取滤镜类型的uniform位置
        muFilterTypeHandle = GLES20.glGetUniformLocation(mProgram, "uFilterType");
        // 获取调整参数的uniform位置
        muBrightnessHandle = GLES20.glGetUniformLocation(mProgram, "uBrightness");
        muContrastHandle = GLES20.glGetUniformLocation(mProgram, "uContrast");
        muSaturationHandle = GLES20.glGetUniformLocation(mProgram, "uSaturation");
        muSharpnessHandle = GLES20.glGetUniformLocation(mProgram, "uSharpness");

        // 检查链接状态
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Shader link failed: " + GLES20.glGetProgramInfoLog(mProgram));
            mProgram = 0;
        } else {
            Log.d(TAG, "✅ Shader program linked");
        }
    }

    // ======== 核心修改：固定 ortho 为 NDC，缩放/平移用额外矩阵 ========
    private final float[] mMvpMatrix = new float[16];      // 固定 ortho
    private final float[] mTransformMatrix = new float[16]; // 缩放+平移
    private float mScale = 1.0f;
    private float mTranslateX = 0.0f, mTranslateY = 0.0f;

    // 裁剪（纹理坐标 0~1）
    // 显示用的裁剪区域（当前不使用，保持全图显示）
    private float mCropLeft = 0.0f, mCropTop = 0.0f, mCropRight = 1.0f, mCropBottom = 1.0f;
    // 导出用的裁剪区域
    private float mExportCropLeft = 0.0f, mExportCropTop = 0.0f, mExportCropRight = 1.0f, mExportCropBottom = 1.0f;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTexCoordBuffer;

    // 导出等字段（略，保持不变）
    public interface OnExportListener {
        void onExportSuccess(Bitmap bitmap);
        void onExportFailed(Exception e);
    }
    private OnExportListener mExportListener;
    private int mFboId = 0;
    private int mRenderTextureId = 0;
    private int mExportWidth = 0, mExportHeight = 0;

    public ImageEditorRenderer(GLSurfaceView surfaceView) {
        this.mSurfaceView = surfaceView;
        initBuffers();
        Log.d(TAG, "✅ ImageEditorRenderer created");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f); // 使用灰色背景色
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        initShaders();

        // 固定 ortho：屏幕坐标 [-1,1] × [-1,1]
        android.opengl.Matrix.orthoM(mMvpMatrix, 0, -1f, 1f, -1f, 1f, -1f, 3f); // 🔧 注意 bottom/top 顺序修正
        tryLoadPendingImage();
        Log.d(TAG, "✅ onSurfaceCreated completed, program: " + mProgram);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        GLES20.glViewport(0, 0, width, height);
        // 缓存 viewport 给 UI 线程用
        mCachedViewport[0] = 0;
        mCachedViewport[1] = 0;
        mCachedViewport[2] = width;
        mCachedViewport[3] = height;

        // 尺寸变化时重算顶点（保持比例）
        updateQuadVertices();
    }

    // 图片的实际显示区域（NDC 空间，[-1,1]）
    private float mImageLeft = -1.0f, mImageRight = 1.0f, mImageBottom = -1.0f, mImageTop = 1.0f;

    // 始终返回铺满整个视口的顶点坐标（NDC 空间）
    private float[] computeQuadVertices(float imageAspect, float viewAspect) {
        float left, right, bottom, top;
        float ndcWidth, ndcHeight; // 声明NDC空间的宽高变量

        // 考虑旋转角度对宽高比的影响
        // 当旋转90度或270度时，图片的宽高会互换
        float effectiveImageAspect = imageAspect;
        int rotationQuadrant = (int) Math.abs(mRotation % 360) / 90;
        if (rotationQuadrant == 1 || rotationQuadrant == 3) {
            // 旋转90度或270度时，宽高互换
            effectiveImageAspect = 1.0f / imageAspect;
        }

        // 直接计算顶点坐标，确保图片按原比例缩放并完全显示在视图内
        if (effectiveImageAspect > viewAspect) {
            // 图像更宽 → 按宽适应，上下留黑边
            ndcWidth = 2.0f; // 充满整个视图宽度
            ndcHeight = ndcWidth * (viewAspect / effectiveImageAspect); // 保持图片原比例
        } else {
            // 图像更高 → 按高适应，左右留黑边
            ndcHeight = 2.0f; // 充满整个视图高度
            ndcWidth = ndcHeight * (effectiveImageAspect / viewAspect); // 保持图片原比例
        }

        left = -ndcWidth / 2.0f;
        right = ndcWidth / 2.0f;
        bottom = -ndcHeight / 2.0f;
        top = ndcHeight / 2.0f;

        mImageLeft = left;
        mImageRight = right;
        mImageBottom = bottom;
        mImageTop = top;

        return new float[] {
                left,  top,     // 左上
                left,  bottom,  // 左下
                right, top,     // 右上
                right, bottom   // 右下
        };
    }

    // 更新顶点缓冲区
    private void updateQuadVertices() {
        if (mImageWidth <= 0 || mImageHeight <= 0) return;
        int[] viewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0);
        int viewW = viewport[2], viewH = viewport[3];
        if (viewW <= 0 || viewH <= 0) return;

        float imageAspect = (float) mImageWidth / mImageHeight;
        float viewAspect = (float) viewW / viewH;

        float[] vertices = computeQuadVertices(imageAspect, viewAspect);
        mVertexBuffer.rewind();
        mVertexBuffer.put(vertices).position(0);

        // 通知图片边界变化
        if (mOnImageBoundsChangedListener != null) {
            mOnImageBoundsChangedListener.onImageBoundsChanged(mImageLeft, mImageTop, mImageRight, mImageBottom);
        }

        // 纹理坐标仍为 [0,1]（比例由顶点控制，纹理无需缩放）
    }

    // 更新变换矩阵：旋转 + 翻转 + 缩放 + 平移（NDC 空间）
    private void updateTransformMatrix() {
        android.opengl.Matrix.setIdentityM(mTransformMatrix, 0);

        // 注意：OpenGL 是列主序，变换顺序要反写
        // 先平移 → 再旋转 → 再翻转 → 最后缩放
        android.opengl.Matrix.translateM(mTransformMatrix, 0, mTranslateX, mTranslateY, 0.0f);
        android.opengl.Matrix.rotateM(mTransformMatrix, 0, mRotation, 0.0f, 0.0f, 1.0f); // 绕 Z 轴旋转

        // 翻转变换
        float flipX = mFlipHorizontal ? -1.0f : 1.0f;
        float flipY = mFlipVertical ? -1.0f : 1.0f;
        android.opengl.Matrix.scaleM(mTransformMatrix, 0, flipX, flipY, 1.0f);

        android.opengl.Matrix.scaleM(mTransformMatrix, 0, mScale, mScale, 1.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 使用灰色背景色
        GLES20.glClearColor(0.12f, 0.12f, 0.12f, 1.0f); // #1E1E1E
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mTextureId == 0 || mImageWidth == 0 || mImageHeight == 0) {
            Log.w(TAG, "Texture or image size not ready");
            return;
        }

        updateTransformMatrix();

        //合并矩阵：final = ortho × transform
        float[] finalMatrix = new float[16];
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mMvpMatrix, 0, mTransformMatrix, 0);

        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(muTransformHandle, 1, false, finalMatrix, 0);
        GLES20.glUniform1i(muFilterTypeHandle, mCurrentFilter);
        // 设置调整参数
        GLES20.glUniform1f(muBrightnessHandle, mBrightness);
        GLES20.glUniform1f(muContrastHandle, mContrast);
        GLES20.glUniform1f(muSaturationHandle, mSaturation);
        GLES20.glUniform1f(muSharpnessHandle, mSharpness);

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        //关键修改：纹理坐标永远使用全图 [0,1]，不再受 mCrop* 影响！
        float[] fullTexCoords = {
                0.0f, 0.0f,  // 左上
                0.0f, 1.0f,  // 左下
                1.0f, 0.0f,  // 右上
                1.0f, 1.0f   // 右下
        };
        mTexCoordBuffer.rewind();
        mTexCoordBuffer.put(fullTexCoords).position(0);
        GLES20.glVertexAttribPointer(maTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(muTextureHandle, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTexCoordHandle);
    }
    public void applyCrop() {
        mSurfaceView.queueEvent(() -> {
            if (mTextureId == 0 || mImageWidth <= 0 || mImageHeight <= 0) return;

            // 1. 计算裁剪后尺寸
            int cropW = (int) ((mExportCropRight - mExportCropLeft) * mImageWidth);
            int cropH = (int) ((mExportCropBottom - mExportCropTop) * mImageHeight);
            if (cropW <= 0 || cropH <= 0) return;

            // 2. 创建 FBO 渲染裁剪区域
            int[] fbo = new int[1], tex = new int[1];
            GLES20.glGenFramebuffers(1, fbo, 0);
            GLES20.glGenTextures(1, tex, 0);
            int newTexId = tex[0];

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, newTexId);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, cropW, cropH, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, newTexId, 0);

            // 3. 渲染：全屏 quad + 裁剪纹理坐标
            // 保存当前视口尺寸
            int[] originalViewport = new int[4];
            GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, originalViewport, 0);
            // 设置视口为裁剪后的尺寸
            GLES20.glViewport(0, 0, cropW, cropH);
            GLES20.glClearColor(0, 0, 0, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);

            // 正交矩阵：-1~1
            float[] ortho = new float[16];
            android.opengl.Matrix.orthoM(ortho, 0, -1, 1, -1, 1, -1, 1);
            GLES20.glUniformMatrix4fv(muTransformHandle, 1, false, ortho, 0);

            // 顶点：全屏
            float[] vertices = {-1, 1, -1, -1, 1, 1, 1, -1};
            mVertexBuffer.rewind();
            mVertexBuffer.put(vertices).position(0);
            GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
            GLES20.glEnableVertexAttribArray(maPositionHandle);

            // 纹理坐标：仅裁剪区域，修复 Y 轴翻转问题
            // OpenGL 纹理坐标 Y 轴与屏幕 Y 轴相反，所以需要翻转纹理坐标的 Y 分量

            float[] texCoords = {
                    mExportCropLeft,      mExportCropBottom,  // 左上 
                    mExportCropLeft,      mExportCropTop,     // 左下
                    mExportCropRight,     mExportCropBottom,  // 右上
                    mExportCropRight,     mExportCropTop      // 右下
            };
            mTexCoordBuffer.rewind();
            mTexCoordBuffer.put(texCoords).position(0);
            GLES20.glVertexAttribPointer(maTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer);
            GLES20.glEnableVertexAttribArray(maTexCoordHandle);


            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
            GLES20.glUniform1i(muTextureHandle, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            // 4. 清理
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            // 恢复原视口尺寸
            GLES20.glViewport(originalViewport[0], originalViewport[1], originalViewport[2], originalViewport[3]);
            GLES20.glDeleteFramebuffers(1, fbo, 0);

            // 5. 替换原纹理
            if (mTextureId != 0) {
                GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
            }
            mTextureId = newTexId;

            // 6. 更新图像尺寸 & 重置状态（保留旋转角度）
            mImageWidth = cropW;
            mImageHeight = cropH;
            mCropLeft = 0.0f;
            mCropTop = 0.0f;
            mCropRight = 1.0f;
            mCropBottom = 1.0f;
            // 同时重置导出的裁剪区域变量
            mExportCropLeft = 0.0f;
            mExportCropTop = 0.0f;
            mExportCropRight = 1.0f;
            mExportCropBottom = 1.0f;
            // 重置缩放和平移，但保留旋转角度
            mScale = 1.0f;
            mTranslateX = 0.0f;
            mTranslateY = 0.0f;
            mFlipHorizontal = false;
            mFlipVertical = false;
            updateQuadVertices(); // 重新计算顶点坐标，确保裁剪后的宽高比正确

            // 7. 通知 UI 更新裁剪框
            new Handler(Looper.getMainLooper()).post(() -> {
                if (mApplyCropListener != null) {
                    mApplyCropListener.onCropApplied(cropW, cropH);
                }
            });

            mSurfaceView.requestRender();
        });
    }

    // 回调接口
    public interface OnApplyCropListener {
        void onCropApplied(int newWidth, int newHeight);
    }
    private OnApplyCropListener mApplyCropListener;

    public void setOnApplyCropListener(OnApplyCropListener listener) {
        mApplyCropListener = listener;
    }

    // 图片显示区域变化的回调接口
    public interface OnImageBoundsChangedListener {
        void onImageBoundsChanged(float left, float top, float right, float bottom);
    }
    private OnImageBoundsChangedListener mOnImageBoundsChangedListener;

    public void setOnImageBoundsChangedListener(OnImageBoundsChangedListener listener) {
        mOnImageBoundsChangedListener = listener;
    }
    // 初始化
    private void initBuffers() {
        // 动态生成，初始可设为 1:1 占位
        mVertexBuffer = ByteBuffer.allocateDirect(8 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(new float[]{ -1,1, -1,-1, 1,1, 1,-1 }).position(0);

        mTexCoordBuffer = ByteBuffer.allocateDirect(8 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoordBuffer.put(new float[]{ 0,0, 0,1, 1,0, 1,1 }).position(0);
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // 检查编译状态
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compile failed: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    // 加载 & 变换
    public void loadImageFromUriString(String uriString) {
        if (uriString == null || uriString.isEmpty()) {
            Log.e(TAG, "Uri string is empty");
            return;
        }
        Log.d(TAG, "▶️ loadImageFromUriString: " + uriString);
        mPendingUriString = uriString;
        mSurfaceView.queueEvent(this::tryLoadPendingImage);
    }

    private void tryLoadPendingImage() {
        if (mProgram == 0) return;
        if (mPendingUriString != null) {
            String uri = mPendingUriString;
            mPendingUriString = null;
            loadImageFromUri(Uri.parse(uri));
        }
    }

    private void loadImageFromUri(Uri uri) {
        Context context = mSurfaceView.getContext();
        
        // 将图片解码和处理操作提交到线程池
        mImageProcessingExecutor.execute(() -> {
            try {
                // 1. 在工作线程中进行图片解码和处理
                final Bitmap processedBitmap;
                final int imageWidth;
                final int imageHeight;
                
                try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                    if (is == null) throw new RuntimeException("InputStream null for " + uri);

                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(is, null, opts);
                    imageWidth = opts.outWidth;
                    imageHeight = opts.outHeight;

                    try (InputStream is2 = context.getContentResolver().openInputStream(uri)) {
                        opts.inSampleSize = Math.max(1, Math.min(
                                opts.outWidth / 2000,
                                opts.outHeight / 2000
                        ));
                        opts.inJustDecodeBounds = false;
                        Bitmap bitmap = BitmapFactory.decodeStream(is2, null, opts);
                        if (bitmap == null) throw new RuntimeException("Bitmap decode returned null");

                        // 确保 config 正确（防 null config）
                        if (bitmap.getConfig() == null) {
                            Bitmap converted = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                            bitmap.recycle();
                            processedBitmap = converted;
                        } else {
                            processedBitmap = bitmap;
                        }
                    }
                }
                
                // 保存最终的位图引用，用于后续在GL线程中使用
                final Bitmap finalBitmap = processedBitmap;
                final int finalWidth = imageWidth;
                final int finalHeight = imageHeight;
                
                // 2. 当图片处理完成后，在GL线程中创建纹理和渲染
                mSurfaceView.queueEvent(() -> {
                    try {
                        if (mProgram == 0) return;
                        
                        if (mTextureId != 0) {
                            GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0);
                        }
                        int[] tex = new int[1];
                        GLES20.glGenTextures(1, tex, 0);
                        if (tex[0] == 0) {
                            Log.e(TAG, "❌ glGenTextures failed! GL Error: 0x" + Integer.toHexString(GLES20.glGetError()));
                            finalBitmap.recycle();
                            return;
                        }
                        mTextureId = tex[0];
                        Log.d(TAG, "✅ New texture ID: " + mTextureId);

                        // 设置纹理参数并上传数据
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
                        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, finalBitmap, 0);
                        int err = GLES20.glGetError();
                        if (err != GLES20.GL_NO_ERROR) {
                            Log.e(TAG, "texImage2D GL error: 0x" + Integer.toHexString(err));
                        }
                        
                        // 回收位图
                        finalBitmap.recycle();

                        // 更新图片尺寸
                        mImageWidth = finalWidth;
                        mImageHeight = finalHeight;
                        
                        // 加载后更新顶点 & 重置变换
                        resetTransformToFit();
                        updateQuadVertices(); //立即更新顶点

                        Log.d(TAG, "✅ Texture loaded: " + mImageWidth + "x" + mImageHeight + " from " + uri);
                        mSurfaceView.requestRender();
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Texture creation failed: " + uri, e);
                        finalBitmap.recycle();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Image processing failed: " + uri, e);
            }
        });
    }

    // 修改：仅重置用户变换，比例由顶点控制
    public void resetTransformToFit() {
        mScale = 1.0f;
        mTranslateX = 0.0f;
        mTranslateY = 0.0f;
        mRotation = 0.0f;
        mFlipHorizontal = false;
        mFlipVertical = false;
        mCropLeft = 0.0f; mCropTop = 0.0f;
        mCropRight = 1.0f; mCropBottom = 1.0f;
    }

    // 重置所有调整参数到默认值
    public void resetAdjustments() {
        mSurfaceView.queueEvent(() -> {
            mBrightness = 0.0f;
            mContrast = 1.0f;
            mSaturation = 1.0f;
            mSharpness = 0.0f;
            mSurfaceView.requestRender();
        });
    }

    // 公开方法：供手势控制调用
    public void setScale(float scale) {
        mSurfaceView.queueEvent(() -> {
            mScale = Math.max(0.3f, Math.min(scale, 10.0f));
            requestRender();
        });
    }


    public void setTranslate(float dx, float dy) {
        mSurfaceView.queueEvent(() -> {
            mTranslateX += dx;
            mTranslateY += dy;
            requestRender();
        });
    }

    public float getTranslateX() {
        return mTranslateX;
    }

    public float getTranslateY() {
        return mTranslateY;
    }

    // 旋转控制方法
    public void rotate(float degrees) {
        mSurfaceView.queueEvent(() -> {
            mRotation += degrees;
            // 保持角度在 [0, 360) 范围内
            while (mRotation >= 360.0f) {
                mRotation -= 360.0f;
            }
            while (mRotation < 0.0f) {
                mRotation += 360.0f;
            }
            requestRender();
        });
    }

    public void setRotation(float degrees) {
        mSurfaceView.queueEvent(() -> {
            mRotation = degrees;
            // 保持角度在 [0, 360) 范围内
            while (mRotation >= 360.0f) {
                mRotation -= 360.0f;
            }
            while (mRotation < 0.0f) {
                mRotation += 360.0f;
            }
            requestRender();
        });
    }

    public float getRotation() {
        return mRotation;
    }

    // 翻转控制方法
    public void setFlipHorizontal(boolean flip) {
        mFlipHorizontal = flip;
        mSurfaceView.requestRender();
    }

    public void setFlipVertical(boolean flip) {
        mFlipVertical = flip;
        mSurfaceView.requestRender();
    }

    public boolean isFlipHorizontal() {
        return mFlipHorizontal;
    }

    public boolean isFlipVertical() {
        return mFlipVertical;
    }

    public void setCropRegion(float left, float top, float right, float bottom) {
        // 更新导出用的裁剪区域，但不影响当前显示
        // 确保裁剪区域始终在图片边界内（0.0f~1.0f）
        mExportCropLeft = Math.max(0.0f, Math.min(left, 1.0f));
        mExportCropTop = Math.max(0.0f, Math.min(top, 1.0f));
        mExportCropRight = Math.max(mExportCropLeft + 0.01f, Math.min(right, 1.0f));
        mExportCropBottom = Math.max(mExportCropTop + 0.01f, Math.min(bottom, 1.0f));
        // 不再立即请求渲染，避免影响当前显示
    }

    // ———— 导出 & 其他（保持不变）————
    public void setOnExportListener(OnExportListener listener) {
        mExportListener = listener;
    }

    public void export() {
        mSurfaceView.queueEvent(this::doExport);
    }

    private void doExport() {
        Log.d(TAG, "doExport started");
        try {
            if (mExportListener == null) {
                Log.e(TAG, "mExportListener is null!");
                return;
            }

            // Check if image dimensions are valid
            if (mImageWidth <= 0 || mImageHeight <= 0) {
                Log.e(TAG, "Image dimensions are invalid: " + mImageWidth + "x" + mImageHeight);
                throw new RuntimeException("Invalid image dimensions");
            }

            // 使用专门的导出裁剪区域，不影响当前显示
            float cropWidth = (mExportCropRight - mExportCropLeft) * mImageWidth;
            float cropHeight = (mExportCropBottom - mExportCropTop) * mImageHeight;

            // 考虑旋转角度对宽高的影响
            // 当旋转90度或270度时，宽高会互换
            int rotationQuadrant = (int) Math.abs(mRotation % 360) / 90;
            int finalWidth, finalHeight;
            
            if (rotationQuadrant == 1 || rotationQuadrant == 3) {
                // 旋转90度或270度时，宽高互换
                finalWidth = (int) cropHeight;
                finalHeight = (int) cropWidth;
            } else {
                finalWidth = (int) cropWidth;
                finalHeight = (int) cropHeight;
            }

            // Ensure crop dimensions are valid
            if (finalWidth <= 0 || finalHeight <= 0) {
                Log.e(TAG, "Crop dimensions are invalid: " + finalWidth + "x" + finalHeight);
                throw new RuntimeException("Invalid crop dimensions");
            }

            mExportWidth = finalWidth;
            mExportHeight = finalHeight;
            Log.d(TAG, "Exporting with dimensions: " + mExportWidth + "x" + mExportHeight);
            setupFboForExport();

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
            GLES20.glViewport(0, 0, mExportWidth, mExportHeight);

            // 清屏为透明
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


            drawForExport();

            ByteBuffer buffer = ByteBuffer.allocateDirect(mExportWidth * mExportHeight * 4);
            GLES20.glReadPixels(0, 0, mExportWidth, mExportHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

            Bitmap bitmap = Bitmap.createBitmap(mExportWidth, mExportHeight, Bitmap.Config.ARGB_8888);
            buffer.rewind();
            bitmap.copyPixelsFromBuffer(buffer);

            // 不再需要额外的垂直翻转，因为纹理坐标已经处理了Y轴问题
            Bitmap flipped = bitmap;

            cleanupFbo();

            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Log.d(TAG, "Calling onExportSuccess");
                mExportListener.onExportSuccess(flipped);
            });

        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            cleanupFbo();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Log.d(TAG, "Calling onExportFailed");
                mExportListener.onExportFailed(e);
            });
        }
    }
    /**
     * 专用于导出：绘制裁剪后图像
     */
    private void drawForExport() {
        if (mProgram == 0 || mTextureId == 0) return;

        // 固定 ortho（-1~1），顶点铺满整个 NDC（FBO 尺寸 = 导出尺寸）
        float[] ortho = new float[16];
        android.opengl.Matrix.orthoM(ortho, 0, -1f, 1f, -1f, 1f, -1f, 1f);

        // 应用变换矩阵（包括旋转、翻转）- 导出时不需要缩放和平移
        // 只需要旋转和翻转效果
        float[] transformMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(transformMatrix, 0);
        
        // 旋转（导出时需要包含旋转效果）
        android.opengl.Matrix.rotateM(transformMatrix, 0, mRotation, 0.0f, 0.0f, 1.0f);
        
        // 翻转
        float flipX = mFlipHorizontal ? -1.0f : 1.0f;
        float flipY = mFlipVertical ? -1.0f : 1.0f;
        android.opengl.Matrix.scaleM(transformMatrix, 0, flipX, flipY, 1.0f);
        
        // 组合矩阵
        float[] finalMatrix = new float[16];
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, ortho, 0, transformMatrix, 0);

        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(muTransformHandle, 1, false, finalMatrix, 0);
        GLES20.glUniform1i(muFilterTypeHandle, mCurrentFilter);

        // 顶点：铺满 FBO 全屏
        float[] fullQuad = {
                -1.0f,  1.0f,  // 左上
                -1.0f, -1.0f,  // 左下
                1.0f,  1.0f,  // 右上
                1.0f, -1.0f   // 右下
        };
        mVertexBuffer.rewind();
        mVertexBuffer.put(fullQuad).position(0);

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        // 纹理坐标 = 导出用的裁剪区域，修复 Y 轴翻转问题
        // 与裁剪操作保持一致的 Y 轴处理方式
        float[] texCoords = {
                mExportCropLeft,      mExportCropBottom,  // 左上
                mExportCropLeft,      mExportCropTop,     // 左下
                mExportCropRight,     mExportCropBottom,  // 右上
                mExportCropRight,     mExportCropTop      // 右下
        };
        mTexCoordBuffer.rewind();
        mTexCoordBuffer.put(texCoords).position(0);

        GLES20.glEnableVertexAttribArray(maTexCoordHandle);
        GLES20.glVertexAttribPointer(maTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(muTextureHandle, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTexCoordHandle);
    }
    private void setupFboForExport() {
        int[] fbo = new int[1], tex = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);
        mFboId = fbo[0];
        GLES20.glGenTextures(1, tex, 0);
        mRenderTextureId = tex[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRenderTextureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mExportWidth, mExportHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mRenderTextureId, 0);

        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("FBO incomplete: " + status);
        }
    }

    private void cleanupFbo() {
        if (mFboId != 0) {
            GLES20.glDeleteFramebuffers(1, new int[]{mFboId}, 0);
            mFboId = 0;
        }
        if (mRenderTextureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{mRenderTextureId}, 0);
            mRenderTextureId = 0;
        }
    }

    public float getScale() {
        return mScale;
    }

    public void getViewport(int[] viewport) {
        if (viewport.length >= 4) {
            System.arraycopy(mCachedViewport, 0, viewport, 0, 4);
        }
    }

    // 暴露图片的实际显示区域
    public float getImageLeft() { return mImageLeft; }
    public float getImageRight() { return mImageRight; }
    public float getImageBottom() { return mImageBottom; }
    public float getImageTop() { return mImageTop; }

}