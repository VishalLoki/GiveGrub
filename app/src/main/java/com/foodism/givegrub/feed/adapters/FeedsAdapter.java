package com.foodism.givegrub.feed.adapters;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.foodism.givegrub.R;
import com.foodism.givegrub.feed.feedFoodOrder.FeedFoodOrder;

import java.util.List;

public class FeedsAdapter extends RecyclerView.Adapter<FeedsAdapter.FeedViewHolder> {
    private final List<FeedItem> feedItemList;
    private final Context context;

    public FeedsAdapter(List<FeedItem> feedItemList, Context context) {
        this.feedItemList = feedItemList;
        this.context = context;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        FeedItem feedItem = feedItemList.get(position);

        holder.usernameTextView.setText("\uD83D\uDD16 "+feedItem.getUsername());
        holder.descriptionTextView.setText("\uD83D\uDE80\uD83D\uDE80"+feedItem.getDescription());
        holder.foodItemTextView.setText("\uD83C\uDF5BFood : "+feedItem.getFoodItem());
        holder.venueTextView.setText("\uD83D\uDCCDLocation : "+feedItem.getVenue());
        holder.peopleCountTextView.setText("\uD83D\uDE4D\uD83C\uDFFBPeople : "+feedItem.getPeopleCount()+" serves");
        holder.timestampTextView.setText(feedItem.getTimestamp());

        // Load image using Glide
        Glide.with(context)
                .load(feedItem.getImageUrl())
                .placeholder(R.drawable.defdp) // Add a placeholder image in drawable
                .into(holder.feedImageView);

        // On click listener for the feed item to show details in a new activity
        holder.itemView.setOnClickListener(v -> {
            // Create an intent to launch FoodDetailsActivity
            Intent intent = new Intent(context, FeedFoodOrder.class);
            intent.putExtra("foodDonorName",feedItem.getUsername());
            intent.putExtra("foodItem",feedItem.getFoodItem());
            intent.putExtra("venue",feedItem.getVenue());
            intent.putExtra("peopleCount",feedItem.getPeopleCount());
            intent.putExtra("imageName",feedItem.getImageName());
            intent.putExtra("uid",feedItem.getUid());
            intent.putExtra("documentId",feedItem.getDocumentId());
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return feedItemList.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView, descriptionTextView, foodItemTextView, venueTextView, peopleCountTextView, timestampTextView;
        ImageView feedImageView;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            foodItemTextView = itemView.findViewById(R.id.foodItemTextView);
            venueTextView = itemView.findViewById(R.id.venueTextView);
            peopleCountTextView = itemView.findViewById(R.id.peopleCountTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            feedImageView = itemView.findViewById(R.id.feedImageView);
        }
    }


}
