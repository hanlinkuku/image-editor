// ImagePreviewActivity.java
package com.hanlin.image_editor_hanlin.activity

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.hanlin.image_editor_hanlin.R
import com.hanlin.image_editor_hanlin.adapter.PreviewAdapter
import com.hanlin.image_editor_hanlin.entity.ImageInfoBean

//TODO 修复1000张图片的预览OOM
class ImagePreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val ivClose = findViewById<ImageView>(R.id.ivClose)

        val imageList = getIntent()
            .getSerializableExtra("image_list") as MutableList<ImageInfoBean?>?
        val position = getIntent().getIntExtra("position", 0)

        // 过滤掉空元素并转换为非空列表
        val nonNullImageList = imageList?.filterNotNull()?.toMutableList() ?: mutableListOf()

        if (nonNullImageList.isEmpty()) {
            finish()
            return
        }

        val adapter = PreviewAdapter(nonNullImageList)
        viewPager.setAdapter(adapter)
        viewPager.setCurrentItem(position, false)

        // 关闭按钮 & 单击退出
        ivClose.setOnClickListener(View.OnClickListener { v: View? -> finish() })
        viewPager.setOnClickListener(View.OnClickListener { v: View? -> finish() })
    }
}