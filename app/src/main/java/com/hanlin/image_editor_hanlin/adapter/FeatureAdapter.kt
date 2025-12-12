package com.hanlin.image_editor_hanlin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hanlin.image_editor_hanlin.R
import com.hanlin.image_editor_hanlin.adapter.FeatureAdapter.FeatureViewHolder
import com.hanlin.image_editor_hanlin.entity.FeatureItem

/**
 * 主功能菜单RecyclerView适配器
 */
class FeatureAdapter(private val featureList: MutableList<FeatureItem>) :
    RecyclerView.Adapter<FeatureViewHolder?>() {
    private var listener: OnFeatureItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_feature, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val featureItem = featureList.get(position)
        holder.bind(featureItem)
    }

    override fun getItemCount(): Int {
        return featureList.size
    }

    inner class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivIcon: ImageView
        var tvName: TextView

        init {
            ivIcon = itemView.findViewById<ImageView>(R.id.ivIcon)
            tvName = itemView.findViewById<TextView>(R.id.tvName)

            // 设置点击事件
            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    val position = getAdapterPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        listener!!.onFeatureItemClick(position, featureList.get(position))
                    }
                }
            })
        }

        fun bind(featureItem: FeatureItem) {
            ivIcon.setImageResource(featureItem.iconResId)
            tvName.setText(featureItem.name)
        }
    }

    /**
     * 功能项点击事件监听器
     */
    interface OnFeatureItemClickListener {
        fun onFeatureItemClick(position: Int, featureItem: FeatureItem?)
    }

    /**
     * 设置功能项点击事件监听器
     */
    fun setOnFeatureItemClickListener(listener: OnFeatureItemClickListener?) {
        this.listener = listener
    }
}