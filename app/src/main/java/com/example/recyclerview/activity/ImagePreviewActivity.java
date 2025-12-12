// ImagePreviewActivity.java
package com.example.recyclerview.activity;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.recyclerview.R;
import com.example.recyclerview.entity.ImageInfoBean;
import com.example.recyclerview.adapter.PreviewAdapter;

import java.util.List;

//TODO 修复1000张图片的预览OOM
public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ImageView ivClose = findViewById(R.id.ivClose);

        @SuppressWarnings("unchecked")
        List<ImageInfoBean> imageList = (List<ImageInfoBean>) getIntent()
                .getSerializableExtra("image_list");
        int position = getIntent().getIntExtra("position", 0);

        if (imageList == null || imageList.isEmpty()) {
            finish();
            return;
        }

        PreviewAdapter adapter = new PreviewAdapter(imageList);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position, false);

        // 关闭按钮 & 单击退出
        ivClose.setOnClickListener(v -> finish());
        viewPager.setOnClickListener(v -> finish());
    }
}