package com.example.recyclerview.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.recyclerview.R

class ProfileFragment : Fragment() {
    private var imageViewAvatar: ImageView? = null
    private var textViewNickname: TextView? = null
    private var textViewSignture: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 加载布局
        val view = inflater.inflate(R.layout.activity_profile, container, false)

        // 初始化用户信息控件
        imageViewAvatar = view.findViewById<ImageView?>(R.id.imageViewAvatar)
        textViewNickname = view.findViewById<TextView?>(R.id.textViewNickname)
        textViewSignture = view.findViewById<TextView?>(R.id.textViewSigniture)

        // 加载用户信息
        loadUserInfo()

        // 设置所有条目点击事件
        view.findViewById<View?>(R.id.layoutInfo)
            .setOnClickListener(View.OnClickListener { v: View? ->
                Toast.makeText(getActivity(), "个人信息功能暂未实现", Toast.LENGTH_SHORT).show()
            })

        view.findViewById<View?>(R.id.layoutFavorites)
            .setOnClickListener(View.OnClickListener { v: View? ->
                Toast.makeText(getActivity(), "我的收藏功能暂未实现", Toast.LENGTH_SHORT).show()
            })

        view.findViewById<View?>(R.id.layoutHistory)
            .setOnClickListener(View.OnClickListener { v: View? ->
                Toast.makeText(getActivity(), "浏览历史功能暂未实现", Toast.LENGTH_SHORT).show()
            })

        view.findViewById<View?>(R.id.layoutSettings)
            .setOnClickListener(View.OnClickListener { v: View? ->
                Toast.makeText(getActivity(), "设置功能暂未实现", Toast.LENGTH_SHORT).show()
            })

        view.findViewById<View?>(R.id.layoutAbout)
            .setOnClickListener(View.OnClickListener { v: View? ->
                Toast.makeText(getActivity(), "关于我们功能暂未实现", Toast.LENGTH_SHORT).show()
            })

        view.findViewById<View?>(R.id.layoutFeedback)
            .setOnClickListener(View.OnClickListener { v: View? ->
                Toast.makeText(getActivity(), "意见反馈功能暂未实现", Toast.LENGTH_SHORT).show()
            })


        return view
    }

    private fun loadUserInfo() {
        // 设置固定的用户信息
        val nickname = "张三"
        val signiture = "签名"
        val avatarId = R.drawable.person

        if (imageViewAvatar != null && textViewNickname != null && textViewSignture != null) {
            imageViewAvatar!!.setImageResource(avatarId)
            textViewNickname!!.setText(nickname)
            textViewSignture!!.setText(signiture)
        }
    }
}