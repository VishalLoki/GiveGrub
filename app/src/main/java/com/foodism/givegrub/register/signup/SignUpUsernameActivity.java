package com.foodism.givegrub.register.signup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.foodism.givegrub.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUpUsernameActivity extends AppCompatActivity {

    String email;
    String password;

    EditText username;
    Button nextButton;
    ProgressBar progressBar; // Declare the ProgressBar

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_username);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        username = findViewById(R.id.editTextUserName);
        nextButton = findViewById(R.id.sign_up_button);
        progressBar = findViewById(R.id.progress_bar); // Initialize the ProgressBar

        email = Objects.requireNonNull(getIntent().getExtras()).getString("email");
        password = Objects.requireNonNull(getIntent().getExtras()).getString("password");

        nextButton.setOnClickListener(v -> {
            String enteredUsername = username.getText().toString().trim();

            if (validateInput(enteredUsername)) {
                checkIfUsernameExists(enteredUsername);
            }
        });
    }

    // Validate username input
    private boolean validateInput(String enteredUsername) {
        if (TextUtils.isEmpty(enteredUsername)) {
            username.setError("Username cannot be empty");
            return false;
        }
        if (enteredUsername.length() > 20) {
            username.setError("Username must be less than 20 characters");
            return false;
        }
        return true;
    }

    // Check if username exists in Firestore
    private void checkIfUsernameExists(String enteredUsername) {
        progressBar.setVisibility(View.VISIBLE); // Show the progress bar
        db.collection("usernames").document(enteredUsername).get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE); // Hide the progress bar
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Username already exists
                    username.setError("Username already taken, please choose another");
                } else {
                    // Username is available
                    authenticateAndSaveUser(enteredUsername);
                }
            } else {
                Toast.makeText(SignUpUsernameActivity.this, "Error checking username. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Authenticate user and save data to Firestore
    private void authenticateAndSaveUser(String enteredUsername) {
        progressBar.setVisibility(View.VISIBLE); // Show the progress bar
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE); // Hide the progress bar
                    if (task.isSuccessful()) {
                        // Authentication successful, get user ID
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            storeUserDetails(uid, email, enteredUsername);
                        }
                    } else {
                        Toast.makeText(SignUpUsernameActivity.this, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Save user details in Firestore
    private void storeUserDetails(String uid, String email, String username) {
        progressBar.setVisibility(View.VISIBLE); // Show the progress bar
        // Create a new user map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("profileImageUrl", ""); // Add profile image URL if applicable
        userMap.put("createdAt", Timestamp.now());
        userMap.put("lastSeen", Timestamp.now());
        userMap.put("status", "online");
        userMap.put("groupIds", new ArrayList<String>()); // Initialize as an empty list
        userMap.put("friendsIds", new ArrayList<String>()); // Initialize as an empty list

        // Reference to Firestore collections
        CollectionReference usersCollection = db.collection("users");
        CollectionReference usernamesCollection = db.collection("usernames");
        CollectionReference emailsCollection = db.collection("emails");

        // Store user details in Firestore
        usersCollection.document(uid).set(userMap).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE); // Hide the progress bar
            if (task.isSuccessful()) {
                // Store username to prevent duplicates
                Map<String, String> usernameMap = new HashMap<>();
                usernameMap.put("uid", uid);
                usernamesCollection.document(username).set(usernameMap).addOnCompleteListener(usernameTask -> {
                    if (usernameTask.isSuccessful()) {
                        // Store email for uniqueness using an array field in the "emails" document
                        emailsCollection.document("email").update("emails", FieldValue.arrayUnion(email))
                                .addOnCompleteListener(emailTask -> {
                                    if (emailTask.isSuccessful()) {
                                        // Create an Intent to navigate to SignUpDpActivity
                                        Intent intent = new Intent(SignUpUsernameActivity.this, SignUpDpActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(SignUpUsernameActivity.this, "Error saving email: " + Objects.requireNonNull(emailTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(SignUpUsernameActivity.this, "Error saving username: " + Objects.requireNonNull(usernameTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(SignUpUsernameActivity.this, "Error saving user details: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}