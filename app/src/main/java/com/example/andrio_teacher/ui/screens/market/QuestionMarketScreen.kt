package com.example.andrio_teacher.ui.screens.market

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.andrio_teacher.network.Question
import com.example.andrio_teacher.network.getQuestionMarket
import com.example.andrio_teacher.ui.navigation.AppRoutes
import com.example.andrio_teacher.utils.UserSession
import com.example.andrio_teacher.utils.NotificationManager
import com.example.andrio_teacher.utils.ErrorHandler
import com.example.andrio_teacher.ui.components.ErrorDisplay
import com.example.andrio_teacher.ui.components.EmptyStateDisplay
import com.example.andrio_teacher.ui.components.LoadingDisplay
import kotlinx.coroutines.delay

// 题目项组件
@Composable
fun QuestionItem(question: Question, onClick: () -> Unit, enabled: Boolean = true) {
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
    
    val academicStageText = when (question.academicStage) {
        "primary" -> "小学"
        "middle", "junior_high" -> "初中"
        "high", "senior_high" -> "高中"
        else -> question.academicStage ?: "未指定"
    }
        val priceText = if (question.price != null && question.price > 0) "${question.price}元" else "待定"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 题目图片
            AsyncImage(
                model = question.imageUrl,
                contentDescription = "题目图片",
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            // 题目信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = subjectText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "学段：$academicStageText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "价格：$priceText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!question.description.isNullOrBlank()) {
                    Text(
                        text = question.description.take(30) + if (question.description.length > 30) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// 科目选择对话框
@Composable
fun SubjectSelectionDialog(
    selectedSubjects: Set<String>,
    onSubjectToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val allSubjects = listOf(
        "语文" to "chinese",
        "数学" to "math",
        "英语" to "english",
        "地理" to "geography",
        "历史" to "history",
        "物理" to "physics",
        "化学" to "chemistry",
        "生物" to "biology",
        "政治" to "politics",
        "科学" to "science",
        "道德" to "morality",
        "法制" to "legal",
        "其他" to "other"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择科目", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 使用网格布局显示科目
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 第一行：语文、数学、英语
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allSubjects.take(3).forEach { (label, value) ->
                            SubjectCheckboxItem(
                                label = label,
                                value = value,
                                isSelected = selectedSubjects.contains(value),
                                onToggle = { onSubjectToggle(value) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // 第二行：地理、历史、物理
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allSubjects.drop(3).take(3).forEach { (label, value) ->
                            SubjectCheckboxItem(
                                label = label,
                                value = value,
                                isSelected = selectedSubjects.contains(value),
                                onToggle = { onSubjectToggle(value) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // 第三行：化学、生物、政治
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allSubjects.drop(6).take(3).forEach { (label, value) ->
                            SubjectCheckboxItem(
                                label = label,
                                value = value,
                                isSelected = selectedSubjects.contains(value),
                                onToggle = { onSubjectToggle(value) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // 第四行：科学、道德、法制
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allSubjects.drop(9).take(3).forEach { (label, value) ->
                            SubjectCheckboxItem(
                                label = label,
                                value = value,
                                isSelected = selectedSubjects.contains(value),
                                onToggle = { onSubjectToggle(value) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // 第五行：其他
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allSubjects.last().let { (label, value) ->
                            SubjectCheckboxItem(
                                label = label,
                                value = value,
                                isSelected = selectedSubjects.contains(value),
                                onToggle = { onSubjectToggle(value) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    // 重置所有科目选择
                    allSubjects.forEach { (_, value) ->
                        if (selectedSubjects.contains(value)) {
                            onSubjectToggle(value)
                        }
                    }
                }) {
                    Text("重置")
                }
                Button(onClick = onConfirm) {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SubjectCheckboxItem(
    label: String,
    value: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionMarketScreen(
    navController: NavController,
    onQuestionClick: (String) -> Unit
) {
    val context = LocalContext.current
    val token = remember { UserSession.getToken(context) ?: "" }
    var verificationStatus by remember { mutableStateOf(UserSession.getVerificationStatus(context)) }
    var isVerified by remember { mutableStateOf(UserSession.isVerified(context)) }
    
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    // 审核通知相关状态
    var showVerificationDialog by remember { mutableStateOf(false) }
    var verificationMessage by remember { mutableStateOf<String?>(null) }
    var verificationStatusUpdate by remember { mutableStateOf<String?>(null) }
    
    // 筛选条件（从SharedPreferences加载）
    var selectedAcademicStage by remember { 
        mutableStateOf(UserSession.getSelectedAcademicStage(context))
    } // 学段：primary, junior_high, senior_high
    var selectedSubjects by remember { 
        mutableStateOf(UserSession.getSelectedSubjects(context))
    } // 多选科目
    var showSubjectDialog by remember { mutableStateOf(false) }
    
    // 保存筛选条件
    fun saveFilterSettings() {
        UserSession.saveFilterSettings(context, selectedSubjects, selectedAcademicStage)
    }
    
    // 已查看的题目ID（从详情页返回时，这些题目不再显示）
    var viewedQuestionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // 刷新题目列表
    fun refreshQuestions() {
        if (token.isBlank()) {
            errorMsg = "未登录或登录已失效"
            isLoading = false
            // 检查是否需要跳转登录
            ErrorHandler.checkTokenExpired("未登录", 401, context, navController)
            return
        }
        isLoading = true
        errorMsg = null
        // 如果选择了多个科目，不传subject参数给后端，让后端返回所有题目，然后在前端过滤
        // 如果只选择了一个科目，传这个科目给后端进行服务端过滤
        val subjectFilter = if (selectedSubjects.size == 1) {
            selectedSubjects.firstOrNull()
        } else {
            null // 多选时不传subject，让后端返回所有题目
        }
        
        getQuestionMarket(
            token = token,
            subject = subjectFilter,
            academicStage = selectedAcademicStage,
            minPrice = null,
            maxPrice = null
        ) { success, questionList, error, statusCode ->
            isLoading = false
            if (success && questionList != null) {
                // 过滤掉已查看的题目
                var filteredQuestions = questionList.filter { it.id !in viewedQuestionIds }
                
                // 如果选择了多个科目，在前端进行过滤
                if (selectedSubjects.isNotEmpty()) {
                    filteredQuestions = filteredQuestions.filter { question ->
                        selectedSubjects.contains(question.subject)
                    }
                }
                
                // 如果选择了学段，在前端进行过滤（双重保险，后端已支持）
                if (selectedAcademicStage != null) {
                    filteredQuestions = filteredQuestions.filter { question ->
                        question.academicStage == selectedAcademicStage
                    }
                }
                
                questions = filteredQuestions
                errorMsg = null
            } else {
                // 检查Token过期（传递statusCode）
                if (ErrorHandler.checkTokenExpired(error, statusCode, context, navController)) {
                    return@getQuestionMarket
                }
                errorMsg = ErrorHandler.handleApiError(error, statusCode)
            }
        }
    }
    
    // 检查审核通知的函数
    fun checkVerificationNotification() {
        if (token.isBlank()) return
        
        NotificationManager.checkVerificationNotification(token) { success, status, message ->
            if (success && message != null && status != null) {
                // 使用AlertDialog显示通知，而不是errorMsg
                verificationMessage = message
                verificationStatusUpdate = status
                showVerificationDialog = true
                
                // 立即更新本地状态
                if (status == "approved") {
                    UserSession.saveVerificationStatus(context, "approved")
                    verificationStatus = "approved"
                    isVerified = true
                    // 审核通过后立即刷新题目列表
                    refreshQuestions()
                } else if (status == "rejected") {
                    UserSession.saveVerificationStatus(context, "rejected")
                    verificationStatus = "rejected"
                    isVerified = false
                }
            }
        }
    }
    
    // 初始加载和定期刷新
    LaunchedEffect(Unit) {
        refreshQuestions()
        checkVerificationNotification()
        
        // 每10秒刷新一次题目列表和检查通知（避免触发速率限制，每分钟6次，在限制范围内）
        while (true) {
            delay(10000)
            refreshQuestions()
            checkVerificationNotification() // 定期检查通知
        }
    }
    
    // 当筛选条件改变时刷新
    LaunchedEffect(selectedAcademicStage, selectedSubjects) {
        refreshQuestions()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("题目大厅", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(AppRoutes.PROFILE) }) {
                        Icon(Icons.Default.Person, contentDescription = "个人中心")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
                // 筛选条幅
                FilterBar(
                    selectedAcademicStage = selectedAcademicStage,
                    selectedSubjects = selectedSubjects,
                    onAcademicStageChange = { stage ->
                        selectedAcademicStage = if (selectedAcademicStage == stage) null else stage
                        saveFilterSettings() // 保存筛选条件
                    },
                    onSubjectClick = { showSubjectDialog = true }
                )
            
            // 审核状态提示栏
            if (verificationStatus != null && verificationStatus != "approved") {
                val statusText = when (verificationStatus) {
                    "pending" -> "您的资料正在审核中，审核通过后即可开始接题"
                    "rejected" -> "您的资料审核未通过，请重新提交"
                    else -> "请完善资料并上传证件"
                }
                val statusColor = when (verificationStatus) {
                    "pending" -> Color(0xFFFF9800) // 橙色
                    "rejected" -> MaterialTheme.colorScheme.error
                    else -> Color(0xFF2196F3) // 蓝色
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (verificationStatus == "rejected") {
                            TextButton(onClick = {
                                // TODO: 跳转到重新提交资料页面
                            }) {
                                Text("重新提交", color = statusColor)
                            }
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        LoadingDisplay(
                            message = "加载题目中...",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    errorMsg != null -> {
                        ErrorDisplay(
                            error = errorMsg!!,
                            errorType = ErrorHandler.analyzeError(errorMsg),
                            onRetry = { refreshQuestions() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    questions.isEmpty() -> {
                        EmptyStateDisplay(
                            title = "暂无题目",
                            message = "题目会在这里滚动显示",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(questions) { question ->
                                QuestionItem(
                                    question = question,
                                    onClick = {
                                        // 检查审核状态
                                        if (!isVerified) {
                                            errorMsg = "请先完成资料审核"
                                            return@QuestionItem
                                        }
                                        // 标记为已查看
                                        viewedQuestionIds = viewedQuestionIds + question.id
                                        android.util.Log.d("QuestionMarket", "点击题目: ${question.id}")
                                        onQuestionClick(question.id)
                                    },
                                    enabled = isVerified
                                )
                            }
                        }
                    }
                }
            }
        }
        
            // 科目选择对话框
            if (showSubjectDialog) {
                SubjectSelectionDialog(
                    selectedSubjects = selectedSubjects,
                    onSubjectToggle = { subject ->
                        selectedSubjects = if (selectedSubjects.contains(subject)) {
                            selectedSubjects - subject
                        } else {
                            selectedSubjects + subject
                        }
                    },
                    onDismiss = { showSubjectDialog = false },
                    onConfirm = { 
                        saveFilterSettings() // 保存科目选择
                        showSubjectDialog = false 
                    }
                )
            }
        
        // 审核通知对话框
        if (showVerificationDialog && verificationMessage != null) {
            val isApproved = verificationStatusUpdate == "approved"
            AlertDialog(
                onDismissRequest = {
                    showVerificationDialog = false
                    verificationMessage = null
                    verificationStatusUpdate = null
                },
                title = {
                    Text(
                        text = if (isApproved) "审核通过" else "审核结果",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(verificationMessage!!)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showVerificationDialog = false
                            verificationMessage = null
                            verificationStatusUpdate = null
                            // 如果审核通过，刷新题目列表
                            if (isApproved) {
                                refreshQuestions()
                            }
                        }
                    ) {
                        Text("确定")
                    }
                },
                containerColor = if (isApproved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        }
    }
}

