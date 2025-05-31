package com.my.bioview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var btnQuiz: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        tvGreeting = findViewById(R.id.tvGreeting)
        ivProfile = findViewById(R.id.ivProfile)
        btnQuiz = findViewById(R.id.btnQuiz)
        progressBar = findViewById(R.id.progressBar)

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

        btnQuiz.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java))
        }

        val features = listOf(
            Pair(R.id.feature1, Pair("AR MODEL\nVISUALIZATION", R.drawable.ic_user)),
            Pair(R.id.feature2, Pair("INTERACTIVE\n3D MODELS", R.drawable.ic_3d)),
            Pair(R.id.feature3, Pair("VOICE\nEXPLORATIONS", R.drawable.ic_home)),
            Pair(R.id.feature4, Pair("QUIZZES &\nCHALLENGES", R.drawable.ic_user)),
            Pair(R.id.feature5, Pair("LEARNING\nREPORT", R.drawable.ic_home)),
            Pair(R.id.feature6, Pair("USER-FRIENDLY\nINTERFACE", R.drawable.ic_3d))
        )

        // Set icons and names for each feature
        for (feature in features) {
            val view = findViewById<LinearLayout>(feature.first)
            val icon = view.findViewById<ImageView>(R.id.feature_icon)
            val name = view.findViewById<TextView>(R.id.feature_name)

            icon.setImageResource(feature.second.second)
            name.text = feature.second.first
        }

        // Check login state
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Fetch user data
        fetchUserData()

        // Set up search functionality (placeholder)
        findViewById<EditText>(R.id.etSearch).setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = findViewById<EditText>(R.id.etSearch).text.toString()
                // Handle search query
                true
            } else {
                false
            }
        }
    }

    private fun fetchUserData() {
        val url = "https://bioview.sahans.online/app/get_user.php"

        // Show ProgressBar
        progressBar.visibility = View.VISIBLE

        // Get session ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)

        val stringRequest = object : StringRequest(
            Method.GET, url,
            { response ->
                // Hide ProgressBar
                progressBar.visibility = View.GONE

                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        val firstName = jsonResponse.getString("first_name")
                        val profilePicture = jsonResponse.getString("profile_picture")
                        Log.d("MainActivity", "Profile Picture URL: $profilePicture")
                        tvGreeting.text = "Hello, $firstName!"
                        if (profilePicture.isNotEmpty()) {
                            Glide.with(this)
                                .load(profilePicture)
                                .placeholder(R.drawable.default_profile) // Shown while loading
                                .error(R.drawable.default_profile) // Shown if load fails
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Log.e("MainActivity", "Glide failed to load image: ${e?.message}")
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
                                        Log.d("MainActivity", "Glide loaded image successfully")
                                        return false
                                    }
                                })
                                .into(ivProfile)
                        } else {
                            Log.d("MainActivity", "Profile picture is empty, using default")
                            ivProfile.setImageResource(R.drawable.default_profile)
                        }
                    } else {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        // Clear login state and redirect to SignInActivity
                        with(sharedPref.edit()) {
                            putBoolean("is_logged_in", false)
                            putString("session_id", null)
                            apply()
                        }
                        val intent = Intent(this, SignInActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing user data: ${e.message}")
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
                    tvGreeting.text = "Hello, User!"
                    ivProfile.setImageResource(R.drawable.default_profile)
                }
            },
            { error ->
                // Hide ProgressBar on error
                progressBar.visibility = View.GONE

                Log.e("MainActivity", "Network error: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                tvGreeting.text = "Hello, User!"
                ivProfile.setImageResource(R.drawable.default_profile)
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

        Volley.newRequestQueue(this).add(stringRequest)
    }
}