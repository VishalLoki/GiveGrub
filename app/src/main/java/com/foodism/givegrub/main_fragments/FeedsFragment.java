package com.foodism.givegrub.main_fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.foodism.givegrub.R;
import com.foodism.givegrub.feed.adapters.FeedsAdapter;
import com.foodism.givegrub.feed.addFeed.FeedAddActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.foodism.givegrub.feed.adapters.FeedItem;

import java.util.ArrayList;
import java.util.List;

public class FeedsFragment extends Fragment {
    private RecyclerView recyclerView;
    private FeedsAdapter feedsAdapter;
    private List<FeedItem> feedItemList;
    private FirebaseFirestore db;
    private TextView noFeedsTextView;  // To show "No Feeds Available" message
    private SwipeRefreshLayout swipeRefreshLayout;  // SwipeRefreshLayout to handle pull-to-refresh

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feeds, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Add Feed Button
        ImageButton addFeedButton = view.findViewById(R.id.addFeedButton);
        addFeedButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), FeedAddActivity.class)));

        // Initialize RecyclerView and SwipeRefreshLayout
        recyclerView = view.findViewById(R.id.feedsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout); // SwipeRefreshLayout initialization

        // Initialize the list and adapter
        feedItemList = new ArrayList<>();
        feedsAdapter = new FeedsAdapter(feedItemList, getContext());
        recyclerView.setAdapter(feedsAdapter);

        // TextView for "No Feeds Available" message
        noFeedsTextView = view.findViewById(R.id.noFeedsTextView);

        // Load data from Firestore
        loadFeedItemsFromFirestore();

        // Set the refresh listener for SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> loadFeedItemsFromFirestore()); // Reload the data when refreshed

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload the feed items when the fragment is resumed
        loadFeedItemsFromFirestore();
    }

    private void loadFeedItemsFromFirestore() {
        swipeRefreshLayout.setRefreshing(true);  // Show the refresh indicator

        db.collection("feeds")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Order by timestamp in descending order
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        feedItemList.clear(); // Clear existing items to avoid duplicates

                        if (querySnapshot.isEmpty()) {
                            // If the collection is empty, show the "No Feeds Available" message
                            noFeedsTextView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            noFeedsTextView.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);

                            for (QueryDocumentSnapshot document : querySnapshot) {
                                // Get fields from Firestore document
                                String username = document.getString("username");
                                String description = document.getString("description");
                                String foodItem = document.getString("foodItem");
                                String venue = document.getString("venue");
                                String peopleCount = document.getString("peopleCount");
                                String imageUrl = document.getString("imageUrl");
                                String imageName = document.getString("imageName");
                                Timestamp timestamp = document.getTimestamp("timestamp"); // Assuming timestamp is stored as a Timestamp object
                                String uid = document.getString("uid");
                                String documentId = document.getId();

                                // Create a FeedItem object and add it to the list
                                FeedItem feedItem = new FeedItem(username, description, foodItem, venue, peopleCount, imageUrl, imageName, timestamp, uid, documentId);
                                feedItemList.add(feedItem);
                            }

                            // Notify the adapter that data has changed
                            feedsAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to load feeds", Toast.LENGTH_SHORT).show();
                    }

                    swipeRefreshLayout.setRefreshing(false); // Stop the refresh indicator
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching feeds: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false); // Stop the refresh indicator in case of failure
                });
    }
}
