package com.my.bioview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private var userId: Int = -1
    private val historyList = mutableListOf<QuizAttempt>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

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

        rvHistory = findViewById(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = HistoryAdapter(historyList)

        // Check login status
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            Log.w("HistoryActivity", "User not logged in, redirecting to SignInActivity")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) {
            Log.w("HistoryActivity", "Invalid userId, redirecting to SignInActivity")
            Toast.makeText(this, "Invalid user session, log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        fetchHistory()
    }

    private fun fetchHistory() {
        val url = "https://bioview.sahans.web.lk/app/get_quiz_history.php?user_id=$userId"
        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("HistoryActivity", "Raw response: $response")
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        val attemptsArray = jsonResponse.getJSONArray("data")
                        historyList.clear()
                        for (i in 0 until attemptsArray.length()) {
                            val attemptObj = attemptsArray.getJSONObject(i)
                            historyList.add(QuizAttempt(
                                attemptObj.getInt("quiz_id"),
                                attemptObj.getString("title"),
                                attemptObj.getString("attempt_time"),
                                attemptObj.getDouble("percentage"),
                                attemptObj.getInt("total_questions"),
                                attemptObj.getInt("correct_answers")
                            ))
                        }
                        rvHistory.adapter?.notifyDataSetChanged()
                        Log.d("HistoryActivity", "Loaded ${historyList.size} attempts")
                    } else {
                        Log.e("HistoryActivity", "Failed to fetch history: ${jsonResponse.getString("message")}")
                        Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("HistoryActivity", "Error parsing history response: ${e.message}, Response: $response")
                    Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("HistoryActivity", "Network error fetching history: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cookie"] = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("session_id", null) ?: ""
                return headers
            }
        }

        stringRequest.tag = "HISTORY_REQUEST"
        Volley.newRequestQueue(this).add(stringRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        Volley.newRequestQueue(this).cancelAll("HISTORY_REQUEST")
    }
}