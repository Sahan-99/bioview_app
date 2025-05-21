package com.my.bioview

import android.Manifest
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.Context

class NoInternetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_internet)

        val btnExitApp = findViewById<Button>(R.id.btnExitApp)
        val btnTryAgain = findViewById<Button>(R.id.btnTryAgain)

        btnExitApp.setOnClickListener {
            finishAffinity() // Close the app
        }

        btnTryAgain.setOnClickListener {
            if (isInternetAvailable()) {
                // Back to main app (e.g., Slider1)
                startActivity(Intent(this, Slider1Activity::class.java))
                finish()
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.isConnected
        }
    }
}
