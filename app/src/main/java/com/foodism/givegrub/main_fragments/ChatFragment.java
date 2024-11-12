package com.foodism.givegrub.main_fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foodism.givegrub.R;
import com.foodism.givegrub.chat.ChatItem;
import com.foodism.givegrub.chat.ChatListAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatFragment extends Fragment {

    private ImageButton addChatButton;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private RecyclerView chatRecyclerView;
    private ChatListAdapter chatListAdapter;
    private List<ChatItem> chatItemList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize Firebase Firestore and Auth
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize RecyclerView and its adapter
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        chatItemList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getContext(), chatItemList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatListAdapter);

        addChatButton = view.findViewById(R.id.addChatButton);
        addChatButton.setOnClickListener(v -> showAddChatDialog());

        loadChats(); // Load existing chats

        return view;
    }

    private void showAddChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_chat, null);
        builder.setView(dialogView);

        final EditText input = dialogView.findViewById(R.id.usernameEditText);
        Button startChatButton = dialogView.findViewById(R.id.startChatButton);

        AlertDialog dialog = builder.create();
        startChatButton.setOnClickListener(v -> {
            String enteredUsername = input.getText().toString().trim();
            if (enteredUsername.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            checkIfUsernameExists(enteredUsername, dialog);
        });

        dialog.show();
    }

    private void checkIfUsernameExists(String enteredUsername, AlertDialog dialog) {
        // Check if the entered username exists in Firestore
        firestore.collection("usernames")
                .document(enteredUsername)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Username exists, get UID
                        String user2Uid = documentSnapshot.getString("uid");
                        startChatWithUser(user2Uid, dialog);
                    } else {
                        Toast.makeText(getContext(), "Username not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking username", Toast.LENGTH_SHORT).show();
                });
    }

    private void startChatWithUser(String user2Uid, AlertDialog dialog) {
        String user1Uid = auth.getCurrentUser().getUid();

        if (user1Uid.equals(user2Uid)) {
            Toast.makeText(getContext(), "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create chat ID (combination of both UIDs)
        String chatId = user1Uid.compareTo(user2Uid) < 0
                ? user1Uid + "_" + user2Uid
                : user2Uid + "_" + user1Uid;

        // Prepare the "chats" document
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("participants", Arrays.asList(user1Uid, user2Uid));
        chatData.put("lastMessage", null); // No last message yet
//        chatData.put("timestamp", System.currentTimeMillis());
        chatData.put("lastMessage_timestamp", Timestamp.now());
        chatData.put("unreadCount", 0); // Initialize unread messages count

        // Store the chat in Firestore under "chats" collection
        firestore.collection("chats")
                .document(chatId)
                .set(chatData)
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Chat started successfully!", Toast.LENGTH_SHORT).show();
                    loadChats(); // Reload chats after starting a new chat
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to start chat", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadChats() {
        String userUid = auth.getCurrentUser().getUid();
        firestore.collection("chats")
                .whereArrayContains("participants", userUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatItemList.clear(); // Clear the list before adding new items
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String chatId = document.getId();
                            String lastMessage = document.getString("lastMessage");
                            if (lastMessage == null || lastMessage.isEmpty()) {
                                lastMessage = "Start your chat..."; // Default message
                            }
                            Comparable<? extends Comparable<?>> timestamp = document.getTimestamp("lastMessage_timestamp");

                            // Get the list of participants
                            List<String> participants = (List<String>) document.get("participants");

                            // Find the UID that is not the current user's UID
                            String participantUid = participants.get(0).equals(userUid) ? participants.get(1) : participants.get(0);
                            int unreadCount = document.getLong("unreadCount") != null ? document.getLong("unreadCount").intValue() : 0;

                            // Retrieve the profile image URL from the users collection
                            String finalLastMessage = lastMessage;
                            firestore.collection("users")
                                    .document(participantUid)
                                    .get()
                                    .addOnCompleteListener(profileTask -> {
                                        String profileImageUrl = ""; // Default to empty string
                                        if (profileTask.isSuccessful() && profileTask.getResult() != null) {
                                            profileImageUrl = profileTask.getResult().getString("profileImageUrl");
                                            if (profileImageUrl == null) {
                                                profileImageUrl = ""; // Set to empty string if null
                                            }
                                        }

                                        // Retrieve the username
                                        String finalProfileImageUrl = profileImageUrl;
                                        retrieveParticipantUsername(participantUid, username -> {
                                            // Add chat item to the list
                                            assert timestamp instanceof Timestamp;
                                            chatItemList.add(new ChatItem(finalProfileImageUrl, username, participantUid, finalLastMessage, formatTimestamp((Timestamp) timestamp), unreadCount));
                                            // Notify the adapter of data change after adding all items
                                            chatListAdapter.notifyDataSetChanged();
                                        });
                                    });
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading chats", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Update the retrieveParticipantUsername method to accept a callback
    private void retrieveParticipantUsername(String participantUid, OnUsernameRetrievedCallback callback) {
        firestore.collection("users")
                .document(participantUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String participantUsername = documentSnapshot.getString("username");
                        callback.onUsernameRetrieved(participantUsername);
                    } else {
                        Toast.makeText(getContext(), "Username not found for participant", Toast.LENGTH_SHORT).show();
                        callback.onUsernameRetrieved("Unknown User"); // Fallback username
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error retrieving username", Toast.LENGTH_SHORT).show();
                    callback.onUsernameRetrieved("Unknown User"); // Fallback username
                });
    }

    // Define the callback interface
    interface OnUsernameRetrievedCallback {
        void onUsernameRetrieved(String username);
    }

    // Format the timestamp method
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        Date messageDate = timestamp.toDate(); // Convert Firestore Timestamp to Date
        SimpleDateFormat formatter;

        Calendar currentCalendar = Calendar.getInstance();
        Calendar messageCalendar = Calendar.getInstance();
        messageCalendar.setTime(messageDate);

        // Check if the message is from today
        if (currentCalendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.DAY_OF_YEAR) == messageCalendar.get(Calendar.DAY_OF_YEAR)) {
            formatter = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // Today's messages
        }
        // Check if the message is from the current week
        else if (currentCalendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.WEEK_OF_YEAR) == messageCalendar.get(Calendar.WEEK_OF_YEAR)) {
            formatter = new SimpleDateFormat("E hh:mm a", Locale.getDefault()); // This week's messages
        } else {
            formatter = new SimpleDateFormat("MM/dd/yy hh:mm a", Locale.getDefault()); // Older messages
        }

        return formatter.format(messageDate);
    }

}
