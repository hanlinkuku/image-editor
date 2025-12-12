//package com.example.cachecleaner
//
//import org.gradle.api.Plugin
//import org.gradle.api.Project
//import org.gradle.api.tasks.Delete
//import org.gradle.api.tasks.TaskProvider
//import java.io.File
//
//class CacheCleanerPlugin : Plugin<Project> {
//    override fun apply(project: Project) {
//        // 创建插件扩展，允许用户配置
//        val extension = project.extensions.create(
//            "cacheCleaner",
//            CacheCleanerExtension::class.java
//        )
//
//        // 创建清除缓存任务
//        val cleanCacheTask: TaskProvider<Delete> = project.tasks.register(
//            "cleanCache",
//            Delete::class.java
//        ) {
//            it.group = "cache"
//            it.description = "Cleans cache directories before app compilation"
//
//            // 配置任务动作
//            it.doFirst {
//                val directories = extension.directories
//                val verbose = extension.verbose
//
//                if (directories.isEmpty()) {
//                    if (verbose) {
//                        project.logger.lifecycle("No cache directories specified for cleaning")
//                    }
//                    return@doFirst
//                }
//
//                if (verbose) {
//                    project.logger.lifecycle("Cleaning cache directories: $directories")
//                }
//
//                // 清除每个指定的目录
//                directories.forEach { dirPath ->
//                    val dir = File(dirPath)
//                    if (dir.exists() && dir.isDirectory) {
//                        if (verbose) {
//                            project.logger.lifecycle("Cleaning directory: ${dir.absolutePath}")
//                        }
//                        // 设置要删除的目录
//                        it.deleteDirs(dir)
//                    } else {
//                        if (verbose) {
//                            project.logger.lifecycle("Directory not found or not a directory: $dirPath")
//                        }
//                    }
//                }
//            }
//        }
//
//        // 配置任务依赖：在编译前执行清除缓存任务
//        project.tasks.whenTaskAdded { task ->
//            // 为所有Android应用模块的编译任务添加依赖
//            if (task.name.startsWith("compile") && task.name.endsWith("JavaWithJavac")) {
//                task.dependsOn(cleanCacheTask)
//            }
//            // 也可以添加对其他编译相关任务的依赖，如Kotlin编译
//            if (task.name.startsWith("compile") && task.name.endsWith("Kotlin")) {
//                task.dependsOn(cleanCacheTask)
//            }
//        }
//    }
//}