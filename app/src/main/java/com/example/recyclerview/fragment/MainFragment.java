package com.example.recyclerview.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.recyclerview.R;
import com.example.recyclerview.activity.GalleryActivity;
import com.example.recyclerview.adapter.ImageInfoAdapter;
import com.example.recyclerview.entity.ImageInfoBean;
import com.example.recyclerview.helper.DatabaseHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {
    private DatabaseHelper mDatabaseHelper;
    private RecyclerView mPortfolioRecyclerView;
    private ImageInfoAdapter mImageInfoAdapter;
    private List<ImageInfoBean> mPortfolioImages;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 加载布局
        View view = inflater.inflate(R.layout.activity_main, container, false);

        // 初始化数据库
        mDatabaseHelper = new DatabaseHelper(getActivity());

        // 初始化作品集图片列表
        mPortfolioImages = new ArrayList<>();

        // 导入照片
        Button importPics = view.findViewById(R.id.importPhotoButton);
        importPics.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GalleryActivity.class);
            startActivity(intent);
        });

        // 打开相机
        MaterialButton openCamera = view.findViewById(R.id.cameraButton);
        openCamera.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "滤镜相机功能暂未实现", Toast.LENGTH_SHORT).show();
        });

        // 底部导航栏按钮已在activity_main.xml中移除，导航功能现在由MainContainerActivity处理

        // 初始化作品集RecyclerView
        initPortfolioRecyclerView(view);

        // 加载作品集图片
        loadPortfolioImages();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 在返回首页时刷新作品集
        loadPortfolioImages();
    }

    private void initPortfolioRecyclerView(View view) {
        mPortfolioRecyclerView = view.findViewById(R.id.portfolioRecyclerView);
        // 使用3列瀑布流布局
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        mPortfolioRecyclerView.setLayoutManager(layoutManager);

        // 添加适当的间距

        mPortfolioRecyclerView.addItemDecoration(new StaggeredGridSpacingItemDecoration(3, 0, true));

        // 创建适配器
        mImageInfoAdapter = new ImageInfoAdapter(getActivity(), mPortfolioImages);
        mPortfolioRecyclerView.setAdapter(mImageInfoAdapter);

    }

    // 自定义瀑布流间距装饰器
    private static class StaggeredGridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public StaggeredGridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                // 设置左右边距
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                // 设置上下边距
                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                // 设置左右边距
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f / spanCount) * spacing)

                // 设置上下边距
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    private void loadPortfolioImages() {
        // 从数据库加载作品集图片
        List<ImageInfoBean> images = mDatabaseHelper.getAllPortfolioImages();
        mPortfolioImages.clear();
        mPortfolioImages.addAll(images);
        if (mImageInfoAdapter != null) {
            mImageInfoAdapter.notifyDataSetChanged();
        }
    }
}