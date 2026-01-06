package com.example.andrio_teacher.utils

import android.content.Context
import android.content.SharedPreferences

object UserSession {
    private const val PREFS_NAME = "teacher_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_PHONE = "phone"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_ROLE = "role"
    private const val KEY_VERIFICATION_STATUS = "verification_status"
    
    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }
    
    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }
    
    fun saveUserId(context: Context, userId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }
    
    fun getUserId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, null)
    }
    
    fun saveUserInfo(context: Context, phone: String, nickname: String, role: String? = null, verificationStatus: String? = null) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHONE, phone)
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_ROLE, role)
            .putString(KEY_VERIFICATION_STATUS, verificationStatus)
            .apply()
    }
    
    fun saveVerificationStatus(context: Context, verificationStatus: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VERIFICATION_STATUS, verificationStatus)
            .apply()
    }
    
    fun getVerificationStatus(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_VERIFICATION_STATUS, null)
    }
    
    fun isVerified(context: Context): Boolean {
        return getVerificationStatus(context) == "approved"
    }
    
    fun getPhone(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PHONE, null)
    }
    
    fun getNickname(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NICKNAME, null)
    }
    
    fun getRole(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, null)
    }
    
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
    
    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }
    
    // 保存筛选条件
    private const val KEY_SELECTED_SUBJECTS = "selected_subjects"
    private const val KEY_SELECTED_ACADEMIC_STAGE = "selected_academic_stage"
    
    fun saveFilterSettings(context: Context, subjects: Set<String>, academicStage: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putStringSet(KEY_SELECTED_SUBJECTS, subjects)
        editor.putString(KEY_SELECTED_ACADEMIC_STAGE, academicStage)
        editor.apply()
    }
    
    fun getSelectedSubjects(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SELECTED_SUBJECTS, emptySet()) ?: emptySet()
    }
    
    fun getSelectedAcademicStage(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_ACADEMIC_STAGE, null)
    }
    
    fun clearFilterSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_SELECTED_SUBJECTS)
            .remove(KEY_SELECTED_ACADEMIC_STAGE)
            .apply()
    }
}

