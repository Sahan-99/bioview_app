package com.my.bioview

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import android.view.KeyEvent
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

class Slider3Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slider3)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted3)
        val btnSkip = findViewById<TextView>(R.id.btnSkip3)
        val indicators = findViewById<LinearLayout>(R.id.indicators)

        updateIndicators(indicators, 2)

        btnGetStarted.setOnClickListener {
            // Check login status using SharedPreferences
            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

            val intent = if (isLoggedIn) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, SignInActivity::class.java)
            }
            startActivity(intent)
            finish()
        }

        btnSkip.setOnClickListener {
            // Check login status using SharedPreferences
            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

            val intent = if (isLoggedIn) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, SignInActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateIndicators(indicators: LinearLayout, position: Int) {
        for (i in 0 until indicators.childCount) {
            val dot = indicators.getChildAt(i) as View
            dot.setBackgroundResource(
                if (i == position) R.drawable.indicator_selected
                else R.drawable.indicator_unselected
            )
        }
    }
}