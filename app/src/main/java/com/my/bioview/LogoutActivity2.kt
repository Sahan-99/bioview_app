package com.my.bioview

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class LogoutActivity2 : AppCompatActivity() {

    private val client by lazy {
        OkHttpClient.Builder().build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_logout2)

        val btnLogout = findViewById<Button>(R.id.btnLogout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnLogout.setOnClickListener {
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Confirm logout from BioView")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    logoutUser()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun logoutUser() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Logging out...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val request = Request.Builder()
            .url("https://bioview.sahans.online/app/logout.php")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        applicationContext,
                        "Logout failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putBoolean("is_logged_in", false)
                            remove("user_type")
                            remove("first_name")
                            remove("session_id")
                            apply()
                        }
                        Toast.makeText(applicationContext, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(applicationContext, SignInActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Logout failed: Server error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }
}