package com.example.recyclerview.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.recyclerview.R;
import com.example.recyclerview.fragment.MainFragment;
import com.example.recyclerview.fragment.ProfileFragment;
import com.example.recyclerview.widget.UnderlineButton;

public class MainContainerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private UnderlineButton homeButton;
    private UnderlineButton profileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        // 设置系统UI，让应用内容延伸到系统导航栏下方


        View decorView = getWindow().getDecorView();

        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);


        getWindow().setNavigationBarColor(Color.WHITE);

// 底部导航栏适配（保留你的 padding 逻辑）
        LinearLayout bottomNavBar = findViewById(R.id.bottomNavBar);
        bottomNavBar.setOnApplyWindowInsetsListener((v, insets) -> {
            WindowInsetsController controller = v.getWindowInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }

            int bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottomInset + getResources().getDimensionPixelSize(R.dimen.bottom_nav_padding)
            );
            return insets;
        });
        // 初始化ViewPager
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 2; // Main和Profile两个页面
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 1) {
                    return new ProfileFragment();
                }
                return new MainFragment();
            }
        });

        // 设置ViewPager页面切换监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // 根据当前页面更新底部导航栏的选中状态
                updateBottomNavBar(position);
            }
        });

        // 初始化底部导航栏按钮
        homeButton = findViewById(R.id.homeButton);
        profileButton = findViewById(R.id.profileButton);

        // 默认选中首页
        updateBottomNavBar(0);

        // 首页按钮点击事件
        homeButton.setOnClickListener(v -> {
            viewPager.setCurrentItem(0, true); // true表示有滑动动画
        });

        // 我的按钮点击事件
        profileButton.setOnClickListener(v -> {
            viewPager.setCurrentItem(1, true); // true表示有滑动动画
        });
    }

    // 更新底部导航栏的选中状态
    private void updateBottomNavBar(int position) {
        if (position == 0) {
            homeButton.setCustomSelected(true);
            profileButton.setCustomSelected(false);
        } else {
            homeButton.setCustomSelected(false);
            profileButton.setCustomSelected(true);
        }
    }
}