package com.example.recyclerview.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.recyclerview.R

class ExportSuccessActivity : AppCompatActivity() {
    private var imgExported: ImageView? = null
    private var mImageUriString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_success)

        // 初始化控件
        val btnBackToEdit = findViewById<ImageView>(R.id.btnBackToEdit)
        val btnHome = findViewById<ImageView>(R.id.btnHome)
        imgExported = findViewById<ImageView>(R.id.imgExported)
        val btnBackToMain = findViewById<Button>(R.id.btnBackToMain)

        // 获取传递过来的图片URI
        val intent = getIntent()
        if (intent != null) {
            mImageUriString = intent.getStringExtra("image_uri")
            if (mImageUriString != null) {
                // 显示图片
                displayImage(mImageUriString)
            }
        }

        // 设置返回编辑页按钮的点击事件
        btnBackToEdit.setOnClickListener(View.OnClickListener { v: View? ->
            // 返回编辑页，与点击图片进入编辑页的思路一致
            val editIntent = Intent(this@ExportSuccessActivity, EditorActivity::class.java)
            editIntent.putExtra("image_uri", mImageUriString)
            editIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(editIntent)
            finish()
        })

        // 设置主页按钮的点击事件
        btnHome.setOnClickListener(View.OnClickListener { v: View? ->
            // 返回主页
            val homeIntent = Intent(this@ExportSuccessActivity, MainContainerActivity::class.java)
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(homeIntent)
            finish()
        })

        // 设置再修一张按钮的点击事件
        btnBackToMain.setOnClickListener(View.OnClickListener { v: View? ->
            // 再修一张，返回相册选择页
            val galleryIntent = Intent(this@ExportSuccessActivity, GalleryActivity::class.java)
            galleryIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(galleryIntent)
            finish()
        })
    }

    private fun displayImage(imageUriString: String?) {
        try {
            val imageUri = Uri.parse(imageUriString)
            val bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri))
            imgExported!!.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("displayImage", "failed")
        }
    }
}