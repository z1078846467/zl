package com.example.andrio_teacher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.andrio_teacher.ui.screens.login.LoginScreen
import com.example.andrio_teacher.ui.screens.register.RegisterScreen
import com.example.andrio_teacher.ui.screens.market.QuestionMarketScreen
import com.example.andrio_teacher.ui.screens.detail.QuestionDetailScreen
import com.example.andrio_teacher.ui.screens.profile.ProfileScreen
import com.example.andrio_teacher.ui.screens.settings.SettingsScreen
import com.example.andrio_teacher.ui.screens.settings.AccountSecurityScreen
import com.example.andrio_teacher.ui.screens.settings.WithdrawCardScreen
import com.example.andrio_teacher.ui.screens.settings.GeneralSettingsScreen
import com.example.andrio_teacher.ui.screens.settings.FeedbackScreen
import com.example.andrio_teacher.ui.screens.settings.AboutScreen
import com.example.andrio_teacher.ui.screens.profile.CompletedQuestionsScreen
import com.example.andrio_teacher.ui.screens.video.VideoCallScreen

object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register/{phone}"
    const val QUESTION_MARKET = "question_market"
    const val QUESTION_DETAIL = "question_detail/{questionId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val ACCOUNT_SECURITY = "account_security"
    const val WITHDRAW_CARD = "withdraw_card"
    const val GENERAL_SETTINGS = "general_settings"
    const val FEEDBACK = "feedback"
    const val ABOUT = "about"
    const val COMPLETED_QUESTIONS = "completed_questions"
    const val VIDEO_CALL = "video_call/{roomId}/{questionId}"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.LOGIN
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    navController.navigate(AppRoutes.QUESTION_MARKET) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = AppRoutes.REGISTER,
            arguments = listOf(
                navArgument("phone") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            RegisterScreen(
                navController = navController,
                phone = phone,
                onRegisterSuccess = {
                    navController.navigate(AppRoutes.QUESTION_MARKET) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(AppRoutes.QUESTION_MARKET) {
            QuestionMarketScreen(
                navController = navController,
                onQuestionClick = { questionId ->
                    navController.navigate("question_detail/$questionId")
                }
            )
        }
        
        composable(
            route = AppRoutes.QUESTION_DETAIL,
            arguments = listOf(
                navArgument("questionId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
            QuestionDetailScreen(
                navController = navController,
                questionId = questionId,
                onQuestionRemoved = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(AppRoutes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        
        composable(AppRoutes.ACCOUNT_SECURITY) {
            AccountSecurityScreen(navController = navController)
        }
        
        composable(AppRoutes.WITHDRAW_CARD) {
            WithdrawCardScreen(navController = navController)
        }
        
        composable(AppRoutes.GENERAL_SETTINGS) {
            GeneralSettingsScreen(navController = navController)
        }
        
        composable(AppRoutes.FEEDBACK) {
            FeedbackScreen(navController = navController)
        }
        
        composable(AppRoutes.ABOUT) {
            AboutScreen(navController = navController)
        }
        
        composable(AppRoutes.COMPLETED_QUESTIONS) {
            CompletedQuestionsScreen(navController = navController)
        }
        composable(
            route = AppRoutes.VIDEO_CALL,
            arguments = listOf(
                navArgument("roomId") {
                    type = NavType.StringType
                },
                navArgument("questionId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
            VideoCallScreen(
                navController = navController,
                roomId = roomId,
                questionId = questionId
            )
        }
    }
}

