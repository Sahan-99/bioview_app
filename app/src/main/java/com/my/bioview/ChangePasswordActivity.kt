package com.my.bioview

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.util.HashMap

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnConfirmChange: Button
    private lateinit var progressDialog: ProgressDialog

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

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
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Initialize views
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnConfirmChange = findViewById(R.id.btnConfirmChange)
        progressDialog = ProgressDialog(this).apply {
            setMessage("Changing password...")
            setCancelable(false)
        }

        // Confirm change button
        btnConfirmChange.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePassword(currentPassword, newPassword)
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        progressDialog.show()
        val url = "https://bioview.sahans.online/app/change_password.php"
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null) ?: run {
            progressDialog.dismiss()
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val stringRequest = object : StringRequest(
            Method.POST, url,
            { response ->
                progressDialog.dismiss()
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                        finish() // Return to previous screen
                    } else {
                        Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ChangePasswordActivity", "Error parsing response: ${e.message}, Response: $response")
                    Toast.makeText(this, "Error changing password", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressDialog.dismiss()
                Log.e("ChangePasswordActivity", "Network error: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["current_password"] = currentPassword
                params["new_password"] = newPassword
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cookie"] = sessionId
                return headers
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(5000, 1, 1.0f)
        Volley.newRequestQueue(this).add(stringRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}