package com.foodism.givegrub.first;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.foodism.givegrub.R;
import com.foodism.givegrub.mainactivity.MainActivity;
import com.foodism.givegrub.register.signup.SignUpEmailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirstActivity extends AppCompatActivity {

    private FirebaseAuth mAuth; // Firebase Auth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_first);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if the user is already logged in
        if (isUserLoggedIn()) {
            // Redirect to MainActivity if the user is logged in
            Intent intent = new Intent(FirstActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close FirstActivity so that the user can't come back with the back button
            return;
        }

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up the button click listener to navigate to SignUpEmailActivity
        Button startButton = findViewById(R.id.button);
        startButton.setOnClickListener(v -> {
            // Create an Intent to start SignUpEmailActivity
            Intent intent = new Intent(FirstActivity.this, SignUpEmailActivity.class);
            startActivity(intent);
        });
    }

    // Function to check if the user is logged in
    private boolean isUserLoggedIn() {
        // Get the currently signed-in user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Return true if the user is not null, indicating that a user is logged in
        return currentUser != null;
    }
}