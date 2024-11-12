package com.foodism.givegrub.group.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.foodism.givegrub.R;
import com.foodism.givegrub.group.groupInterface.GroupChatInterfaceActivity;
import java.util.List;
import java.util.Objects;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    private Context context;
    private List<GroupItem> groupItemList;

    // Constructor
    public GroupListAdapter(Context context, List<GroupItem> groupItemList) {
        this.context = context;
        this.groupItemList = groupItemList;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupItem groupItem = groupItemList.get(position);

        holder.groupNameTextView.setText(groupItem.getGroupName());

        // Display last message
        String lastMessage = groupItem.getLastMessage();
        if (lastMessage == null || lastMessage.isEmpty()) {
            holder.lastMessageTextView.setText("No messages yet");
        } else {
            holder.lastMessageTextView.setText(lastMessage);
        }

        // Display timestamp
        holder.timestampTextView.setText(groupItem.formatTimestamp(groupItem.getLastMessageTimestamp()));

        // Load the profile image using Glide from Firebase Cloud Storage
        loadProfileImage(groupItem.getProfileImageUrl(), holder.profileImageView);

        // Show unread count if greater than 0
        if (groupItem.getUnreadCount() > 0) {
            holder.unreadCountTextView.setVisibility(View.VISIBLE);
            holder.unreadCountTextView.setText(String.valueOf(groupItem.getUnreadCount()));
        } else {
            holder.unreadCountTextView.setVisibility(View.GONE);
        }

        // Profile picture click listener
        holder.profileImageView.setOnClickListener(v -> {
            showProfilePictureDialog(context, Uri.parse(groupItem.getProfileImageUrl()));
        });

        // Navigate to ChatInterfaceActivity when the chat item is clicked
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GroupChatInterfaceActivity.class);
            intent.putExtra("groupId",groupItem.getGroupId());
            intent.putExtra("groupName",groupItem.getGroupName());
            intent.putExtra("profileImageUrl",groupItem.getProfileImageUrl());
            intent.putExtra("lastMessageType",groupItem.getLastMessageType());
            intent.putExtra("lastMessageSender",groupItem.getLastMessageSender());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return groupItemList.size();
    }

    private void loadProfileImage(String imageUrl, ImageView imageView) {
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.defdp) // Placeholder image
                .error(R.drawable.defdp) // Error image in case of failure
                .into(imageView);
    }

    public void showProfilePictureDialog(Context context, Uri profileImageUri) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_profile_picture);

        ImageView imageView = dialog.findViewById(R.id.dialogProfileImageView);

        // Use Glide to load the image into the ImageView
        Glide.with(context)
                .load(profileImageUri)
                .placeholder(R.drawable.defdp) // Placeholder image while loading
                .error(R.drawable.defdp) // Error image if loading fails
                .into(imageView);

        // Setting transparent background and square dimensions
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }


    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameTextView;
        TextView lastMessageTextView;
        TextView timestampTextView;
        TextView unreadCountTextView;
        ImageView profileImageView;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            unreadCountTextView = itemView.findViewById(R.id.unreadCountTextView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
        }
    }
}
