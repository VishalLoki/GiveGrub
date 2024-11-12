package com.foodism.givegrub.feed.feedFoodOrder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.foodism.givegrub.R;
import com.foodism.givegrub.feed.adapters.FeedItem;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;

public class FeedFoodOrder extends AppCompatActivity {
    private TextView headingTextView,foodItemTextView, venueTextView, usernameTextView, peopleCountTextView;
    private Button acceptButton, cancelButton;
    private String foodDonorName, foodItem, venue, peopleCount, imageName, foodDonorUid, documentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().setNavigationBarColor(getColor(R.color.primary_the_darkest_color));
        getWindow().setStatusBarColor(getColor(R.color.primary_the_darkest_color));
        setContentView(R.layout.activity_feed_food_order);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        headingTextView = findViewById(R.id.heading);
        foodItemTextView = findViewById(R.id.foodItemTextView);
        venueTextView = findViewById(R.id.venueTextView);
        usernameTextView = findViewById(R.id.usernameTextView);
        peopleCountTextView = findViewById(R.id.peopleCountTextView);
        acceptButton = findViewById(R.id.acceptButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Get the data passed from the previous activity
        FeedItem feedItem = (FeedItem) getIntent().getSerializableExtra("feedItem");


        //fetching data from intent extras
        foodDonorName = getIntent().getStringExtra("foodDonorName");
        foodItem = getIntent().getStringExtra("foodItem");
        venue = getIntent().getStringExtra("venue");
        peopleCount = getIntent().getStringExtra("peopleCount");
        imageName = getIntent().getStringExtra("imageName");
        foodDonorUid = getIntent().getStringExtra("uid");
        documentId = getIntent().getStringExtra("documentId");

        // Set the data in the views
        headingTextView.setText("Food Order"+" \uD83C\uDF5C\uD83C\uDF2E");
        foodItemTextView.setText(foodItem+"\uD83C\uDF5C\uD83C\uDF2E");
        venueTextView.setText("\uD83D\uDCCDLocation : " + venue);
        usernameTextView.setText("\uD83D\uDE4BFood Donor : " + foodDonorName);
        peopleCountTextView.setText("\uD83D\uDE4D\uD83C\uDFFBPeople : " + peopleCount+ " serves");

        // On click listener for accept button
        acceptButton.setOnClickListener(v -> {

            if(uid.equals(foodDonorUid)){
                Toast.makeText(this,"You can't accept your own donor",Toast.LENGTH_SHORT).show();
                return;
            }

            String orderHistoryId = FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .collection("orderhistory")
                    .document().getId();

            // Prepare the feed data in a HashMap
            HashMap<String, Object> orderHistoryData = new HashMap<>();
            orderHistoryData.put("foodDonor", foodDonorUid);
            orderHistoryData.put("foodDonorName", foodDonorName);
            orderHistoryData.put("foodItem", foodItem);
            orderHistoryData.put("venue", venue);
            orderHistoryData.put("peopleCount", peopleCount);
            orderHistoryData.put("timestamp", Timestamp.now());

            // Add the details to the 'orderhistory' subcollection
            FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .collection("orderhistory")
                    .document(orderHistoryId)
                    .set(orderHistoryData)
                    .addOnSuccessListener(aVoid -> {
                        // Delete the feed from the 'feeds' collection
                        FirebaseFirestore.getInstance().collection("feeds")
                                .document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    // Delete the feed image from Firebase Storage
                                    FirebaseStorage.getInstance().getReference()
                                            .child("feeds_images/" + foodDonorUid + "/" + imageName)
                                            .delete()
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(this, "Order Accepted", Toast.LENGTH_SHORT).show();
                                                finish(); // Close the activity
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Failed to delete image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to delete feed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add to order history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // On click listener for cancel button
        cancelButton.setOnClickListener(v -> {
            // Close the activity without making any changes
            finish();
        });
    }
}
