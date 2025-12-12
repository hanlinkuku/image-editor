package com.hanlin.image_editor_hanlin.entity

import java.io.Serializable

class ImageInfoBean : Serializable {
    @JvmField
    var id: Long = 0

    @JvmField
    var name: String? = null
    @JvmField
    var size: Long = 0
    @JvmField
    var path: String? = null

    override fun toString(): String {
        return "ImageInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", path='" + path + '\'' +
                '}'
    }

    constructor(id: Long, name: String?, size: Long, path: String?) {
        this.id = id
        this.name = name
        this.size = size
        this.path = path
    }

    constructor()
}
