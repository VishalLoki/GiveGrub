package com.foodism.givegrub.register.signup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.foodism.givegrub.R;
import com.foodism.givegrub.register.forget.ForgotActivity;
import com.foodism.givegrub.register.login.LoginEmailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.regex.Pattern;

public class SignUpEmailActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar; // Declare ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_email);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Find views
        emailEditText = findViewById(R.id.editTextTextEmailAddress);
        passwordEditText = findViewById(R.id.editTextTextPassword);
        Button signUpButton = findViewById(R.id.sign_up_button);
        TextView forgotPasswordTextView = findViewById(R.id.forgot_password);
        TextView alreadyHaveAccountTextView = findViewById(R.id.already_have_account);
        progressBar = findViewById(R.id.progress_bar); // Initialize ProgressBar

        // Forgot Password click listener
        forgotPasswordTextView.setOnClickListener(v -> {
            String email_for = emailEditText.getText().toString().trim();
            if (TextUtils.isEmpty(email_for) || !Patterns.EMAIL_ADDRESS.matcher(email_for).matches()) {
                emailEditText.setError("Please enter a valid email address");
                return;
            }
            // Send password reset email using Firebase
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
            mAuth.sendPasswordResetEmail(email_for).addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE); // Hide progress bar
                if (task.isSuccessful()) {
                    Intent intent = new Intent(SignUpEmailActivity.this, ForgotActivity.class);
                    intent.putExtra("email",email_for);
                    startActivity(intent);
                } else {
                    Toast.makeText(SignUpEmailActivity.this, "Error sending password reset email. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Already Have Account click listener
        alreadyHaveAccountTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpEmailActivity.this, LoginEmailActivity.class);
            startActivity(intent);
        });

        // Sign Up Button click listener
        signUpButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Check if both fields are filled and validate email and password
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Email is required");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.setError("Enter a valid email address");
                return;
            }

            if (!isValidPassword(password)) {
                passwordEditText.setError("Password must be at least 6 characters, include an uppercase letter, lowercase letter, number, and special character.");
                return;
            }

            // Check if email is available in Firestore
            checkEmailAvailability(email, password);
        });
    }

    private void checkEmailAvailability(String email, String password) {
        progressBar.setVisibility(View.VISIBLE); // Show progress bar
        CollectionReference emailsRef = db.collection("emails");
        emailsRef.get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE); // Hide progress bar after task completion
            if (task.isSuccessful()) {
                boolean emailExists = false;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    List<String> emailList = (List<String>) document.get("emails");
                    if (emailList != null && emailList.contains(email)) {
                        emailExists = true;
                        break;
                    }
                }

                if (emailExists) {
                    // Show error if email is already in the array
                    Toast.makeText(SignUpEmailActivity.this, "Email already exists. Please use a different email.", Toast.LENGTH_SHORT).show();
                } else {
                    // Proceed to the next activity
                    Intent intent = new Intent(SignUpEmailActivity.this, SignUpUsernameActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(SignUpEmailActivity.this, "Error checking email availability. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidPassword(String password) {
        // Regex pattern to check password criteria
        Pattern pattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$");
        return pattern.matcher(password).matches();
    }
}
