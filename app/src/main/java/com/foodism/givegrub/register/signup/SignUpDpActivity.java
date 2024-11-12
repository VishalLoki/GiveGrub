package com.foodism.givegrub.register.signup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.foodism.givegrub.R;
import com.foodism.givegrub.mainactivity.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SignUpDpActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST_CODE = 101; // Request code for gallery
    private ImageView profileImageView;
    private ProgressBar progressBar; // Declare ProgressBar

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_dp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        profileImageView = findViewById(R.id.profileImageView);
        // Button to pick from gallery
        Button pickButton = findViewById(R.id.pickButton); // Pick from gallery button
        progressBar = findViewById(R.id.progressBar); // Initialize ProgressBar

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");

        // Set up the button to open the gallery
        pickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
    }

    // Method to open the gallery
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE); // Start gallery activity
    }

    // Handle the result of the gallery activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData(); // Get the image URI from the gallery
            cropImage(imageUri); // Start cropping
        }
    }

    // Method to crop the image to a 1:1 aspect ratio and compress it
    private void cropImage(Uri uri) {
        try {
            // Load the image from the URI
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // Crop to a square bitmap
            int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap,
                    (bitmap.getWidth() - size) / 2,
                    (bitmap.getHeight() - size) / 2,
                    size, size);

            // Resize the cropped image to a smaller size
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 500, 500, true);

            // Set the cropped and resized image to ImageView
            profileImageView.setImageBitmap(resizedBitmap);

            // Upload the cropped and resized image to Firebase
            uploadImageToFirebase(resizedBitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Method to upload the cropped and resized image to Firebase Storage
    private void uploadImageToFirebase(Bitmap bitmap) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            StorageReference fileReference = storageReference.child(uid + ".jpg");

            // Convert the Bitmap to a byte array with compression
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Compress with 70% quality
            byte[] data = baos.toByteArray();

            // Show the progress bar while uploading
            progressBar.setVisibility(View.VISIBLE);

            // Upload the image to Firebase Storage
            fileReference.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileReference.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            updateProfileImageUrl(downloadUri.toString()); // Update the Firestore document with the image URL
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignUpDpActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnCompleteListener(task -> {
                        // Hide the progress bar after upload completes
                        progressBar.setVisibility(View.GONE);
                    });
        }
    }

    // Method to update the profile image URL in Firestore
    private void updateProfileImageUrl(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .update("profileImageUrl", imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SignUpDpActivity.this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity(); // Navigate to MainActivity after successful update
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignUpDpActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Method to navigate to MainActivity
    private void navigateToMainActivity() {
        Intent intent = new Intent(SignUpDpActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the activity stack
        startActivity(intent);
        finish(); // Finish this activity
    }
}