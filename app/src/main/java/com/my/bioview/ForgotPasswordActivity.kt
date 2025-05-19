package com.my.bioview

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import android.view.KeyEvent
import android.widget.Toast

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val btnSendResetLink = findViewById<Button>(R.id.btnSendResetLink)
        val btnSignIn = findViewById<TextView>(R.id.btnSignIn)
        val editTextEmail = findViewById<TextInputEditText>(R.id.editTextEmail)

        btnSendResetLink.setOnClickListener {
            val email = editTextEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Placeholder for sending reset link (replace with actual logic, e.g., Firebase)
            Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_SHORT).show()

            // Navigate back to SignInActivity
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}