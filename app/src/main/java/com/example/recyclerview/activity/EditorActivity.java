// app/src/main/java/com/example/recyclerview/activity/EditorActivity.java
package com.example.recyclerview.activity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recyclerview.R;
import com.example.recyclerview.adapter.FeatureAdapter;
import com.example.recyclerview.adapter.SubFeatureAdapter;
import com.example.recyclerview.entity.FeatureItem;
import com.example.recyclerview.entity.SubFeatureItem;
import com.example.recyclerview.helper.EditHistoryManager;
import com.example.recyclerview.helper.DatabaseHelper;
import com.example.recyclerview.view.CropOverlayView;
import com.example.recyclerview.view.ImageEditorRenderer;
import com.example.recyclerview.view.ImageEditorView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class EditorActivity extends AppCompatActivity {
    private static final String TAG = "ImageEditorApp";

    private ImageEditorView mEditorView;
    private CropOverlayView mCropOverlayView;
    private DatabaseHelper mDatabaseHelper;
    private RecyclerView mSubFeatureRecyclerView;
    private SubFeatureAdapter mSubFeatureAdapter;
    private List<FeatureItem> mFeatureItems;
    private List<SubFeatureItem> mSubFeatureItems;
    private ImageView mUndoButton;
    private ImageView mRedoButton;
    private ImageView mApplyButton;

    // 当前选中的功能ID
    private int mCurrentFeatureId = -1;
    // 撤销/重做历史记录
    private final EditHistoryManager mHistoryManager = new EditHistoryManager();

    // 历史记录项类
//    private static class EditHistoryItem {
//        int featureId;
//        int subFeatureId;
//        Object data;
//
//        EditHistoryItem(int featureId, int subFeatureId, Object data) {
//            this.featureId = featureId;
//            this.subFeatureId = subFeatureId;
//            this.data = data;
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // 初始化数据库
        mDatabaseHelper = new DatabaseHelper(this);

        mEditorView = findViewById(R.id.glEditor);
        mCropOverlayView = findViewById(R.id.cropOverlayView);
        // 设置编辑器视图引用，让裁剪框能够获取图片的实际显示区域
        mCropOverlayView.setEditorView(mEditorView);

        Button btnApplyCrop = findViewById(R.id.btnApplyCrop);

        ImageView mBackButton = findViewById(R.id.btnBack);
        Button mExportButton = findViewById(R.id.btnExport);
        mUndoButton = findViewById(R.id.btnUndo);
        mRedoButton = findViewById(R.id.btnRedo);
        mApplyButton = findViewById(R.id.btnApply);

        // 初始化按钮状态
        updateHistoryButtons();
        updateApplyButtonState();

        // 撤销按钮点击事件
        mUndoButton.setOnClickListener(v -> {
            if (canUndo()) {
                undo();
                updateHistoryButtons();
            }
        });

        // 前进按钮点击事件
        mRedoButton.setOnClickListener(v -> {
            if (canRedo()) {
                redo();
                updateHistoryButtons();
            }
        });

        // 应用按钮点击事件
        mApplyButton.setOnClickListener(v -> applyCurrentEdit());

        // 初始化底部功能菜单RecyclerView
        RecyclerView mMainFeatureRecyclerView = findViewById(R.id.mainFeatureRecyclerView);
        mSubFeatureRecyclerView = findViewById(R.id.subFeatureRecyclerView);

        // 设置RecyclerView布局管理器
        LinearLayoutManager mainLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mMainFeatureRecyclerView.setLayoutManager(mainLayoutManager);

        LinearLayoutManager subLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mSubFeatureRecyclerView.setLayoutManager(subLayoutManager);

        // 初始化功能菜单项
        initFeatureItems();

        // 初始化适配器
        FeatureAdapter mFeatureAdapter = new FeatureAdapter(mFeatureItems);
        mFeatureAdapter.setOnFeatureItemClickListener((position, featureItem) -> showSubFeatureMenu(featureItem.id));

        mMainFeatureRecyclerView.setAdapter(mFeatureAdapter);

        // 初始化子功能菜单项
        mSubFeatureItems = new ArrayList<>();
        mSubFeatureAdapter = new SubFeatureAdapter(mSubFeatureItems);
        mSubFeatureAdapter.setOnSubFeatureItemClickListener((position, subFeatureItem) -> handleSubFeatureClick(subFeatureItem));

        mSubFeatureRecyclerView.setAdapter(mSubFeatureAdapter);
        // 默认隐藏子功能菜单
        mSubFeatureRecyclerView.setVisibility(View.GONE);

        // 裁剪区域变化 → 同步到 Renderer
        mCropOverlayView.setOnCropRegionChangedListener((left, top, right, bottom) -> mEditorView.setCropRegion(left, top, right, bottom));

        // 监听图片边界变化，设置裁剪框初始位置
        mEditorView.getRenderer().setOnImageBoundsChangedListener((left, top, right, bottom) -> {
            // 图片加载完成后，将裁剪框初始化为图片显示区域的100%（留边距）
            float padding = 0.00f;
            float cropRight = 1.0f - padding;
            float cropBottom = 1.0f - padding;

            // 设置初始裁剪区域
            mCropOverlayView.setCropRegion(padding, padding, cropRight, cropBottom);
            mEditorView.setCropRegion(padding, padding, cropRight, cropBottom);
        });
        @SuppressLint("CutPasteId") Button btnExport = findViewById(R.id.btnExport);

        String mImageUriString = getIntent().getStringExtra("image_uri"); // 统一使用image_uri作为关键字

        if (mImageUriString != null) {
            mEditorView.loadImageFromUriString(mImageUriString); // ✅ 关键修改
        } else {
            Toast.makeText(this, "图片 URI 为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 设置监听（重置裁剪框 UI）
        mEditorView.setOnApplyCropListener((newW, newH) -> {
            // 裁剪后，裁剪框应回到全图
            mCropOverlayView.setCropRegion(0, 0, 1, 1);


        });
        btnExport.setOnClickListener(v -> exportImage());
        btnApplyCrop.setOnClickListener(v -> applyCrop());



        // 返回按钮点击事件
        mBackButton.setOnClickListener(v -> finish());

        // 导出按钮点击事件
        mExportButton.setOnClickListener(v -> exportImage());

    }

    // 初始化功能菜单项
    private void initFeatureItems() {
        mFeatureItems = new ArrayList<>();
        mFeatureItems.add(new FeatureItem(1, "裁剪", R.drawable.ic_cutting));
        mFeatureItems.add(new FeatureItem(2, "旋转", R.drawable.ic_rotate));
        mFeatureItems.add(new FeatureItem(3, "滤镜", R.drawable.ic_adjustment));
        mFeatureItems.add(new FeatureItem(4, "文字", R.drawable.ic_words));
        mFeatureItems.add(new FeatureItem(5, "贴纸", R.drawable.ic_sticker));
        mFeatureItems.add(new FeatureItem(6, "调整", R.drawable.ic_filter));

    }

    // 显示子功能菜单
    @SuppressLint("NotifyDataSetChanged")
    private void showSubFeatureMenu(int featureId) {
        mCurrentFeatureId = featureId;
        mSubFeatureItems.clear();

        // 根据功能ID控制编辑框的显示
        if (featureId == 1) { // 只有裁剪功能显示编辑框
            mCropOverlayView.setVisibility(View.VISIBLE);
        } else {
            mCropOverlayView.setVisibility(View.GONE);
        }

        // 更新应用按钮状态
        updateApplyButtonState();

        switch (featureId) {
            case 1: // 裁剪
                mSubFeatureItems.add(new SubFeatureItem(101, "自由", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(102, "1:1", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(103, "3:4", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(104, "4:3", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(105, "16:9", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(106, "9:16", R.drawable.ic_arrow_back));
                break;
            case 2: // 旋转
                mSubFeatureItems.add(new SubFeatureItem(201, "右旋", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(202, "左旋", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(203, "水平翻转", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(204, "垂直翻转", R.drawable.ic_arrow_back));
                break;
            case 3: // 滤镜
                mSubFeatureItems.add(new SubFeatureItem(301, "原图", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(302, "黑白", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(303, "冷色调", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(304, "暖色调", R.drawable.ic_arrow_back));
                break;

            case 4: // 文字
                mSubFeatureItems.add(new SubFeatureItem(401, "添加文字", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(402, "字体", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(403, "大小", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(404, "颜色", R.drawable.ic_arrow_back));
                break;
            case 5: // 贴纸
                mSubFeatureItems.add(new SubFeatureItem(501, "表情", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(502, "装饰", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(503, "标签", R.drawable.ic_arrow_back));
                break;
            case 6: // 调整
                mSubFeatureItems.add(new SubFeatureItem(601, "亮度", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(602, "对比度", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(603, "饱和度", R.drawable.ic_arrow_back));
                mSubFeatureItems.add(new SubFeatureItem(604, "锐度", R.drawable.ic_arrow_back));
                break;


        }

        mSubFeatureAdapter.notifyDataSetChanged();
        mSubFeatureRecyclerView.setVisibility(View.VISIBLE);
    }

    // 处理子功能项点击事件
    private void handleSubFeatureClick(SubFeatureItem item) {
        // 这里可以根据不同的子功能项执行相应的操作

        // 保存当前操作到历史记录
        int subFeatureId = item.id;
        saveToHistory(mCurrentFeatureId, subFeatureId, null);
        updateHistoryButtons();

        // 根据当前功能ID和子功能ID执行不同的操作
        switch (mCurrentFeatureId) {
            case 1: // 裁剪
                switch (subFeatureId) {
                    case 101: // 自由
                        mCropOverlayView.setAspectRatio(0.0f); // 0表示自由裁剪
                        break;
                    case 102: // 1:1
                        mCropOverlayView.setAspectRatio(1.0f);
                        break;
                    case 103: // 3:4
                        mCropOverlayView.setAspectRatio(3.0f / 4.0f);
                        break;
                    case 104: // 4:3
                        mCropOverlayView.setAspectRatio(4.0f / 3.0f);
                        break;
                    case 105: // 16:9
                        mCropOverlayView.setAspectRatio(16.0f / 9.0f);
                        break;
                    case 106: // 9:16
                        mCropOverlayView.setAspectRatio(9.0f / 16.0f);
                        break;
                }
                break;
            case 2: // 旋转
                switch (subFeatureId) {
                    case 201: // 左旋 (逆时针90度)
                        mEditorView.rotate(-90.0f);
                        saveToHistory(mCurrentFeatureId, subFeatureId, -90.0f);
                        break;
                    case 202: // 右旋 (顺时针90度)
                        mEditorView.rotate(90.0f);
                        saveToHistory(mCurrentFeatureId, subFeatureId, 90.0f);
                        break;
                    case 203: // 水平翻转
                        boolean flipHorizontal = !mEditorView.isFlipHorizontal();
                        mEditorView.setFlipHorizontal(flipHorizontal);
                        saveToHistory(mCurrentFeatureId, subFeatureId, flipHorizontal);
                        break;
                    case 204: // 垂直翻转
                        boolean flipVertical = !mEditorView.isFlipVertical();
                        mEditorView.setFlipVertical(flipVertical);
                        saveToHistory(mCurrentFeatureId, subFeatureId, flipVertical);
                        break;
                }
                break;
            case 3: // 滤镜
                switch (subFeatureId) {
                    case 301: // 原图
                        mEditorView.setFilterType(ImageEditorRenderer.FILTER_NONE);
                        break;
                    case 302: // 黑白
                        mEditorView.setFilterType(ImageEditorRenderer.FILTER_GRAYSCALE);
                        break;
                    case 303: // 冷色调
                        mEditorView.setFilterType(ImageEditorRenderer.FILTER_COLD);
                        break;
                    case 304: // 暖色调
                        mEditorView.setFilterType(ImageEditorRenderer.FILTER_WARM);
                        break;
                }
                break;
        }
    }
    private void syncCropRegionToRenderer() {
        float[] crop = mCropOverlayView.getCropRegion();
        mEditorView.setCropRegion(crop[0], crop[1], crop[2], crop[3]);
    }
    private void exportImage() {
        syncCropRegionToRenderer();
        mEditorView.setOnExportListener(new ImageEditorRenderer.OnExportListener() {
            @Override
            public void onExportSuccess(Bitmap bitmap) {
                Log.d(TAG, "✅ onExportSuccess called, saving bitmap...");
                saveBitmapToGallery(bitmap);
            }

            @Override
            public void onExportFailed(Exception e) {
                Log.e(TAG, "❌ Export failed", e);
                runOnUiThread(() -> Toast.makeText(EditorActivity.this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        mEditorView.export();
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        OutputStream fos = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "edit_image_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "RecyclerViewApp");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                Log.d(TAG, "✅ Saved image to gallery with URI: " + uri);
            }

            if (uri != null) {
                fos = getContentResolver().openOutputStream(uri);
            }
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            }

            // 将图片信息保存到数据库
            long dbResult = 0;
            if (uri != null) {
                dbResult = mDatabaseHelper.addPortfolioImage(uri.toString());
            }
            Log.d(TAG, "✅ Saved image to database with result: " + dbResult);

            // 跳转到导出成功页面
            Intent intent = new Intent(EditorActivity.this, ExportSuccessActivity.class);
            if (uri != null) {
                intent.putExtra("image_uri", uri.toString()); // 传递图片URI
            }
            startActivity(intent);
            finish(); // 关闭当前编辑页面
        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to save image: " + e.getMessage());
            Toast.makeText(this, "❌ 保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyCrop() {
        // 同步最新裁剪区域
        float[] crop = mCropOverlayView.getCropRegion();
        mEditorView.setCropRegion(crop[0], crop[1], crop[2], crop[3]);

        // 保存裁剪操作到历史记录
        saveToHistory(mCurrentFeatureId, -1, crop);
        updateHistoryButtons();

        // 应用裁剪
        mEditorView.applyCrop(); // 需在 ImageEditorView 中暴露此方法
    }

    // 应用当前编辑
    private void applyCurrentEdit() {
        if (mCurrentFeatureId == -1) {
            Toast.makeText(this, "请先选择一个编辑功能", Toast.LENGTH_SHORT).show();
            return;
        }

        // 根据当前选中的功能执行相应的应用操作
        switch (mCurrentFeatureId) {
            case 1: // 裁剪
                applyCrop();
                break;
            case 2: // 旋转
                // 应用旋转操作
                Toast.makeText(this, "旋转应用功能待实现", Toast.LENGTH_SHORT).show();
                break;
            case 3: // 滤镜
                // 应用滤镜操作
                // 滤镜效果已实时应用，无需额外操作
                Toast.makeText(this, "滤镜已应用", Toast.LENGTH_SHORT).show();
                break;
            case 4: // 文字
                // 应用文字操作
                Toast.makeText(this, "文字应用功能待实现", Toast.LENGTH_SHORT).show();
                break;
            case 5: // 贴纸
                // 应用贴纸操作
                Toast.makeText(this, "贴纸应用功能待实现", Toast.LENGTH_SHORT).show();
                break;
            case 6: // 调整
                // 应用调整操作
                Toast.makeText(this, "调整应用功能待实现", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, "当前功能的应用操作待实现", Toast.LENGTH_SHORT).show();
        }
    }

    // 保存操作到历史记录
    private void saveToHistory(int featureId, int subFeatureId, Object data) {
        mHistoryManager.save(featureId, subFeatureId, data);
    }

    // 撤销操作
    private void undo() {
        EditHistoryManager.EditHistoryItem item = mHistoryManager.undo();
        if (item != null) {
            Toast.makeText(this, "撤销未实现", Toast.LENGTH_SHORT).show();
        }
    }

    private void redo() {
        EditHistoryManager.EditHistoryItem item = mHistoryManager.redo();
        if (item != null) {
            Toast.makeText(this, "重做未实现: " + item.featureId + "/" + item.subFeatureId, Toast.LENGTH_SHORT).show();
        }
    }

    // 检查是否可以撤销
    private boolean canUndo() { return mHistoryManager.canUndo(); }
    private boolean canRedo() { return mHistoryManager.canRedo(); }

    //更新按钮状态
    private void updateButtonState(ImageView button, boolean enabled) {
        button.setEnabled(enabled);
        // 应用、撤销和重做按钮可点击时显示绿色，其他情况下与回退按钮颜色保持一致
        int color;
        if (button == mApplyButton || button == mUndoButton || button == mRedoButton) {
            color = enabled ? ContextCompat.getColor(this, android.R.color.holo_green_light) : ContextCompat.getColor(this, android.R.color.darker_gray);
        } else {
            color = enabled ? ContextCompat.getColor(this, android.R.color.holo_blue_light) : ContextCompat.getColor(this, android.R.color.darker_gray);
        }
        // 对于ImageView，使用setColorFilter来改变图像颜色
        button.setColorFilter(color);
    }
    // 检查当前功能是否可以应用
    private boolean canApply() {
        // 只有当选择了功能且该功能需要应用操作时返回true
        // 目前只有裁剪功能需要应用，其他功能要么实时生效要么待实现
        return mCurrentFeatureId == 1; // 裁剪功能
    }

    // 更新应用按钮状态
    private void updateApplyButtonState() {
        boolean enabled = canApply();
        updateButtonState(mApplyButton, enabled);
    }

    // 更新历史记录按钮状态
    private void updateHistoryButtons() {
        updateButtonState(mUndoButton, canUndo());
        updateButtonState(mRedoButton, canRedo());
    }
}