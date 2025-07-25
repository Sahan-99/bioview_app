package com.my.bioview

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private val client by lazy {
        OkHttpClient.Builder().build()
    }

    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var ivReportBanner: ImageView
    private lateinit var ivTeacherBanner: ImageView
    private lateinit var ivAdminBanner: ImageView
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
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

        // Initialize views
        profileImage = findViewById(R.id.profileImage)
        profileName = findViewById(R.id.profileName)
        ivReportBanner = findViewById(R.id.ivReportBanner)
        ivTeacherBanner = findViewById(R.id.ivTeacherBanner)
        ivAdminBanner = findViewById(R.id.ivAdminBanner)

        ivReportBanner.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

        ivTeacherBanner.setOnClickListener {
            val intent = Intent(this, AdminRequestActivity::class.java)
            startActivity(intent)
        }

        ivAdminBanner.setOnClickListener {
            val url = "https://bioview.sahans.web.lk/bv-admin"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }


        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading user data...")
        progressDialog.setCancelable(false)

        // Check login state
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Fetch user data
        fetchUserData()

        // Button click listeners
        val btnEditProfile = findViewById<CardView>(R.id.cardEditProfile)
        val btnChangePassword = findViewById<CardView>(R.id.cardChangePassword)
        val btnHelpSupport = findViewById<CardView>(R.id.cardHelpSupport)
        val btnAboutUs = findViewById<CardView>(R.id.cardAboutUs)
        val btnLogOut = findViewById<CardView>(R.id.cardLogOut)

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        btnHelpSupport.setOnClickListener {
            startActivity(Intent(this, HelpAndSupportActivity::class.java))
        }

        btnAboutUs.setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
        }

        btnLogOut.setOnClickListener {
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

    private fun fetchUserData() {
        val url = "https://bioview.sahans.web.lk/app/get_user.php"

        // Show ProgressDialog
        progressDialog.show()

        // Get session ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)

        val startTime = System.currentTimeMillis()
        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                val endTime = System.currentTimeMillis()
                Log.d("ProfileActivity", "Fetch time: ${endTime - startTime} ms")

                // Hide ProgressDialog
                progressDialog.dismiss()

                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        val firstName = jsonResponse.getString("first_name")
                        val lastName = jsonResponse.getString("last_name")
                        val profilePicture = jsonResponse.getString("profile_picture")
                        val userType = jsonResponse.getString("user_type") // Assuming this field exists

                        Log.d("ProfileActivity", "User Type: $userType, Profile Picture URL: $profilePicture")
                        profileName.text = "$firstName $lastName" // Concatenate first name and last name

                        // Show or hide Banner based on user type
                        ivReportBanner.visibility = if (userType.toLowerCase() == "student") View.VISIBLE else View.GONE
                        ivTeacherBanner.visibility = if (userType.toLowerCase() == "teacher") View.VISIBLE else View.GONE
                        ivAdminBanner.visibility = if (userType.toLowerCase() == "admin") View.VISIBLE else View.GONE

                        if (profilePicture.isNotEmpty()) {
                            Glide.with(this)
                                .load(profilePicture)
                                .override(100, 100) // Resize to match ImageView (100dp)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Log.e("ProfileActivity", "Glide failed to load image: ${e?.message}")
                                        if (e != null) e.logRootCauses("Glide Error")
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: android.graphics.drawable.Drawable,
                                        model: Any,
                                        target: Target<android.graphics.drawable.Drawable>?,
                                        dataSource: com.bumptech.glide.load.DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Log.d("ProfileActivity", "Glide loaded image successfully")
                                        return false
                                    }
                                })
                                .into(profileImage)
                        } else {
                            Log.d("ProfileActivity", "Profile picture is empty, using default")
                            profileImage.setImageResource(R.drawable.default_profile)
                        }
                    } else {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        with(sharedPref.edit()) {
                            putBoolean("is_logged_in", false)
                            putString("session_id", null)
                            apply()
                        }
                        startActivity(Intent(this, SignInActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Error parsing user data: ${e.message}")
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
                    profileName.text = "User"
                    profileImage.setImageResource(R.drawable.default_profile)
                    ivReportBanner.visibility = View.GONE // Hide on error
                }
            },
            { error ->
                // Hide ProgressDialog on error
                progressDialog.dismiss()

                val endTime = System.currentTimeMillis()
                Log.e("ProfileActivity", "Fetch error time: ${endTime - startTime} ms, ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                profileName.text = "User"
                profileImage.setImageResource(R.drawable.default_profile)
                ivReportBanner.visibility = View.GONE // Hide on error
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
            5000, // Initial timeout in milliseconds
            1,    // Max retries
            1.0f  // Backoff multiplier
        )
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

    private fun logoutUser() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Logging out...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val request = okhttp3.Request.Builder()
            .url("https://bioview.sahans.web.lk/app/logout.php")
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