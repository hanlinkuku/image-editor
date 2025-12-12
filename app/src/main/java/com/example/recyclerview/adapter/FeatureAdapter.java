package com.example.recyclerview.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recyclerview.R;
import com.example.recyclerview.entity.FeatureItem;

import java.util.List;

/**
 * 主功能菜单RecyclerView适配器
 */
public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder> {

    private final List<FeatureItem> featureList;
    private OnFeatureItemClickListener listener;

    public FeatureAdapter(List<FeatureItem> featureList) {
        this.featureList = featureList;
    }

    @NonNull
    @Override
    public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feature, parent, false);
        return new FeatureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
        FeatureItem featureItem = featureList.get(position);
        holder.bind(featureItem);
    }

    @Override
    public int getItemCount() {
        return featureList.size();
    }

    class FeatureViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;

        FeatureViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);

            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onFeatureItemClick(position, featureList.get(position));
                    }
                }
            });
        }

        void bind(FeatureItem featureItem) {
            ivIcon.setImageResource(featureItem.getIconResId());
            tvName.setText(featureItem.getName());
        }
    }

    /**
     * 功能项点击事件监听器
     */
    public interface OnFeatureItemClickListener {
        void onFeatureItemClick(int position, FeatureItem featureItem);
    }

    /**
     * 设置功能项点击事件监听器
     */
    public void setOnFeatureItemClickListener(OnFeatureItemClickListener listener) {
        this.listener = listener;
    }
}