[//]: # (# Cache Cleaner Gradle Plugin)

[//]: # ()
[//]: # (一个用于在Android应用编译前自动清除指定目录缓存的Gradle插件。)

[//]: # ()
[//]: # (## 功能特点)

[//]: # ()
[//]: # (- 在App编译前自动清除指定目录的缓存)

[//]: # (- 支持配置多个缓存目录)

[//]: # (- 提供详细的日志输出)

[//]: # (- 支持自定义配置选项)

[//]: # (- 与Android应用编译流程无缝集成)

[//]: # ()
[//]: # (## 安装方法)

[//]: # ()
[//]: # (插件已集成在项目的`buildSrc`目录中，无需额外安装。)

[//]: # ()
[//]: # (## 配置选项)

[//]: # ()
[//]: # (| 选项 | 类型 | 默认值 | 描述 |)

[//]: # (|------|------|--------|------|)

[//]: # (| `directories` | `List<String>` | `emptyList&#40;&#41;` | 需要清除的缓存目录列表 |)

[//]: # (| `verbose` | `Boolean` | `true` | 是否输出详细日志信息 |)

[//]: # ()
[//]: # (## 使用示例)

[//]: # ()
[//]: # (在应用模块的`build.gradle.kts`文件中使用插件：)

[//]: # ()
[//]: # (```kotlin)

[//]: # (plugins {)

[//]: # (    alias&#40;libs.plugins.android.application&#41;)

[//]: # (    alias&#40;libs.plugins.kotlin.android&#41;)

[//]: # (    id&#40;"com.example.cachecleaner"&#41; // 应用插件)

[//]: # (})

[//]: # ()
[//]: # (// 配置插件)

[//]: # (cacheCleaner {)

[//]: # (    // 指定要清除的目录列表)

[//]: # (    directories = listOf&#40;)

[//]: # (        "${project.rootDir}/app/build/intermediates",)

[//]: # (        "${project.rootDir}/app/build/generated",)

[//]: # (        "${System.getProperty&#40;"user.home"&#41;}/.gradle/caches/build-cache-1")

[//]: # (    &#41;)

[//]: # (    // 开启详细日志（可选，默认已开启）)

[//]: # (    verbose = true)

[//]: # (})

[//]: # (```)

[//]: # ()
[//]: # (## 任务说明)

[//]: # ()
[//]: # (插件提供了以下任务：)

[//]: # ()
[//]: # (| 任务名称 | 描述 | 分组 |)

[//]: # (|----------|------|------|)

[//]: # (| `cleanCache` | 清除指定的缓存目录 | `cache` |)

[//]: # ()
[//]: # (## 工作原理)

[//]: # ()
[//]: # (插件会在以下编译任务执行前自动运行`cleanCache`任务：)

[//]: # (- Java编译任务（如`compileDebugJavaWithJavac`）)

[//]: # (- Kotlin编译任务（如`compileDebugKotlin`）)

[//]: # ()
[//]: # (这样可以确保在每次编译前，指定的缓存目录都会被清除，避免缓存导致的编译问题。)

[//]: # ()
[//]: # (## 注意事项)

[//]: # ()
[//]: # (1. 请谨慎配置要清除的目录，避免误删重要文件)

[//]: # (2. 清除系统级缓存目录（如`~/.gradle/caches`）可能会影响其他项目的构建性能)

[//]: # (3. 插件默认会在编译前自动运行，无需手动触发)

[//]: # (4. 如果不需要清除缓存，可以将`directories`设置为空列表)

[//]: # ()
[//]: # (## 自定义配置)

[//]: # ()
[//]: # (您可以根据项目需求自定义要清除的缓存目录。例如：)

[//]: # ()
[//]: # (```kotlin)

[//]: # (cacheCleaner {)

[//]: # (    directories = listOf&#40;)

[//]: # (        "${project.rootDir}/app/build", // 清除整个build目录)

[//]: # (        "${project.rootDir}/app/src/main/assets/cache", // 清除应用内缓存目录)

[//]: # (        "${System.getProperty&#40;"user.home"&#41;}/AppData/Local/Android/Sdk/build-cache" // 清除Android SDK构建缓存)

[//]: # (    &#41;)

[//]: # (    verbose = false // 关闭日志输出)

[//]: # (})

[//]: # (```)

[//]: # ()
[//]: # (## 联系方式)

[//]: # ()
[//]: # (如有问题或建议，请在项目中提交issue。)