// ImageInfoAdapter.java
package com.example.recyclerview.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.recyclerview.R;
import com.example.recyclerview.activity.EditorActivity;
import com.example.recyclerview.activity.ImagePreviewActivity;
import com.example.recyclerview.entity.ImageInfoBean;

import java.util.ArrayList;
import java.util.List;

public class ImageInfoAdapter extends RecyclerView.Adapter<ImageInfoAdapter.ViewHolder> {

    private final Context context;
    private final List<ImageInfoBean> imageList;

    public ImageInfoAdapter(Context context, List<ImageInfoBean> imageList) {
        this.context = context;
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageInfoBean item = imageList.get(position);

        // 加载图片
        Glide.with(context)
                .load(item.getPath())
                .placeholder(R.drawable.ic_loading)
                .error(R.drawable.ic_error)
                .centerCrop()
                .into(holder.imageView);

        // 图片点击 → 编辑器
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditorActivity.class);
            intent.putExtra("image_uri", item.getPath()); // 统一使用image_uri作为关键字
            context.startActivity(intent);
        });

        // 放大按钮点击 → 预览
        holder.imageView2.setOnClickListener(v -> {
            Intent intent = new Intent(context, ImagePreviewActivity.class);
            intent.putExtra("image_list", (ArrayList<ImageInfoBean>) imageList);
            intent.putExtra("position", position);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView imageView2;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            imageView2 = itemView.findViewById(R.id.imageView2);
        }
    }
}