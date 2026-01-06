package com.example.andrio_teacher.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.andrio_teacher.network.TeacherEarning
import com.example.andrio_teacher.network.getTeacherEarnings
import com.example.andrio_teacher.network.uploadAvatar
import com.example.andrio_teacher.network.updateUserAvatar
import com.example.andrio_teacher.network.getUserProfile
import com.example.andrio_teacher.ui.navigation.AppRoutes
import com.example.andrio_teacher.utils.UserSession
import com.example.andrio_teacher.utils.CacheManager
import com.example.andrio_teacher.utils.ErrorHandler
import com.example.andrio_teacher.ui.components.ErrorDisplay
import com.example.andrio_teacher.ui.components.EmptyStateDisplay
import com.example.andrio_teacher.ui.components.LoadingDisplay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import kotlin.io.copyTo
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val token = remember { UserSession.getToken(context) ?: "" }
    val nickname = remember { UserSession.getNickname(context) ?: "教师" }
    val phone = remember { UserSession.getPhone(context) ?: "" }
    
    var earnings by remember { mutableStateOf<List<TeacherEarning>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // 计算统计数据
    val totalEarnings = earnings.sumOf { it.price }
    val completedCount = earnings.size
    val ratedEarnings = earnings.filter { it.rating != null }
    val ratingCount = ratedEarnings.size
    val averageRating = if (ratedEarnings.isNotEmpty()) {
        ratedEarnings.map { it.rating!! }.average()
    } else 0.0
    
    // 头像相关状态
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 从URI上传头像的辅助函数
    fun uploadAvatarFromUri(
        context: Context,
        uri: Uri,
        token: String,
        callback: (Boolean, String?, String?) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
            inputStream?.use { stream ->
                file.outputStream().use { output ->
                    stream.copyTo(output)
                }
            }
            uploadAvatar(token, file, callback)
        } catch (e: Exception) {
            android.util.Log.e("ProfileScreen", "处理头像文件失败", e)
            callback(false, null, "处理文件失败: ${e.message}")
        }
    }
    
    // 图片选择器（相册）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUri = it
            // 上传头像到服务器
            uploadAvatarFromUri(context, it, token) { success: Boolean, imageUrl: String?, error: String? ->
                if (success && imageUrl != null) {
                    // 更新用户头像
                    updateUserAvatar(token, imageUrl) { updateSuccess: Boolean, updateError: String? ->
                        if (updateSuccess) {
                            // 头像更新成功，更新本地状态并重新加载用户信息
                            try {
                                avatarUri = android.net.Uri.parse(imageUrl)
                                android.util.Log.d("ProfileScreen", "头像更新成功: $imageUrl")
                                // 重新加载用户信息以确保数据同步
                                getUserProfile(token) { reloadSuccess, reloadAvatarUrl, reloadError ->
                                    if (reloadSuccess && reloadAvatarUrl != null && reloadAvatarUrl.isNotBlank()) {
                                        try {
                                            avatarUri = android.net.Uri.parse(reloadAvatarUrl)
                                            android.util.Log.d("ProfileScreen", "重新加载头像成功: $reloadAvatarUrl")
                                        } catch (e: Exception) {
                                            android.util.Log.e("ProfileScreen", "解析头像URL失败", e)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileScreen", "解析头像URL失败", e)
                            }
                        } else {
                            android.util.Log.e("ProfileScreen", "头像更新失败: $updateError")
                        }
                    }
                } else {
                    android.util.Log.e("ProfileScreen", "头像上传失败: $error")
                }
            }
        }
    }
    
    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            avatarUri = cameraImageUri
            // 上传头像到服务器
            uploadAvatarFromUri(context, cameraImageUri!!, token) { uploadSuccess: Boolean, imageUrl: String?, error: String? ->
                if (uploadSuccess && imageUrl != null) {
                    // 更新用户头像
                    updateUserAvatar(token, imageUrl) { updateSuccess: Boolean, updateError: String? ->
                        if (updateSuccess) {
                            android.util.Log.d("ProfileScreen", "头像更新成功: $imageUrl")
                            // 重新加载用户信息以确保数据同步
                            getUserProfile(token) { success, avatarUrl, error ->
                                if (success && avatarUrl != null && avatarUrl.isNotBlank()) {
                                    try {
                                        avatarUri = android.net.Uri.parse(avatarUrl)
                                        android.util.Log.d("ProfileScreen", "重新加载头像成功: $avatarUrl")
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProfileScreen", "解析头像URL失败", e)
                                    }
                                }
                            }
                        } else {
                            android.util.Log.e("ProfileScreen", "头像更新失败: $updateError")
                        }
                    }
                } else {
                    android.util.Log.e("ProfileScreen", "头像上传失败: $error")
                }
            }
        }
    }
    
    // 今日统计
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayEarnings = earnings.filter { it.completedAt.startsWith(today) }
    val todayTotal = todayEarnings.sumOf { it.price }
    val todayCount = todayEarnings.size
    
    // 加载用户头像 - 在页面进入时和从其他页面返回时重新加载
    LaunchedEffect(navController.currentBackStackEntry) {
        if (token.isNotBlank()) {
            getUserProfile(token) { success, avatarUrl, error ->
                if (success && avatarUrl != null && avatarUrl.isNotBlank()) {
                    try {
                        avatarUri = android.net.Uri.parse(avatarUrl)
                        android.util.Log.d("ProfileScreen", "加载头像成功: $avatarUrl")
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileScreen", "解析头像URL失败", e)
                    }
                } else {
                    android.util.Log.e("ProfileScreen", "加载头像失败: $error")
                }
            }
        }
    }
    
    // 加载收入列表
    LaunchedEffect(Unit) {
        if (token.isBlank()) {
            isLoading = false
            errorMsg = "未登录或登录已失效"
            ErrorHandler.checkTokenExpired("未登录", 401, context, navController)
            return@LaunchedEffect
        }
        isLoading = true
        getTeacherEarnings(token) { success, earningList, error ->
            isLoading = false
            if (success && earningList != null) {
                earnings = earningList.sortedByDescending { it.completedAt }
                errorMsg = null
            } else {
                val errorType = ErrorHandler.analyzeError(error)
                if (ErrorHandler.checkTokenExpired(error, null, context, navController)) {
                    return@getTeacherEarnings
                }
                errorMsg = ErrorHandler.handleApiError(error)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMsg != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            isLoading = true
                            getTeacherEarnings(token) { success, earningList, error ->
                                isLoading = false
                                if (success && earningList != null) {
                                    earnings = earningList.sortedByDescending { it.completedAt }
                                    errorMsg = null
                                } else {
                                    errorMsg = error ?: "获取失败"
                                }
                            }
                        }) {
                            Text("重试")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // 顶部个人信息卡片（美团风格：蓝色渐变背景）
                        item {
                            ProfileHeaderCard(
                                nickname = nickname,
                                phone = phone,
                                totalEarnings = totalEarnings,
                                completedCount = completedCount,
                                averageRating = averageRating,
                                ratingCount = ratingCount,
                                avatarUri = avatarUri,
                                onAvatarClick = { showAvatarDialog = true }
                            )
                        }
                        
                        // 今日收入卡片（美团风格：白色卡片，突出显示）
                        item {
                            TodayEarningsCard(
                                todayTotal = todayTotal,
                                todayCount = todayCount,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        
                        // 功能列表（条幅式）
                        item {
                            ProfileMenuSection(
                                navController = navController,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // 收入明细标题
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "收入明细",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "共${completedCount}单",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        // 收入列表
                        if (earnings.isEmpty()) {
                            item {
                                EmptyEarningsCard(
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            items(earnings) { earning ->
                                EarningItemCard(
                                    earning = earning,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 头像选择对话框
        if (showAvatarDialog) {
            AlertDialog(
                onDismissRequest = { showAvatarDialog = false },
                title = { Text("设置个人头像") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showAvatarDialog = false
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("从相册选择")
                        }
                        TextButton(
                            onClick = {
                                showAvatarDialog = false
                                // 创建临时文件用于存储拍照结果
                                val photoFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                                cameraImageUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraLauncher.launch(cameraImageUri)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("拍一张照片")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAvatarDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// 顶部个人信息卡片（美团风格：蓝色渐变背景）
@Composable
fun ProfileHeaderCard(
    nickname: String,
    phone: String,
    totalEarnings: Int,
    completedCount: Int,
    averageRating: Double,
    ratingCount: Int,
    avatarUri: Uri?,
    onAvatarClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1890FF), // 美团蓝
                        Color(0xFF096DD9)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 头像（可点击上传）
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = nickname.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 昵称和手机号
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            // 统计数据（美团风格：横向排列）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "总收入",
                    value = "¥${totalEarnings}",
                    icon = Icons.Filled.Settings,
                    color = Color.White
                )
                StatItem(
                    label = "完成单数",
                    value = "${completedCount}单",
                    icon = Icons.Filled.CheckCircle,
                    color = Color.White
                )
                StatItem(
                    label = "平均评分",
                    value = if (averageRating > 0) {
                        if (ratingCount > 0) {
                            String.format("%.1f", averageRating) + " ($ratingCount)"
                        } else {
                            String.format("%.1f", averageRating)
                        }
                    } else "-",
                    icon = Icons.Filled.Star,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 使用简单的圆形背景代替图标，避免图标不存在的问题
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.9f)
        )
    }
}

// 今日收入卡片（美团风格：白色卡片，大号数字）
@Composable
fun TodayEarningsCard(
    todayTotal: Int,
    todayCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "今日收入",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¥${todayTotal}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35) // 美团橙色
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "今日完成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${todayCount}单",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1890FF) // 美团蓝
                )
            }
        }
    }
}

// 功能列表（条幅式）
@Composable
fun ProfileMenuSection(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 已完成题目
        ProfileMenuItem(
            icon = Icons.Filled.CheckCircle,
            iconColor = Color(0xFF4CAF50),
            title = "已完成题目",
            subtitle = "查看视频通话录屏",
            onClick = { navController.navigate(AppRoutes.COMPLETED_QUESTIONS) }
        )
        
        // 通用
        ProfileMenuItem(
            icon = Icons.Filled.Settings,
            iconColor = Color(0xFF1890FF),
            title = "通用",
            subtitle = "清理缓存",
            onClick = { navController.navigate(AppRoutes.GENERAL_SETTINGS) }
        )
        
        // 设置
        ProfileMenuItem(
            icon = Icons.Filled.Settings,
            iconColor = Color(0xFFFF6B35),
            title = "设置",
            onClick = { navController.navigate(AppRoutes.SETTINGS) }
        )
        
        // 关于
        ProfileMenuItem(
            icon = Icons.Filled.Info,
            iconColor = Color(0xFF03A9F4),
            title = "关于",
            subtitle = "版本号 1.0.0",
            onClick = { navController.navigate(AppRoutes.ABOUT) }
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "进入",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// 收入明细卡片（美团风格：简洁列表项）
@Composable
fun EarningItemCard(
    earning: TeacherEarning,
    modifier: Modifier = Modifier
) {
    val subjectText = when (earning.subject) {
        "physics" -> "物理"
        "chemistry" -> "化学"
        "math" -> "数学"
        "biology" -> "生物"
        "english" -> "英语"
        "chinese" -> "语文"
        "history" -> "历史"
        "geography" -> "地理"
        "other" -> "其他"
        else -> earning.subject
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = subjectText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (earning.rating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "评分",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${earning.rating}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Text(
                    text = formatDate(earning.completedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "+¥${earning.price}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B35) // 美团橙色
            )
        }
    }
}

@Composable
fun EmptyEarningsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = "空",
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "暂无收入记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Text(
                    text = "完成题目后收入会显示在这里",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        try {
            val inputFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val date = inputFormat2.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e2: Exception) {
            dateString
        }
    }
}
