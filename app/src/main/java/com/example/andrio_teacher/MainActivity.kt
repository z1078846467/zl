package com.example.andrio_teacher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.andrio_teacher.ui.navigation.AppRoutes
import com.example.andrio_teacher.ui.navigation.NavGraph
import com.example.andrio_teacher.ui.theme.TeacherAppTheme
import com.example.andrio_teacher.utils.UserSession

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeacherAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeacherApp()
                }
            }
        }
    }
}

@Composable
fun TeacherApp() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isCheckingAuth by remember { mutableStateOf(true) }
    var shouldNavigateToMarket by remember { mutableStateOf(false) }
    
    // 始终创建NavGraph，确保导航图已设置
    NavGraph(navController = navController)
    
    // 启动时验证登录状态
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val token = UserSession.getToken(context)
        val userId = UserSession.getUserId(context)
        val phone = UserSession.getPhone(context)
        val nickname = UserSession.getNickname(context)
        
        // 如果没有token，直接跳转到登录页
        if (token == null) {
            isCheckingAuth = false
            return@LaunchedEffect
        }
        
        // 如果token存在但用户信息缺失，清除session并跳转到登录页
        if (userId == null || phone == null || nickname == null) {
            android.util.Log.w("MainActivity", "Token exists but user info is missing, clearing session")
            UserSession.clear(context)
            isCheckingAuth = false
            return@LaunchedEffect
        }
        
        // 验证token是否有效（通过调用getUserProfile API）
        com.example.andrio_teacher.network.getUserProfile(token) { success, avatarUrl, error ->
            if (success) {
                // Token有效，标记需要导航到题目市场
                shouldNavigateToMarket = true
            } else {
                // Token无效或过期，清除session
                android.util.Log.w("MainActivity", "Token validation failed: $error, clearing session")
                UserSession.clear(context)
            }
            isCheckingAuth = false
        }
    }
    
    // 在NavGraph创建后执行导航
    androidx.compose.runtime.LaunchedEffect(shouldNavigateToMarket) {
        if (shouldNavigateToMarket) {
            // 等待一小段时间确保NavGraph完全初始化
            kotlinx.coroutines.delay(100)
            navController.navigate(AppRoutes.QUESTION_MARKET) {
                popUpTo(0) { inclusive = true }
            }
            shouldNavigateToMarket = false
        }
    }
    
    // 显示加载状态（覆盖在NavGraph上方）
    if (isCheckingAuth) {
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }
}