package com.my.bioview

import android.app.ProgressDialog
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var progressDialog: ProgressDialog
    private val features = listOf(
        Pair(R.id.feature1, Pair("AR Model\nVisualization", R.drawable.ic_ar)),
        Pair(R.id.feature2, Pair("Interactive\n3D Models", R.drawable.ic_3d_model)),
        Pair(R.id.feature3, Pair("Voice\nExplanations", R.drawable.ic_voice)),
        Pair(R.id.feature4, Pair("Quizzes &\nChallenges", R.drawable.ic_quiz)),
        Pair(R.id.feature5, Pair("Learning\nReport", R.drawable.ic_report)),
        Pair(R.id.feature6, Pair("User-Friendly\nInterface", R.drawable.ic_ui))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        tvGreeting = findViewById(R.id.tvGreeting)
        ivProfile = findViewById(R.id.ivProfile)

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

        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val quizBanner = findViewById<ImageView>(R.id.ivQuizBanner)
        quizBanner.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            startActivity(intent)
        }

        // Set icons, names, and click listeners for each feature
        for (feature in features) {
            val view = findViewById<LinearLayout>(feature.first)
            val icon = view.findViewById<ImageView>(R.id.feature_icon)
            val name = view.findViewById<TextView>(R.id.feature_name)

            icon.setImageResource(feature.second.second)
            name.text = feature.second.first

            // Set click listener for popup
            view.setOnClickListener {
                showFeaturePopup(feature.second.first, feature.second.second)
            }
        }

        // Check login state
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading user data...")
        progressDialog.setCancelable(false)

        // Fetch user data
        fetchUserData()

        // Set up search functionality
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = etSearch.text.toString().trim().toLowerCase()
                performSearch(query)
                true
            } else {
                false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        logAppUsage()
    }

    private fun logAppUsage() {
        val url = "https://bioview.sahans.web.lk/app/log_usage.php"

        val request = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                Log.d("UsageLog", "Response: '${response.trim()}'")
            },
            Response.ErrorListener { error ->
                Log.e("UsageLog", "Volley Error: ${error.message}")
                error.networkResponse?.let {
                    val responseData = String(it.data)
                    Log.e("UsageLog", "Volley Error Body: $responseData")
                }
            }) {}

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchUserData() {
        val url = "https://bioview.sahans.web.lk/app/get_user.php"

        progressDialog.show()

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)

        val startTime = System.currentTimeMillis()
        val stringRequest = object : StringRequest(
            Method.GET, url,
            { response ->
                val endTime = System.currentTimeMillis()
                Log.d("MainActivity", "Fetch time: ${endTime - startTime} ms")

                progressDialog.dismiss()

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
                                .override(100, 100)
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
                progressDialog.dismiss()

                val endTime = System.currentTimeMillis()
                Log.e("MainActivity", "Fetch error time: ${endTime - startTime} ms, ${error.message}")
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

        stringRequest.retryPolicy = DefaultRetryPolicy(
            5000,
            1,
            1.0f
        )
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun performSearch(query: String) {
        Log.d("MainActivity", "Searching for: $query")
        if (query.isEmpty()) {
            for (feature in features) {
                findViewById<LinearLayout>(feature.first).visibility = View.VISIBLE
            }
            return
        }

        for (feature in features) {
            findViewById<LinearLayout>(feature.first).visibility = View.GONE
        }

        for (feature in features) {
            val featureName = feature.second.first.toLowerCase()
            if (featureName.contains(query)) {
                findViewById<LinearLayout>(feature.first).visibility = View.VISIBLE
                Log.d("MainActivity", "Showing feature: ${feature.second.first}")
            }
        }

        if (features.none { findViewById<LinearLayout>(it.first).visibility == View.VISIBLE }) {
            Toast.makeText(this, "No matching features found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFeaturePopup(featureName: String, featureIconRes: Int) {
        val explanation = when (featureName) {
            "AR Model\nVisualization" -> "Explore realistic 3D augmented reality models of biological systems by scanning textbook images for an immersive learning experience with BioView."
            "Interactive\n3D Models" -> "Rotate, zoom, and interact with detailed biological structures in 3D to deepen your understanding through an engaging, hands-on approach."
            "Voice\nExplanations" -> "Tap annotation buttons to hear clear voice explanations that help students grasp complex biological concepts with ease and clarity."
            "Quizzes &\nChallenges" -> "After exploring models, challenge yourself with interactive quizzes that reinforce learning and boost your biology knowledge in a fun way."
            "Learning\nReport" -> "Monitor your learning journey with comprehensive reports that highlight completed topics, quiz scores, and areas that need improvement."
            "User-Friendly\nInterface" -> "Navigate effortlessly through BioView's clean and user-friendly design, created specifically for students to learn quickly and comfortably."
            else -> "More information coming soon!"
        }

        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.custom_popup_feature, null)
        val tvPopupTitle = dialogView.findViewById<TextView>(R.id.tvPopupTitle)
        val tvPopupExplanation = dialogView.findViewById<TextView>(R.id.tvPopupExplanation)
        val btnClosePopup = dialogView.findViewById<Button>(R.id.btnClosePopup)

        // Set data
        tvPopupTitle.text = featureName
        tvPopupExplanation.text = explanation

        // Create and configure the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set close button action
        btnClosePopup.setOnClickListener { dialog.dismiss() }

        // Apply custom animations
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
}