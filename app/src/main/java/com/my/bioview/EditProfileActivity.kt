package com.my.bioview

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap

class EditProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var cameraIcon: ImageView
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnSaveChanges: Button
    private lateinit var progressDialog: ProgressDialog
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .override(100, 100)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(profileImage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
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

        // Initialize views
        profileImage = findViewById(R.id.profileImage)
        cameraIcon = findViewById(R.id.cameraIcon)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading user data...")
        progressDialog.setCancelable(false)

        // Check login state
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            Log.w("EditProfileActivity", "User not logged in, redirecting to SignInActivity")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Fetch and display user data
        fetchUserData()

        // Camera icon click to pick image
        cameraIcon.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Save Changes button click
        btnSaveChanges.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateUserData(firstName, lastName, email)
        }
    }

    private fun fetchUserData() {
        progressDialog.show()
        val url = "https://bioview.sahans.online/app/get_user.php"

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)
        if (sessionId == null) {
            Log.e("EditProfileActivity", "No session ID found")
            progressDialog.dismiss()
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                progressDialog.dismiss()
                Log.d("EditProfileActivity", "Response: $response")
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        val firstName = jsonResponse.getString("first_name")
                        val lastName = jsonResponse.getString("last_name")
                        val email = jsonResponse.getString("email")
                        val profilePicture = jsonResponse.getString("profile_picture")

                        etFirstName.setText(firstName)
                        etLastName.setText(lastName)
                        etEmail.setText(email)

                        if (profilePicture.isNotEmpty()) {
                            Glide.with(this)
                                .load(profilePicture)
                                .override(100, 100)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profileImage)
                        } else {
                            Log.w("EditProfileActivity", "No profile picture available")
                        }
                    } else {
                        Log.w("EditProfileActivity", "Server returned error: ${jsonResponse.getString("message")}")
                        Toast.makeText(this, "Failed to load user data: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileActivity", "Error parsing user data: ${e.message}, Response: $response")
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressDialog.dismiss()
                Log.e("EditProfileActivity", "Fetch error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (sessionId != null) {
                    headers["Cookie"] = sessionId
                }
                Log.d("EditProfileActivity", "Headers: $headers")
                return headers
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(5000, 1, 1.0f)
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun updateUserData(firstName: String, lastName: String, email: String) {
        progressDialog.setMessage("Updating user data...")
        progressDialog.show()
        val url = "https://bioview.sahans.online/app/update_user.php"

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                progressDialog.dismiss()
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        if (selectedImageUri != null) {
                            uploadImage()
                        }
                    } else {
                        Toast.makeText(this, "Update failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileActivity", "Error parsing update response: ${e.message}")
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressDialog.dismiss()
                Log.e("EditProfileActivity", "Update error: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["first_name"] = firstName
                params["last_name"] = lastName
                params["email"] = email
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (sessionId != null) {
                    headers["Cookie"] = sessionId
                }
                return headers
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(5000, 1, 1.0f)
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun uploadImage() {
        progressDialog.setMessage("Uploading image...")
        progressDialog.show()
        val url = "https://bioview.sahans.online/app/upload_profile.php"

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val sessionId = sharedPref.getString("session_id", null)

        val file = File(cacheDir, "temp_image.jpg")
        selectedImageUri?.let { uri ->
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        // Validate file size (2MB limit as per server)
        if (file.exists() && file.length() > 2 * 1024 * 1024) {
            progressDialog.dismiss()
            Toast.makeText(this, "File size exceeds 2MB limit", Toast.LENGTH_SHORT).show()
            file.delete()
            return
        }

        val volleyMultipartRequest = object : VolleyMultipartRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                progressDialog.dismiss()
                try {
                    val responseString = String(response, Charsets.UTF_8)
                    Log.d("EditProfileActivity", "Raw Response: $responseString")
                    val jsonResponse = JSONObject(responseString)
                    if (jsonResponse.getString("status") == "success") {
                        Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                        fetchUserData() // Refresh profile picture
                    } else {
                        Toast.makeText(this, "Image upload failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileActivity", "Error parsing upload response: ${e.message}, Data: ${String(response, Charsets.UTF_8)}")
                    Toast.makeText(this, "Error uploading image: Invalid response format", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                progressDialog.dismiss()
                Log.e("EditProfileActivity", "Upload error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                if (sessionId != null) {
                    headers["Cookie"] = sessionId
                }
                return headers
            }

            @Throws(AuthFailureError::class)
            override fun getByteData(): Map<String, File> {
                val params = HashMap<String, File>()
                params["image"] = file // Key "image" must match $_FILES['image'] on server
                return params
            }
        }

        volleyMultipartRequest.retryPolicy = DefaultRetryPolicy(5000, 1, 1.0f)
        Volley.newRequestQueue(this).add(volleyMultipartRequest)
        file.delete() // Clean up temporary file
    }

    // Custom VolleyMultipartRequest class
    open class VolleyMultipartRequest(
        method: Int,
        url: String,
        listener: Response.Listener<ByteArray>,
        errorListener: Response.ErrorListener
    ) : Request<ByteArray>(method, url, errorListener) {

        private val mListener: Response.Listener<ByteArray> = listener

        @Throws(AuthFailureError::class)
        override fun getHeaders(): Map<String, String> {
            return HashMap()
        }

        @Throws(AuthFailureError::class)
        open fun getByteData(): Map<String, File> {
            return HashMap()
        }

        override fun deliverResponse(response: ByteArray) {
            mListener.onResponse(response)
        }

        override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray> {
            return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response))
        }

        @Throws(AuthFailureError::class)
        override fun getBodyContentType(): String {
            return "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"
        }

        @Throws(AuthFailureError::class)
        override fun getBody(): ByteArray {
            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
            val dataOutputStream = DataOutputStream(byteArrayOutputStream)

            try {
                val params = getByteData()
                val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"

                for ((key, file) in params) {
                    dataOutputStream.writeBytes("--$boundary\r\n")
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"$key\"; filename=\"${file.name}\"\r\n")
                    dataOutputStream.writeBytes("Content-Type: ${getContentType(file)}\r\n\r\n") // Dynamic content type

                    val fileInputStream = FileInputStream(file)
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                        dataOutputStream.write(buffer, 0, bytesRead)
                    }
                    fileInputStream.close()
                    dataOutputStream.writeBytes("\r\n")
                }

                dataOutputStream.writeBytes("--$boundary--\r\n")
                dataOutputStream.flush()
            } catch (e: Exception) {
                Log.e("VolleyMultipartRequest", "Error building multipart body: ${e.message}")
            }

            return byteArrayOutputStream.toByteArray()
        }

        private fun getContentType(file: File): String {
            return when (file.extension.toLowerCase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}