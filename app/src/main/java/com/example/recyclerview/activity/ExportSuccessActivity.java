package com.example.recyclerview.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.recyclerview.R;

public class ExportSuccessActivity extends AppCompatActivity {

    private ImageView imgExported;
    private String mImageUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_success);

        // 初始化控件
        ImageView btnBackToEdit = findViewById(R.id.btnBackToEdit);
        ImageView btnHome = findViewById(R.id.btnHome);
        imgExported = findViewById(R.id.imgExported);
        Button btnBackToMain = findViewById(R.id.btnBackToMain);

        // 获取传递过来的图片URI
        Intent intent = getIntent();
        if (intent != null) {
            mImageUriString = intent.getStringExtra("image_uri");
            if (mImageUriString != null) {
                // 显示图片
                displayImage(mImageUriString);
            }
        }

        // 设置返回编辑页按钮的点击事件
        btnBackToEdit.setOnClickListener(v -> {
            // 返回编辑页，与点击图片进入编辑页的思路一致
            Intent editIntent = new Intent(ExportSuccessActivity.this, EditorActivity.class);
            editIntent.putExtra("image_uri", mImageUriString);
            editIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(editIntent);
            finish();
        });

        // 设置主页按钮的点击事件
        btnHome.setOnClickListener(v -> {
            // 返回主页
            Intent homeIntent = new Intent(ExportSuccessActivity.this, MainContainerActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            finish();
        });

        // 设置再修一张按钮的点击事件
        btnBackToMain.setOnClickListener(v -> {
            // 再修一张，返回相册选择页
            Intent galleryIntent = new Intent(ExportSuccessActivity.this, GalleryActivity.class);
            galleryIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(galleryIntent);
            finish();
        });
    }

    private void displayImage(String imageUriString) {
        try {
            Uri imageUri = Uri.parse(imageUriString);
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            imgExported.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e("displayImage","failed");
        }
    }
}