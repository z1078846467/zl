package com.example.andrio_teacher.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.andrio_teacher.ui.navigation.AppRoutes
import com.example.andrio_teacher.utils.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 账号与安全
            item {
                SettingsItem(
                    icon = Icons.Filled.Person,
                    iconColor = Color(0xFFFF6B35),
                    title = "账号与安全",
                    onClick = { navController.navigate(AppRoutes.ACCOUNT_SECURITY) }
                )
            }
            
            // 提现绑卡解绑
            item {
                SettingsItem(
                    icon = Icons.Filled.Settings,
                    iconColor = Color(0xFF1890FF),
                    title = "提现绑卡解绑",
                    onClick = { navController.navigate(AppRoutes.WITHDRAW_CARD) }
                )
            }
            
            // 通用
            item {
                SettingsItem(
                    icon = Icons.Filled.Settings,
                    iconColor = Color(0xFFFF6B35),
                    title = "通用",
                    onClick = { navController.navigate(AppRoutes.GENERAL_SETTINGS) }
                )
            }
            
            // 反馈
            item {
                SettingsItem(
                    icon = Icons.Filled.Edit,
                    iconColor = Color(0xFFFF6B35),
                    title = "反馈与投诉",
                    onClick = { navController.navigate(AppRoutes.FEEDBACK) }
                )
            }
            
            // 关于
            item {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    iconColor = Color(0xFF1890FF),
                    title = "关于",
                    subtitle = "版本号 1.0.0",
                    onClick = { navController.navigate(AppRoutes.ABOUT) }
                )
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // 登录其他账号
            item {
                Button(
                    onClick = {
                        UserSession.clear(context)
                        navController.navigate(AppRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1890FF)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("登录其他账号", fontWeight = FontWeight.Bold)
                }
            }
            
            // 退出登录
            item {
                Button(
                    onClick = {
                        UserSession.clear(context)
                        navController.navigate(AppRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFFF3B30)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("退出登录", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
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
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
            
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "进入",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

