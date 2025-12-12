package com.hanlin.image_editor_hanlin.fragment

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hanlin.image_editor_hanlin.R
import com.hanlin.image_editor_hanlin.activity.GalleryActivity
import com.hanlin.image_editor_hanlin.adapter.ImageInfoAdapter
import com.hanlin.image_editor_hanlin.entity.ImageInfoBean
import com.hanlin.image_editor_hanlin.helper.DatabaseHelper
import com.google.android.material.button.MaterialButton

class MainFragment : Fragment() {
    private var mDatabaseHelper: DatabaseHelper? = null
    private var mPortfolioRecyclerView: RecyclerView? = null
    private var mImageInfoAdapter: ImageInfoAdapter? = null
    private var mPortfolioImages: MutableList<ImageInfoBean> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 加载布局
        val view = inflater.inflate(R.layout.activity_main, container, false)

        // 初始化数据库
        mDatabaseHelper = DatabaseHelper(getActivity())

        // 初始化作品集图片列表


        // 导入照片
        val importPics = view.findViewById<Button>(R.id.importPhotoButton)
        importPics.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(getActivity(), GalleryActivity::class.java)
            startActivity(intent)
        })

        // 打开相机
        val openCamera = view.findViewById<MaterialButton>(R.id.cameraButton)
        openCamera.setOnClickListener(View.OnClickListener { v: View? ->
            Toast.makeText(getActivity(), "滤镜相机功能暂未实现", Toast.LENGTH_SHORT).show()
        })

        // 底部导航栏按钮已在activity_main.xml中移除，导航功能现在由MainContainerActivity处理

        // 初始化作品集RecyclerView
        initPortfolioRecyclerView(view)

        // 加载作品集图片
        loadPortfolioImages()

        return view
    }

    override fun onResume() {
        super.onResume()
        // 在返回首页时刷新作品集
        loadPortfolioImages()
    }

    private fun initPortfolioRecyclerView(view: View) {
        mPortfolioRecyclerView = view.findViewById<RecyclerView>(R.id.portfolioRecyclerView)
        // 使用3列瀑布流布局
        val layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        mPortfolioRecyclerView!!.setLayoutManager(layoutManager)

        // 添加适当的间距
        mPortfolioRecyclerView!!.addItemDecoration(StaggeredGridSpacingItemDecoration(3, 0, true))

        // 创建适配器
        mImageInfoAdapter = ImageInfoAdapter(getActivity()!!, mPortfolioImages)
        mPortfolioRecyclerView!!.setAdapter(mImageInfoAdapter)
    }

    // 自定义瀑布流间距装饰器
    private class StaggeredGridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view) // item position
            val column = position % spanCount // item column

            if (includeEdge) {
                // 设置左右边距
                outRect.left =
                    spacing - column * spacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
                outRect.right =
                    (column + 1) * spacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

                // 设置上下边距
                if (position < spanCount) { // top edge
                    outRect.top = spacing
                }
                outRect.bottom = spacing // item bottom
            } else {
                // 设置左右边距
                outRect.left = column * spacing / spanCount // column * ((1f / spanCount) * spacing)
                outRect.right =
                    spacing - (column + 1) * spacing / spanCount // spacing - (column + 1) * ((1f / spanCount) * spacing)

                // 设置上下边距
                if (position >= spanCount) {
                    outRect.top = spacing // item top
                }
            }
        }
    }

    private fun loadPortfolioImages() {
        // 从数据库加载作品集图片
        val images = mDatabaseHelper!!.allPortfolioImages
        mPortfolioImages!!.clear()
        mPortfolioImages!!.addAll(images.filterNotNull())
        if (mImageInfoAdapter != null) {
            mImageInfoAdapter!!.notifyDataSetChanged()
        }
    }
}