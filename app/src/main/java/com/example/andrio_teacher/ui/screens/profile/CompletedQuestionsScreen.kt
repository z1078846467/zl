package com.example.andrio_teacher.ui.screens.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedQuestionsScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedDays by remember { mutableStateOf(getSavedVideoDays(context)) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已完成题目", fontWeight = FontWeight.Bold) },
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
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 视频保存设置
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "视频保存天数",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "后端保存7天，个人保存由您决定",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        // 选项列表
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoDaysOption(
                                days = 3,
                                label = "3天",
                                selected = selectedDays == 3,
                                onClick = {
                                    selectedDays = 3
                                    saveVideoDays(context, 3)
                                }
                            )
                            VideoDaysOption(
                                days = 5,
                                label = "5天",
                                selected = selectedDays == 5,
                                onClick = {
                                    selectedDays = 5
                                    saveVideoDays(context, 5)
                                }
                            )
                            VideoDaysOption(
                                days = 7,
                                label = "7天",
                                selected = selectedDays == 7,
                                onClick = {
                                    selectedDays = 7
                                    saveVideoDays(context, 7)
                                }
                            )
                            VideoDaysOption(
                                days = -1,
                                label = "永久",
                                selected = selectedDays == -1,
                                onClick = {
                                    selectedDays = -1
                                    saveVideoDays(context, -1)
                                }
                            )
                        }
                    }
                }
            }
            
            // 视频列表（待实现）
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "视频列表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "功能待实现...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoDaysOption(
    days: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

// 保存视频保存天数到 SharedPreferences
private fun saveVideoDays(context: Context, days: Int) {
    val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    prefs.edit().putInt("video_save_days", days).apply()
}

// 从 SharedPreferences 读取视频保存天数
private fun getSavedVideoDays(context: Context): Int {
    val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    return prefs.getInt("video_save_days", 7) // 默认7天
}

