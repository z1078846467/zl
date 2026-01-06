package com.example.andrio_teacher.trtc

import android.content.Context
import android.util.Log
import com.tencent.cloud.tuikit.engine.room.TUIRoomDefine
import com.tencent.cloud.tuikit.engine.room.TUIRoomEngine
import com.tencent.cloud.tuikit.engine.room.TUIRoomObserver
import com.tencent.qcloud.tuicore.TUILogin

/**
 * TRTC视频流管理器（教师端）
 * 使用底层API，不需要RoomKit购买
 */
class TRTCVideoManager private constructor() {
    
    companion object {
        private const val TAG = "TRTCVideoManager"
        
        @Volatile
        private var INSTANCE: TRTCVideoManager? = null
        
        fun getInstance(): TRTCVideoManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TRTCVideoManager().also { INSTANCE = it }
            }
        }
    }
    
    private var roomEngine: TUIRoomEngine? = null
    private var currentRoomId: String? = null
    private var currentUserId: String? = null
    private var isInRoom = false
    
    // 回调
    var onUserVideoAvailable: ((String, Boolean) -> Unit)? = null
    var onUserAudioAvailable: ((String, Boolean) -> Unit)? = null
    var onRemoteUserEnterRoom: ((String) -> Unit)? = null
    var onRemoteUserLeaveRoom: ((String) -> Unit)? = null
    
    private val roomObserver = object : TUIRoomObserver() {
        override fun onRemoteUserEnterRoom(roomId: String, userInfo: TUIRoomDefine.UserInfo) {
            Log.d(TAG, "Remote user entered room: ${userInfo.userId}")
            onRemoteUserEnterRoom?.invoke(userInfo.userId)
        }
        
        override fun onRemoteUserLeaveRoom(roomId: String, userInfo: TUIRoomDefine.UserInfo) {
            Log.d(TAG, "Remote user left room: ${userInfo.userId}")
            onRemoteUserLeaveRoom?.invoke(userInfo.userId)
        }
        
        override fun onUserVideoStateChanged(
            userId: String,
            streamType: TUIRoomDefine.VideoStreamType,
            hasVideo: Boolean,
            reason: TUIRoomDefine.ChangeReason
        ) {
            Log.d(TAG, "User video state changed: userId=$userId, hasVideo=$hasVideo")
            onUserVideoAvailable?.invoke(userId, hasVideo)
        }
        
        override fun onUserAudioStateChanged(
            userId: String,
            hasAudio: Boolean,
            reason: TUIRoomDefine.ChangeReason
        ) {
            Log.d(TAG, "User audio state changed: userId=$userId, hasAudio=$hasAudio")
            onUserAudioAvailable?.invoke(userId, hasAudio)
        }
    }
    
    /**
     * 初始化TRTC房间引擎
     */
    fun initialize(context: Context) {
        if (roomEngine != null) {
            Log.d(TAG, "TRTCVideoManager already initialized")
            return
        }
        
        roomEngine = TUIRoomEngine.sharedInstance()
        
        // 添加观察者
        roomEngine?.addObserver(roomObserver)
        
        Log.d(TAG, "TRTCVideoManager initialized")
    }
    
    /**
     * 创建或加入房间（智能判断：先尝试加入，如果房间不存在则创建）
     */
    fun createOrJoinRoom(
        context: Context,
        roomId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit
    ) {
        // 如果已经在同一个房间，直接返回成功
        if (isInRoom && currentRoomId == roomId) {
            Log.d(TAG, "Already in room: $roomId")
            onSuccess()
            return
        }
        
        // 如果已经在其他房间，先退出旧房间
        if (isInRoom && currentRoomId != null && currentRoomId != roomId) {
            Log.w(TAG, "Currently in different room: $currentRoomId, need to exit before joining: $roomId")
            exitRoom(
                onSuccess = {
                    proceedToCreateOrJoin(context, roomId, userId, onSuccess, onError)
                },
                onError = { code, msg ->
                    Log.e(TAG, "Failed to exit old room: $code - $msg, proceeding anyway")
                    // 即使退出失败，也继续尝试加入新房间
                    proceedToCreateOrJoin(context, roomId, userId, onSuccess, onError)
                }
            )
            return
        }
        
        // 直接进行创建或加入
        proceedToCreateOrJoin(context, roomId, userId, onSuccess, onError)
    }
    
    private fun proceedToCreateOrJoin(
        context: Context,
        roomId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit
    ) {
        currentUserId = userId
        
        // 确保房间ID格式正确（TRTC要求：只支持数字和字母）
        val validRoomId = roomId.filter { it.isLetterOrDigit() }.take(64)
        if (validRoomId.isEmpty()) {
            Log.e(TAG, "Invalid room ID format: $roomId")
            onError(-1, "房间ID格式无效")
            return
        }
        
        currentRoomId = validRoomId
        
        // 注意：TRTC登录应该在调用此方法前已经完成
        if (!TUILogin.isUserLogined()) {
            Log.w(TAG, "TUILogin not logged in, proceeding anyway")
        }
        
        Log.d(TAG, "Proceeding to create or join room: $validRoomId (original: $roomId)")
        
        // 对于1v1场景，直接尝试创建房间（教师端创建，学生端加入）
        // 如果创建失败（房间已存在），则尝试加入
        createNewRoom(validRoomId, onSuccess, onError)
    }
    
    /**
     * 加入已存在的房间
     */
    private fun joinExistingRoom(
        roomId: String,
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit
    ) {
        roomEngine?.enterRoom(roomId, object : TUIRoomDefine.GetRoomInfoCallback {
            override fun onSuccess(roomInfo: TUIRoomDefine.RoomInfo) {
                Log.d(TAG, "Room joined successfully: $roomId")
                isInRoom = true
                onSuccess()
            }
            
            override fun onError(error: com.tencent.cloud.tuikit.engine.common.TUICommonDefine.Error, message: String) {
                Log.e(TAG, "Failed to enter existing room: $error - $message")
                onError(error.ordinal, message)
            }
        })
    }
    
    /**
     * 创建新房间
     */
    private fun createNewRoom(
        roomId: String,
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit
    ) {
        // 确保房间ID格式正确（TRTC要求：只支持数字和字母，长度不超过64字符）
        val validRoomId = roomId.filter { it.isLetterOrDigit() }.take(64)
        if (validRoomId.isEmpty()) {
            Log.e(TAG, "Invalid room ID format: $roomId")
            onError(-1, "房间ID格式无效")
            return
        }
        
        // 检查roomEngine是否已初始化
        if (roomEngine == null) {
            Log.e(TAG, "RoomEngine not initialized")
            onError(-1, "视频引擎未初始化")
            return
        }
        
        val roomInfo = TUIRoomDefine.RoomInfo()
        roomInfo.roomId = validRoomId
        roomInfo.roomType = TUIRoomDefine.RoomType.CONFERENCE
        roomInfo.name = "1v1答疑房间"
        // 设置房间模式为自由发言（1v1场景）
        roomInfo.speechMode = TUIRoomDefine.SpeechMode.FREE_TO_SPEAK
        
        Log.d(TAG, "Attempting to create room with ID: $validRoomId (original: $roomId)")
        Log.d(TAG, "RoomInfo: roomId=$validRoomId, roomType=CONFERENCE, name=${roomInfo.name}")
        
        roomEngine.createRoom(roomInfo, object : TUIRoomDefine.ActionCallback {
            override fun onSuccess() {
                Log.d(TAG, "Room created successfully: $validRoomId")
                isInRoom = true
                currentRoomId = validRoomId // 更新为有效的房间ID
                onSuccess()
            }
            
            override fun onError(error: com.tencent.cloud.tuikit.engine.common.TUICommonDefine.Error, message: String) {
                // 记录详细的错误信息
                Log.e(TAG, "Failed to create room: errorCode=${error.ordinal}, errorName=${error.name}, message=$message")
                
                // 检查是否是权限或配置问题
                if (message.contains("bill", ignoreCase = true) || 
                    message.contains("purchase", ignoreCase = true) ||
                    error.ordinal == 100007 ||
                    message.contains("100007")) {
                    Log.e(TAG, "RoomKit purchase required or permission denied")
                    onError(error.ordinal, "需要购买RoomKit或权限不足: $message")
                    return
                }
                
                // 如果创建失败，尝试直接加入房间（可能房间已经存在或正在创建中）
                Log.w(TAG, "Room creation failed, attempting to join room directly")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    joinExistingRoom(validRoomId, onSuccess, onError)
                }, 500) // 短暂延迟后尝试加入
            }
        })
    }
    
    /**
     * 重试查询并加入房间（用于创建失败后的重试）
     */
    private fun retryFetchAndJoin(
        roomId: String,
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit,
        retryCount: Int
    ) {
        if (retryCount <= 0) {
            Log.e(TAG, "Max retry count reached, giving up for room: $roomId")
            onError(-1, "Failed to create or join room after retries. Room ID: $roomId")
            return
        }
        
        Log.d(TAG, "Retrying to fetch room info: $roomId (remaining retries: $retryCount)")
        
        roomEngine?.fetchRoomInfo(roomId, TUIRoomDefine.RoomType.CONFERENCE, object : TUIRoomDefine.GetRoomInfoCallback {
            override fun onSuccess(roomInfo: TUIRoomDefine.RoomInfo) {
                // 房间已存在，加入
                Log.d(TAG, "Room found on retry, joining: $roomId")
                joinExistingRoom(roomId, onSuccess, onError)
            }
            
            override fun onError(error: com.tencent.cloud.tuikit.engine.common.TUICommonDefine.Error, message: String) {
                // 仍然不存在，再次延迟重试
                Log.w(TAG, "Room still not found on retry: $error - $message, will retry again (remaining: ${retryCount - 1})")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    retryFetchAndJoin(roomId, onSuccess, onError, retryCount - 1)
                }, 1000) // 增加延迟到1秒
            }
        })
    }
    
    /**
     * 离开房间
     */
    fun exitRoom(
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit
    ) {
        if (!isInRoom) {
            Log.d(TAG, "Not in room, exitRoom ignored")
            onSuccess()
            return
        }
        
        roomEngine?.exitRoom(false, object : TUIRoomDefine.ActionCallback {
            override fun onSuccess() {
                Log.d(TAG, "Exited room successfully")
                isInRoom = false
                currentRoomId = null
                onSuccess()
            }
            
            override fun onError(error: com.tencent.cloud.tuikit.engine.common.TUICommonDefine.Error, message: String) {
                Log.e(TAG, "Failed to exit room: $error - $message")
                isInRoom = false
                currentRoomId = null
                onError(error.ordinal, message)
            }
        })
    }
    
    /**
     * 设置本地视频视图
     */
    fun setLocalVideoView(view: com.tencent.cloud.tuikit.engine.common.TUIVideoView?) {
        if (view == null) {
            roomEngine?.setLocalVideoView(TUIRoomDefine.VideoStreamType.CAMERA_STREAM, null)
        } else {
            roomEngine?.setLocalVideoView(TUIRoomDefine.VideoStreamType.CAMERA_STREAM, view)
        }
        Log.d(TAG, "Local video view ${if (view != null) "set" else "removed"}")
    }
    
    /**
     * 设置远程用户视频视图
     */
    fun setRemoteVideoView(userId: String, view: com.tencent.cloud.tuikit.engine.common.TUIVideoView?) {
        if (view == null) {
            roomEngine?.setRemoteVideoView(userId, TUIRoomDefine.VideoStreamType.CAMERA_STREAM, null)
        } else {
            roomEngine?.setRemoteVideoView(userId, TUIRoomDefine.VideoStreamType.CAMERA_STREAM, view)
        }
        Log.d(TAG, "Remote video view ${if (view != null) "set" else "removed"} for user: $userId")
    }
    
    /**
     * 开启/关闭摄像头
     */
    fun enableCamera(enabled: Boolean, onResult: ((Boolean) -> Unit)? = null) {
        if (enabled) {
            roomEngine?.openLocalCamera(
                true, // 前置摄像头
                TUIRoomDefine.VideoQuality.Q_720P, // 视频质量
                object : TUIRoomDefine.ActionCallback {
                    override fun onSuccess() {
                        Log.d(TAG, "Camera enabled")
                        onResult?.invoke(true)
                    }
                    
                    override fun onError(error: com.tencent.cloud.tuikit.engine.common.TUICommonDefine.Error, message: String) {
                        Log.e(TAG, "Failed to enable camera: $error - $message")
                        onResult?.invoke(false)
                    }
                }
            )
        } else {
            roomEngine?.closeLocalCamera()
            Log.d(TAG, "Camera disabled")
            onResult?.invoke(true)
        }
    }
    
    /**
     * 开启/关闭麦克风
     */
    fun enableMicrophone(enabled: Boolean, onResult: ((Boolean) -> Unit)? = null) {
        if (enabled) {
            roomEngine?.openLocalMicrophone(
                TUIRoomDefine.AudioQuality.DEFAULT,
                object : TUIRoomDefine.ActionCallback {
                    override fun onSuccess() {
                        Log.d(TAG, "Microphone enabled")
                        onResult?.invoke(true)
                    }
                    
                    override fun onError(error: com.tencent.cloud.tuikit.engine.common.TUICommonDefine.Error, message: String) {
                        Log.e(TAG, "Failed to enable microphone: $error - $message")
                        onResult?.invoke(false)
                    }
                }
            )
        } else {
            roomEngine?.closeLocalMicrophone()
            Log.d(TAG, "Microphone disabled")
            onResult?.invoke(true)
        }
    }
    
    /**
     * 获取当前房间ID
     */
    fun getCurrentRoomId(): String? = currentRoomId
    
    /**
     * 检查是否在房间中
     */
    fun isInRoom(): Boolean = isInRoom
    
    /**
     * 清理资源
     */
    fun release() {
        if (isInRoom) {
            exitRoom(
                onSuccess = { releaseInternal() },
                onError = { _, _ -> releaseInternal() }
            )
        } else {
            releaseInternal()
        }
    }
    
    private fun releaseInternal() {
        roomEngine?.removeObserver(roomObserver)
        roomEngine = null
        currentRoomId = null
        currentUserId = null
        Log.d(TAG, "TRTCVideoManager released")
    }
}

