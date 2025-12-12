# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 保留应用入口类和四大组件
-keep public class com.example.recyclerview.MainActivity {
    public *;
}
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 保留 Gson 序列化/反序列化类
-keep class com.example.recyclerview.model.** {
    *;
}
-keep class com.google.gson.** {
    *;
}
-keepattributes Signature
-keepattributes *Annotation*

# 保留 Glide 相关类
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# 保留 OkHttp3 相关类
-keep class okhttp3.** {
    *;
}
-keep interface okhttp3.** {
    *;
}
-dontwarn okhttp3.**

# 保留 OkHttp3 日志拦截器
-dontwarn okhttp3.logging.**
-keep class okhttp3.internal.** {
    *;
}

# 保留 AndroidX 组件
-keep class androidx.** {
    *;
}
-keep interface androidx.** {
    *;
}
-dontwarn androidx.**

# 保留 Material Design 组件
-keep class com.google.android.material.** {
    *;
}
-dontwarn com.google.android.material.**

# 保留 RecyclerView 相关类
-keep class androidx.recyclerview.widget.** {
    *;
}

# 保留 CameraX 相关类
-keep class androidx.camera.** {
    *;
}
-dontwarn androidx.camera.**

# 保留注解
-keepattributes *Annotation*

# 保留泛型
-keepattributes Signature

# 保留调试信息，便于排查问题
-keepattributes SourceFile,LineNumberTable

# 隐藏原始源文件名
-renamesourcefileattribute SourceFile

# 如果使用了 WebView 和 JavaScript 接口，取消下面注释
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}