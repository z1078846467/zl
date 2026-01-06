package com.example.andrio_teacher.ui.screens.register

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.andrio_teacher.network.registerTeacher
import com.example.andrio_teacher.network.uploadImage
import com.example.andrio_teacher.utils.UserSession
import com.example.andrio_teacher.utils.ErrorHandler
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    phone: String,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var certificateType by remember { mutableStateOf("teacher_certificate") } // teacher_certificate or school_proof
    var certificateImageUri by remember { mutableStateOf<Uri?>(null) }
    var certificateImageFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableStateOf(0) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            certificateImageUri = it
            // 将 URI 转换为 File（简化处理，实际可能需要更复杂的逻辑）
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "certificate_${System.currentTimeMillis()}.jpg")
                inputStream?.use { stream ->
                    file.outputStream().use { output ->
                        stream.copyTo(output)
                    }
                }
                certificateImageFile = file
            } catch (e: Exception) {
                errorMsg = "图片处理失败: ${e.message}"
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("完善资料", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "欢迎加入教师端",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "请完善以下信息并上传相关证件，审核通过后即可开始接题",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Divider()
            
            // 手机号（只读）
            OutlinedTextField(
                value = phone,
                onValueChange = {},
                label = { Text("手机号") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                singleLine = true
            )
            
            // 昵称
            OutlinedTextField(
                value = nickname,
                onValueChange = { 
                    nickname = it
                    errorMsg = null
                },
                label = { Text("昵称 *") },
                placeholder = { Text("请输入您的昵称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMsg != null && nickname.isBlank()
            )
            
            // 密码
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMsg = null
                },
                label = { Text("密码 *") },
                placeholder = { Text("请输入密码（至少6位）") },
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
                isError = errorMsg != null && (password.isBlank() || password.length < 6)
            )
            
            // 确认密码
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    errorMsg = null
                },
                label = { Text("确认密码 *") },
                placeholder = { Text("请再次输入密码") },
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
                isError = errorMsg != null && (confirmPassword.isBlank() || confirmPassword != password)
            )
            
            // 学校（可选）
            OutlinedTextField(
                value = school,
                onValueChange = { school = it },
                label = { Text("学校（可选）") },
                placeholder = { Text("请输入所在学校") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 证件类型选择
            Text(
                text = "证件类型 *",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = certificateType == "teacher_certificate",
                    onClick = { certificateType = "teacher_certificate" },
                    label = { Text("教师资格证") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = certificateType == "school_proof",
                    onClick = { certificateType = "school_proof" },
                    label = { Text("在校证明") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 证件上传
            Text(
                text = "上传证件 *",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        imagePickerLauncher.launch("image/*")
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (certificateImageUri != null) {
                        AsyncImage(
                            model = certificateImageUri,
                            contentDescription = "证件图片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "上传图片",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = "点击上传证件照片",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
            
            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 提交按钮
            Button(
                onClick = {
                    if (nickname.isBlank()) {
                        errorMsg = "请输入昵称"
                        return@Button
                    }
                    if (password.isBlank() || password.length < 6) {
                        errorMsg = "密码至少需要6位"
                        return@Button
                    }
                    if (confirmPassword != password) {
                        errorMsg = "两次输入的密码不一致"
                        return@Button
                    }
                    if (certificateImageFile == null) {
                        errorMsg = "请上传证件照片"
                        return@Button
                    }
                    
                    isLoading = true
                    errorMsg = null
                    uploadProgress = 0
                    
                    // 先上传图片获取URL（不需要token，因为/api/upload/image支持无token上传）
                    uploadImage(
                        token = "", // 空token，因为后端支持无token上传
                        imageFile = certificateImageFile!!
                    ) { uploadSuccess, imageUrl, uploadError ->
                        if (!uploadSuccess || imageUrl == null) {
                            isLoading = false
                            errorMsg = ErrorHandler.handleApiError(uploadError)
                            return@uploadImage
                        }
                        
                        // 图片上传成功，现在提交注册信息
                        registerTeacher(
                            phone = phone,
                            nickname = nickname,
                            password = password,
                            school = school.takeIf { it.isNotBlank() },
                            certificateType = certificateType,
                            certificateImageUrl = imageUrl
                        ) { success, token, userInfo, registerError ->
                            isLoading = false
                            if (success && token != null) {
                                UserSession.saveToken(context, token)
                                userInfo?.let {
                                    UserSession.saveUserId(context, it.id)
                                    UserSession.saveUserInfo(context, it.phone, it.nickname, it.role, it.verificationStatus)
                                }
                                onRegisterSuccess()
                            } else {
                                errorMsg = ErrorHandler.handleApiError(registerError)
                            }
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
                    Text("提交审核", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Text(
                text = "提交后，我们将在1-3个工作日内完成审核，审核通过后即可开始接题",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

