package com.my.bioview

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ResetPasswordActivity : AppCompatActivity() {

    private val client by lazy { OkHttpClient.Builder().build() }
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Get email from intent
        email = intent.getStringExtra("email") ?: ""
        if (email.isEmpty()) {
            Toast.makeText(this, "Email not provided", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tvEmail).text = "Email: $email"

        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val btnSignIn = findViewById<TextView>(R.id.btnSignIn)
        val editTextPassword = findViewById<TextInputEditText>(R.id.editTextPassword)

        btnResetPassword.setOnClickListener {
            val newPassword = editTextPassword.text.toString().trim()
            if (newPassword.isEmpty()) {
                Toast.makeText(this, "New password is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resetPassword(newPassword)
        }

        btnSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun resetPassword(newPassword: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Resetting password...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val formBody = FormBody.Builder()
            .add("email", email)
            .add("password", newPassword)
            .build()

        val request = Request.Builder()
            .url("https://bioview.sahans.web.lk/app/reset_password.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "Failed to reset password: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    progressDialog.dismiss()
                    val responseBody = response.body?.string()
                    try {
                        val json = org.json.JSONObject(responseBody)
                        val status = json.getString("status")
                        val message = json.getString("message")
                        if (status == "success") {
                            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                            val intent = Intent(applicationContext, SignInActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(applicationContext, "Error processing response: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}