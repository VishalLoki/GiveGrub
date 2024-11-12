package com.foodism.givegrub.register.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.foodism.givegrub.mainactivity.MainActivity;
import com.foodism.givegrub.register.forget.ForgotActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.foodism.givegrub.R;
import com.foodism.givegrub.register.signup.SignUpEmailActivity;

import java.util.Objects;

public class LoginEmailActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_email);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progress_bar);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        emailInput = findViewById(R.id.editTextTextEmailAddress);
        passwordInput = findViewById(R.id.editTextTextPassword);
        Button loginButton = findViewById(R.id.sign_up_button);
        TextView dontHaveAccount = findViewById(R.id.already_have_account);
        TextView forgotPassword = findViewById(R.id.forgot_password);

        // Set up "Don't have an account" click listener
        dontHaveAccount.setOnClickListener(v -> startActivity(new Intent(LoginEmailActivity.this, SignUpEmailActivity.class)));

        // Forgot Password click listener
        forgotPassword.setOnClickListener(v -> {
            String email_for = emailInput.getText().toString().trim();
            if (TextUtils.isEmpty(email_for) || !Patterns.EMAIL_ADDRESS.matcher(email_for).matches()) {
                emailInput.setError("Please enter a valid email address");
                return;
            }
            // Send password reset email using Firebase
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
            mAuth.sendPasswordResetEmail(email_for).addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE); // Hide progress bar
                if (task.isSuccessful()) {
                    Intent intent = new Intent(LoginEmailActivity.this, ForgotActivity.class);
                    intent.putExtra("email",email_for);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginEmailActivity.this, "Error sending password reset email. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Set up login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (isValidEmail(email) && validatePassword(password)) {
                checkIfEmailExistsInFirestore(email, password);
            } else {
                if (!isValidEmail(email)) {
                    emailInput.setError("Please enter a valid email");
                }
                if (!validatePassword(password)) {
                    passwordInput.setError("Password must be at least 6 characters");
                }
            }
        });
    }

    // Email validation method
    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Validate password input
    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password cannot be empty");
            return false;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    // Function to check if the email exists in Firestore and then log in
    private void checkIfEmailExistsInFirestore(String email, String password) {
        // Show the progress bar
        progressBar.setVisibility(View.VISIBLE);

        db.collection("emails")
                .whereArrayContains("emails", email)
                .get()
                .addOnCompleteListener(task -> {
                    // Hide the progress bar
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            // Email exists in the array, proceed to login
                            loginUser(email, password);
                        } else {
                            // Email does not exist, show an error
                            emailInput.setError("Email not found. Please sign up.");
                        }
                    } else {
                        // Error occurred while checking Firestore
                        emailInput.setError("Error occurred. Please try again.");
                    }
                }).addOnFailureListener(e -> {
                    // Hide the progress bar
                    progressBar.setVisibility(View.GONE);
                    emailInput.setError("An unexpected error occurred: " + e.getMessage());
                });
    }

    // Authenticate user with Firebase
    private void loginUser(String email, String password) {
        // Show the progress bar
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            // Hide the progress bar
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    // User successfully logged in, redirect to the main activity
                    Toast.makeText(LoginEmailActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginEmailActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the activity stack
                    startActivity(intent);
                    finish(); // Finish this activity
                }
            } else {
                // Login failed, show an error message
                Toast.makeText(LoginEmailActivity.this, "Login Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
