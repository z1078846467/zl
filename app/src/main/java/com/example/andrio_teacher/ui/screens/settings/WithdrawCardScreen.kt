package com.example.andrio_teacher.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.andrio_teacher.network.bindBankCard
import com.example.andrio_teacher.network.getBankCard
import com.example.andrio_teacher.network.unbindBankCard
import com.example.andrio_teacher.utils.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawCardScreen(navController: NavController) {
    val context = LocalContext.current
    val token = remember { UserSession.getToken(context) ?: "" }
    
    var cardNumber by remember { mutableStateOf<String?>(null) }
    var cardHolder by remember { mutableStateOf<String?>(null) }
    var bankName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showBindDialog by remember { mutableStateOf(false) }
    var showUnbindDialog by remember { mutableStateOf(false) }
    
    // 加载银行卡信息
    LaunchedEffect(Unit) {
        if (token.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        getBankCard(token) { success, number, holder, bank, error ->
            isLoading = false
            if (success) {
                cardNumber = number
                cardHolder = holder
                bankName = bank
            } else {
                // 未绑定或获取失败
                cardNumber = null
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("提现绑卡解绑", fontWeight = FontWeight.Bold) },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        if (cardNumber == null) {
                            // 未绑定银行卡
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "银行卡",
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFF1890FF)
                                    )
                                    Text(
                                        text = "暂未绑定银行卡",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "绑定银行卡后可进行提现操作",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                    Button(
                                        onClick = { showBindDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1890FF)
                                        )
                                    ) {
                                        Text("绑定银行卡")
                                    }
                                }
                            }
                        } else {
                            // 已绑定银行卡
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "已绑定银行卡",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { showUnbindDialog = true }) {
                                            Text("解绑", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Divider()
                                    Text(
                                        text = "银行：${bankName ?: "未知"}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "持卡人：${cardHolder ?: "未知"}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "卡号：${cardNumber?.takeLast(4)?.let { "**** **** **** $it" } ?: "未知"}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
            
                    item {
                        Text(
                            text = "提现说明",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
            
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "• 提现需要绑定银行卡",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "• 提现将在1-3个工作日内到账",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "• 单次提现金额不少于10元",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            
            // 绑定银行卡对话框
            if (showBindDialog) {
                BindBankCardDialog(
                    token = token,
                    onDismiss = { showBindDialog = false },
                    onSuccess = {
                        showBindDialog = false
                        // 重新加载银行卡信息
                        isLoading = true
                        getBankCard(token) { success, number, holder, bank, error ->
                            isLoading = false
                            if (success) {
                                cardNumber = number
                                cardHolder = holder
                                bankName = bank
                            }
                        }
                    }
                )
            }
            
            // 解绑银行卡对话框
            if (showUnbindDialog) {
                AlertDialog(
                    onDismissRequest = { showUnbindDialog = false },
                    title = { Text("解绑银行卡", fontWeight = FontWeight.Bold) },
                    text = { Text("确定要解绑银行卡吗？解绑后将无法进行提现操作。") },
                    confirmButton = {
                        Button(
                            onClick = {
                                unbindBankCard(token) { success, error ->
                                    if (success) {
                                        cardNumber = null
                                        cardHolder = null
                                        bankName = null
                                        showUnbindDialog = false
                                    } else {
                                        // 显示错误
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("确定解绑")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnbindDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BindBankCardDialog(
    token: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    fun bindCard() {
        if (cardNumber.isBlank() || cardNumber.length < 16) {
            errorMsg = "请输入正确的银行卡号（至少16位）"
            return
        }
        if (cardHolder.isBlank()) {
            errorMsg = "请输入持卡人姓名"
            return
        }
        if (bankName.isBlank()) {
            errorMsg = "请输入银行名称"
            return
        }
        isLoading = true
        errorMsg = null
        bindBankCard(token, cardNumber, cardHolder, bankName) { success, error ->
            isLoading = false
            if (success) {
                onSuccess()
            } else {
                errorMsg = error ?: "绑定失败"
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("绑定银行卡", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it; errorMsg = null },
                    label = { Text("银行卡号 *") },
                    placeholder = { Text("请输入银行卡号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it; errorMsg = null },
                    label = { Text("持卡人姓名 *") },
                    placeholder = { Text("请输入持卡人姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it; errorMsg = null },
                    label = { Text("银行名称 *") },
                    placeholder = { Text("例如：中国工商银行") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
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
                onClick = { bindCard() },
                enabled = !isLoading && cardNumber.isNotBlank() && cardHolder.isNotBlank() && bankName.isNotBlank()
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
