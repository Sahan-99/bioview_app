package com.my.bioview

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.KeyEvent
import androidx.core.view.ViewCompat
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

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
        val quizId = intent.getIntExtra("quiz_id", -1) // Default to -1 to detect missing quiz_id
        Log.d("StartQuizActivity", "Received quizName: $quizName, quizId: $quizId")

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
                Log.d("StartQuizActivity", "Navigating to QuestionActivity with quizId: ${if (quizId == -1) 1 else quizId}")
            }
            startActivity(intent)
            // Fade-out animation with fallback
            try {
                val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
                findViewById<ConstraintLayout>(R.id.main).startAnimation(fadeOut)
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