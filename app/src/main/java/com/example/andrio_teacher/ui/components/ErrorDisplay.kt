package com.example.andrio_teacher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.andrio_teacher.utils.ErrorHandler

/**
 * 统一的错误显示组件
 */
@Composable
fun ErrorDisplay(
    error: String,
    errorType: ErrorHandler.ErrorType = ErrorHandler.ErrorType.UNKNOWN,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 错误图标
        Icon(
            imageVector = when (errorType) {
                ErrorHandler.ErrorType.NETWORK_ERROR -> Icons.Default.WifiOff
                ErrorHandler.ErrorType.TOKEN_EXPIRED -> Icons.Default.Error
                ErrorHandler.ErrorType.PERMISSION_DENIED -> Icons.Default.Error
                ErrorHandler.ErrorType.NOT_FOUND -> Icons.Default.Error
                ErrorHandler.ErrorType.SERVER_ERROR -> Icons.Default.Error
                ErrorHandler.ErrorType.RATE_LIMIT -> Icons.Default.Error
                ErrorHandler.ErrorType.UNKNOWN -> Icons.Default.Error
            },
            contentDescription = "错误",
            modifier = Modifier.size(64.dp),
            tint = when (errorType) {
                ErrorHandler.ErrorType.NETWORK_ERROR -> Color(0xFF2196F3)
                ErrorHandler.ErrorType.TOKEN_EXPIRED -> Color(0xFFFF9800)
                ErrorHandler.ErrorType.PERMISSION_DENIED -> MaterialTheme.colorScheme.error
                ErrorHandler.ErrorType.NOT_FOUND -> Color(0xFF9E9E9E)
                ErrorHandler.ErrorType.SERVER_ERROR -> MaterialTheme.colorScheme.error
                ErrorHandler.ErrorType.RATE_LIMIT -> Color(0xFFFF9800)
                ErrorHandler.ErrorType.UNKNOWN -> MaterialTheme.colorScheme.error
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 错误消息
        Text(
            text = error,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // 重试按钮
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重试",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("重试")
            }
        }
    }
}

/**
 * 空状态显示组件
 */
@Composable
fun EmptyStateDisplay(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        if (message != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * 加载状态显示组件
 */
@Composable
fun LoadingDisplay(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
        
        if (message != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

