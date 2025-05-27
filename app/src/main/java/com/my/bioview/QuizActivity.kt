package com.my.bioview

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.KeyEvent
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.volley.Cache

class QuizActivity : AppCompatActivity() {

    private lateinit var rvQuizList: RecyclerView
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 10000L // 10 seconds in milliseconds
    private var isRefreshing = false
    private var quizAdapter: QuizAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button click listener
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Bottom menu click listeners
        val itemHome = findViewById<LinearLayout>(R.id.itemHome)
        val item3D = findViewById<LinearLayout>(R.id.item3D)
        val itemProfile = findViewById<LinearLayout>(R.id.itemProfile)

        itemHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        item3D.setOnClickListener {
            // Already on 3D screen (no action needed or highlight toggle)
        }

        itemProfile.setOnClickListener {
            val intent = Intent(this, LogoutActivity2::class.java)
            startActivity(intent)
        }

        // Set up RecyclerView for quiz list
        rvQuizList = findViewById(R.id.rvQuizList)
        rvQuizList.layoutManager = LinearLayoutManager(this)
        quizAdapter = QuizAdapter()
        rvQuizList.adapter = quizAdapter

        // Initial fetch and start auto-refresh
        fetchQuizzes()
        startAutoRefresh()
    }

    private fun fetchQuizzes() {
        if (isRefreshing) {
            Log.d("QuizActivity", "Fetch skipped: Already refreshing")
            return
        }

        isRefreshing = true
        Log.d("QuizActivity", "Fetching quizzes at ${System.currentTimeMillis()}")

        // Add timestamp to URL to bypass server-side caching
        val url = "https://bioview.sahans.online/app/get_quizzes.php?t=${System.currentTimeMillis()}"

        val stringRequest = object : StringRequest(
            Method.GET, url,
            { response ->
                Log.d("QuizActivity", "Response received: $response")
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        val quizList = jsonResponse.getJSONArray("data")
                        val quizzes = mutableListOf<Quiz>()
                        for (i in 0 until quizList.length()) {
                            val quizObj = quizList.getJSONObject(i)
                            quizzes.add(Quiz(
                                quizObj.getInt("quiz_id"),
                                quizObj.getInt("model_id"),
                                quizObj.getString("title")
                            ))
                        }
                        Log.d("QuizActivity", "Parsed quizzes: $quizzes")
                        quizAdapter?.updateQuizzes(quizzes)
                    } else {
                        Log.e("QuizActivity", "Failed to fetch quizzes: ${jsonResponse.getString("message")}")
                    }
                } catch (e: Exception) {
                    Log.e("QuizActivity", "Error parsing response: ${e.message}")
                } finally {
                    isRefreshing = false
                }
            },
            { error ->
                Log.e("QuizActivity", "Error fetching quizzes: ${error.message}")
                isRefreshing = false
            }
        ) {
            override fun getCacheEntry(): Cache.Entry? {
                return null // Disable caching
            }
        }

        stringRequest.tag = "QUIZ_REQUEST"
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun startAutoRefresh() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isFinishing && !isDestroyed) {
                    Log.d("QuizActivity", "Auto-refresh triggered")
                    fetchQuizzes()
                    handler.postDelayed(this, refreshInterval)
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null) // Stop auto-refresh when paused
    }

    override fun onResume() {
        super.onResume()
        startAutoRefresh() // Restart auto-refresh when resumed
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Clean up handler
        Volley.newRequestQueue(this).cancelAll("QUIZ_REQUEST") // Cancel Volley requests
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

// Data class for Quiz
data class Quiz(val quizId: Int, val modelId: Int, val title: String)

// DiffUtil Callback for Quiz items
class QuizDiffCallback(
    private val oldList: List<Quiz>,
    private val newList: List<Quiz>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].quizId == newList[newItemPosition].quizId
    }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

// Adapter for RecyclerView with DiffUtil
class QuizAdapter : RecyclerView.Adapter<QuizAdapter.QuizViewHolder>() {

    private var quizList: List<Quiz> = emptyList()

    class QuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuizTitle: TextView = itemView.findViewById(R.id.tvQuizTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quiz, parent, false)
        return QuizViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        val quiz = quizList[position]
        holder.tvQuizTitle.text = quiz.title
    }

    override fun getItemCount(): Int = quizList.size

    fun updateQuizzes(newQuizzes: List<Quiz>) {
        val diffCallback = QuizDiffCallback(quizList, newQuizzes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        quizList = newQuizzes
        diffResult.dispatchUpdatesTo(this)
        Log.d("QuizActivity", "Quiz list updated with ${newQuizzes.size} items")
    }
}