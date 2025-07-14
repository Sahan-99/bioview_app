package com.my.bioview

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.HashMap
import java.util.Locale

class ResultActivity : AppCompatActivity() {

    private lateinit var progressResult: CircularProgressIndicator
    private lateinit var tvPercentage: TextView
    private lateinit var llStats: LinearLayout
    private lateinit var btnFinish: Button
    private lateinit var tvIncorrectAnswers: TextView
    private lateinit var progressDialog: ProgressDialog
    private var totalQuestions = 0
    private var correctAnswers = 0
    private var quizId = -1
    private var userId = -1
    private lateinit var quizName: String
    private val incorrectAnswers = mutableListOf<IncorrectAnswer>() // List of incorrect answers with question text

    data class IncorrectAnswer(
        val questionId: Int,
        val questionText: String,
        val userAnswer: String,
        val correctAnswer: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        progressResult = findViewById(R.id.progressResult)
        tvPercentage = findViewById(R.id.tvPercentage)
        llStats = findViewById(R.id.llStats)
        btnFinish = findViewById(R.id.btnFinish)
        tvIncorrectAnswers = findViewById(R.id.tvIncorrectAnswers)
        progressDialog = ProgressDialog(this).apply {
            setMessage("Saving results...")
            setCancelable(false)
        }

        // Retrieve intent extras with validation
        quizId = intent.getIntExtra("quiz_id", -1)
        userId = intent.getIntExtra("user_id", -1)
        val questions = intent.getIntegerArrayListExtra("questions") ?: arrayListOf()
        val userSelections = intent.getSerializableExtra("user_selections") as? Map<Int, Int> ?: emptyMap()
        quizName = intent.getStringExtra("quiz_name") ?: "Unknown Quiz"

        // Check login status
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            Log.w("ResultActivity", "User not logged in, redirecting to SignInActivity")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        Log.d("ResultActivity", "Received - quizId: $quizId, userId: $userId, questions: $questions, selections: $userSelections, quizName: $quizName")

        if (quizId == -1 || questions.isEmpty()) {
            Log.e("ResultActivity", "Invalid data - quizId: $quizId, questions size: ${questions.size}")
            Toast.makeText(this, "Error: Missing required quiz data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        totalQuestions = questions.size
        calculateCorrectAnswers(questions, userSelections) { correct ->
            correctAnswers = correct
            val incorrectCount = totalQuestions - correct
            tvIncorrectAnswers.text = "Incorrect Answers: $incorrectCount"
            val percentage = if (totalQuestions > 0) (correctAnswers.toDouble() / totalQuestions * 100).toInt() else 0
            updateUI(totalQuestions, correctAnswers, percentage)
            saveResult(quizId, userId, correctAnswers, totalQuestions, percentage)
        }

        // Set quiz name on result page
        findViewById<TextView>(R.id.tvQuizName)?.text = "Result for $quizName"

        val btnShare = findViewById<Button>(R.id.btnShare)
        btnShare.setOnClickListener {
            showSharePopup()
        }

        btnFinish.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        tvIncorrectAnswers.setOnClickListener {
            showIncorrectAnswersDialog()
        }
    }

    fun onIncorrectAnswersClick(view: View) {
        showIncorrectAnswersDialog()
    }

    private fun showSharePopup() {
        val dialogView = layoutInflater.inflate(R.layout.popup_share, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imageShare)
        val shareButton = dialogView.findViewById<Button>(R.id.btnShareImage)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        shareButton.setOnClickListener {
            shareDrawableImage(R.drawable.quiz_share)
        }

        dialog.show()
    }

    private fun shareDrawableImage(drawableResId: Int) {
        val drawable = ContextCompat.getDrawable(this, drawableResId) ?: return
        val bitmap = (drawable as android.graphics.drawable.BitmapDrawable).bitmap

        // Save to cache
        val cachePath = File(cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "share_image.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun calculateCorrectAnswers(questions: List<Int>, userSelections: Map<Int, Int>, callback: (Int) -> Unit) {
        var correct = 0
        var completed = 0
        val totalRequests = questions.size

        questions.forEach { questionId ->
            val selectedAnswer = userSelections[questionId]
            if (selectedAnswer != null) {
                val questionUrl = "https://bioview.sahans.web.lk/app/get_questions.php?quiz_id=$quizId"
                Volley.newRequestQueue(this).add(object : StringRequest(
                    Request.Method.GET, questionUrl,
                    { response ->
                        try {
                            val jsonResponse = JSONObject(response)
                            if (jsonResponse.getString("status") == "success") {
                                val questionsArray = jsonResponse.getJSONArray("data")
                                var questionText = "Question $questionId" // Fallback
                                for (i in 0 until questionsArray.length()) {
                                    val questionObj = questionsArray.getJSONObject(i)
                                    if (questionObj.getInt("question_id") == questionId) {
                                        questionText = questionObj.getString("question_text")
                                        break
                                    }
                                }

                                val answerUrl = "https://bioview.sahans.web.lk/app/get_answers.php?question_id=$questionId"
                                Volley.newRequestQueue(this).add(object : StringRequest(
                                    Request.Method.GET, answerUrl,
                                    { answerResponse ->
                                        try {
                                            val answerJson = JSONObject(answerResponse)
                                            if (answerJson.getString("status") == "success") {
                                                val answersArray = answerJson.getJSONArray("data")
                                                var correctAnswerText = ""
                                                var userAnswerText = ""
                                                for (i in 0 until answersArray.length()) {
                                                    val answerObj = answersArray.getJSONObject(i)
                                                    val answerId = answerObj.getInt("answer_id")
                                                    val answerText = answerObj.getString("answer_text")
                                                    if (answerObj.getInt("is_correct") == 1) {
                                                        correctAnswerText = answerText
                                                    }
                                                    if (answerId == selectedAnswer) {
                                                        userAnswerText = answerText
                                                    }
                                                }
                                                if (correctAnswerText.isNotEmpty() && userAnswerText.isNotEmpty() && correctAnswerText != userAnswerText) {
                                                    incorrectAnswers.add(IncorrectAnswer(questionId, questionText, userAnswerText, correctAnswerText))
                                                } else if (correctAnswerText == userAnswerText) {
                                                    correct++
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ResultActivity", "Error fetching answers for $questionId: ${e.message}")
                                        } finally {
                                            completed++
                                            if (completed == totalRequests) callback(correct)
                                        }
                                    },
                                    { error ->
                                        Log.e("ResultActivity", "Network error for answers $questionId: ${error.message}")
                                        completed++
                                        if (completed == totalRequests) callback(correct)
                                    }
                                ) {
                                    override fun getHeaders(): MutableMap<String, String> {
                                        val headers = HashMap<String, String>()
                                        headers["Cookie"] = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("session_id", null) ?: ""
                                        return headers
                                    }
                                })
                            }
                        } catch (e: Exception) {
                            Log.e("ResultActivity", "Error fetching questions for $quizId: ${e.message}")
                            completed++
                            if (completed == totalRequests) callback(correct)
                        }
                    },
                    { error ->
                        Log.e("ResultActivity", "Network error fetching questions for $quizId: ${error.message}")
                        completed++
                        if (completed == totalRequests) callback(correct)
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Cookie"] = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("session_id", null) ?: ""
                        return headers
                    }
                })
            } else {
                completed++
                if (completed == totalRequests) callback(correct)
            }
        }
    }

    private fun updateUI(total: Int, correct: Int, percentage: Int) {
        progressResult.progress = percentage.coerceIn(0, 100)
        tvPercentage.text = "$percentage%"
        llStats.findViewById<TextView>(R.id.tvTotalQuestions)?.text = "Total Questions: $total"
        llStats.findViewById<TextView>(R.id.tvCorrectAnswers)?.text = "Correct Answers: $correct"
        Log.d("ResultActivity", "UI updated - Total: $total, Correct: $correct, Percentage: $percentage")
    }

    private fun saveResult(quizId: Int, userId: Int, correctAnswers: Int, totalQuestions: Int, percentage: Int) {
        progressDialog.show()
        val url = "https://bioview.sahans.web.lk/app/save_quiz_attempt.php"
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val attemptTime = sdf.format(System.currentTimeMillis())
        Log.d("ResultActivity", "Generated attempt time: $attemptTime")

        val stringRequest = object : StringRequest(
            Method.POST, url,
            { response ->
                progressDialog.dismiss()
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        Log.d("ResultActivity", "Result saved successfully")
                        Toast.makeText(this, "Results saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ResultActivity", "Save failed: ${jsonResponse.getString("message")}")
                        Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ResultActivity", "Error parsing response: ${e.message}, Response: $response")
                    Toast.makeText(this, "Error saving results", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressDialog.dismiss()
                Log.e("ResultActivity", "Network error: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId.toString()
                params["quiz_id"] = quizId.toString()
                params["score"] = correctAnswers.toString()
                params["total_questions"] = totalQuestions.toString()
                params["correct_answers"] = correctAnswers.toString()
                params["percentage"] = percentage.toString()
                params["attempt_time"] = attemptTime
                Log.d("ResultActivity", "Sending params: $params")
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (sessionId != null) headers["Cookie"] = sessionId
                return headers
            }
        }

        stringRequest.tag = "RESULT_REQUEST"
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun showIncorrectAnswersDialog() {
        if (incorrectAnswers.isEmpty()) {
            Toast.makeText(this, "No incorrect answers to display", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_incorrect_answers, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvIncorrectAnswers)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDialog)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = IncorrectAnswerAdapter(incorrectAnswers)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private class IncorrectAnswerAdapter(private val incorrectAnswers: List<IncorrectAnswer>) :
        RecyclerView.Adapter<IncorrectAnswerAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvQuestionId: TextView = itemView.findViewById(R.id.tvQuestionId)
            val tvQuestionText: TextView = itemView.findViewById(R.id.tvQuestionText)
            val tvUserAnswer: TextView = itemView.findViewById(R.id.tvUserAnswer)
            val tvCorrectAnswer: TextView = itemView.findViewById(R.id.tvCorrectAnswer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_incorrect_answer, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val answer = incorrectAnswers[position]
            holder.tvQuestionId.text = "Question ID: ${answer.questionId}"
            holder.tvQuestionText.text = answer.questionText
            holder.tvUserAnswer.text = "Your Answer: ${answer.userAnswer}"
            holder.tvCorrectAnswer.text = "Correct Answer: ${answer.correctAnswer}"
        }

        override fun getItemCount(): Int = incorrectAnswers.size
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::progressDialog.isInitialized && progressDialog.isShowing) progressDialog.dismiss()
        Volley.newRequestQueue(this).cancelAll("RESULT_REQUEST")
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