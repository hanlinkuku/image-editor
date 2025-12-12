package com.example.recyclerview.entity;

/**
 * 功能菜单项实体类
 */
public class FeatureItem {
    public int id;
    public String name;
    public int iconResId;
    
    public FeatureItem(int id, String name, int iconResId) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
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
}