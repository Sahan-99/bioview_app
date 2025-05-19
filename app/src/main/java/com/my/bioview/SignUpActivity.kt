package com.my.bioview

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import android.view.KeyEvent
import android.widget.Toast

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val btnSignIn = findViewById<TextView>(R.id.btnSignIn)
        val editTextFirstName = findViewById<TextInputEditText>(R.id.editTextFirstName)
        val editTextLastName = findViewById<TextInputEditText>(R.id.editTextLastName)
        val editTextEmail = findViewById<TextInputEditText>(R.id.editTextEmail)
        val spinnerProfileType = findViewById<Spinner>(R.id.spinnerProfileType)
        val editTextPassword = findViewById<TextInputEditText>(R.id.editTextPassword)
        val editTextConfirmPassword = findViewById<TextInputEditText>(R.id.editTextConfirmPassword)

        // Set up the Profile Type dropdown
        val profileTypes = arrayOf("Student", "Teacher")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, profileTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProfileType.adapter = adapter

        btnSignUp.setOnClickListener {
            // Get field values
            val firstName = editTextFirstName.text.toString().trim()
            val lastName = editTextLastName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val profileType = spinnerProfileType.selectedItem.toString()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()

            // Basic validation
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || profileType.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simulate successful sign-up (replace with actual authentication)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
    }
}