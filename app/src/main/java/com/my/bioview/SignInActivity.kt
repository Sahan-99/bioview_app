package com.my.bioview

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException

class SignInActivity : AppCompatActivity() {

    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var btnSignIn: Button
    private lateinit var btnSignUp: TextView
    private lateinit var btnForgotPassword: TextView

    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)

        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        btnSignIn.setOnClickListener {
            loginUser()
        }

        btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        btnForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Logging in...")
        progressDialog.show()

        val formBody = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("https://bioview.sahans.web.lk/app/login.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        applicationContext,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            @SuppressLint("MissingPermission")
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    progressDialog.dismiss()
                    val responseBody = response.body?.string()
                    try {
                        val json = JSONObject(responseBody)
                        val status = json.getString("status")
                        val message = json.getString("message")
                        if (response.isSuccessful && status == "success") {
                            val userType = json.getString("type")
                            val userId = json.getInt("user_id")
                            val cookies = response.headers("Set-Cookie")
                            var sessionId: String? = null
                            for (cookie in cookies) {
                                if (cookie.contains("PHPSESSID")) {
                                    sessionId = cookie.split(";")[0]
                                    break
                                }
                            }

                            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putBoolean("is_logged_in", true)
                                putInt("user_id", userId)
                                putString("user_type", userType)
                                if (sessionId != null) {
                                    putString("session_id", sessionId)
                                }
                                apply()
                            }

                            Toast.makeText(
                                applicationContext,
                                "$message (Type: $userType)",
                                Toast.LENGTH_SHORT
                            ).show()


                            showLoginSuccessNotification()

                            val intent = Intent(applicationContext, MainActivity::class.java)
                            intent.putExtra("USER_TYPE", userType)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                applicationContext,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            applicationContext,
                            "Error parsing response: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("SignInActivity", "Response: $responseBody, Error: ${e.message}")
                    }
                }
            }
        })
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showLoginSuccessNotification() {
        val channelId = "login_channel"
        val channelName = "Login Notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Login Successful")
            .setContentText("Welcome to BioView!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, builder.build())
    }
}
