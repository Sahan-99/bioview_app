package com.my.bioview

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.animation.Animation
import android.view.animation.AnimationUtils

class StartQuizActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_quiz)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Apply fade-in animation to the layout
        ViewCompat.animate(findViewById(R.id.main))
            .alpha(0f)
            .setDuration(0)
            .withEndAction {
                ViewCompat.animate(findViewById(R.id.main))
                    .alpha(1f)
                    .setDuration(500)
                    .start()
            }

        // Get quiz name and quiz_id from Intent
        val quizName = intent.getStringExtra("quiz_name") ?: "Unknown Quiz"
        val quizId = intent.getIntExtra("quiz_id", -1)
        Log.d("StartQuizActivity", "Received quizName: $quizName, quizId: $quizId")

        // Check login status
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            Log.w("StartQuizActivity", "User not logged in, redirecting to SignInActivity")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) {
            Log.w("StartQuizActivity", "Invalid userId, redirecting to SignInActivity")
            Toast.makeText(this, "Invalid user session, log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Set quiz name
        val tvQuizName = findViewById<TextView>(R.id.tvQuizName)
        tvQuizName.text = quizName

        // Start Quiz button click listener
        val btnStartQuiz = findViewById<Button>(R.id.btnStartQuiz)
        btnStartQuiz.setOnClickListener {
            if (quizId == -1) {
                Log.e("StartQuizActivity", "quiz_id not received, defaulting to 1")
                Toast.makeText(this, "Error: Quiz ID not found", Toast.LENGTH_SHORT).show()
            }
            val intent = Intent(this, QuestionActivity::class.java).apply {
                putExtra("quiz_id", if (quizId == -1) 1 else quizId)
                putExtra("user_id", userId)
                putExtra("quiz_name", quizName) // Pass quiz name
                Log.d("StartQuizActivity", "Navigating to QuestionActivity with quizId: ${if (quizId == -1) 1 else quizId}, userId: $userId, quizName: $quizName")
            }
            startActivity(intent)
            // Fade-out animation with fallback
            try {
                val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
                findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main).startAnimation(fadeOut)
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        finish()
                    }
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            } catch (e: Exception) {
                Log.e("StartQuizActivity", "Animation error: ${e.message}")
                finish()
            }
            overridePendingTransition(0, 0)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ViewCompat.animate(findViewById(R.id.main))
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                .start()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}