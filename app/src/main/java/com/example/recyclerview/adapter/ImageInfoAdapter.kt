// ImageInfoAdapter.java
package com.example.recyclerview.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recyclerview.R
import com.example.recyclerview.activity.EditorActivity
import com.example.recyclerview.activity.ImagePreviewActivity
import com.example.recyclerview.entity.ImageInfoBean

class ImageInfoAdapter(
    private val context: Context,
    private val imageList: MutableList<ImageInfoBean>
) : RecyclerView.Adapter<ImageInfoAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = imageList.get(position)

        // 加载图片
        Glide.with(context)
            .load(item.path)
            .placeholder(R.drawable.ic_loading)
            .error(R.drawable.ic_error)
            .centerCrop()
            .into(holder.imageView)

        // 图片点击 → 编辑器
        holder.imageView.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(context, EditorActivity::class.java)
            intent.putExtra("image_uri", item.path) // 统一使用image_uri作为关键字
            context.startActivity(intent)
        })

        // 放大按钮点击 → 预览
        holder.imageView2.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.putExtra("image_list", imageList as ArrayList<ImageInfoBean?>)
            intent.putExtra("position", position)
            context.startActivity(intent)
        })
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView
        var imageView2: ImageView

        init {
            imageView = itemView.findViewById<ImageView>(R.id.imageView)
            imageView2 = itemView.findViewById<ImageView>(R.id.imageView2)
        }
    }
}