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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.andrio_teacher.utils.CacheManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var notificationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }
    var clearedSize by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通用", fontWeight = FontWeight.Bold) },
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
            item {
                Text(
                    text = "通知设置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.Gray
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "新消息通知",
                    checked = notificationEnabled,
                    onCheckedChange = { notificationEnabled = it }
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "声音",
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it }
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "震动",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Text(
                    text = "缓存管理",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.Gray
                )
            }
            
            item {
                SettingsClickableItem(
                    title = "清理题目缓存",
                    onClick = {
                        showClearCacheDialog = true
                        clearedSize = null
                        isClearing = true
                        scope.launch(Dispatchers.IO) {
                            val size = CacheManager.clearQuestionCache(context)
                            withContext(Dispatchers.Main) {
                                isClearing = false
                                clearedSize = size
                            }
                        }
                    }
                )
            }
            
            item {
                SettingsClickableItem(
                    title = "清理视频缓存",
                    onClick = {
                        showClearCacheDialog = true
                        clearedSize = null
                        isClearing = true
                        scope.launch(Dispatchers.IO) {
                            val size = CacheManager.clearVideoCache(context)
                            withContext(Dispatchers.Main) {
                                isClearing = false
                                clearedSize = size
                            }
                        }
                    }
                )
            }
            
            item {
                SettingsClickableItem(
                    title = "清理全部缓存",
                    onClick = {
                        showClearCacheDialog = true
                        clearedSize = null
                        isClearing = true
                        scope.launch(Dispatchers.IO) {
                            val size = CacheManager.clearAllCache(context)
                            withContext(Dispatchers.Main) {
                                isClearing = false
                                clearedSize = size
                            }
                        }
                    }
                )
            }
        }
        
        // 清理缓存结果对话框
        if (showClearCacheDialog || clearedSize != null) {
            AlertDialog(
                onDismissRequest = {
                    showClearCacheDialog = false
                    clearedSize = null
                },
                title = {
                    Text(
                        text = if (clearedSize != null) "清理完成" else "清理中...",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    if (clearedSize != null) {
                        Text(
                            text = "已清理 ${CacheManager.formatSize(clearedSize!!)}",
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("清理中...")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearCacheDialog = false
                            clearedSize = null
                        }
                    ) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "进入",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

