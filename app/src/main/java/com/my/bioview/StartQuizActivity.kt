package com.my.bioview

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.KeyEvent
import androidx.core.view.ViewCompat

class StartQuizActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_quiz)

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

        // Get quiz name from Intent
        val quizName = intent.getStringExtra("quiz_name") ?: "Unknown Quiz"

        // Set quiz name
        val tvQuizName = findViewById<TextView>(R.id.tvQuizName)
        tvQuizName.text = quizName

        // Start Quiz button click listener (placeholder)
        val btnStartQuiz = findViewById<Button>(R.id.btnStartQuiz)
        btnStartQuiz.setOnClickListener {
            // Add logic to start the quiz (e.g., navigate to QuizQuestionActivity)
            // For now, just finish the activity with a fade-out animation
            ViewCompat.animate(findViewById(R.id.main))
                .alpha(0f)
                .setDuration(300)
                .withEndAction { finish() }
                .start()
            overridePendingTransition(0, R.anim.fade_out)
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