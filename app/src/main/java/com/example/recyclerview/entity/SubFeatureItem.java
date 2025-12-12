package com.example.recyclerview.entity;

/**
 * 子功能菜单项实体类
 */
public class SubFeatureItem {
    public int id;
    public String name;
    public int iconResId;
    public String value; // 用于存储子功能的具体值，如滤镜强度、裁剪比例等
    
    public SubFeatureItem(int id, String name, int iconResId) {
        this(id, name, iconResId, "");
    }
    
    public SubFeatureItem(int id, String name, int iconResId, String value) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
        this.value = value;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getIconResId() {
        return iconResId;
    }
    
    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
}