package com.example.andrio_teacher.ui.screens.video

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.andrio_teacher.network.endVideoCall
import com.example.andrio_teacher.trtc.TRTCVideoManager
import com.example.andrio_teacher.utils.UserSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * 1v1视频通话界面（教师端）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallScreen(
    navController: NavController,
    roomId: String,
    questionId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = remember { UserSession.getToken(context) ?: "" }
    val userId = remember { UserSession.getUserId(context) ?: "" }
    
    val videoManager = remember { TRTCVideoManager.getInstance() }
    
    var isCameraEnabled by remember { mutableStateOf(true) }
    var isMicrophoneEnabled by remember { mutableStateOf(true) }
    var remoteUserId by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // 初始化视频管理器
    LaunchedEffect(Unit) {
        videoManager.initialize(context)
        
        // 监听远程用户进入
        videoManager.onRemoteUserEnterRoom = { userId ->
            remoteUserId = userId
        }
        
        // 监听远程用户离开
        videoManager.onRemoteUserLeaveRoom = { userId ->
            if (remoteUserId == userId) {
                remoteUserId = null
            }
        }
    }
    
    // 结束通话
    fun endCall() {
        scope.launch(Dispatchers.IO) {
            // 调用后端API结束视频通话
            endVideoCall(token, questionId) { success, error ->
                if (!success) {
                    errorMsg = error ?: "结束视频通话失败"
                }
            }
            
            // 离开TRTC房间
            videoManager.exitRoom(
                onSuccess = {
                    scope.launch(Dispatchers.Main) {
                        navController.popBackStack()
                    }
                },
                onError = { _, _ ->
                    scope.launch(Dispatchers.Main) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 远程视频（全屏）
            if (remoteUserId != null) {
                com.example.andrio_teacher.ui.components.TRTCVideoViewComposable(
                    userId = remoteUserId,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "等待学生加入...",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            
            // 本地视频（小窗口，右上角）
            if (isCameraEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(120.dp, 160.dp)
                        .background(Color.Gray)
                ) {
                    com.example.andrio_teacher.ui.components.TRTCVideoViewComposable(
                        userId = null, // null表示本地视频
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // 底部控制栏
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 控制按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 摄像头开关
                    FloatingActionButton(
                        onClick = {
                            isCameraEnabled = !isCameraEnabled
                            videoManager.enableCamera(isCameraEnabled)
                        },
                        containerColor = if (isCameraEnabled) Color.White else Color.Red,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = if (isCameraEnabled) "关闭摄像头" else "开启摄像头",
                            tint = if (isCameraEnabled) Color.Black else Color.White
                        )
                    }
                    
                    // 麦克风开关
                    FloatingActionButton(
                        onClick = {
                            isMicrophoneEnabled = !isMicrophoneEnabled
                            videoManager.enableMicrophone(isMicrophoneEnabled)
                        },
                        containerColor = if (isMicrophoneEnabled) Color.White else Color.Red,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (isMicrophoneEnabled) "关闭麦克风" else "开启麦克风",
                            tint = if (isMicrophoneEnabled) Color.Black else Color.White
                        )
                    }
                    
                    // 结束通话
                    FloatingActionButton(
                        onClick = { endCall() },
                        containerColor = Color.Red,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "结束通话",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // 错误提示
            errorMsg?.let { error ->
                AlertDialog(
                    onDismissRequest = { errorMsg = null },
                    title = { Text("错误") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { errorMsg = null }) {
                            Text("确定")
                        }
                    }
                )
            }
        }
    }
    
    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            // 注意：不要在这里退出房间，因为用户可能只是暂时离开界面
            // 房间退出应该在用户点击"结束通话"时执行
        }
    }
}

