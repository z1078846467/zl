package com.example.andrio_teacher.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.andrio_teacher.trtc.TRTCVideoManager
import com.tencent.cloud.tuikit.engine.common.TUIVideoView
import android.content.Context
import android.util.Log

/**
 * TRTC视频视图Compose组件（教师端）
 * @param userId 用户ID，null表示本地视频
 * @param modifier 修饰符
 */
@Composable
fun TRTCVideoViewComposable(
    userId: String?,
    modifier: Modifier = Modifier,
    videoManager: TRTCVideoManager = remember { TRTCVideoManager.getInstance() }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var videoView by remember { mutableStateOf<TUIVideoView?>(null) }
    
    LaunchedEffect(userId) {
        videoView = TUIVideoView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        if (userId == null) {
            // 本地视频
            videoManager.setLocalVideoView(videoView)
            Log.d("TRTCVideoView", "Setting local video view")
        } else {
            // 远程视频
            videoManager.setRemoteVideoView(userId, videoView)
            Log.d("TRTCVideoView", "Setting remote video view for user: $userId")
        }
    }
    
    DisposableEffect(userId) {
        onDispose {
            // 清理视频视图
            if (userId == null) {
                videoManager.setLocalVideoView(null)
            } else {
                videoManager.setRemoteVideoView(userId, null)
            }
        }
    }
    
    videoView?.let { view ->
        AndroidView(
            factory = { view },
            modifier = modifier.fillMaxSize()
        )
    }
}

