package com.my.bioview

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import java.io.File
import java.io.FileOutputStream
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.FileProvider

class ReportActivity : AppCompatActivity() {

    private lateinit var btnGenerateReport: Button
    private var userId: Int = -1

    private val CHANNEL_ID = "report_channel"
    private val NOTIFICATION_ID = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

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

        btnGenerateReport = findViewById(R.id.btnGenerateReport)

        // Check login status
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            Log.w("ReportActivity", "User not logged in, redirecting to SignInActivity")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) {
            Log.w("ReportActivity", "Invalid userId, redirecting to SignInActivity")
            Toast.makeText(this, "Invalid user session, log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        btnGenerateReport.setOnClickListener {
            generateReport()
        }

        // Create notification channel for Android 8.0+
        createNotificationChannel()
    }

    private fun generateReport() {
        val url = "https://bioview.sahans.web.lk/app/generate_report.php"
        val queue = Volley.newRequestQueue(this)

        val request = object : Request<ByteArray>(
            Request.Method.POST, url, { error ->
                Log.e("ReportActivity", "Network error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf("user_id" to userId.toString())
            }

            override fun getHeaders(): Map<String, String> {
                val headers = mutableMapOf<String, String>()
                val sessionId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("session_id", null)
                if (sessionId != null) headers["Cookie"] = sessionId
                return headers
            }

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded"
            }

            override fun parseNetworkResponse(response: NetworkResponse): com.android.volley.Response<ByteArray> {
                Log.d("ReportActivity", "Response headers: ${response.headers}")
                Log.d("ReportActivity", "Response status: ${response.statusCode}, data length: ${response.data.size}")
                return com.android.volley.Response.success(response.data, null) // No caching
            }

            @SuppressLint("MissingPermission")
            override fun deliverResponse(response: ByteArray) {
                Log.d("ReportActivity", "Received data length in deliverResponse: ${response.size}")
                saveAndOpenPdf(response)
                showReportGeneratedNotification()
            }
        }

        request.tag = "REPORT_REQUEST"
        queue.add(request)
    }

    private fun saveAndOpenPdf(pdfData: ByteArray) {
        val file = File(cacheDir, "BioView_Report_${System.currentTimeMillis()}.pdf") // Unique filename
        try {
            FileOutputStream(file).use { fos ->
                fos.write(pdfData)
                fos.flush()
            }
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
            Toast.makeText(this, "Report downloaded and opened", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ReportActivity", "Error saving or opening PDF: ${e.message}")
            Toast.makeText(this, "Error saving or opening report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Report Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showReportGeneratedNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // Replace with your notification icon
            .setContentTitle("Report Generate Successful")
            .setContentText("Your report has been generated successfully!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        Volley.newRequestQueue(this).cancelAll("REPORT_REQUEST")
    }
}