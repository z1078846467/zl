package com.example.andrio_teacher.network

object NetworkConfig {
    // 服务器配置（与学生端保持一致）
    const val SERVER_IP = "10.0.2.2"  // Android模拟器使用
    // const val SERVER_IP = "192.168.x.x"  // 真机使用，替换为实际IP
    
    // 端口配置
    const val GATEWAY_PORT = "3002"
    const val QUESTION_PORT = "3011"
    const val WEBSOCKET_PORT = "3006"
    
    // URL配置
    val GATEWAY_URL: String get() = "http://$SERVER_IP:$GATEWAY_PORT"
    val QUESTION_URL: String get() = "http://$SERVER_IP:$GATEWAY_PORT/api/question"
    val WEBSOCKET_URL: String get() = "http://$SERVER_IP:$WEBSOCKET_PORT"
    
    // 超时配置
    const val CONNECT_TIMEOUT = 10000L  // 10秒
    const val REQUEST_TIMEOUT = 30000L  // 30秒
}

