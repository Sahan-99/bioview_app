package com.my.bioview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat

class HistoryAdapter(private val historyList: List<QuizAttempt>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuizName: TextView = itemView.findViewById(R.id.tvQuizName)
        val tvAttemptTime: TextView = itemView.findViewById(R.id.tvAttemptTime)
        val tvTotalQuestions: TextView = itemView.findViewById(R.id.tvTotalQuestions)
        val tvCorrectAnswers: TextView = itemView.findViewById(R.id.tvCorrectAnswers)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val cardView: CardView = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        Log.d("HistoryAdapter", "Inflated view: $view")
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val attempt = historyList[position]
        holder.tvQuizName.text = attempt.title
        holder.tvAttemptTime.text = attempt.attemptTime
        holder.tvTotalQuestions.text = "Total Questions: ${attempt.totalQuestions}"
        holder.tvCorrectAnswers.text = "Correct Answers: ${attempt.correctAnswers}"
        holder.tvPercentage.text = String.format("%.1f%%", attempt.percentage)
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, if (position % 2 == 0) R.color.light_gray else R.color.white))
    }

    override fun getItemCount(): Int = historyList.size
}