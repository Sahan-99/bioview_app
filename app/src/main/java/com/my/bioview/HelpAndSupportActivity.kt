package com.my.bioview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

class HelpAndSupportActivity : AppCompatActivity() {

    private lateinit var expandableListView: ExpandableListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_and_support)

        // Status bar color and icon visibility
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Back button functionality
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Bottom navigation
        val itemHome = findViewById<LinearLayout>(R.id.itemHome)
        val item3D = findViewById<LinearLayout>(R.id.item3D)
        val itemProfile = findViewById<LinearLayout>(R.id.itemProfile)

        itemHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        item3D.setOnClickListener {
            // Already on 3D screen
        }

        itemProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Initialize FAQ section
        expandableListView = findViewById(R.id.expandableListView)

        val faqData = mapOf(
            "How do I scan an image for AR models?" to "Tap the 'Scan AR Model' button on the home screen and point your camera at a supported image.",
            "How do I listen to voice explanations?" to "After scanning an AR model, tap the 'Play Audio' button to hear a detailed explanation.",
            "Can I interact with the 3D models?" to "Yes! You can zoom, rotate, and move the models using touch gestures.",
            "Where can I find saved models?" to "Go to the 'My Library' section to view all previously scanned models.",
            "Do I need an internet connection to use the app?" to "An internet connection is required for initial loading and updates of AR models."
        )

        expandableListView.setAdapter(FAQAdapter(faqData))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class FAQAdapter(private val faqData: Map<String, String>) : BaseExpandableListAdapter() {

        private val questions = faqData.keys.toList()
        private val answers = faqData.values.toList()

        override fun getGroupCount(): Int = questions.size
        override fun getChildrenCount(groupPosition: Int): Int = 1
        override fun getGroup(groupPosition: Int): String = questions[groupPosition]
        override fun getChild(groupPosition: Int, childPosition: Int): String = answers[groupPosition]
        override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()
        override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()
        override fun hasStableIds(): Boolean = true

        override fun getGroupView(
            groupPosition: Int,
            isExpanded: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val view = convertView ?: LayoutInflater.from(this@HelpAndSupportActivity)
                .inflate(R.layout.faq_group, parent, false)

            val questionText = view.findViewById<TextView>(R.id.questionText)
            val expandIcon = view.findViewById<ImageView>(R.id.expandIcon)

            questionText.text = getGroup(groupPosition)
            expandIcon.setImageResource(
                if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
            )

            return view
        }

        override fun getChildView(
            groupPosition: Int,
            childPosition: Int,
            isLastChild: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val view = convertView ?: LayoutInflater.from(this@HelpAndSupportActivity)
                .inflate(R.layout.faq_child, parent, false)

            val answerText = view.findViewById<TextView>(R.id.answerText)
            answerText.text = getChild(groupPosition, childPosition)

            return view
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = false
    }
}
