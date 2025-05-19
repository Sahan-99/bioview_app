package com.my.bioview

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.view.KeyEvent

class SignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val btnSignUp = findViewById<TextView>(R.id.btnSignUp)
        val btnForgotPassword = findViewById<TextView>(R.id.btnForgotPassword)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)

        btnSignIn.setOnClickListener {
            // Placeholder for authentication logic
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Simulate successful sign-in (replace with actual authentication)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Show error (for now, just proceed; add validation as needed)
                editTextEmail.error = if (email.isEmpty()) "Email is required" else null
                editTextPassword.error = if (password.isEmpty()) "Password is required" else null
            }
        }

        btnSignUp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnForgotPassword.setOnClickListener {
            // Placeholder for forgot password logic
            // Could navigate to a ForgotPasswordActivity if needed
        }
    }
}