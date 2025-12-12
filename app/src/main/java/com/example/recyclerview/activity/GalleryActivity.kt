// app/src/main/java/com/example/recyclerview/activity/GalleryActivity.java
package com.example.recyclerview.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.recyclerview.R
import com.example.recyclerview.adapter.ImageInfoAdapter
import com.example.recyclerview.entity.ImageInfoBean

class GalleryActivity : AppCompatActivity() {
    private val mImageList: MutableList<ImageInfoBean> = ArrayList()
    private var mAdapter: ImageInfoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置状态栏为沉浸式，并让内容延伸到状态栏下方
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        getWindow().setStatusBarColor(Color.TRANSPARENT)

        setContentView(R.layout.activity_gallery)

        // 为整个布局添加顶部padding，确保内容不会被状态栏遮挡
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        val statusBarHeight = this.statusBarHeight
        mainLayout.setPadding(
            mainLayout.getPaddingLeft(),
            statusBarHeight + mainLayout.getPaddingTop(),
            mainLayout.getPaddingRight(),
            mainLayout.getPaddingBottom()
        )

        // 初始化回退按钮
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener(View.OnClickListener { v: View? -> finish() })

        val mRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        mAdapter = ImageInfoAdapter(this, mImageList)
        mRecyclerView.setLayoutManager(
            StaggeredGridLayoutManager(
                3,
                StaggeredGridLayoutManager.VERTICAL
            )
        )
        mRecyclerView.setAdapter(mAdapter)

        checkAndRequestPermission()
    }

    private val statusBarHeight: Int
        // 获取系统状态栏高度
        get() {
            var statusBarHeight = 0

            @SuppressLint(
                "InternalInsetResource",
                "DiscouragedApi"
            ) val resourceId =
                getResources().getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resourceId)
            }
            return statusBarHeight
        }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(permission)) {
                Toast.makeText(this, "需要相册权限以加载图片", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(permission),
                REQUEST_CODE_READ_MEDIA
            )
        } else {
            loadImagesFromMediaStore()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_MEDIA) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImagesFromMediaStore()
            } else {
                Toast.makeText(this, "权限被拒绝，无法加载图片", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadImagesFromMediaStore() {
        Thread(Runnable {
            val tempList: MutableList<ImageInfoBean> = ArrayList<ImageInfoBean>()
            val projection = arrayOf<String?>(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.TITLE,
                MediaStore.Images.Media.SIZE

            )

            val selection = MediaStore.Images.Media.SIZE + " < ?"
            val selectionArgs = arrayOf<String?>("30720000") // < 30MB

            try {
                getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
                ).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                        val titleCol = cursor.getColumnIndex(MediaStore.Images.Media.TITLE)
                        val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)

                        var count = 0
                        do {
                            if (count >= 1000) break

                            val id = cursor.getLong(idCol)
                            var name = cursor.getString(titleCol)
                            if (name == null) name = "IMG_" + id
                            val size = cursor.getLong(sizeCol)

                            // 用 ID 构造 content:
                            val uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            val uriString = uri.toString()

                            tempList.add(ImageInfoBean(id, name, size, uriString))
                            Log.d("MediaLoader", "Loaded: " + name + " | URI: " + uriString)
                            count++
                        } while (cursor.moveToNext())
                    }
                }
            } catch (e: Exception) {
                Log.e("GalleryActivity", "Query MediaStore failed", e)
            }
            runOnUiThread(Runnable {
                mImageList.clear()
                mImageList.addAll(tempList)
                mAdapter!!.notifyDataSetChanged()
                if (mImageList.isEmpty()) {
                    Toast.makeText(this, "未找到图片", Toast.LENGTH_SHORT).show()
                }
            })
        }).start()
    }

    companion object {
        private const val REQUEST_CODE_READ_MEDIA = 1001
    }
}