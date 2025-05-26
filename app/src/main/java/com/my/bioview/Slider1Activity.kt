package com.my.bioview

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class Slider1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for internet first
        if (!isInternetAvailable()) {
            startActivity(Intent(this, NoInternetActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_slider1)

        val btnNext = findViewById<Button>(R.id.btnNext1)
        val btnSkip = findViewById<TextView>(R.id.btnSkip1)
        val indicators = findViewById<LinearLayout>(R.id.indicators)

        logAppUsage()
        updateIndicators(indicators, 0)

        btnNext.setOnClickListener {
            startActivity(Intent(this, Slider2Activity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnSkip.setOnClickListener {
            // Check login status using SharedPreferences
            val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

            val intent = if (isLoggedIn) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, SignInActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateIndicators(indicators: LinearLayout, position: Int) {
        for (i in 0 until indicators.childCount) {
            val dot = indicators.getChildAt(i) as View
            dot.setBackgroundResource(
                if (i == position) R.drawable.indicator_selected
                else R.drawable.indicator_unselected
            )
        }
    }

    private fun logAppUsage() {
        val url = "https://bioview.sahans.online/app/log_usage.php"

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                Log.d("UsageLog", "Response (Success): '${response.trim()}' (Length: ${response.length})")
            },
            Response.ErrorListener { error ->
                Log.e("UsageLog", "Volley Error: ${error.message}")
                error.networkResponse?.let {
                    val responseData = String(it.data)
                    Log.e("UsageLog", "Volley Error Body: $responseData")
                }
            }) {
            override fun getParams(): Map<String, String> {
                return mapOf("dummy" to "1")
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    // Internet Check Method
    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            networkInfo.isConnected
        }
    }
}
