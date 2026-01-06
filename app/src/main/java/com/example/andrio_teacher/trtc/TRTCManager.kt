package com.example.andrio_teacher.trtc

import android.content.Context
import android.util.Log
import com.tencent.qcloud.tuicore.TUILogin
import com.tencent.qcloud.tuicore.interfaces.TUICallback
import com.tencent.cloud.tuikit.roomkit.debug.GenerateTestUserSig

/**
 * TRTC视频房间管理器（教师端）
 * 负责处理TUILogin登录和1v1视频会议功能
 */
class TRTCManager private constructor() {
    
    companion object {
        private const val TAG = "TRTCManager"
        
        // 腾讯云TRTC配置（与学生端保持一致）
        private const val SDK_APP_ID = 1600110603L
        private const val SDK_SECRET_KEY = "e363cbb6ce0f53593dc8b415b8a5fe1d446fcdb90e3122bf772cc7ad07bcdd15"
        
        @Volatile
        private var INSTANCE: TRTCManager? = null
        
        fun getInstance(): TRTCManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TRTCManager().also { INSTANCE = it }
            }
        }
    }
    
    private var isLoggedIn = false
    private var currentUserId: String? = null
    
    /**
     * 登录TRTC
     */
    fun login(
        context: Context,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit
    ) {
        if (isLoggedIn && currentUserId == userId) {
            onSuccess()
            return
        }
        // 生成UserSig
        val userSig = GenerateTestUserSig.genTestUserSig(SDK_APP_ID.toInt(), userId, SDK_SECRET_KEY)
        
        TUILogin.login(
            context,
            SDK_APP_ID.toInt(),
            userId,
            userSig,
            object : TUICallback() {
                override fun onSuccess() {
                    isLoggedIn = true
                    currentUserId = userId
                    Log.d(TAG, "TRTC login success: $userId")
                    onSuccess()
                }
                
                override fun onError(errorCode: Int, errorMessage: String?) {
                    Log.e(TAG, "TRTC login failed: $errorCode - $errorMessage")
                    isLoggedIn = false
                    currentUserId = null
                    onError(errorCode, errorMessage ?: "Unknown error")
                }
            }
        )
    }
    
    /**
     * 发起1v1视频会议（创建房间）
     * 使用TRTCVideoManager底层API，不需要RoomKit购买
     */
    fun startConference(
        context: Context,
        roomId: String,
        userId: String,
        onSuccess: () -> Unit = {},
        onError: (Int, String) -> Unit = { _, _ -> }
    ) {
        if (!isLoggedIn) {
            Log.e(TAG, "Not logged in, cannot start conference")
            onError(-1, "未登录TRTC")
            return
        }
        
        // 使用TRTCVideoManager创建或加入房间
        val videoManager = TRTCVideoManager.getInstance()
        videoManager.initialize(context)
        
        videoManager.createOrJoinRoom(
            context = context,
            roomId = roomId,
            userId = userId,
            onSuccess = {
                // 开启摄像头和麦克风
                videoManager.enableCamera(true)
                videoManager.enableMicrophone(true)
                Log.d(TAG, "Started 1v1 video conference: roomId=$roomId, userId=$userId")
                onSuccess()
            },
            onError = { code, msg ->
                Log.e(TAG, "Failed to start conference: $code - $msg")
                onError(code, msg)
            }
        )
    }
    
    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean = isLoggedIn
    
    /**
     * 获取当前用户ID
     */
    fun getCurrentUserId(): String? = currentUserId
    
    /**
     * 登出
     */
    fun logout() {
        isLoggedIn = false
        currentUserId = null
        TUILogin.logout(null as TUICallback?)
        Log.d(TAG, "TRTC logged out")
    }
}

