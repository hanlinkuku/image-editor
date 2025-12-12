package com.example.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recyclerview.R
import com.example.recyclerview.adapter.SubFeatureAdapter.SubFeatureViewHolder
import com.example.recyclerview.entity.SubFeatureItem

/**
 * 子功能菜单RecyclerView适配器
 */
class SubFeatureAdapter(private val subFeatureList: MutableList<SubFeatureItem>) :
    RecyclerView.Adapter<SubFeatureViewHolder?>() {
    private var listener: OnSubFeatureItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubFeatureViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_sub_feature, parent, false)
        return SubFeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubFeatureViewHolder, position: Int) {
        val subFeatureItem = subFeatureList.get(position)
        holder.bind(subFeatureItem)
    }

    override fun getItemCount(): Int {
        return subFeatureList.size
    }

    inner class SubFeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivIcon: ImageView
        var tvName: TextView
        var tvValue: TextView

        init {
            ivIcon = itemView.findViewById<ImageView>(R.id.ivIcon)
            tvName = itemView.findViewById<TextView>(R.id.tvName)
            tvValue = itemView.findViewById<TextView>(R.id.tvValue)

            // 设置点击事件
            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    val position = getAdapterPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        listener!!.onSubFeatureItemClick(position, subFeatureList.get(position))
                    }
                }
            })
        }

        fun bind(subFeatureItem: SubFeatureItem) {
            ivIcon.setImageResource(subFeatureItem.iconResId)
            tvName.setText(subFeatureItem.name)


            // 如果有值，显示值文本
            if (subFeatureItem.value != null && !subFeatureItem.value!!.isEmpty()) {
                tvValue.setText(subFeatureItem.value)
                tvValue.setVisibility(View.VISIBLE)
            } else {
                tvValue.setVisibility(View.GONE)
            }
        }
    }

    /**
     * 子功能项点击事件监听器
     */
    interface OnSubFeatureItemClickListener {
        fun onSubFeatureItemClick(position: Int, subFeatureItem: SubFeatureItem?)
    }

    /**
     * 设置子功能项点击事件监听器
     */
    fun setOnSubFeatureItemClickListener(listener: OnSubFeatureItemClickListener?) {
        this.listener = listener
    }
}