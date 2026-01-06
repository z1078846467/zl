package com.example.andrio_teacher.ui.screens.detail

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.andrio_teacher.network.Question
import com.example.andrio_teacher.network.acceptQuestion
import com.example.andrio_teacher.network.abandonQuestion
import com.example.andrio_teacher.network.getQuestionDetail
import com.example.andrio_teacher.network.startVideoCall
import com.example.andrio_teacher.utils.UserSession
import com.example.andrio_teacher.utils.ErrorHandler
import com.example.andrio_teacher.ui.components.ErrorDisplay
import com.example.andrio_teacher.ui.components.LoadingDisplay
import com.example.andrio_teacher.ui.components.EmptyStateDisplay
import com.example.andrio_teacher.ui.navigation.AppRoutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    navController: NavController,
    questionId: String,
    onQuestionRemoved: () -> Unit
) {
    val context = LocalContext.current
    val token = remember { UserSession.getToken(context) ?: "" }
    val scope = rememberCoroutineScope() // 在Composable作用域中创建
    
    var question by remember { mutableStateOf<Question?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // 倒计时状态
    var timeRemaining by remember { mutableStateOf(300) } // 5分钟 = 300秒
    var isAccepted by remember { mutableStateOf(false) }
    var isAbandoned by remember { mutableStateOf(false) }
    var abandonCountToday by remember { mutableStateOf(0) } // 今日放弃次数
    
    // 加载题目详情
    LaunchedEffect(questionId) {
        if (token.isBlank()) {
            isLoading = false
            errorMsg = "未登录或登录已失效"
            ErrorHandler.checkTokenExpired("未登录", 401, context, navController)
            return@LaunchedEffect
        }
        isLoading = true
        getQuestionDetail(token, questionId) { success, q, err ->
            isLoading = false
            if (success && q != null) {
                question = q
                isAccepted = q.status == "assigned" || q.status == "in_progress"
                // 从 SharedPreferences 读取今日放弃次数
                val prefs = context.getSharedPreferences("teacher_prefs", Context.MODE_PRIVATE)
                val lastResetDate = prefs.getString("last_reset_date", "")
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (lastResetDate != today) {
                    // 新的一天，重置计数
                    prefs.edit().putString("last_reset_date", today).putInt("abandon_count", 0).apply()
                    abandonCountToday = 0
                } else {
                    abandonCountToday = prefs.getInt("abandon_count", 0)
                }
                errorMsg = null
            } else {
                val errorType = ErrorHandler.analyzeError(err)
                // 检查Token过期
                if (ErrorHandler.checkTokenExpired(err, null, context, navController)) {
                    return@getQuestionDetail
                }
                errorMsg = ErrorHandler.handleApiError(err)
            }
        }
    }
    
    // 倒计时逻辑
    LaunchedEffect(timeRemaining, isAccepted, isAbandoned) {
        if (timeRemaining > 0 && !isAbandoned) {
            delay(1000)
            timeRemaining--
        } else if (timeRemaining == 0 && !isAccepted && !isAbandoned) {
            // 5分钟未操作，自动退出
            onQuestionRemoved()
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("题目详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            if (!isLoading && question != null && !isAbandoned) {
                QuestionDetailBottomBar(
                    isAccepted = isAccepted,
                    timeRemaining = timeRemaining,
                    abandonCountToday = abandonCountToday,
                    onAccept = {
                        if (token.isBlank()) return@QuestionDetailBottomBar
                        acceptQuestion(token, questionId) { success, error ->
                            if (success) {
                                isAccepted = true
                                timeRemaining = 300 // 重新开始5分钟倒计时
                            } else {
                                errorMsg = error ?: "接收失败"
                            }
                        }
                    },
                    onAbandon = {
                        if (token.isBlank()) return@QuestionDetailBottomBar
                        if (isAccepted && abandonCountToday >= 3) {
                            errorMsg = "今日放弃次数已达上限（3次）"
                            return@QuestionDetailBottomBar
                        }
                        abandonQuestion(token, questionId) { success, error ->
                            if (success) {
                                isAbandoned = true
                                if (isAccepted) {
                                    // 接收后放弃，增加计数
                                    val prefs = context.getSharedPreferences("teacher_prefs", Context.MODE_PRIVATE)
                                    val newCount = abandonCountToday + 1
                                    prefs.edit().putInt("abandon_count", newCount).apply()
                                    abandonCountToday = newCount
                                }
                                // 延迟执行导航，避免状态更新冲突
                                scope.launch(Dispatchers.Main) {
                                    delay(100)
                                    onQuestionRemoved()
                                    navController.popBackStack()
                                }
                            } else {
                                val errorType = ErrorHandler.analyzeError(error)
                                if (ErrorHandler.checkTokenExpired(error, null, context, navController)) {
                                    return@abandonQuestion
                                }
                                errorMsg = ErrorHandler.handleApiError(error)
                            }
                        }
                    },
                    onStartVideo = {
                        if (token.isBlank()) return@QuestionDetailBottomBar
                        // 先保存question的引用，避免smart cast问题
                        val currentQuestion = question
                        if (currentQuestion == null) return@QuestionDetailBottomBar
                        
                        // 生成房间ID（使用题目ID作为房间ID，确保唯一性）
                        // TRTC房间ID只支持数字和字母，不能包含下划线、连字符等特殊字符
                        // 格式：question + 题目ID（去掉所有非字母数字字符）
                        val roomId = "question${currentQuestion.id.filter { it.isLetterOrDigit() }}"
                        val userId = UserSession.getUserId(context) ?: return@QuestionDetailBottomBar
                        
                        // 先登录TRTC（如果未登录）
                        val trtcManager = com.example.andrio_teacher.trtc.TRTCManager.getInstance()
                        if (!trtcManager.isLoggedIn()) {
                            trtcManager.login(
                                context = context,
                                userId = userId,
                                onSuccess = {
                                    // TRTC登录成功，创建房间并通知后端
                                    // roomId已经在上面生成时处理过了，直接使用
                                    trtcManager.startConference(
                                        context = context,
                                        roomId = roomId,
                                        userId = userId,
                                        onSuccess = {
                                            // 房间创建成功，调用后端API通知学生端
                                            com.example.andrio_teacher.network.startVideoCall(
                                                token = token,
                                                questionId = currentQuestion.id,
                                                roomId = roomId
                                            ) { success, error ->
                                                if (success) {
                                                    // 视频通话已发起，导航到视频通话界面
                                                    navController.navigate("video_call/$roomId/${currentQuestion.id}")
                                                } else {
                                                    errorMsg = error ?: "发起视频通话失败"
                                                }
                                            }
                                        },
                                        onError = { code, msg ->
                                            errorMsg = "创建视频房间失败: $msg"
                                        }
                                    )
                                },
                                onError = { code, msg ->
                                    errorMsg = "TRTC登录失败: $msg"
                                }
                            )
                        } else {
                            // 已登录，直接创建房间
                            // roomId已经在上面生成时处理过了，直接使用
                            trtcManager.startConference(
                                context = context,
                                roomId = roomId,
                                userId = userId,
                                onSuccess = {
                                    // 房间创建成功，调用后端API通知学生端
                                    com.example.andrio_teacher.network.startVideoCall(
                                        token = token,
                                        questionId = currentQuestion.id,
                                        roomId = roomId
                                    ) { success, error ->
                                        if (success) {
                                            // 视频通话已发起，导航到视频通话界面
                                            navController.navigate("video_call/$roomId/${currentQuestion.id}")
                                        } else {
                                            errorMsg = error ?: "发起视频通话失败"
                                        }
                                    }
                                },
                                onError = { code, msg ->
                                    errorMsg = "创建视频房间失败: $msg"
                                }
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                isLoading -> {
                    LoadingDisplay(
                        message = "加载题目详情中...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMsg != null -> {
                    ErrorDisplay(
                        error = errorMsg!!,
                        errorType = ErrorHandler.analyzeError(errorMsg),
                        onRetry = {
                            // 重新加载题目详情
                            if (token.isNotBlank()) {
                                isLoading = true
                                errorMsg = null
                                getQuestionDetail(token, questionId) { success, q, err ->
                                    isLoading = false
                                    if (success && q != null) {
                                        question = q
                                        isAccepted = q.status == "assigned" || q.status == "in_progress"
                                        errorMsg = null
                                    } else {
                                        val errorType = ErrorHandler.analyzeError(err)
                                        if (ErrorHandler.checkTokenExpired(err, null, context, navController)) {
                                            return@getQuestionDetail
                                        }
                                        errorMsg = ErrorHandler.handleApiError(err)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                question == null -> {
                    EmptyStateDisplay(
                        title = "题目不存在",
                        message = "该题目可能已被删除或不存在",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    QuestionDetailContent(
                        question = question!!,
                        timeRemaining = timeRemaining,
                        isAccepted = isAccepted
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionDetailContent(
    question: Question,
    timeRemaining: Int,
    isAccepted: Boolean
) {
    val subjectText = when (question.subject) {
        "physics" -> "物理"
        "chemistry" -> "化学"
        "math" -> "数学"
        "biology" -> "生物"
        "english" -> "英语"
        "chinese" -> "语文"
        "history" -> "历史"
        "geography" -> "地理"
        "other" -> "其他"
        else -> question.subject
    }
    
    val academicStageText = question.academicStage ?: "未指定"
    val priceText = if (question.price != null) "${question.price}元" else "待定"
    
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 倒计时提示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (timeRemaining < 60) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAccepted) "准备时间" else "查看时间",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (timeRemaining < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 题目图片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            AsyncImage(
                model = question.imageUrl,
                contentDescription = "题目图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        }
        
        // 题目信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow("学科", subjectText)
                InfoRow("学段", academicStageText)
                InfoRow("价格", priceText)
                InfoRow("状态", question.status)
                if (!question.description.isNullOrBlank()) {
                    InfoRow("描述", question.description)
                }
                InfoRow("题目ID", question.id.take(8))
                if (question.roomId != null) {
                    InfoRow("自习室", question.roomId)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun QuestionDetailBottomBar(
    isAccepted: Boolean,
    timeRemaining: Int,
    abandonCountToday: Int,
    onAccept: () -> Unit,
    onAbandon: () -> Unit,
    onStartVideo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAccepted) {
                // 已接收状态：发起视频 + 放弃
                Button(
                    onClick = onStartVideo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发起视频")
                }
                OutlinedButton(
                    onClick = onAbandon,
                    modifier = Modifier.weight(1f),
                    enabled = abandonCountToday < 3
                ) {
                    Text("放弃")
                    if (abandonCountToday > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(${3 - abandonCountToday}/3)", fontSize = 12.sp)
                    }
                }
                if (abandonCountToday >= 3) {
                    Text(
                        text = "今日放弃次数已达上限",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                // 未接收状态：接收 + 放弃
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("接收题目", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onAbandon,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("放弃")
                }
            }
        }
    }
}

