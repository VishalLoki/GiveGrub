package com.foodism.givegrub.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.foodism.givegrub.R;
import com.foodism.givegrub.orderHistory.OrderHistoryActivity;
import com.foodism.givegrub.register.signup.SignUpEmailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST_CODE = 101;

    private ImageView profileIcon;
    private TextView usernameTextView, emailTextView;
    private Button updateProfileButton;
    private Button notificationButton,orderHistoryButton;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().setStatusBarColor(getColor(R.color.primary_the_darkest_color));
        getWindow().setNavigationBarColor(getColor(R.color.primary_the_darkest_color));
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Get references to UI elements
        profileIcon = findViewById(R.id.profile_icon);
        usernameTextView = findViewById(R.id.username);
        emailTextView = findViewById(R.id.email);
        updateProfileButton = findViewById(R.id.update_profile_btn);
        orderHistoryButton = findViewById(R.id.btn_order_history);
        Button signOutButton = findViewById(R.id.btn_sign_out);

        ImageView backBtn = findViewById(R.id.back_btn);

        signOutButton.setOnClickListener(v -> signOutUser());

        if (backBtn != null) {
            backBtn.setOnClickListener(view -> onBackPressed());
        }

        orderHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, OrderHistoryActivity.class);
            startActivity(intent);
        });

        // Fetch and display user information
        fetchUserProfile();

        // Set up the button to update profile picture
        updateProfileButton.setOnClickListener(view -> openGallery());
    }

    private void signOutUser() {
        auth.signOut();
        // Redirect the user to the login screen (or any other appropriate screen)
        Intent intent = new Intent(ProfileActivity.this, SignUpEmailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish the ProfileActivity
    }

    private void fetchUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        StorageReference profileImageRef = storage.getReference("profile_images/" + uid + ".jpg");
        profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(ProfileActivity.this)
                    .load(uri)
                    .placeholder(R.drawable.defdp)
                    .into(profileIcon);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show();
        });

        DocumentReference userRef = firestore.collection("users").document(uid);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");

                usernameTextView.setText(username != null ? username : "Unknown User");
                emailTextView.setText(email != null ? email : "No Email Provided");
            } else {
                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show();
        });
    }

    // Method to open the gallery
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            cropImage(imageUri);
        }
    }

    private void cropImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap,
                    (bitmap.getWidth() - size) / 2,
                    (bitmap.getHeight() - size) / 2,
                    size, size);

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 500, 500, true);
            profileIcon.setImageBitmap(resizedBitmap);
            uploadImageToFirebase(resizedBitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase(Bitmap bitmap) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            StorageReference fileReference = storage.getReference("profile_images/" + uid + ".jpg");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] data = baos.toByteArray();

            fileReference.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        updateProfileImageUrl(downloadUri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateProfileImageUrl(String imageUrl) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .update("profileImageUrl", imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfileActivity.this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}