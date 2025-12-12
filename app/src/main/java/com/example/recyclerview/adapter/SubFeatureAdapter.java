package com.example.recyclerview.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recyclerview.R;
import com.example.recyclerview.entity.SubFeatureItem;

import java.util.List;

/**
 * 子功能菜单RecyclerView适配器
 */
public class SubFeatureAdapter extends RecyclerView.Adapter<SubFeatureAdapter.SubFeatureViewHolder> {

    private final List<SubFeatureItem> subFeatureList;
    private OnSubFeatureItemClickListener listener;

    public SubFeatureAdapter(List<SubFeatureItem> subFeatureList) {
        this.subFeatureList = subFeatureList;
    }

    @NonNull
    @Override
    public SubFeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sub_feature, parent, false);
        return new SubFeatureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubFeatureViewHolder holder, int position) {
        SubFeatureItem subFeatureItem = subFeatureList.get(position);
        holder.bind(subFeatureItem);
    }

    @Override
    public int getItemCount() {
        return subFeatureList.size();
    }

    class SubFeatureViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvValue;

        SubFeatureViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvValue = itemView.findViewById(R.id.tvValue);

            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onSubFeatureItemClick(position, subFeatureList.get(position));
                    }
                }
            });
        }

        void bind(SubFeatureItem subFeatureItem) {
            ivIcon.setImageResource(subFeatureItem.getIconResId());
            tvName.setText(subFeatureItem.getName());
            
            // 如果有值，显示值文本
            if (subFeatureItem.getValue() != null && !subFeatureItem.getValue().isEmpty()) {
                tvValue.setText(subFeatureItem.getValue());
                tvValue.setVisibility(View.VISIBLE);
            } else {
                tvValue.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 子功能项点击事件监听器
     */
    public interface OnSubFeatureItemClickListener {
        void onSubFeatureItemClick(int position, SubFeatureItem subFeatureItem);
    }

    /**
     * 设置子功能项点击事件监听器
     */
    public void setOnSubFeatureItemClickListener(OnSubFeatureItemClickListener listener) {
        this.listener = listener;
    }
}