// 创建新文件：app/src/main/java/com/example/recyclerview/PreviewAdapter.java
package com.example.recyclerview.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.recyclerview.R;
import com.example.recyclerview.entity.ImageInfoBean;

import java.util.List;

public class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.PreviewViewHolder> {

    private final List<ImageInfoBean> imageList;

    public PreviewAdapter(List<ImageInfoBean> imageList) {
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preview, parent, false);
        return new PreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
        ImageInfoBean item = imageList.get(position);
        Glide.with(holder.imageView.getContext())
                .load(item.getPath())
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imageView);

        // 初始化为 fitCenter（完整显示）
        holder.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class PreviewViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        boolean isZoomed = false;

        PreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }

        void toggleZoom() {
            if (isZoomed) {
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            isZoomed = !isZoomed;
        }
    }
}