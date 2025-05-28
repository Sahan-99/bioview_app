package com.my.bioview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val helloTextView = findViewById<TextView>(R.id.hello)
        val itemHome = findViewById<LinearLayout>(R.id.itemHome)
        val item3D = findViewById<LinearLayout>(R.id.item3D)
        val itemProfile = findViewById<LinearLayout>(R.id.itemProfile)

        itemHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        item3D.setOnClickListener {
            // Already on 3D screen
        }

        itemProfile.setOnClickListener {
            val intent = Intent(this, LogoutActivity2::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get first_name from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val firstName = sharedPref.getString("first_name", "User") ?: "User"
        helloTextView.text = "Hello $firstName!"

        btnNext.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}