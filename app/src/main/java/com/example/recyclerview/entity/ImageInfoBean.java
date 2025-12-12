package com.example.recyclerview.entity;

public class ImageInfoBean implements java.io.Serializable{
    public long id;

    public String name;
    public long size;
    public String path;

    @Override
    public String toString() {
        return "ImageInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", path='" + path + '\'' +
                '}';
    }

    public ImageInfoBean(long id, String name, long size, String path) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.path = path;
    }

    public ImageInfoBean() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
