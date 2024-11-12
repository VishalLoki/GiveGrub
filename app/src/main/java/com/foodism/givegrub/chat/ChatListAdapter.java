package com.foodism.givegrub.chat;

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
import com.foodism.givegrub.chatinterface.ChatInterfaceActivity;

import java.util.List;
import java.util.Objects;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<ChatItem> chatList;
    private Context context;

    public ChatListAdapter(Context context, List<ChatItem> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chatItem = chatList.get(position);

        holder.usernameTextView.setText(chatItem.getUsername());
        holder.lastMessageTextView.setText(chatItem.getLastMessage());
        holder.timestampTextView.setText(chatItem.getTimestamp()); // Use the formatted timestamp directly
        holder.unreadCountTextView.setText(String.valueOf(chatItem.getUnreadMessages()));

        // Load the profile image using Glide from Firebase Cloud Storage
        loadProfileImage(chatItem.getProfileUrl(), holder.profileImageView);

        // Set unread count visibility
        if (chatItem.getUnreadMessages() > 0) {
            holder.unreadCountTextView.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCountTextView.setVisibility(View.GONE);
        }

        // Profile picture click listener
        holder.profileImageView.setOnClickListener(v -> {
            showProfilePictureDialog(context, Uri.parse(chatItem.getProfileUrl()));
        });

        // Navigate to ChatInterfaceActivity when the chat item is clicked
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatInterfaceActivity.class);
            intent.putExtra("participant_uid",chatItem.getUid2());
            intent.putExtra("participant_username",chatItem.getUsername());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
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

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        TextView lastMessageTextView;
        TextView timestampTextView;
        TextView unreadCountTextView;
        ImageView profileImageView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            unreadCountTextView = itemView.findViewById(R.id.unreadCountTextView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
        }
    }
}
