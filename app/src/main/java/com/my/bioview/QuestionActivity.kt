package com.my.bioview

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.RequiresPermission

class QuestionActivity : AppCompatActivity() {

    private lateinit var tvProgress: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var rvAnswers: RecyclerView
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var progressBar: ProgressBar
    private var questions = mutableListOf<Question>()
    private var currentQuestionIndex = 0
    private val userSelections = mutableMapOf<Int, Int>()
    private var quizId: Int = -1
    private var userId: Int = -1
    private lateinit var quizName: String

    private val CHANNEL_ID = "quiz_channel"
    private val NOTIFICATION_ID = 2

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Retrieve quizId, userId, and quizName from Intent
        quizId = intent?.getIntExtra("quiz_id", -1) ?: -1
        userId = intent?.getIntExtra("user_id", -1) ?: -1
        quizName = intent?.getStringExtra("quiz_name") ?: "Unknown Quiz"
        Log.d("QuestionActivity", "onCreate started with quizId: $quizId, userId: $userId, quizName: $quizName")

        if (quizId == -1) {
            Log.e("QuestionActivity", "quiz_id not received, defaulting to 1")
            Toast.makeText(this, "Error: Quiz ID not found", Toast.LENGTH_SHORT).show()
            quizId = 1
        }

        // Check login status
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            Log.w("QuestionActivity", "User not logged in, redirecting to SignInActivity")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        if (userId == -1) {
            Log.w("QuestionActivity", "Invalid userId, redirecting to SignInActivity")
            Toast.makeText(this, "Invalid user session, log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Apply fade-in animation
        ViewCompat.animate(findViewById(R.id.main))
            .alpha(0f)
            .setDuration(0)
            .withEndAction {
                ViewCompat.animate(findViewById(R.id.main))
                    .alpha(1f)
                    .setDuration(500)
                    .start()
            }
            .start()

        // Initialize views
        tvProgress = findViewById(R.id.tvProgress)
        tvQuestion = findViewById(R.id.tvQuestion)
        rvAnswers = findViewById(R.id.rvAnswers)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        progressBar = findViewById(R.id.progressBar)

        rvAnswers.layoutManager = LinearLayoutManager(this)
        rvAnswers.adapter = AnswerAdapter(this) { answerId ->
            userSelections[questions[currentQuestionIndex].questionId] = answerId
            Log.d("QuestionActivity", "Selected answer ID: $answerId for question ${questions[currentQuestionIndex].questionId}")
            (rvAnswers.adapter as AnswerAdapter).notifyDataSetChanged()
        }

        // Show loading
        showLoading(true)

        // Fetch questions
        fetchQuestions()

        // Button listeners
        btnPrevious.setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                displayQuestion()
                Log.d("QuestionActivity", "Moved to previous question: $currentQuestionIndex")
            }
        }

        btnNext.setOnClickListener {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                displayQuestion()
                Log.d("QuestionActivity", "Moved to next question: $currentQuestionIndex")
            } else {
                if (questions.isEmpty()) {
                    Log.e("QuestionActivity", "No questions available to submit")
                    Toast.makeText(this, "No questions to submit", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val questionIds = questions.map { it.questionId }
                if (questionIds.isEmpty()) {
                    Log.e("QuestionActivity", "Question IDs list is empty")
                    Toast.makeText(this, "Error: No question data", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                showQuizCompletedNotification()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("quiz_id", quizId)
                    putIntegerArrayListExtra("questions", ArrayList(questionIds))
                    putExtra("user_selections", userSelections as java.io.Serializable)
                    putExtra("user_id", userId)
                    putExtra("quiz_name", quizName) // Pass quiz name
                    Log.d("QuestionActivity", "Passing quizId: $quizId, questions: $questionIds, selections: $userSelections, userId: $userId, quizName: $quizName")
                }
                try {
                    startActivity(intent)
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.fade_out)
                } catch (e: Exception) {
                    Log.e("QuestionActivity", "Failed to start ResultActivity: ${e.message}")
                    Toast.makeText(this, "Error navigating to results", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchQuestions() {
        Log.d("QuestionActivity", "Fetching questions for quizId: $quizId")
        val url = "https://bioview.sahans.web.lk/app/get_questions.php?quiz_id=$quizId"

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("QuestionActivity", "Questions response: $response")
                showLoading(false)
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        val questionsArray = jsonResponse.getJSONArray("data")
                        questions.clear()
                        for (i in 0 until minOf(10, questionsArray.length())) {
                            val questionObj = questionsArray.getJSONObject(i)
                            val question = Question(
                                questionObj.getInt("question_id"),
                                questionObj.getInt("quiz_id"),
                                questionObj.getString("question_text")
                            )
                            questions.add(question)
                        }
                        if (questions.isNotEmpty()) {
                            displayQuestion()
                            Log.d("QuestionActivity", "Loaded ${questions.size} questions for quizId: ${questions[0].quizId}")
                        } else {
                            Log.e("QuestionActivity", "No questions found")
                            Toast.makeText(this, "No questions available", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("QuestionActivity", "Failed to fetch questions: ${jsonResponse.getString("message")}")
                        Toast.makeText(this, "Failed to load questions", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("QuestionActivity", "Error parsing questions response: ${e.message}")
                    Toast.makeText(this, "Error loading questions", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("QuestionActivity", "Network error fetching questions: ${error.message}")
                showLoading(false)
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cookie"] = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("session_id", null) ?: ""
                return headers
            }
        }

        stringRequest.tag = "QUESTION_REQUEST"
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun displayQuestion() {
        Log.d("QuestionActivity", "Displaying question at index: $currentQuestionIndex")
        if (questions.isNotEmpty() && currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            tvProgress.text = "Question ${currentQuestionIndex + 1} of ${minOf(10, questions.size)}"
            tvQuestion.text = question.questionText
            fetchAnswers(question.questionId)

            // Update Next button text
            btnNext.text = if (currentQuestionIndex == questions.size - 1) "Submit" else "Next"
        } else {
            Log.e("QuestionActivity", "No question to display at index $currentQuestionIndex")
            Toast.makeText(this, "No question available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAnswers(questionId: Int) {
        Log.d("QuestionActivity", "Fetching answers for questionId: $questionId")
        val url = "https://bioview.sahans.web.lk/app/get_answers.php?question_id=$questionId"

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("QuestionActivity", "Answers response: $response")
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        val answersArray = jsonResponse.getJSONArray("data")
                        val answers = mutableListOf<Answer>()
                        for (i in 0 until answersArray.length()) {
                            val answerObj = answersArray.getJSONObject(i)
                            answers.add(Answer(
                                answerObj.getInt("answer_id"),
                                answerObj.getInt("question_id"),
                                answerObj.getString("answer_text"),
                                answerObj.getInt("is_correct") == 1
                            ))
                        }
                        if (answers.size >= 4) {
                            (rvAnswers.adapter as AnswerAdapter).updateAnswers(answers, userSelections[questionId])
                            Log.d("QuestionActivity", "Loaded ${answers.size} answers")
                        } else {
                            Log.e("QuestionActivity", "Insufficient answers: ${answers.size}")
                            Toast.makeText(this, "Incomplete answer set", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("QuestionActivity", "Failed to fetch answers: ${jsonResponse.getString("message")}")
                        Toast.makeText(this, "Failed to load answers", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("QuestionActivity", "Error parsing answers response: ${e.message}")
                    Toast.makeText(this, "Error loading answers", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("QuestionActivity", "Network error fetching answers: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cookie"] = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("session_id", null) ?: ""
                return headers
            }
        }

        stringRequest.tag = "ANSWER_REQUEST"
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        tvProgress.visibility = if (show) View.GONE else View.VISIBLE
        tvQuestion.visibility = if (show) View.GONE else View.VISIBLE
        rvAnswers.visibility = if (show) View.GONE else View.VISIBLE
        btnPrevious.visibility = if (show) View.GONE else View.VISIBLE
        btnNext.visibility = if (show) View.GONE else View.VISIBLE
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showQuizCompletedNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quiz Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // Replace with your notification icon
            .setContentTitle("Quiz Completed")
            .setContentText("You have completed the $quizName quiz!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        Volley.newRequestQueue(this).cancelAll("QUESTION_REQUEST")
        Volley.newRequestQueue(this).cancelAll("ANSWER_REQUEST")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ViewCompat.animate(findViewById(R.id.main))
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                .start()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

data class Question(val questionId: Int, val quizId: Int, val questionText: String)
data class Answer(val answerId: Int, val questionId: Int, val answerText: String, val isCorrect: Boolean)

class AnswerAdapter(
    private val context: Context,
    private val onAnswerSelected: (Int) -> Unit
) : RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder>() {

    private var answers = mutableListOf<Answer>()
    private var selectedAnswerId: Int? = null

    class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOptionLetter: TextView = itemView.findViewById(R.id.tvOptionLetter)
        val tvAnswerText: TextView = itemView.findViewById(R.id.tvAnswerText)
        val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.answer_item, parent, false)
        return AnswerViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val answer = answers[position]
        val label = when (position) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            else -> ""
        }
        holder.tvOptionLetter.text = label
        holder.tvAnswerText.text = answer.answerText

        val isSelected = answer.answerId == selectedAnswerId
        holder.cardView.setCardBackgroundColor(
            if (isSelected)
                ContextCompat.getColor(context, R.color.primary_blue)
            else
                ContextCompat.getColor(context, R.color.light_gray)
        )
        holder.tvAnswerText.setTextColor(
            if (isSelected)
                ContextCompat.getColor(context, android.R.color.white)
            else
                ContextCompat.getColor(context, android.R.color.black)
        )
        holder.tvOptionLetter.setTextColor(
            if (isSelected)
                ContextCompat.getColor(context, R.color.primary_blue)
            else
                ContextCompat.getColor(context, R.color.white)
        )
        holder.tvOptionLetter.background = if (isSelected)
            ContextCompat.getDrawable(context, R.drawable.indicator_unselectedw)
        else
            ContextCompat.getDrawable(context, R.drawable.indicator_selected)

        holder.itemView.setOnClickListener {
            selectedAnswerId = answer.answerId
            onAnswerSelected(answer.answerId)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = answers.size

    fun updateAnswers(newAnswers: List<Answer>, selectedId: Int? = null) {
        answers.clear()
        answers.addAll(newAnswers)
        selectedAnswerId = selectedId
        notifyDataSetChanged()
    }
}