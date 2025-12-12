// app/src/main/java/com/example/recyclerview/activity/GalleryActivity.java
package com.example.recyclerview.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.recyclerview.R;
import com.example.recyclerview.adapter.ImageInfoAdapter;
import com.example.recyclerview.entity.ImageInfoBean;

import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_READ_MEDIA = 1001;
    private final List<ImageInfoBean> mImageList = new ArrayList<>();
    private ImageInfoAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置状态栏为沉浸式，并让内容延伸到状态栏下方
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_gallery);

        // 为整个布局添加顶部padding，确保内容不会被状态栏遮挡
        ConstraintLayout mainLayout = findViewById(R.id.main);
        int statusBarHeight = getStatusBarHeight();
        mainLayout.setPadding(
                mainLayout.getPaddingLeft(),
                statusBarHeight + mainLayout.getPaddingTop(),
                mainLayout.getPaddingRight(),
                mainLayout.getPaddingBottom());

        // 初始化回退按钮
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);
        mAdapter = new ImageInfoAdapter(this, mImageList);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);

        checkAndRequestPermission();
    }

    // 获取系统状态栏高度
    private int getStatusBarHeight() {
        int statusBarHeight = 0;
        @SuppressLint({"InternalInsetResource", "DiscouragedApi"}) int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    private void checkAndRequestPermission() {
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(permission)) {
                Toast.makeText(this, "需要相册权限以加载图片", Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_READ_MEDIA);
        } else {
            loadImagesFromMediaStore();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_MEDIA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImagesFromMediaStore();
            } else {
                Toast.makeText(this, "权限被拒绝，无法加载图片", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadImagesFromMediaStore() {
        new Thread(() -> {
            List<ImageInfoBean> tempList = new ArrayList<>();
            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.TITLE,
                    MediaStore.Images.Media.SIZE

            };

            String selection = MediaStore.Images.Media.SIZE + " < ?";
            String[] selectionArgs = {"30720000"}; // < 30MB

            try (Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    MediaStore.Images.Media.DATE_ADDED + " DESC")) {

                if (cursor != null && cursor.moveToFirst()) {
                    int idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                    int titleCol = cursor.getColumnIndex(MediaStore.Images.Media.TITLE);
                    int sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);

                    int count = 0;
                    do {
                        if (count >= 1000) break;

                        long id = cursor.getLong(idCol);
                        String name = cursor.getString(titleCol);
                        if (name == null) name = "IMG_" + id;
                        long size = cursor.getLong(sizeCol);

                        // 用 ID 构造 content:
                        Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        String uriString = uri.toString();

                        tempList.add(new ImageInfoBean(id, name, size, uriString));
                        Log.d("MediaLoader", "Loaded: " + name + " | URI: " + uriString);
                        count++;
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e("GalleryActivity", "Query MediaStore failed", e);
            }

            runOnUiThread(() -> {
                mImageList.clear();
                mImageList.addAll(tempList);
                mAdapter.notifyDataSetChanged();
                if (mImageList.isEmpty()) {
                    Toast.makeText(this, "未找到图片", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}