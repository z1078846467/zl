package com.example.andrio_teacher.utils

import android.content.Context
import android.util.Log
import java.io.File

object CacheManager {
    private const val TAG = "CacheManager"
    
    /**
     * 清理题目缓存
     */
    fun clearQuestionCache(context: Context): Long {
        var clearedSize = 0L
        try {
            // 清理Coil图片缓存
            val cacheDir = context.cacheDir
            val imageCacheDir = File(cacheDir, "image_cache")
            if (imageCacheDir.exists()) {
                clearedSize += getDirectorySize(imageCacheDir)
                imageCacheDir.deleteRecursively()
                Log.d(TAG, "清理图片缓存成功")
            }
            
            // 清理题目相关的临时文件
            val questionCacheDir = File(cacheDir, "question_cache")
            if (questionCacheDir.exists()) {
                clearedSize += getDirectorySize(questionCacheDir)
                questionCacheDir.deleteRecursively()
                Log.d(TAG, "清理题目缓存成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理题目缓存失败", e)
        }
        return clearedSize
    }
    
    /**
     * 清理视频缓存
     */
    fun clearVideoCache(context: Context): Long {
        var clearedSize = 0L
        try {
            val cacheDir = context.cacheDir
            val videoCacheDir = File(cacheDir, "video_cache")
            if (videoCacheDir.exists()) {
                clearedSize += getDirectorySize(videoCacheDir)
                videoCacheDir.deleteRecursively()
                Log.d(TAG, "清理视频缓存成功")
            }
            
            // 清理外部存储的视频缓存（如果存在）
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null) {
                val externalVideoDir = File(externalCacheDir, "videos")
                if (externalVideoDir.exists()) {
                    clearedSize += getDirectorySize(externalVideoDir)
                    externalVideoDir.deleteRecursively()
                    Log.d(TAG, "清理外部视频缓存成功")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理视频缓存失败", e)
        }
        return clearedSize
    }
    
    /**
     * 清理全部缓存
     */
    fun clearAllCache(context: Context): Long {
        var totalSize = 0L
        try {
            totalSize += clearQuestionCache(context)
            totalSize += clearVideoCache(context)
            
            // 清理其他缓存
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles()
            files?.forEach { file ->
                if (file.isDirectory && file.name != "image_cache" && file.name != "question_cache" && file.name != "video_cache") {
                    totalSize += getDirectorySize(file)
                    file.deleteRecursively()
                }
            }
            
            Log.d(TAG, "清理全部缓存成功，总大小: ${formatSize(totalSize)}")
        } catch (e: Exception) {
            Log.e(TAG, "清理全部缓存失败", e)
        }
        return totalSize
    }
    
    /**
     * 获取目录大小
     */
    private fun getDirectorySize(directory: File): Long {
        var size = 0L
        try {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    size += if (file.isDirectory) {
                        getDirectorySize(file)
                    } else {
                        file.length()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "计算目录大小失败", e)
        }
        return size
    }
    
    /**
     * 格式化文件大小
     */
    fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            else -> "${size / (1024 * 1024)}MB"
        }
    }
    
    /**
     * 获取缓存总大小
     */
    fun getCacheSize(context: Context): Long {
        var totalSize = 0L
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                totalSize += getDirectorySize(cacheDir)
            }
            
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                totalSize += getDirectorySize(externalCacheDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取缓存大小失败", e)
        }
        return totalSize
    }
}

