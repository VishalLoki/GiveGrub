package com.foodism.givegrub.orderHistory;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foodism.givegrub.orderHistory.adapters.Order;
import com.foodism.givegrub.orderHistory.adapters.OrderHistoryAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.foodism.givegrub.R;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView orderHistoryRecyclerView;
    private OrderHistoryAdapter orderHistoryAdapter;
    private FirebaseFirestore db;
    private String userId;
    private ImageView backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Enable edge-to-edge for system UI
        getWindow().setStatusBarColor(getColor(R.color.primary_the_darkest_color));
        getWindow().setNavigationBarColor(getColor(R.color.primary_the_darkest_color));

        setContentView(R.layout.activity_order_history);

        // Initialize Firestore and get current user UID
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        backBtn = findViewById(R.id.backBtn);
        // Set up RecyclerView for order history
        orderHistoryRecyclerView = findViewById(R.id.orderHistoryRecyclerView);
        orderHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Linear layout for vertical list

        // Initialize adapter with an empty list initially
        orderHistoryAdapter = new OrderHistoryAdapter(new ArrayList<>());
        orderHistoryRecyclerView.setAdapter(orderHistoryAdapter);

        // Fetch order history data from Firestore
        fetchOrderHistory();

        // Apply WindowInsets for edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (backBtn != null) {
            backBtn.setOnClickListener(view -> onBackPressed());
        }
    }

    private void fetchOrderHistory() {
        // Query Firestore for order history sorted by timestamp in descending order
        db.collection("users")
                .document(userId)
                .collection("orderhistory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if the orderhistory collection exists
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<Order> orders = new ArrayList<>();
                        queryDocumentSnapshots.forEach(documentSnapshot -> {
                            // Get the order data from the document
                            String foodDonor = documentSnapshot.getString("foodDonor");
                            String foodDonorName = documentSnapshot.getString("foodDonorName");
                            String foodItem = documentSnapshot.getString("foodItem");
                            String venue = documentSnapshot.getString("venue");
                            String peopleCount = documentSnapshot.getString("peopleCount");
                            Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");

                            // Create an Order object and add it to the list
                            orders.add(new Order(foodDonor, foodDonorName, foodItem, venue, peopleCount, timestamp));
                        });

                        // Update the RecyclerView with the fetched data
                        orderHistoryAdapter.updateOrderHistory(orders);
                    } else {
                        // No orders found (either collection is empty or doesn't exist)
                        Toast.makeText(OrderHistoryActivity.this, "No orders found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OrderHistoryActivity", "Error fetching order history: ", e);
                    Toast.makeText(OrderHistoryActivity.this, "Failed to load order history.", Toast.LENGTH_SHORT).show();
                });
    }
}
