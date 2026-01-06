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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.navigation.NavController
import com.example.andrio_teacher.network.sendSmsCode
import com.example.andrio_teacher.network.changePasswordByCode
import com.example.andrio_teacher.utils.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecurityScreen(navController: NavController) {
    val context = LocalContext.current
    val phone = remember { UserSession.getPhone(context) ?: "" }
    
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showIdentityDialog by remember { mutableStateOf(false) }
    var showSecurityCenterDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账号与安全", fontWeight = FontWeight.Bold) },
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
            // 绑定手机号
            item {
                SecurityItem(
                    title = "绑定手机号",
                    value = phone,
                    onClick = { showPhoneDialog = true }
                )
            }
            
            // 登录密码
            item {
                SecurityItem(
                    title = "登录密码",
                    value = "已设置",
                    onClick = { showPasswordDialog = true }
                )
            }
            
            // 身份信息
            item {
                SecurityItem(
                    title = "身份信息",
                    value = "查看证件信息",
                    onClick = { showIdentityDialog = true }
                )
            }
            
            // 安全中心
            item {
                SecurityItem(
                    title = "安全中心",
                    value = "账号挂失/注销",
                    onClick = { showSecurityCenterDialog = true }
                )
            }
        }
    }
    
    // 修改手机号对话框
    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = { Text("修改手机号") },
            text = {
                Text("修改手机号需要先验证当前手机号，此功能待实现")
            },
            confirmButton = {
                TextButton(onClick = { showPhoneDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 修改密码对话框
    if (showPasswordDialog) {
        ChangePasswordDialog(
            phone = phone,
            token = remember { UserSession.getToken(context) ?: "" },
            onDismiss = { showPasswordDialog = false },
            onSuccess = { showPasswordDialog = false }
        )
    }
    
    // 身份信息对话框
    if (showIdentityDialog) {
        AlertDialog(
            onDismissRequest = { showIdentityDialog = false },
            title = { Text("身份信息") },
            text = {
                Column {
                    Text("证件类型：教师资格证")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("证件图片：已上传")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("审核状态：待审核/已通过/已拒绝")
                }
            },
            confirmButton = {
                TextButton(onClick = { showIdentityDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 安全中心对话框
    if (showSecurityCenterDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityCenterDialog = false },
            title = { Text("安全中心") },
            text = {
                Column {
                    Text("账号挂失：临时限制登录，联系管理员处理")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("账号注销：永久删除账号，不可恢复")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityCenterDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun SecurityItem(
    title: String,
    value: String,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
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
}

@Composable
fun ChangePasswordDialog(
    phone: String,
    token: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var codeCountdown by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // 发送验证码
    fun sendCode() {
        if (codeCountdown > 0) return
        isLoading = true
        errorMsg = null
        sendSmsCode(phone) { success, error ->
            isLoading = false
            if (success) {
                codeCountdown = 60
                // 倒计时
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    while (codeCountdown > 0) {
                        delay(1000)
                        codeCountdown--
                    }
                }
            } else {
                errorMsg = error ?: "发送验证码失败"
            }
        }
    }
    
    // 修改密码
    fun changePassword() {
        if (newPassword.length < 6) {
            errorMsg = "密码至少需要6位"
            return
        }
        if (newPassword != confirmPassword) {
            errorMsg = "两次输入的密码不一致"
            return
        }
        if (code.isBlank()) {
            errorMsg = "请输入验证码"
            return
        }
        isLoading = true
        errorMsg = null
        changePasswordByCode(token, phone, code, newPassword) { success, error ->
            isLoading = false
            if (success) {
                onSuccess()
            } else {
                errorMsg = error ?: "修改密码失败"
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 验证码输入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; errorMsg = null },
                        label = { Text("验证码") },
                        placeholder = { Text("请输入验证码") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isLoading && codeCountdown == 0
                    )
                    Button(
                        onClick = { sendCode() },
                        enabled = codeCountdown == 0 && !isLoading
                    ) {
                        Text(if (codeCountdown > 0) "${codeCountdown}秒" else "发送验证码")
                    }
                }
                
                // 新密码
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMsg = null },
                    label = { Text("新密码") },
                    placeholder = { Text("请输入新密码（至少6位）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    enabled = !isLoading
                )
                
                // 确认密码
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMsg = null },
                    label = { Text("确认密码") },
                    placeholder = { Text("请再次输入新密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    enabled = !isLoading
                )
                
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { changePassword() },
                enabled = !isLoading && code.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        }
    )
}

