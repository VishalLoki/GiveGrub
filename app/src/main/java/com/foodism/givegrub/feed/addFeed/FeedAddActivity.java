package com.foodism.givegrub.feed.addFeed;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.foodism.givegrub.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FeedAddActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView photoPreview;
    private Button uploadPhotoButton, submitButton;
    private EditText descriptionEditText, foodItemEditText, venueEditText, peopleCountEditText;
    private Uri imageUri;
    private Bitmap croppedBitmap;
    private ProgressBar progressBar; // ProgressBar for loading feedback

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.primary_the_darkest_color));
        getWindow().setNavigationBarColor(getColor(R.color.primary_the_darkest_color));
        setContentView(R.layout.activity_feed_add);

        // Set system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        photoPreview = findViewById(R.id.photoPreview);
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        foodItemEditText = findViewById(R.id.foodItemEditText);
        venueEditText = findViewById(R.id.venueEditText);
        peopleCountEditText = findViewById(R.id.peopleCountEditText);
        submitButton = findViewById(R.id.submitButton);
        ImageButton backBtn = findViewById(R.id.backBtn);
        progressBar = findViewById(R.id.progressBar); // Find ProgressBar in layout

        progressBar.setVisibility(View.GONE); // Initially hide the ProgressBar

        if (backBtn != null) {
            backBtn.setOnClickListener(view -> onBackPressed());
        }

        uploadPhotoButton.setOnClickListener(v -> openImageChooser());
        submitButton.setOnClickListener(v -> submitFeed());
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                croppedBitmap = cropBitmapToAspectRatio(bitmap, 4, 3);
                photoPreview.setImageBitmap(croppedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap cropBitmapToAspectRatio(Bitmap source, int aspectX, int aspectY) {
        int width = source.getWidth();
        int height = source.getHeight();

        int targetWidth, targetHeight;
        if (width * aspectY > height * aspectX) {
            targetHeight = height;
            targetWidth = height * aspectX / aspectY;
        } else {
            targetWidth = width;
            targetHeight = width * aspectY / aspectX;
        }

        int offsetX = (width - targetWidth) / 2;
        int offsetY = (height - targetHeight) / 2;

        return Bitmap.createBitmap(source, offsetX, offsetY, targetWidth, targetHeight);
    }

    private void submitFeed() {
        String description = descriptionEditText.getText().toString();
        String foodItem = foodItemEditText.getText().toString();
        String venue = venueEditText.getText().toString();
        String peopleCount = peopleCountEditText.getText().toString();

        if (description.isEmpty() || foodItem.isEmpty() || venue.isEmpty() || peopleCount.isEmpty() || croppedBitmap == null) {
            Toast.makeText(this, "Please fill all fields and upload a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // Show ProgressBar during submission
        uploadImageToFirebase(description, foodItem, venue, peopleCount);
    }

    private void uploadImageToFirebase(String description, String foodItem, String venue, String peopleCount) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Compress image to 70% quality
        byte[] data = baos.toByteArray();

        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String imageName = "IMG_" + new Random().nextInt(100000) + ".jpg";
        StorageReference imageRef = FirebaseStorage.getInstance().getReference()
                .child("feeds_images")
                .child(userUid)
                .child(imageName);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Retrieve all documents in "usernames" collection
        db.collection("usernames").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String username = queryDocumentSnapshots.getDocuments().stream().filter(document -> userUid.equals(document.getString("uid"))).findFirst().map(DocumentSnapshot::getId).orElse(null);

                    // Loop through documents to find the one with matching UID
                    // Document ID is the username

                    if (username != null) {
                        // Proceed with image upload and feed submission after retrieving the username
                        UploadTask uploadTask = imageRef.putBytes(data);
                        uploadTask.addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveFeedDataToFirestore(description, foodItem, venue, peopleCount, imageUrl, imageName,username);
                        })).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE); // Hide ProgressBar on failure
                            Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        progressBar.setVisibility(View.GONE); // Hide ProgressBar if username retrieval fails
                        Toast.makeText(this, "Username not found for this user", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE); // Hide ProgressBar on failure
                    Toast.makeText(this, "Failed to retrieve username", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveFeedDataToFirestore(String description, String foodItem, String venue, String peopleCount, String imageUrl,String imageName, String username) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> feedData = new HashMap<>();
        feedData.put("imageUrl", imageUrl);
        feedData.put("imageName",imageName);
        feedData.put("description", description);
        feedData.put("foodItem", foodItem);
        feedData.put("venue", venue);
        feedData.put("peopleCount", peopleCount);
        feedData.put("timestamp", Timestamp.now());
        feedData.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        feedData.put("username", username);

        db.collection("feeds").add(feedData).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Feed submitted successfully!", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE); // Hide ProgressBar on success
            finish(); // Close the activity
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE); // Hide ProgressBar on failure
            Toast.makeText(this, "Failed to submit feed", Toast.LENGTH_SHORT).show();
        });
    }
}
