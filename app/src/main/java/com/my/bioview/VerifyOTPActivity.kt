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
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class VerifyOTPActivity : AppCompatActivity() {

    private val client by lazy { OkHttpClient.Builder().build() }
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otpactivity)

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

        val btnVerifyOTP = findViewById<Button>(R.id.btnVerifyOTP)
        val btnSignIn = findViewById<TextView>(R.id.btnSignIn)
        val otp1 = findViewById<TextInputEditText>(R.id.editTextOTP1)
        val otp2 = findViewById<TextInputEditText>(R.id.editTextOTP2)
        val otp3 = findViewById<TextInputEditText>(R.id.editTextOTP3)
        val otp4 = findViewById<TextInputEditText>(R.id.editTextOTP4)
        val otp5 = findViewById<TextInputEditText>(R.id.editTextOTP5)
        val otp6 = findViewById<TextInputEditText>(R.id.editTextOTP6)

        // Move focus between OTP fields
        val otpFields = listOf(otp1, otp2, otp3, otp4, otp5, otp6)
        otpFields.forEachIndexed { index, editText ->
            editText.doAfterTextChanged { editable ->
                if (editable?.length == 1 && index < 5) {
                    otpFields[index + 1].requestFocus()
                }
                if (editable?.isEmpty() == true && index > 0) {
                    otpFields[index - 1].requestFocus()
                }
            }
        }

        btnVerifyOTP.setOnClickListener {
            val otp = listOf(otp1, otp2, otp3, otp4, otp5, otp6)
                .map { it.text.toString().trim() }
                .joinToString("")
            if (otp.length != 6 || !otp.all { it.isDigit() }) {
                Toast.makeText(this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyOTP(otp)
        }

        btnSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun verifyOTP(otp: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Verifying OTP...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val formBody = FormBody.Builder()
            .add("email", email)
            .add("otp", otp)
            .build()

        val request = Request.Builder()
            .url("https://bioview.sahans.web.lk/app/verify_otp.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(applicationContext, "Failed to verify OTP: ${e.message}", Toast.LENGTH_LONG).show()
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
                            val intent = Intent(applicationContext, ResetPasswordActivity::class.java)
                            intent.putExtra("email", email)
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