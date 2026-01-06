package com.example.andrio_teacher.ui.screens.login

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.andrio_teacher.network.loginByCode
import com.example.andrio_teacher.network.sendSmsCode
import com.example.andrio_teacher.ui.navigation.AppRoutes
import com.example.andrio_teacher.utils.UserSession
import com.example.andrio_teacher.utils.ErrorHandler
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    // 倒计时
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/标题区域
        Text(
            text = "教师端",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 手机号输入
        OutlinedTextField(
            value = phone,
            onValueChange = { 
                phone = it.filter { char -> char.isDigit() }
                errorMsg = null
            },
            label = { Text("手机号") },
            placeholder = { Text("请输入手机号") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMsg != null
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 验证码输入和发送按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { 
                    code = it.filter { char -> char.isDigit() }
                    errorMsg = null
                },
                label = { Text("验证码") },
                placeholder = { Text("请输入验证码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = errorMsg != null
            )
            
            Button(
                onClick = {
                    if (phone.length != 11) {
                        errorMsg = "请输入正确的手机号"
                        return@Button
                    }
                    isLoading = true
                    sendSmsCode(phone) { success, error ->
                        isLoading = false
                        if (success) {
                            countdown = 60
                            errorMsg = null
                        } else {
                            errorMsg = ErrorHandler.handleApiError(error)
                        }
                    }
                },
                enabled = countdown == 0 && !isLoading && phone.length == 11,
                modifier = Modifier.height(56.dp)
            ) {
                if (countdown > 0) {
                    Text("${countdown}秒")
                } else {
                    Text("发送")
                }
            }
        }
        
        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMsg!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 登录按钮
        Button(
            onClick = {
                if (phone.length != 11) {
                    errorMsg = "请输入正确的手机号"
                    return@Button
                }
                if (code.length != 6) {
                    errorMsg = "请输入6位验证码"
                    return@Button
                }
                isLoading = true
                errorMsg = null
                loginByCode(phone, code) { success, token, userInfo, isNew, error ->
                    isLoading = false
                    if (success) {
                        if (isNew == true) {
                            // 新用户，跳转到注册页面
                            navController.navigate("register/$phone") {
                                popUpTo(AppRoutes.LOGIN) { inclusive = false }
                            }
                        } else if (token != null) {
                            // 已注册用户，直接登录
                            UserSession.saveToken(context, token)
                            userInfo?.let {
                                UserSession.saveUserId(context, it.id)
                                UserSession.saveUserInfo(context, it.phone, it.nickname, it.role, it.verificationStatus)
                            }
                            onLoginSuccess()
                        } else {
                            errorMsg = "登录失败，请重试"
                        }
                    } else {
                        errorMsg = ErrorHandler.handleApiError(error)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("登录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

