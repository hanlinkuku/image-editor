package com.example.recyclerview.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.recyclerview.R;

public class ProfileFragment extends Fragment {

    private ImageView imageViewAvatar;
    private TextView textViewNickname;
    private TextView textViewSignture;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 加载布局
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        // 初始化用户信息控件
        imageViewAvatar = view.findViewById(R.id.imageViewAvatar);
        textViewNickname = view.findViewById(R.id.textViewNickname);
        textViewSignture = view.findViewById(R.id.textViewSigniture);

        // 加载用户信息
        loadUserInfo();

        // 设置所有条目点击事件
        view.findViewById(R.id.layoutInfo).setOnClickListener(v -> {
            Toast.makeText(getActivity(), "个人信息功能暂未实现", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.layoutFavorites).setOnClickListener(v -> {
            Toast.makeText(getActivity(), "我的收藏功能暂未实现", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.layoutHistory).setOnClickListener(v -> {
            Toast.makeText(getActivity(), "浏览历史功能暂未实现", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.layoutSettings).setOnClickListener(v -> {
            Toast.makeText(getActivity(), "设置功能暂未实现", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.layoutAbout).setOnClickListener(v -> {
            Toast.makeText(getActivity(), "关于我们功能暂未实现", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.layoutFeedback).setOnClickListener(v -> {
            Toast.makeText(getActivity(), "意见反馈功能暂未实现", Toast.LENGTH_SHORT).show();
        });


        return view;
    }

    private void loadUserInfo() {
        // 设置固定的用户信息
        String nickname = "张三";
        String signiture = "签名";
        int avatarId = R.drawable.person;

        if (imageViewAvatar != null && textViewNickname != null && textViewSignture != null) {
            imageViewAvatar.setImageResource(avatarId);
            textViewNickname.setText(nickname);
            textViewSignture.setText(signiture);
        }
    }
}