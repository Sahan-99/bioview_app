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
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
    private var uploadFile: File? = null

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
        btnBack.setOnClickListener { finish() }

        val itemHome = findViewById<LinearLayout>(R.id.itemHome)
        val item3D = findViewById<LinearLayout>(R.id.item3D)
        val itemProfile = findViewById<LinearLayout>(R.id.itemProfile)

        itemHome.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        item3D.setOnClickListener { } // Already on 3D screen
        itemProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        profileImage = findViewById(R.id.profileImage)
        cameraIcon = findViewById(R.id.cameraIcon)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading user data...")
            setCancelable(false)
        }

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (!sharedPref.getBoolean("is_logged_in", false)) {
            Log.w("EditProfileActivity", "User not logged in, redirecting to SignInActivity")
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        fetchUserData()
        cameraIcon.setOnClickListener { pickImage.launch("image/*") }
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
        val sessionId = sharedPref.getString("session_id", null) ?: run {
            progressDialog.dismiss()
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val stringRequest = object : StringRequest(
            Method.GET, url,
            { response ->
                progressDialog.dismiss()
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        etFirstName.setText(jsonResponse.getString("first_name"))
                        etLastName.setText(jsonResponse.getString("last_name"))
                        etEmail.setText(jsonResponse.getString("email"))
                        val profilePicture = jsonResponse.getString("profile_picture")
                        Log.d("EditProfileActivity", "Profile Picture URL: $profilePicture")
                        if (profilePicture.isNotEmpty()) {
                            Glide.with(this)
                                .load(profilePicture)
                                .override(100, 100)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profileImage)
                        } else {
                            Log.d("EditProfileActivity", "Profile picture is empty, using default")
                            profileImage.setImageResource(R.drawable.default_profile)
                        }
                    } else {
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
                headers["Cookie"] = sessionId
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
        val sessionId = sharedPref.getString("session_id", null) ?: return

        val stringRequest = object : StringRequest(
            Method.POST, url,
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
                headers["Cookie"] = sessionId
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
        val sessionId = sharedPref.getString("session_id", null) ?: return

        val file = File(cacheDir, "temp_image.jpg")
        selectedImageUri?.let { uri ->
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    Log.d("EditProfileActivity", "File saved to ${file.absolutePath}, bytes copied: $bytesCopied")
                }
            } ?: run {
                Log.e("EditProfileActivity", "Failed to open input stream for URI: $uri")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to read image file", Toast.LENGTH_SHORT).show()
                return
            }
        } ?: run {
            Log.e("EditProfileActivity", "No selected image URI")
            progressDialog.dismiss()
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            return
        }

        if (file.exists() && file.length() > 2 * 1024 * 1024) {
            progressDialog.dismiss()
            Toast.makeText(this, "File size exceeds 2MB limit", Toast.LENGTH_SHORT).show()
            file.delete()
            return
        }

        if (!file.exists()) {
            Log.e("EditProfileActivity", "Temporary file not created: ${file.absolutePath}")
            progressDialog.dismiss()
            Toast.makeText(this, "Failed to prepare image for upload", Toast.LENGTH_SHORT).show()
            return
        }

        uploadFile = file // Store file reference for the request
        val volleyMultipartRequest = object : VolleyMultipartRequest(
            Method.POST, url,
            Response.Listener { response ->
                progressDialog.dismiss()
                try {
                    val responseString = String(response, Charsets.UTF_8)
                    Log.d("EditProfileActivity", "Raw Response: $responseString")
                    val jsonResponse = JSONObject(responseString)
                    if (jsonResponse.getString("status") == "success") {
                        Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                        fetchUserData()
                    } else {
                        Toast.makeText(this, "Image upload failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileActivity", "Error parsing upload response: ${e.message}, Data: ${String(response, Charsets.UTF_8)}")
                    Toast.makeText(this, "Error uploading image: Invalid response format", Toast.LENGTH_SHORT).show()
                } finally {
                    uploadFile?.delete() // Delete file after request completes
                    uploadFile = null
                }
            },
            Response.ErrorListener { error ->
                progressDialog.dismiss()
                Log.e("EditProfileActivity", "Upload error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                uploadFile?.delete() // Clean up on error
                uploadFile = null
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Cookie"] = sessionId
                return headers
            }

            @Throws(AuthFailureError::class)
            override fun getByteData(): Map<String, File> {
                val params = HashMap<String, File>()
                uploadFile?.let { file ->
                    if (file.exists()) {
                        Log.d("EditProfileActivity", "Adding file to request: ${file.name}, size: ${file.length()}")
                        params["image"] = file
                    } else {
                        Log.e("EditProfileActivity", "File does not exist: ${file.absolutePath}")
                    }
                } ?: Log.e("EditProfileActivity", "No upload file available")
                return params
            }
        }

        volleyMultipartRequest.retryPolicy = DefaultRetryPolicy(5000, 1, 1.0f)
        Volley.newRequestQueue(this).add(volleyMultipartRequest)
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
        override fun getHeaders(): Map<String, String> = HashMap()

        @Throws(AuthFailureError::class)
        open fun getByteData(): Map<String, File> = HashMap()

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
            val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"

            try {
                val params = getByteData()
                if (params.isEmpty()) {
                    Log.e("VolleyMultipartRequest", "No files to upload")
                    return byteArrayOutputStream.toByteArray()
                }
                for ((key, file) in params) {
                    Log.d("VolleyMultipartRequest", "Processing file: $key, path: ${file.absolutePath}, size: ${file.length()}")
                    dataOutputStream.writeBytes("--$boundary\r\n")
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"$key\"; filename=\"${file.name}\"\r\n")
                    dataOutputStream.writeBytes("Content-Type: ${getContentType(file)}\r\n\r\n")

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
            return when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::progressDialog.isInitialized && progressDialog.isShowing) progressDialog.dismiss()
        uploadFile?.delete()
        uploadFile = null
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}