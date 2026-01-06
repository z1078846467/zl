package com.example.andrio_teacher.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.andrio_teacher.utils.ErrorHandler
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// 数据模型
data class Question(
    val id: String,
    val studentId: String,
    val teacherId: String?,
    val imageUrl: String,
    val subject: String,
    val description: String?,
    val status: String, // pending, assigned, in_progress, completed, rated
    val roomId: String?,
    val videoRoomId: String?,
    val rating: Int?,
    val ratingComment: String?,
    val createdAt: String,
    val assignedAt: String?,
    val startedAt: String?,
    val completedAt: String?,
    val ratedAt: String?,
    // 扩展字段（需要后端支持）
    val academicStage: String? = null, // 学段：初中、高中等
    val price: Int? = null  // 价格（元）
)

data class UserInfo(
    val id: String,
    val phone: String,
    val nickname: String,
    val role: String? = null, // teacher or student
    val avatarResId: Int = 0,
    val avatarUri: String? = null,
    val userAcademicStage: String? = null,
    val userGender: String? = null,
    val verificationStatus: String? = null // pending, approved, rejected
)

data class TeacherEarning(
    val questionId: String,
    val studentId: String,
    val subject: String,
    val price: Int,
    val completedAt: String,
    val rating: Int?
)

private val client = OkHttpClient.Builder()
    .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
    .readTimeout(NetworkConfig.REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
    .writeTimeout(NetworkConfig.REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
    .build()

/**
 * 发送验证码
 */
fun sendSmsCode(phone: String, callback: (Boolean, String?) -> Unit) {
    val json = JSONObject().apply { put("phone", phone) }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/verification/send_sms_code")
        .post(body)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "sendSmsCode failed", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = com.example.andrio_teacher.utils.ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    try {
                        val jsonObj = JSONObject(responseBody ?: "{}")
                        val success = jsonObj.optBoolean("success", true)
                        Handler(Looper.getMainLooper()).post {
                            callback(success, null)
                        }
                    } catch (e: Exception) {
                        Log.e("NETWORK_ERROR", "sendSmsCode parse error", e)
                        Handler(Looper.getMainLooper()).post {
                            callback(true, null) // 解析失败但HTTP成功，认为成功
                        }
                    }
                } else {
                    Log.w("NETWORK_ERROR", "sendSmsCode failed: ${response.code}")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, "请求失败: ${response.code}")
                    }
                }
            }
        }
    })
}

/**
 * 验证码登录
 */
fun loginByCode(
    phone: String,
    code: String,
    callback: (Boolean, String?, UserInfo?, Boolean?, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("phone", phone)
        put("code", code)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/verification/login_by_code")
        .post(body)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "loginByCode failed", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, null, null, null, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonObj = JSONObject(responseBody)
                        val dataObj = jsonObj.optJSONObject("data")
                        val token = dataObj?.optString("token", "")?.takeIf { it.isNotEmpty() }
                        val isNew = dataObj?.optBoolean("isNew", false) ?: false
                        
                        var userInfo: UserInfo? = null
                        if (dataObj?.has("user") == true) {
                            val userObj = dataObj.getJSONObject("user")
                            userInfo = UserInfo(
                                id = userObj.optString("id", ""),
                                phone = userObj.optString("phone", phone),
                                nickname = userObj.optString("nickname", "用户"),
                                role = userObj.optString("role"),
                                avatarResId = userObj.optInt("avatarResId", 0),
                                avatarUri = userObj.optString("avatarUri").takeIf { it.isNotBlank() },
                                userAcademicStage = userObj.optString("userAcademicStage").takeIf { it.isNotBlank() },
                                userGender = userObj.optString("userGender").takeIf { it.isNotBlank() },
                                verificationStatus = userObj.optString("verificationStatus").takeIf { it.isNotBlank() }
                            )
                        }
                        
                        Handler(Looper.getMainLooper()).post {
                            callback(true, token, userInfo, isNew, null)
                        }
                    } catch (e: Exception) {
                        Log.e("NETWORK_ERROR", "JSON parsing failed for loginByCode", e)
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, null, null, "服务器响应格式错误")
                        }
                    }
                } else {
                    val errorMsg = try {
                        val jsonObj = JSONObject(responseBody ?: "{}")
                        ErrorHandler.handleApiError(jsonObj.optString("message", jsonObj.optString("msg", "登录失败")), response.code)
                    } catch (e: Exception) {
                        ErrorHandler.handleApiError("登录失败", response.code)
                    }
                    Handler(Looper.getMainLooper()).post {
                        callback(false, null, null, null, errorMsg)
                    }
                }
            }
        }
    })
}

/**
 * 教师注册（完善资料并上传证件）
 */
fun registerTeacher(
    phone: String,
    nickname: String,
    password: String,
    school: String?,
    certificateType: String, // "teacher_certificate" or "school_proof"
    certificateImageUrl: String,
    callback: (Boolean, String?, UserInfo?, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("phone", phone)
        put("nickname", nickname)
        put("password", password)
        put("role", "teacher")
        if (school != null) put("school", school)
        put("certificateType", certificateType)
        put("certificateImageUrl", certificateImageUrl)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/register")
        .post(body)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "registerTeacher failed", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, null, null, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonObj = JSONObject(responseBody)
                        val token = jsonObj.optString("token", "").takeIf { it.isNotEmpty() }
                        
                        var userInfo: UserInfo? = null
                        if (jsonObj.has("user")) {
                            val userObj = jsonObj.getJSONObject("user")
                            userInfo = UserInfo(
                                id = userObj.optString("id", ""),
                                phone = userObj.optString("phone", phone),
                                nickname = userObj.optString("nickname", nickname),
                                role = "teacher",
                                avatarResId = userObj.optInt("avatarResId", 0),
                                avatarUri = userObj.optString("avatarUri").takeIf { it.isNotBlank() },
                                userAcademicStage = userObj.optString("userAcademicStage").takeIf { it.isNotBlank() },
                                userGender = userObj.optString("userGender").takeIf { it.isNotBlank() },
                                verificationStatus = "pending" // 注册后待审核
                            )
                        }
                        
                        Handler(Looper.getMainLooper()).post {
                            callback(true, token, userInfo, null)
                        }
                    } catch (e: Exception) {
                        Log.e("NETWORK_ERROR", "JSON parsing failed for registerTeacher", e)
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, null, "服务器响应格式错误")
                        }
                    }
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody ?: "{}").optString("message", "注册失败: ${response.code}")
                    } catch (e: Exception) {
                        "注册失败: ${response.code}"
                    }
                    Handler(Looper.getMainLooper()).post {
                        callback(false, null, null, errorMsg)
                    }
                }
            }
        }
    })
}

/**
 * 上传图片（用于上传证件）
 */
fun uploadImage(
    token: String,
    imageFile: java.io.File,
    callback: (Boolean, String?, String?) -> Unit
) {
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "image",
            imageFile.name,
            imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        .build()
    
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/upload/image")
        .post(requestBody)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "uploadImage failed", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, null, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    try {
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val dataObj = jsonResponse.optJSONObject("data")
                        val imageUrl = dataObj?.optString("url") ?: dataObj?.optString("imageUrl")
                        Handler(Looper.getMainLooper()).post {
                            callback(true, imageUrl, null)
                        }
                    } catch (e: Exception) {
                        Log.e("NETWORK_ERROR", "解析上传结果失败", e)
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, "数据解析错误")
                        }
                    }
                } else {
                    Log.w("NETWORK_ERROR", "上传图片失败: ${response.code} - $responseBody")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, null, "上传失败: ${response.code}")
                    }
                }
            }
        }
    })
}

/**
 * 获取题目市场（待处理题目）
 */
fun getQuestionMarket(
    token: String,
    subject: String? = null,
    academicStage: String? = null,
    minPrice: Int? = null,
    maxPrice: Int? = null,
    callback: (Boolean, List<Question>?, String?, Int?) -> Unit // 添加statusCode参数
) {
    val urlBuilder = StringBuilder("${NetworkConfig.QUESTION_URL}/market")
    val params = mutableListOf<String>()
    if (subject != null) params.add("subject=$subject")
    if (academicStage != null) params.add("academicStage=$academicStage")
    if (minPrice != null) params.add("minPrice=$minPrice")
    if (maxPrice != null) params.add("maxPrice=$maxPrice")
    if (params.isNotEmpty()) {
        urlBuilder.append("?").append(params.joinToString("&"))
    }
    
    val request = Request.Builder()
        .url(urlBuilder.toString())
        .get()
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("QUESTION_API_ERROR", "获取题目市场失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, null, errorMsg, null)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        
                        // 安全地获取 data 对象
                        val dataObj = jsonResponse.optJSONObject("data")
                        if (dataObj == null) {
                            Log.e("QUESTION_API_ERROR", "响应中没有 data 字段: $responseBody")
                            Handler(Looper.getMainLooper()).post {
                                callback(false, null, "服务器响应格式错误：缺少 data 字段", null)
                            }
                            return
                        }
                        
                        // 安全地获取 questions 数组
                        val questionsArray = dataObj.optJSONArray("questions")
                        if (questionsArray == null) {
                            Log.e("QUESTION_API_ERROR", "响应中没有 questions 数组: $responseBody")
                            Handler(Looper.getMainLooper()).post {
                                callback(false, null, "服务器响应格式错误：缺少 questions 数组", null)
                            }
                            return
                        }
                        
                        Log.d("QUESTION_API_DEBUG", "找到 ${questionsArray.length()} 个题目")
                        
                        val questions = mutableListOf<Question>()
                        
                        for (i in 0 until questionsArray.length()) {
                            try {
                                // 安全地获取 JSON 对象
                                val qObj = questionsArray.optJSONObject(i)
                                if (qObj == null) {
                                    Log.w("QUESTION_API_ERROR", "题目 $i 不是有效的 JSON 对象")
                                    continue
                                }
                                
                                // 打印题目对象内容（用于调试）
                                Log.d("QUESTION_API_DEBUG", "解析题目 $i: ${qObj.toString()}")
                                
                                // 验证必填字段
                                val id = qObj.optString("id", "").takeIf { it.isNotEmpty() }
                                val studentId = qObj.optString("studentId", "").takeIf { it.isNotEmpty() }
                                val imageUrl = qObj.optString("imageUrl", "").takeIf { it.isNotEmpty() }
                                val subject = qObj.optString("subject", "").takeIf { it.isNotEmpty() }
                                
                                if (id == null || studentId == null || imageUrl == null || subject == null) {
                                    Log.w("QUESTION_API_ERROR", "题目缺少必填字段，跳过: id=$id, studentId=$studentId, imageUrl=$imageUrl, subject=$subject")
                                    continue
                                }
                                
                                // 安全地获取所有字段
                                val teacherId = try {
                                    qObj.optString("teacherId", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val description = try {
                                    qObj.optString("description", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val status = try {
                                    qObj.optString("status", "pending")
                                } catch (e: Exception) { "pending" }
                                
                                val roomId = try {
                                    qObj.optString("roomId", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val videoRoomId = try {
                                    qObj.optString("videoRoomId", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val rating = try {
                                    if (qObj.has("rating") && !qObj.isNull("rating")) {
                                        qObj.optInt("rating", 0).takeIf { it > 0 }
                                    } else null
                                } catch (e: Exception) { null }
                                
                                val ratingComment = try {
                                    qObj.optString("ratingComment", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val createdAt = try {
                                    qObj.optString("createdAt", "").takeIf { it.isNotEmpty() } ?: System.currentTimeMillis().toString()
                                } catch (e: Exception) { System.currentTimeMillis().toString() }
                                
                                val assignedAt = try {
                                    qObj.optString("assignedAt", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val startedAt = try {
                                    qObj.optString("startedAt", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val completedAt = try {
                                    qObj.optString("completedAt", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val ratedAt = try {
                                    qObj.optString("ratedAt", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val academicStage = try {
                                    qObj.optString("academicStage", null).takeIf { it != "null" && it.isNotEmpty() }
                                } catch (e: Exception) { null }
                                
                                val price = try {
                                    if (qObj.has("price") && !qObj.isNull("price")) {
                                        qObj.optInt("price", 0).takeIf { it > 0 }
                                    } else null
                                } catch (e: Exception) { null }
                                
                                questions.add(
                                    Question(
                                        id = id,
                                        studentId = studentId,
                                        teacherId = teacherId,
                                        imageUrl = imageUrl,
                                        subject = subject,
                                        description = description,
                                        status = status,
                                        roomId = roomId,
                                        videoRoomId = videoRoomId,
                                        rating = rating,
                                        ratingComment = ratingComment,
                                        createdAt = createdAt,
                                        assignedAt = assignedAt,
                                        startedAt = startedAt,
                                        completedAt = completedAt,
                                        ratedAt = ratedAt,
                                        academicStage = academicStage,
                                        price = price
                                    )
                                )
                                
                                Log.d("QUESTION_API_DEBUG", "成功解析题目: id=$id, subject=$subject")
                            } catch (e: Exception) {
                                Log.e("QUESTION_API_ERROR", "解析单个题目失败，跳过", e)
                                Log.e("QUESTION_API_ERROR", "异常类型: ${e.javaClass.simpleName}, 消息: ${e.message ?: "null"}")
                                Log.e("QUESTION_API_ERROR", "异常堆栈: ${e.stackTraceToString()}")
                                // 继续处理下一个题目
                                continue
                            }
                        }
                        
                        Handler(Looper.getMainLooper()).post {
                            callback(true, questions, null, null)
                        }
                    } catch (e: Exception) {
                        Log.e("QUESTION_API_ERROR", "解析题目市场失败", e)
                        Log.e("QUESTION_API_ERROR", "响应内容: $responseBody")
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, "数据解析错误: ${e.message}", null)
                        }
                    }
                } else {
                    Log.w("QUESTION_API_ERROR", "获取题目市场失败: ${response.code} - $responseBody")
                    val errorMsg = try {
                        JSONObject(responseBody ?: "{}").optString("message", "获取失败: ${response.code}")
                    } catch (e: Exception) {
                        "获取失败: ${response.code}"
                    }
                    Handler(Looper.getMainLooper()).post {
                        callback(false, null, errorMsg, response.code)
                    }
                }
            }
        }
    })
}

/**
 * 获取题目详情
 */
fun getQuestionDetail(
    token: String,
    questionId: String,
    callback: (Boolean, Question?, String?) -> Unit
) {
    val url = "${NetworkConfig.QUESTION_URL}/$questionId"
    val request = Request.Builder()
        .url(url)
        .get()
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("QUESTION_API_ERROR", "获取题目详情失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, null, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    try {
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        
                        // 检查响应结构
                        if (!jsonResponse.has("data")) {
                            throw Exception("响应中缺少data字段")
                        }
                        
                        val dataObj = jsonResponse.getJSONObject("data")
                        if (!dataObj.has("question")) {
                            throw Exception("响应中缺少question字段")
                        }
                        
                        val qObj = dataObj.getJSONObject("question")
                        
                        // 安全地获取必填字段
                        val id = qObj.optString("id", "").takeIf { it.isNotEmpty() }
                            ?: throw Exception("题目ID为空")
                        val studentId = qObj.optString("studentId", "").takeIf { it.isNotEmpty() }
                            ?: throw Exception("学生ID为空")
                        val imageUrl = qObj.optString("imageUrl", "").takeIf { it.isNotEmpty() }
                            ?: throw Exception("图片URL为空")
                        val subject = qObj.optString("subject", "").takeIf { it.isNotEmpty() }
                            ?: throw Exception("科目为空")
                        val status = qObj.optString("status", "pending")
                        val createdAt = qObj.optString("createdAt", System.currentTimeMillis().toString())
                        
                        // 安全地获取可选字段
                        val teacherId = try {
                            qObj.optString("teacherId", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val description = try {
                            qObj.optString("description", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val roomId = try {
                            qObj.optString("roomId", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val videoRoomId = try {
                            qObj.optString("videoRoomId", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val rating = try {
                            if (qObj.has("rating") && !qObj.isNull("rating")) {
                                qObj.optInt("rating", 0).takeIf { it > 0 }
                            } else null
                        } catch (e: Exception) { null }
                        
                        val ratingComment = try {
                            qObj.optString("ratingComment", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val assignedAt = try {
                            qObj.optString("assignedAt", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val startedAt = try {
                            qObj.optString("startedAt", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val completedAt = try {
                            qObj.optString("completedAt", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val ratedAt = try {
                            qObj.optString("ratedAt", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                        val academicStage = try {
                            qObj.optString("academicStage", null).takeIf { it != "null" && it.isNotEmpty() }
                        } catch (e: Exception) { null }
                        
                                val price = try {
                                    if (qObj.has("price") && !qObj.isNull("price")) {
                                        val priceValue = qObj.optInt("price", 0)
                                        // 如果price为0或负数，视为null（待定）
                                        if (priceValue > 0) priceValue else null
                                    } else null
                                } catch (e: Exception) { 
                                    Log.w("QUESTION_API_DEBUG", "解析price失败: ${e.message}", e)
                                    null 
                                }
                        
                        val q = Question(
                            id = id,
                            studentId = studentId,
                            teacherId = teacherId,
                            imageUrl = imageUrl,
                            subject = subject,
                            description = description,
                            status = status,
                            roomId = roomId,
                            videoRoomId = videoRoomId,
                            rating = rating,
                            ratingComment = ratingComment,
                            createdAt = createdAt,
                            assignedAt = assignedAt,
                            startedAt = startedAt,
                            completedAt = completedAt,
                            ratedAt = ratedAt,
                            academicStage = academicStage,
                            price = price
                        )
                        Handler(Looper.getMainLooper()).post {
                            callback(true, q, null)
                        }
                    } catch (e: Exception) {
                        Log.e("QUESTION_API_ERROR", "解析题目详情失败", e)
                        Log.e("QUESTION_API_ERROR", "响应内容: $responseBody")
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, "数据解析错误: ${e.message}")
                        }
                    }
                } else {
                    Log.w("QUESTION_API_ERROR", "获取题目详情失败: ${response.code} - $responseBody")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, null, "获取失败: ${response.code}")
                    }
                }
            }
        }
    })
}

/**
 * 接收题目
 */
fun acceptQuestion(
    token: String,
    questionId: String,
    callback: (Boolean, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("questionId", questionId)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.QUESTION_URL}/accept")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("QUESTION_API_ERROR", "接收题目失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        callback(true, null)
                    }
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody ?: "{}").optString("message", "接收失败")
                    } catch (e: Exception) {
                        "接收失败: ${response.code}"
                    }
                    Handler(Looper.getMainLooper()).post {
                        callback(false, errorMsg)
                    }
                }
            }
        }
    })
}

/**
 * 放弃题目（接收后放弃）
 */
fun abandonQuestion(
    token: String,
    questionId: String,
    callback: (Boolean, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("questionId", questionId)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.QUESTION_URL}/abandon")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("QUESTION_API_ERROR", "放弃题目失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        callback(true, null)
                    }
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody ?: "{}").optString("message", "放弃失败")
                    } catch (e: Exception) {
                        "放弃失败: ${response.code}"
                    }
                    Handler(Looper.getMainLooper()).post {
                        callback(false, errorMsg)
                    }
                }
            }
        }
    })
}

/**
 * 获取教师收入列表
 */
fun getTeacherEarnings(
    token: String,
    callback: (Boolean, List<TeacherEarning>?, String?) -> Unit
) {
    val request = Request.Builder()
        .url("${NetworkConfig.QUESTION_URL}/earnings")
        .get()
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("QUESTION_API_ERROR", "获取收入列表失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, null, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    try {
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val dataObj = jsonResponse.getJSONObject("data")
                        val earningsArray = dataObj.getJSONArray("earnings")
                        val earnings = mutableListOf<TeacherEarning>()
                        
                        for (i in 0 until earningsArray.length()) {
                            val eObj = earningsArray.getJSONObject(i)
                            earnings.add(
                                TeacherEarning(
                                    questionId = eObj.getString("questionId"),
                                    studentId = eObj.getString("studentId"),
                                    subject = eObj.getString("subject"),
                                    price = eObj.getInt("price"),
                                    completedAt = eObj.getString("completedAt"),
                                    rating = if (eObj.has("rating") && !eObj.isNull("rating")) eObj.getInt("rating") else null
                                )
                            )
                        }
                        Handler(Looper.getMainLooper()).post {
                            callback(true, earnings, null)
                        }
                    } catch (e: Exception) {
                        Log.e("QUESTION_API_ERROR", "解析收入列表失败", e)
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, "数据解析错误")
                        }
                    }
                } else {
                    Log.w("QUESTION_API_ERROR", "获取收入列表失败: ${response.code} - $responseBody")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, null, "获取失败: ${response.code}")
                    }
                }
            }
        }
    })
}

/**
 * 获取用户信息
 */
fun getUserProfile(
    token: String,
    callback: (Boolean, String?, String?) -> Unit // success, avatarUrl, error
) {
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/users/profile")
        .get()
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "获取用户信息失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, null, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    try {
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val dataObj = jsonResponse.optJSONObject("data")
                        val userObj = dataObj?.optJSONObject("user")
                        val avatarUrl = userObj?.optString("avatarUri")
                        Handler(Looper.getMainLooper()).post {
                            callback(true, avatarUrl, null)
                        }
                    } catch (e: Exception) {
                        Log.e("NETWORK_ERROR", "解析用户信息失败", e)
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, "数据解析错误")
                        }
                    }
                } else {
                    Log.w("NETWORK_ERROR", "获取用户信息失败: ${response.code} - $responseBody")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, null, "获取失败: ${response.code}")
                    }
                }
            }
        }
    })
}

/**
 * 上传头像
 */
fun uploadAvatar(
    token: String,
    imageFile: java.io.File,
    callback: (Boolean, String?, String?) -> Unit
) {
    uploadImage(token, imageFile, callback)
}

/**
 * 更新用户头像
 */
fun updateUserAvatar(
    token: String,
    avatarUrl: String,
    callback: (Boolean, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("avatarUri", avatarUrl)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/users/profile")
        .put(body)
        .addHeader("Authorization", "Bearer $token")
        .addHeader("Content-Type", "application/json")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "更新头像失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        callback(true, null)
                    }
                } else {
                    Log.w("NETWORK_ERROR", "更新头像失败: ${response.code} - $responseBody")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, "更新失败: ${response.code}")
                    }
                }
            }
        }
    })
}

/**
 * 修改密码（使用验证码验证）
 */
fun changePasswordByCode(
    token: String,
    phone: String,
    code: String,
    newPassword: String,
    callback: (Boolean, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("phone", phone)
        put("code", code)
        put("newPassword", newPassword)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/users/change-password-by-code")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .addHeader("Content-Type", "application/json")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "修改密码失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        callback(true, null)
                    }
                } else {
                    Log.w("NETWORK_ERROR", "修改密码失败: ${response.code} - $responseBody")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, "修改失败: ${response.code}")
                    }
                }
            }
        }
    })
}

/**
 * 绑定银行卡
 */
fun bindBankCard(
    token: String,
    cardNumber: String,
    cardHolder: String,
    bankName: String,
    callback: (Boolean, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("cardNumber", cardNumber)
        put("cardHolder", cardHolder)
        put("bankName", bankName)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/users/bank-card")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .addHeader("Content-Type", "application/json")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "绑定银行卡失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        callback(true, null)
                    }
                } else {
                    Log.w("NETWORK_ERROR", "绑定银行卡失败: ${response.code} - $responseBody")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, "绑定失败: ${response.code}")
                    }
                }
            }
        }
    })
}

/**
 * 获取银行卡信息
 */
fun getBankCard(
    token: String,
    callback: (Boolean, String?, String?, String?, String?) -> Unit
) {
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/users/bank-card")
        .get()
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "获取银行卡信息失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, null, null, null, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    try {
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val dataObj = jsonResponse.optJSONObject("data")
                        val cardNumber = dataObj?.optString("cardNumber")
                        val cardHolder = dataObj?.optString("cardHolder")
                        val bankName = dataObj?.optString("bankName")
                        Handler(Looper.getMainLooper()).post {
                            callback(true, cardNumber, cardHolder, bankName, null)
                        }
                    } catch (e: Exception) {
                        Log.e("NETWORK_ERROR", "解析银行卡信息失败", e)
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, null, null, "数据解析错误")
                        }
                    }
                } else {
                    if (response.code == 404) {
                        // 未绑定银行卡
                        Handler(Looper.getMainLooper()).post {
                            callback(true, null, null, null, null)
                        }
                    } else {
                        Log.w("NETWORK_ERROR", "获取银行卡信息失败: ${response.code} - $responseBody")
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null, null, null, "获取失败: ${response.code}")
                        }
                    }
                }
            }
        }
    })
}

/**
 * 发起视频通话
 */
fun startVideoCall(
    token: String,
    questionId: String,
    roomId: String,
    callback: (Boolean, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("questionId", questionId)
        put("roomId", roomId)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.QUESTION_URL}/start-video")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .addHeader("Content-Type", "application/json")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("VIDEO_API_ERROR", "发起视频通话失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        callback(true, null)
                    }
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody ?: "{}").optString("message", "发起视频通话失败")
                    } catch (e: Exception) {
                        "发起视频通话失败: ${response.code}"
                    }
                    Handler(Looper.getMainLooper()).post {
                        callback(false, errorMsg)
                    }
                }
            }
        }
    })
}

/**
 * 结束视频通话
 */
fun endVideoCall(
    token: String,
    questionId: String,
    callback: (Boolean, String?) -> Unit
) {
    val json = JSONObject().apply {
        put("questionId", questionId)
    }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url("${NetworkConfig.QUESTION_URL}/end-video")
        .post(body)
        .addHeader("Authorization", "Bearer $token")
        .addHeader("Content-Type", "application/json")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("VIDEO_API_ERROR", "结束视频通话失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        callback(true, null)
                    }
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody ?: "{}").optString("message", "结束视频通话失败")
                    } catch (e: Exception) {
                        "结束视频通话失败: ${response.code}"
                    }
                    Handler(Looper.getMainLooper()).post {
                        callback(false, errorMsg)
                    }
                }
            }
        }
    })
}

/**
 * 解绑银行卡
 */
fun unbindBankCard(
    token: String,
    callback: (Boolean, String?) -> Unit
) {
    val request = Request.Builder()
        .url("${NetworkConfig.GATEWAY_URL}/api/users/bank-card")
        .delete()
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NETWORK_ERROR", "解绑银行卡失败", e)
            Handler(Looper.getMainLooper()).post {
                val errorMsg = ErrorHandler.handleNetworkError(e)
                callback(false, errorMsg)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        callback(true, null)
                    }
                } else {
                    Log.w("NETWORK_ERROR", "解绑银行卡失败: ${response.code} - $responseBody")
                    Handler(Looper.getMainLooper()).post {
                        callback(false, "解绑失败: ${response.code}")
                    }
                }
            }
        }
    })
}
