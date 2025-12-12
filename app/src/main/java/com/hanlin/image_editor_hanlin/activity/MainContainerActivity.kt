package com.hanlin.image_editor_hanlin.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.hanlin.image_editor_hanlin.R
import com.hanlin.image_editor_hanlin.fragment.MainFragment
import com.hanlin.image_editor_hanlin.fragment.ProfileFragment
import com.hanlin.image_editor_hanlin.widget.UnderlineButton

class MainContainerActivity : AppCompatActivity() {
    private var viewPager: ViewPager2? = null
    private var homeButton: UnderlineButton? = null
    private var profileButton: UnderlineButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_container)


        // 设置系统UI，让应用内容延伸到系统导航栏下方
        val decorView = getWindow().getDecorView()

        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE)


        getWindow().setNavigationBarColor(Color.WHITE)

        // 底部导航栏适配（保留你的 padding 逻辑）
        val bottomNavBar = findViewById<LinearLayout>(R.id.bottomNavBar)
        bottomNavBar.setOnApplyWindowInsetsListener { v: View?, insets: WindowInsets? ->
            val controller = v!!.getWindowInsetsController()
            if (controller != null) {
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }

            val bottomInset = insets!!.getInsets(WindowInsets.Type.navigationBars()).bottom
            v.setPadding(
                v.getPaddingLeft(),
                v.getPaddingTop(),
                v.getPaddingRight(),
                bottomInset + getResources().getDimensionPixelSize(R.dimen.bottom_nav_padding)
            )
            insets
        }
        // 初始化ViewPager
        viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager!!.setAdapter(object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2 // Main和Profile两个页面
            }

            override fun createFragment(position: Int): Fragment {
                if (position == 1) {
                    return ProfileFragment()
                }
                return MainFragment()
            }
        })

        // 设置ViewPager页面切换监听
        viewPager!!.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 根据当前页面更新底部导航栏的选中状态
                updateBottomNavBar(position)
            }
        })

        // 初始化底部导航栏按钮
        homeButton = findViewById<UnderlineButton>(R.id.homeButton)
        profileButton = findViewById<UnderlineButton>(R.id.profileButton)

        // 默认选中首页
        updateBottomNavBar(0)

        // 首页按钮点击事件
        homeButton!!.setOnClickListener { v: View? ->
            viewPager!!.setCurrentItem(0, true) // true表示有滑动动画
        }

        // 我的按钮点击事件
        profileButton!!.setOnClickListener { v: View? ->
            viewPager!!.setCurrentItem(1, true) // true表示有滑动动画
        }
    }

    // 更新底部导航栏的选中状态
    private fun updateBottomNavBar(position: Int) {
        if (position == 0) {
            homeButton!!.isCustomSelected = true
            profileButton!!.isCustomSelected = false
        } else {
            homeButton!!.isCustomSelected = false
            profileButton!!.isCustomSelected = true
        }
    }
}