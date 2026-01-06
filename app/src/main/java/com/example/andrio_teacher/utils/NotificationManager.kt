package com.example.andrio_teacher.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.andrio_teacher.network.NetworkConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object NotificationManager {
    private const val TAG = "NotificationManager"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * 检查审核通知
     */
    fun checkVerificationNotification(
        token: String,
        callback: (Boolean, String?, String?) -> Unit
    ) {
        val request = Request.Builder()
            .url("${NetworkConfig.GATEWAY_URL}/api/notifications/verification")
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "检查通知失败", e)
                Handler(Looper.getMainLooper()).post {
                    callback(false, null, "网络错误: ${e.message}")
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()
                    Handler(Looper.getMainLooper()).post {
                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val jsonObj = JSONObject(responseBody)
                                val dataObj = jsonObj.optJSONObject("data")
                                
                                if (dataObj?.optBoolean("hasNotification", false) == true) {
                                    val notificationObj = dataObj.getJSONObject("notification")
                                    val data = notificationObj.getJSONObject("data")
                                    val status = data.optString("verificationStatus", "")
                                    val reason = data.optString("reason", null)
                                    
                                    val message = when (status) {
                                        "approved" -> "恭喜！您的教师资格证审核已通过，现在可以开始接题了"
                                        "rejected" -> "很抱歉，您的教师资格证审核未通过${if (reason != null) "：$reason" else ""}，请重新提交"
                                        else -> "您的审核状态已更新"
                                    }
                                    
                                    callback(true, status, message)
                                } else {
                                    callback(true, null, null) // 没有通知
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "解析通知失败", e)
                                callback(false, null, "数据解析错误")
                            }
                        } else {
                            callback(false, null, "获取通知失败: ${response.code}")
                        }
                    }
                }
            }
        })
    }
}

