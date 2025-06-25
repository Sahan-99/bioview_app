package com.my.bioview

data class QuizAttempt(
    val quizId: Int,
    val title: String,
    val attemptTime: String,
    val percentage: Double,
    val totalQuestions: Int,
    val correctAnswers: Int
)