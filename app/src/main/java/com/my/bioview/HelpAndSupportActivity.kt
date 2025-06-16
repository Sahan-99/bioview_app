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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class HelpAndSupportActivity : AppCompatActivity() {

    private lateinit var expandableListView: ExpandableListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_and_support)

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

        // Initialize ExpandableListView
        expandableListView = findViewById(R.id.expandableListView)

        // Sample FAQ data
        val faqData = mapOf(
            "How do I scan an image for AR models?" to "Tap the 'Scan AR Model' button on the home screen and point your camera at a supported image.",
            "How do I listen to voice explanations?" to "After scanning an AR model, tap the 'Play Audio' button to hear a detailed explanation.",
            "Can I interact with the 3D models?" to "Yes! You can zoom, rotate, and move the models using touch gestures.",
            "Where can I find saved models?" to "Go to the 'My Library' section to view all previously scanned models.",
            "Do I need an internet connection to use the app?" to "An internet connection is required for initial loading."
        )

        // Set adapter
        val adapter = FAQAdapter(faqData)
        expandableListView.setAdapter(adapter)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Custom Adapter for ExpandableListView
    inner class FAQAdapter(private val faqData: Map<String, String>) : BaseExpandableListAdapter() {

        override fun getGroupCount(): Int = faqData.size

        override fun getChildrenCount(groupPosition: Int): Int = 1

        override fun getGroup(groupPosition: Int): String = faqData.keys.elementAt(groupPosition)

        override fun getChild(groupPosition: Int, childPosition: Int): String = faqData.values.elementAt(groupPosition)

        override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

        override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

        override fun hasStableIds(): Boolean = true

        override fun getGroupView(
            groupPosition: Int,
            isExpanded: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            var view = convertView
            if (view == null) {
                view = LayoutInflater.from(parent?.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            }
            (view as TextView).text = getGroup(groupPosition)
            return view
        }

        override fun getChildView(
            groupPosition: Int,
            childPosition: Int,
            isLastChild: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            var view = convertView
            if (view == null) {
                view = LayoutInflater.from(parent?.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            }
            (view as TextView).text = getChild(groupPosition, childPosition)
            return view
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = false
    }
}