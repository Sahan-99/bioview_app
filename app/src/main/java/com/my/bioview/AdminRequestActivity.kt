package com.my.bioview

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class AdminRequestActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnGenerateReport: Button
    private lateinit var progressDialog: ProgressDialog
    private lateinit var tvSubTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_request)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnBack = findViewById(R.id.btnBack)
        btnGenerateReport = findViewById(R.id.btnGenerateReport)
        tvSubTitle = findViewById(R.id.tvSubTitle)

        btnBack.setOnClickListener {
            finish()
        }

        val itemHome = findViewById<LinearLayout>(R.id.itemHome)
        val item3D = findViewById<LinearLayout>(R.id.item3D)
        val itemProfile = findViewById<LinearLayout>(R.id.itemProfile)

        itemHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        item3D.setOnClickListener {
            // Already on 3D screen
        }

        itemProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Processing request...")
        progressDialog.setCancelable(false)

        btnGenerateReport.setOnClickListener {
            requestAdminAccess()
        }

        // Check current user type and request status
        checkUserRequestStatus()
    }

    private fun checkUserRequestStatus() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)
        val userType = sharedPref.getString("user_type", "teacher")

        if (userType == "admin") {
            tvSubTitle.text = "You are already an Admin."
            btnGenerateReport.visibility = View.GONE
        } else if (userType == "teacher") {
            val url = "https://bioview.sahans.web.lk/bv-admin/check_admin_request.php"
            progressDialog.show()

            val stringRequest = object : StringRequest(
                Request.Method.GET, url,
                { response ->
                    progressDialog.dismiss()
                    try {
                        val jsonResponse = JSONObject(response)
                        if (jsonResponse.getString("status") == "success") {
                            val requestStatus = jsonResponse.getString("request_status")
                            if (requestStatus == "pending") {
                                tvSubTitle.text = "Your admin access request is pending approval."
                                btnGenerateReport.text = "Pending Admin Access"
                                btnGenerateReport.isEnabled = false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AdminRequestActivity", "Error checking request status: ${e.message}")
                        Toast.makeText(this, "Error checking request status", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    progressDialog.dismiss()
                    Log.e("AdminRequestActivity", "Error: ${error.message}")
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    if (sessionId != null) {
                        headers["Cookie"] = sessionId
                    }
                    return headers
                }
            }

            stringRequest.retryPolicy = DefaultRetryPolicy(
                5000, 1, 1.0f
            )
            Volley.newRequestQueue(this).add(stringRequest)
        }
    }

    private fun requestAdminAccess() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)

        if (sessionId == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val url = "https://bioview.sahans.web.lk/bv-admin/request_admin_access.php"
        progressDialog.show()

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                progressDialog.dismiss()
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        tvSubTitle.text = "Your admin access request has been submitted and is pending approval."
                        btnGenerateReport.text = "Pending Admin Access"
                        btnGenerateReport.isEnabled = false
                        Toast.makeText(this, "Request submitted successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("AdminRequestActivity", "Error parsing response: ${e.message}")
                    Toast.makeText(this, "Error processing request", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressDialog.dismiss()
                Log.e("AdminRequestActivity", "Error: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cookie"] = sessionId
                return headers
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "request"
                return params
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            5000, 1, 1.0f
        )
        Volley.newRequestQueue(this).add(stringRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
}