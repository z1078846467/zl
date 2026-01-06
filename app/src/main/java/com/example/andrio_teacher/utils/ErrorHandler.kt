package com.example.andrio_teacher.utils

import android.content.Context
import android.util.Log
import com.example.andrio_teacher.ui.navigation.AppRoutes
import androidx.navigation.NavController

object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    /**
     * 处理API错误，返回用户友好的错误消息
     */
    fun handleApiError(
        error: String?,
        statusCode: Int? = null
    ): String {
        if (error == null) {
            return "操作失败，请重试"
        }
        
        // 根据状态码返回不同消息
        statusCode?.let {
            return when (it) {
                401 -> "登录已过期，请重新登录"
                403 -> "权限不足，无法执行此操作"
                404 -> "资源不存在"
                429 -> "请求过于频繁，请稍后再试"
                500, 502, 503 -> "服务器错误，请稍后再试"
                else -> error
            }
        }
        
        // 根据错误消息内容返回友好提示
        return when {
            error.contains("网络", ignoreCase = true) || 
            error.contains("network", ignoreCase = true) ||
            error.contains("timeout", ignoreCase = true) ||
            error.contains("连接", ignoreCase = true) -> "网络连接失败，请检查网络设置"
            
            error.contains("token", ignoreCase = true) ||
            error.contains("登录", ignoreCase = true) ||
            error.contains("expired", ignoreCase = true) -> "登录已过期，请重新登录"
            
            error.contains("权限", ignoreCase = true) ||
            error.contains("permission", ignoreCase = true) ||
            error.contains("forbidden", ignoreCase = true) -> "权限不足，无法执行此操作"
            
            error.contains("未找到", ignoreCase = true) ||
            error.contains("not found", ignoreCase = true) -> "资源不存在"
            
            error.contains("频繁", ignoreCase = true) ||
            error.contains("rate limit", ignoreCase = true) ||
            error.contains("too many", ignoreCase = true) -> "请求过于频繁，请稍后再试"
            
            else -> error
        }
    }
    
    /**
     * 检查是否是Token过期错误，如果是则跳转到登录页
     */
    fun checkTokenExpired(
        error: String?,
        statusCode: Int?,
        context: Context,
        navController: NavController?
    ): Boolean {
        val isExpired = statusCode == 401 || 
                       statusCode == 403 ||
                       error?.contains("token", ignoreCase = true) == true ||
                       error?.contains("expired", ignoreCase = true) == true ||
                       error?.contains("登录", ignoreCase = true) == true
        
        if (isExpired) {
            Log.w(TAG, "Token已过期，清除会话并跳转登录")
            UserSession.clear(context)
            navController?.navigate(AppRoutes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
            return true
        }
        return false
    }
    
    /**
     * 处理网络错误
     */
    fun handleNetworkError(
        throwable: Throwable?,
        context: Context? = null,
        navController: NavController? = null
    ): String {
        val errorMessage = throwable?.message ?: "网络错误"
        
        Log.e(TAG, "网络错误", throwable)
        
        return when {
            errorMessage.contains("timeout", ignoreCase = true) -> 
                "请求超时，请检查网络连接"
            
            errorMessage.contains("connection", ignoreCase = true) ||
            errorMessage.contains("连接", ignoreCase = true) -> 
                "无法连接到服务器，请检查网络设置"
            
            errorMessage.contains("unknown host", ignoreCase = true) -> 
                "无法解析服务器地址，请检查网络设置"
            
            else -> "网络连接失败，请重试"
        }
    }
    
    /**
     * 获取错误类型
     */
    enum class ErrorType {
        NETWORK_ERROR,      // 网络错误
        TOKEN_EXPIRED,      // Token过期
        PERMISSION_DENIED,  // 权限不足
        NOT_FOUND,          // 资源不存在
        SERVER_ERROR,       // 服务器错误
        RATE_LIMIT,         // 请求过于频繁
        UNKNOWN             // 未知错误
    }
    
    /**
     * 分析错误类型
     */
    fun analyzeError(
        error: String?,
        statusCode: Int? = null
    ): ErrorType {
        statusCode?.let {
            return when (it) {
                401, 403 -> ErrorType.TOKEN_EXPIRED
                404 -> ErrorType.NOT_FOUND
                429 -> ErrorType.RATE_LIMIT
                500, 502, 503 -> ErrorType.SERVER_ERROR
                else -> ErrorType.UNKNOWN
            }
        }
        
        error?.let {
            return when {
                it.contains("网络", ignoreCase = true) ||
                it.contains("network", ignoreCase = true) ||
                it.contains("timeout", ignoreCase = true) ||
                it.contains("连接", ignoreCase = true) -> ErrorType.NETWORK_ERROR
                
                it.contains("token", ignoreCase = true) ||
                it.contains("expired", ignoreCase = true) ||
                it.contains("登录", ignoreCase = true) -> ErrorType.TOKEN_EXPIRED
                
                it.contains("权限", ignoreCase = true) ||
                it.contains("permission", ignoreCase = true) ||
                it.contains("forbidden", ignoreCase = true) -> ErrorType.PERMISSION_DENIED
                
                it.contains("未找到", ignoreCase = true) ||
                it.contains("not found", ignoreCase = true) -> ErrorType.NOT_FOUND
                
                it.contains("频繁", ignoreCase = true) ||
                it.contains("rate limit", ignoreCase = true) ||
                it.contains("too many", ignoreCase = true) -> ErrorType.RATE_LIMIT
                
                else -> ErrorType.UNKNOWN
            }
        }
        
        return ErrorType.UNKNOWN
    }
}

