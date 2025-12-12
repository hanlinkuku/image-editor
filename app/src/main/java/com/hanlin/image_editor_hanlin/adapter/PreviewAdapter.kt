// 创建新文件：app/src/main/java/com/example/recyclerview/PreviewAdapter.java
package com.hanlin.image_editor_hanlin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hanlin.image_editor_hanlin.R
import com.hanlin.image_editor_hanlin.adapter.PreviewAdapter.PreviewViewHolder
import com.hanlin.image_editor_hanlin.entity.ImageInfoBean

class PreviewAdapter(private val imageList: MutableList<ImageInfoBean>) :
    RecyclerView.Adapter<PreviewViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_preview, parent, false)
        return PreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val item = imageList.get(position)
        Glide.with(holder.imageView.getContext())
            .load(item.path)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_background)
            .into(holder.imageView)

        // 初始化为 fitCenter（完整显示）
        holder.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class PreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView
        var isZoomed: Boolean = false

        init {
            imageView = itemView.findViewById<ImageView>(R.id.imageView)
        }

        fun toggleZoom() {
            if (isZoomed) {
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER)
            } else {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP)
            }
            isZoomed = !isZoomed
        }
    }
}