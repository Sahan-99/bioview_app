package com.my.bioview

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class QuizActivity : AppCompatActivity() {

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
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        item3D.setOnClickListener {
            // Already on 3D screen (no action needed or highlight toggle)
        }

        itemProfile.setOnClickListener {
            // Navigate to Profile Activity (placeholder)
        }

        // Set up RecyclerView for quiz list
        val rvQuizList = findViewById<RecyclerView>(R.id.rvQuizList)
        rvQuizList.layoutManager = LinearLayoutManager(this)

        // Fetch quizzes from server
        fetchQuizzes(rvQuizList)
    }

    private fun fetchQuizzes(recyclerView: RecyclerView) {
        val url = "https://bioview.sahans.online/app/get_quizzes.php" // Replace with your server URL

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
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
                        recyclerView.adapter = QuizAdapter(quizzes)
                    } else {
                        Log.e("QuizActivity", "Failed to fetch quizzes: ${jsonResponse.getString("message")}")
                    }
                } catch (e: Exception) {
                    Log.e("QuizActivity", "Error parsing response: ${e.message}")
                }
            },
            { error ->
                Log.e("QuizActivity", "Error fetching quizzes: ${error.message}")
            }
        )

        Volley.newRequestQueue(this).add(stringRequest)
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

// Adapter for RecyclerView
class QuizAdapter(private val quizList: List<Quiz>) : RecyclerView.Adapter<QuizAdapter.QuizViewHolder>() {

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
}