package com.hanlin.image_editor_hanlin.entity

/**
 * 子功能菜单项实体类
 */
class SubFeatureItem @JvmOverloads constructor(
    @JvmField var id: Int, @JvmField var name: String?, @JvmField var iconResId: Int, // 用于存储子功能的具体值，如滤镜强度、裁剪比例等
    @JvmField var value: String? = ""
) {
    init {
        this.value = value
    }
}