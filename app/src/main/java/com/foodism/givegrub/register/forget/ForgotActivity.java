package com.foodism.givegrub.register.forget;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.foodism.givegrub.R;
import com.foodism.givegrub.register.login.LoginEmailActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotActivity extends AppCompatActivity {

    private Button resendEmailButton;
    private TextView timerTextView;
    private TextView passwordChangedText;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private static final long TIMER_DURATION = 60000; // 1 minute
    private long timeLeftInMillis = TIMER_DURATION;
    private String email; // Store the email from the intent
    private FirebaseAuth mAuth; // Firebase Auth instance
    private ProgressBar progressBar; // ProgressBar for email sending

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        resendEmailButton = findViewById(R.id.resend_email_button);
        timerTextView = findViewById(R.id.timer_text_view);
        passwordChangedText = findViewById(R.id.password_changed_text);
        progressBar = findViewById(R.id.progress_bar); // Make sure you have a ProgressBar in your layout

        // Get the email from the intent
        email = getIntent().getStringExtra("email"); // Correctly retrieve email

        // Set button click listener
        resendEmailButton.setOnClickListener(v -> {
            // Handle the resend email action
            resendEmail();

            // Start or restart the timer
            startTimer();
        });

        // Initialize window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Disable button initially and start the timer
        resendEmailButton.setEnabled(false);
        startTimer();

        passwordChangedText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotActivity.this, LoginEmailActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void resendEmail() {
        // Show progress bar while sending email
        progressBar.setVisibility(View.VISIBLE); // Show progress bar

        // Send password reset email
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE); // Hide progress bar
            if (task.isSuccessful()) {
                Toast.makeText(ForgotActivity.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ForgotActivity.this, "Error sending password reset email. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTimer() {
        if (isTimerRunning) {
            countDownTimer.cancel(); // Cancel the existing timer if it's running
        }

        // Disable the button and start the countdown
        resendEmailButton.setEnabled(false);
        isTimerRunning = true;

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {
                resendEmailButton.setEnabled(true); // Enable the button when timer finishes
                isTimerRunning = false; // Reset timer running state
                timeLeftInMillis = TIMER_DURATION; // Reset timer duration
                timerTextView.setText("00:00"); // Reset timer display
            }
        }.start();
    }

    private void updateTimer() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        timerTextView.setText(timeLeftFormatted); // Update the timer display
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Cancel timer on activity destruction to prevent memory leaks
        }
    }
}