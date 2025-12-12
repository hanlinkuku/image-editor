// app/src/main/java/com/example/recyclerview/activity/EditorActivity.java
package com.example.recyclerview.activity

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recyclerview.R
import com.example.recyclerview.adapter.FeatureAdapter
import com.example.recyclerview.adapter.FeatureAdapter.OnFeatureItemClickListener
import com.example.recyclerview.adapter.SubFeatureAdapter
import com.example.recyclerview.adapter.SubFeatureAdapter.OnSubFeatureItemClickListener
import com.example.recyclerview.entity.FeatureItem
import com.example.recyclerview.entity.SubFeatureItem
import com.example.recyclerview.helper.DatabaseHelper
import com.example.recyclerview.helper.EditHistoryManager
import com.example.recyclerview.view.CropOverlayView
import com.example.recyclerview.view.CropOverlayView.OnCropRegionChangedListener
import com.example.recyclerview.view.ImageEditorRenderer
import com.example.recyclerview.view.ImageEditorRenderer.OnExportListener
import com.example.recyclerview.view.ImageEditorView
import java.io.File
import java.io.IOException
import java.io.OutputStream

class EditorActivity : AppCompatActivity() {
    private var mEditorView: ImageEditorView? = null
    private var mCropOverlayView: CropOverlayView? = null
    private var mDatabaseHelper: DatabaseHelper? = null
    private var mSubFeatureRecyclerView: RecyclerView? = null
    private var mSubFeatureAdapter: SubFeatureAdapter? = null
    private var mFeatureItems: MutableList<FeatureItem>? = null
    private var mSubFeatureItems: MutableList<SubFeatureItem>? = null
    private var mUndoButton: ImageView? = null
    private var mRedoButton: ImageView? = null
    private var mApplyButton: ImageView? = null

    // 调整功能相关
    private var mAdjustmentSeekBarContainer: View? = null
    private var mAdjustmentLabel: TextView? = null
    private var mAdjustmentSeekBar: SeekBar? = null
    private var mAdjustmentValue: TextView? = null
    private var mCurrentAdjustmentType = 601 // 默认亮度

    // 当前选中的功能ID
    private var mCurrentFeatureId = -1

    // 撤销/重做历史记录
    private val mHistoryManager = EditHistoryManager()

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        // 初始化数据库
        mDatabaseHelper = DatabaseHelper(this)

        mEditorView = findViewById(R.id.glEditor)
        mCropOverlayView = findViewById(R.id.cropOverlayView)
        // 设置编辑器视图引用，让裁剪框能够获取图片的实际显示区域
        mCropOverlayView!!.setEditorView(mEditorView)

        val btnApplyCrop = findViewById<Button>(R.id.btnApplyCrop)

        val mBackButton = findViewById<ImageView>(R.id.btnBack)
        val mExportButton = findViewById<Button>(R.id.btnExport)
        mUndoButton = findViewById(R.id.btnUndo)
        mRedoButton = findViewById(R.id.btnRedo)
        mApplyButton = findViewById(R.id.btnApply)

        // 初始化调整功能UI组件
        mAdjustmentSeekBarContainer = findViewById(R.id.adjustmentSeekBarContainer)
        mAdjustmentLabel = findViewById(R.id.adjustmentLabel)
        mAdjustmentSeekBar = findViewById(R.id.adjustmentSeekBar)
        mAdjustmentValue = findViewById(R.id.adjustmentValue)

        // 设置滑动条监听器
        mAdjustmentSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            @SuppressLint("DefaultLocale")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return

                // 将进度值从0-200转换为实际的调整值
                var value = 0.0f
                when (mCurrentAdjustmentType) {
                    601 -> {
                        value = (progress - 100) / 100.0f
                        mEditorView!!.brightness = value
                    }

                    602 -> {
                        value = progress / 66.67f
                        mEditorView!!.contrast = value
                    }

                    603 -> {
                        value = progress / 66.67f
                        mEditorView!!.saturation = value
                    }

                    604 -> {
                        value = (progress - 100) / 20.0f
                        mEditorView!!.setSharpness(value)
                    }
                }

                // 更新显示的调整值
                mAdjustmentValue!!.setText(String.format("%.1f", value))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 保存调整操作到历史记录
                var value = 0.0f
                when (mCurrentAdjustmentType) {
                    601 -> value = mEditorView!!.getBrightness()
                    602 -> value = mEditorView!!.getContrast()
                    603 -> value = mEditorView!!.getSaturation()
                    604 -> value = mEditorView!!.getSharpness()
                }
                saveToHistory(mCurrentFeatureId, mCurrentAdjustmentType, value)
            }
        })

        // 初始化按钮状态
        updateHistoryButtons()
        updateApplyButtonState()

        // 撤销按钮点击事件
        mUndoButton!!.setOnClickListener { v: View? ->
            if (canUndo()) {
                undo()
                updateHistoryButtons()
            }
        }

        // 前进按钮点击事件
        mRedoButton!!.setOnClickListener { v: View? ->
            if (canRedo()) {
                redo()
                updateHistoryButtons()
            }
        }

        // 应用按钮点击事件
        mApplyButton!!.setOnClickListener { v: View? -> applyCurrentEdit() }

        // 初始化底部功能菜单RecyclerView
        val mMainFeatureRecyclerView = findViewById<RecyclerView>(R.id.mainFeatureRecyclerView)
        mSubFeatureRecyclerView = findViewById<RecyclerView>(R.id.subFeatureRecyclerView)

        // 设置RecyclerView布局管理器
        val mainLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mMainFeatureRecyclerView.setLayoutManager(mainLayoutManager)

        val subLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mSubFeatureRecyclerView!!.setLayoutManager(subLayoutManager)

        // 初始化功能菜单项
        initFeatureItems()

        // 初始化适配器
        val mFeatureAdapter = FeatureAdapter(mFeatureItems!!)
        mFeatureAdapter.setOnFeatureItemClickListener(object : OnFeatureItemClickListener {
            override fun onFeatureItemClick(position: Int, featureItem: FeatureItem?) {
                showSubFeatureMenu(
                    featureItem!!.id
                )
            }
        })

        mMainFeatureRecyclerView.setAdapter(mFeatureAdapter)

        // 初始化子功能菜单项
        mSubFeatureItems = ArrayList()
        mSubFeatureAdapter = SubFeatureAdapter(mSubFeatureItems!!)
        mSubFeatureAdapter!!.setOnSubFeatureItemClickListener(object : OnSubFeatureItemClickListener {
            override fun onSubFeatureItemClick(position: Int, subFeatureItem: SubFeatureItem?) {
                handleSubFeatureClick(
                    subFeatureItem!!
                )
            }
        })

        mSubFeatureRecyclerView!!.setAdapter(mSubFeatureAdapter)
        // 默认隐藏子功能菜单
        mSubFeatureRecyclerView!!.visibility = View.GONE

        // 裁剪区域变化 → 同步到 Renderer
        mCropOverlayView!!.setOnCropRegionChangedListener(object : OnCropRegionChangedListener {
            override fun onCropRegionChanged(left: Float, top: Float, right: Float, bottom: Float) {
                mEditorView!!.setCropRegion(
                    left,
                    top,
                    right,
                    bottom
                )
            }
        })

        // 监听图片边界变化，设置裁剪框初始位置
        mEditorView!!.renderer
            .setOnImageBoundsChangedListener { left: Float, top: Float, right: Float, bottom: Float ->
                // 图片加载完成后，将裁剪框初始化为图片显示区域的100%（留边距）
                val padding = 0.00f
                val cropRight = 1.0f - padding
                val cropBottom = 1.0f - padding

                // 设置初始裁剪区域
                mCropOverlayView!!.setCropRegion(padding, padding, cropRight, cropBottom)
                mEditorView!!.setCropRegion(padding, padding, cropRight, cropBottom)
            }
        @SuppressLint("CutPasteId") val btnExport = findViewById<Button>(R.id.btnExport)

        val mImageUriString = getIntent().getStringExtra("image_uri") // 统一使用image_uri作为关键字

        if (mImageUriString != null) {
            mEditorView!!.loadImageFromUriString(mImageUriString) // ✅ 关键修改
        } else {
            Toast.makeText(this, "图片 URI 为空", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // 设置监听（重置裁剪框 UI）
        mEditorView!!.setOnApplyCropListener { newW: Int, newH: Int ->
            // 裁剪后，裁剪框应回到全图
            mCropOverlayView!!.setCropRegion(0f, 0f, 1f, 1f)
        }
        btnExport.setOnClickListener { v: View? -> exportImage() }
        btnApplyCrop.setOnClickListener { v: View? -> applyCrop() }


        // 返回按钮点击事件
        mBackButton.setOnClickListener { v: View? -> finish() }

        // 导出按钮点击事件
        mExportButton.setOnClickListener { v: View? -> exportImage() }
    }

    // 初始化功能菜单项
    private fun initFeatureItems() {
        mFeatureItems = ArrayList()
        mFeatureItems!!.add(FeatureItem(1, "裁剪", R.drawable.ic_cutting))
        mFeatureItems!!.add(FeatureItem(2, "旋转", R.drawable.ic_rotate))
        mFeatureItems!!.add(FeatureItem(3, "滤镜", R.drawable.ic_adjustment))
        mFeatureItems!!.add(FeatureItem(4, "调整", R.drawable.ic_filter))
        mFeatureItems!!.add(FeatureItem(5, "文字", R.drawable.ic_words))
        mFeatureItems!!.add(FeatureItem(6, "贴纸", R.drawable.ic_sticker))
    }

    // 显示子功能菜单
    @SuppressLint("NotifyDataSetChanged")
    private fun showSubFeatureMenu(featureId: Int) {
        mCurrentFeatureId = featureId
        mSubFeatureItems!!.clear()

        // 根据功能ID控制编辑框的显示
        if (featureId == 1) { // 只有裁剪功能显示编辑框
            mCropOverlayView!!.setVisibility(View.VISIBLE)
        } else {
            mCropOverlayView!!.setVisibility(View.GONE)
        }

        // 隐藏调整滑动条，除非当前功能是调整
        if (featureId != 4) {
            mAdjustmentSeekBarContainer!!.setVisibility(View.GONE)
        }

        // 更新应用按钮状态
        updateApplyButtonState()

        when (featureId) {
            1 -> {
                mSubFeatureItems!!.add(SubFeatureItem(101, "自由", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(102, "1:1", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(103, "3:4", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(104, "4:3", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(105, "16:9", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(106, "9:16", R.drawable.ic_component))
            }

            2 -> {
                Toast.makeText(this, "旋转与裁剪适配未完成", Toast.LENGTH_SHORT).show()
                mSubFeatureItems!!.add(SubFeatureItem(201, "右旋", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(202, "左旋", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(203, "水平翻转", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(204, "垂直翻转", R.drawable.ic_component))
            }

            3 -> {
                mSubFeatureItems!!.add(SubFeatureItem(301, "原图", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(302, "黑白", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(303, "冷色调", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(304, "暖色调", R.drawable.ic_component))
            }

            5 -> {
                Toast.makeText(this, "该功能未完成", Toast.LENGTH_SHORT).show()
                mSubFeatureItems!!.add(SubFeatureItem(401, "添加文字", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(402, "字体", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(403, "大小", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(404, "颜色", R.drawable.ic_component))
            }

            6 -> {
                Toast.makeText(this, "该功能未完成", Toast.LENGTH_SHORT).show()
                mSubFeatureItems!!.add(SubFeatureItem(501, "表情", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(502, "装饰", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(503, "标签", R.drawable.ic_component))
            }

            4 -> {
                mSubFeatureItems!!.add(SubFeatureItem(601, "亮度", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(602, "对比度", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(603, "饱和度", R.drawable.ic_component))
                mSubFeatureItems!!.add(SubFeatureItem(604, "锐度", R.drawable.ic_component))
                mAdjustmentSeekBarContainer!!.setVisibility(View.VISIBLE)
            }
        }

        mSubFeatureAdapter!!.notifyDataSetChanged()
        mSubFeatureRecyclerView!!.setVisibility(View.VISIBLE)
    }

    // 处理子功能项点击事件
    private fun handleSubFeatureClick(item: SubFeatureItem) {
        // 这里可以根据不同的子功能项执行相应的操作

        // 保存当前操作到历史记录

        val subFeatureId = item.id
        saveToHistory(mCurrentFeatureId, subFeatureId, null)
        updateHistoryButtons()

        // 根据当前功能ID和子功能ID执行不同的操作
        when (mCurrentFeatureId) {
            1 -> when (subFeatureId) {
                101 -> mCropOverlayView!!.setAspectRatio(0.0f) // 0表示自由裁剪
                102 -> mCropOverlayView!!.setAspectRatio(1.0f)
                103 -> mCropOverlayView!!.setAspectRatio(3.0f / 4.0f)
                104 -> mCropOverlayView!!.setAspectRatio(4.0f / 3.0f)
                105 -> mCropOverlayView!!.setAspectRatio(16.0f / 9.0f)
                106 -> mCropOverlayView!!.setAspectRatio(9.0f / 16.0f)
            }

            2 -> when (subFeatureId) {
                201 -> {
                    mEditorView!!.rotate(-90.0f)
                    saveToHistory(mCurrentFeatureId, subFeatureId, -90.0f)
                }

                202 -> {
                    mEditorView!!.rotate(90.0f)
                    saveToHistory(mCurrentFeatureId, subFeatureId, 90.0f)
                }

                203 -> {
                    val flipHorizontal = !mEditorView!!.isFlipHorizontal()
                    mEditorView!!.setFlipHorizontal(flipHorizontal)
                    saveToHistory(mCurrentFeatureId, subFeatureId, flipHorizontal)
                }

                204 -> {
                    val flipVertical = !mEditorView!!.isFlipVertical()
                    mEditorView!!.setFlipVertical(flipVertical)
                    saveToHistory(mCurrentFeatureId, subFeatureId, flipVertical)
                }
            }

            3 -> when (subFeatureId) {
                301 -> mEditorView!!.setFilterType(ImageEditorRenderer.FILTER_NONE)
                302 -> mEditorView!!.setFilterType(ImageEditorRenderer.FILTER_GRAYSCALE)
                303 -> mEditorView!!.setFilterType(ImageEditorRenderer.FILTER_COLD)
                304 -> mEditorView!!.setFilterType(ImageEditorRenderer.FILTER_WARM)
            }

            4 -> {
                mCurrentAdjustmentType = subFeatureId
                updateAdjustmentUI()
            }
        }
    }

    // 更新调整功能UI
    @SuppressLint("DefaultLocale")
    private fun updateAdjustmentUI() {
        var currentValue = 0.0f
        var label = ""
        val max = 200
        var progress = 100

        when (mCurrentAdjustmentType) {
            601 -> {
                label = "亮度"
                currentValue = mEditorView!!.getBrightness()
                progress = (currentValue * 100 + 100).toInt() // -1.0 到 1.0 转换为 0 到 200
            }

            602 -> {
                label = "对比度"
                currentValue = mEditorView!!.getContrast()
                progress = (currentValue * 66.67f).toInt() // 0.0 到 3.0 转换为 0 到 200
            }

            603 -> {
                label = "饱和度"
                currentValue = mEditorView!!.getSaturation()
                progress = (currentValue * 66.67f).toInt() // 0.0 到 3.0 转换为 0 到 200
            }

            604 -> {
                label = "锐度"
                currentValue = mEditorView!!.getSharpness()
                progress = (currentValue * 20 + 100).toInt() // -5.0 到 5.0 转换为 0 到 200
            }
        }

        mAdjustmentLabel!!.setText(label)
        mAdjustmentValue!!.setText(String.format("%.1f", currentValue))
        mAdjustmentSeekBar!!.setProgress(progress)
        mAdjustmentSeekBar!!.setMax(max)
    }

    private fun syncCropRegionToRenderer() {
        val crop = mCropOverlayView!!.cropRegion
        mEditorView!!.setCropRegion(crop!![0], crop[1], crop[2], crop[3])
    }

    private fun exportImage() {
        syncCropRegionToRenderer()
        mEditorView!!.setOnExportListener(object : OnExportListener {
            override fun onExportSuccess(bitmap: Bitmap) {
                Log.d(TAG, "✅ onExportSuccess called, saving bitmap...")
                saveBitmapToGallery(bitmap)
            }

            override fun onExportFailed(e: Exception) {
                Log.e(TAG, "❌ Export failed", e)
                runOnUiThread {
                    Toast.makeText(
                        this@EditorActivity,
                        "导出失败: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
        mEditorView!!.export()
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        var fos: OutputStream? = null
        try {
            val values = ContentValues()
            values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "edit_image_" + System.currentTimeMillis() + ".png"
            )
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + "RecyclerViewApp"
            )

            val uri =
                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                Log.d(TAG, "✅ Saved image to gallery with URI: " + uri)
            }

            if (uri != null) {
                fos = getContentResolver().openOutputStream(uri)
            }
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
            }

            // 将图片信息保存到数据库
            var dbResult: Long = 0
            if (uri != null) {
                dbResult = mDatabaseHelper!!.addPortfolioImage(uri.toString())
            }
            Log.d(TAG, "✅ Saved image to database with result: " + dbResult)

            // 跳转到导出成功页面
            val intent = Intent(this@EditorActivity, ExportSuccessActivity::class.java)
            if (uri != null) {
                intent.putExtra("image_uri", uri.toString()) // 传递图片URI
            }
            startActivity(intent)
            finish() // 关闭当前编辑页面
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to save image: " + e.message)
            Toast.makeText(this, "❌ 保存失败: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyCrop() {
        // 同步最新裁剪区域
        val crop = mCropOverlayView!!.cropRegion
        mEditorView!!.setCropRegion(crop!![0], crop[1], crop[2], crop[3])

        // 保存裁剪操作到历史记录
        saveToHistory(mCurrentFeatureId, -1, crop)
        updateHistoryButtons()

        // 应用裁剪
        mEditorView!!.applyCrop() // 需在 ImageEditorView 中暴露此方法
    }

    // 应用当前编辑
    private fun applyCurrentEdit() {
        if (mCurrentFeatureId == -1) {
            Toast.makeText(this, "请先选择一个编辑功能", Toast.LENGTH_SHORT).show()
            return
        }

        // 根据当前选中的功能执行相应的应用操作
        when (mCurrentFeatureId) {
            1 -> applyCrop()
            2 ->                 // 应用旋转操作
                Toast.makeText(this, "旋转应用功能待实现", Toast.LENGTH_SHORT).show()

            3 ->                 // 应用滤镜操作
                // 滤镜效果已实时应用，无需额外操作
                Toast.makeText(this, "滤镜已应用", Toast.LENGTH_SHORT).show()

            4 ->                 // 应用文字操作
                Toast.makeText(this, "文字应用功能待实现", Toast.LENGTH_SHORT).show()

            5 ->                 // 应用贴纸操作
                Toast.makeText(this, "贴纸应用功能待实现", Toast.LENGTH_SHORT).show()

            6 ->                 // 应用调整操作
                Toast.makeText(this, "调整应用功能待实现", Toast.LENGTH_SHORT).show()

            else -> Toast.makeText(this, "当前功能的应用操作待实现", Toast.LENGTH_SHORT).show()
        }
    }

    // 保存操作到历史记录
    private fun saveToHistory(featureId: Int, subFeatureId: Int, data: Any?) {
        mHistoryManager.save(featureId, subFeatureId, data)
    }

    // 撤销操作
    private fun undo() {
        val item = mHistoryManager.undo()
        if (item != null) {
            Toast.makeText(this, "撤销未实现", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redo() {
        val item = mHistoryManager.redo()
        if (item != null) {
            Toast.makeText(
                this,
                "重做未实现: " + item.featureId + "/" + item.subFeatureId,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 检查是否可以撤销
    private fun canUndo(): Boolean {
        return mHistoryManager.canUndo()
    }

    private fun canRedo(): Boolean {
        return mHistoryManager.canRedo()
    }

    //更新按钮状态
    private fun updateButtonState(button: ImageView, enabled: Boolean) {
        button.setEnabled(enabled)
        // 应用、撤销和重做按钮可点击时显示绿色，其他情况下与回退按钮颜色保持一致
        val color: Int
        if (button === mApplyButton || button === mUndoButton || button === mRedoButton) {
            color = if (enabled) ContextCompat.getColor(
                this,
                android.R.color.holo_green_light
            ) else ContextCompat.getColor(this, android.R.color.darker_gray)
        } else {
            color = if (enabled) ContextCompat.getColor(
                this,
                android.R.color.holo_blue_light
            ) else ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        // 对于ImageView，使用setColorFilter来改变图像颜色
        button.setColorFilter(color)
    }

    // 检查当前功能是否可以应用
    private fun canApply(): Boolean {
        // 只有当选择了功能且该功能需要应用操作时返回true
        // 目前只有裁剪功能需要应用，其他功能要么实时生效要么待实现
        return mCurrentFeatureId == 1 // 裁剪功能
    }

    // 更新应用按钮状态
    private fun updateApplyButtonState() {
        val enabled = canApply()
        updateButtonState(mApplyButton!!, enabled)
    }

    // 更新历史记录按钮状态
    private fun updateHistoryButtons() {
        updateButtonState(mUndoButton!!, canUndo())
        updateButtonState(mRedoButton!!, canRedo())
    }

    companion object {
        private const val TAG = "ImageEditorApp"
    }
}