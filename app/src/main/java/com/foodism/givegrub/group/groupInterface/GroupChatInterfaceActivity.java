package com.foodism.givegrub.group.groupInterface;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foodism.givegrub.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import com.foodism.givegrub.group.messageAdapters.Message;
import com.foodism.givegrub.group.messageAdapters.MessageAdapter;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatInterfaceActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private String groupId,groupName,profileImageUrl,lastMessageType,lastMessageSender;
    private String userUid;
    private EditText messageInput;
    private ImageView sendButton, diningButton, addUserButton;
    private TextView groupNameTextView;
    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().setNavigationBarColor(getColor(R.color.primary_the_darkest_color));
        getWindow().setStatusBarColor(getColor(R.color.primary_the_darkest_color));
        setContentView(R.layout.activity_group_chat_interface);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestore = FirebaseFirestore.getInstance();
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        diningButton = findViewById(R.id.diningButton);
        addUserButton = findViewById(R.id.addUserButton);
        groupNameTextView = findViewById(R.id.groupName);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");
        profileImageUrl = getIntent().getStringExtra("profileImageUrl");
        lastMessageType = getIntent().getStringExtra("lastMessageType");
        lastMessageSender = getIntent().getStringExtra("lastMessageSender");

        ImageView backBtn = findViewById(R.id.backButton);
        if (backBtn != null) {
            backBtn.setOnClickListener(view -> onBackPressed());
        }
        groupNameTextView.setText(groupName);

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                sendButton.setEnabled(charSequence.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        sendButton.setOnClickListener(view -> checkAndSendMessage());
        diningButton.setOnClickListener(view -> showFoodDetailsDialog());
        addUserButton.setOnClickListener(view -> showAddUserDialog());

        // RecyclerView setup
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, userUid);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);

        retrieveMessages();

        final View rootLayout = findViewById(R.id.main);
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                rootLayout.getWindowVisibleDisplayFrame(rect);

                int screenHeight = rootLayout.getRootView().getHeight();
                int keypadHeight = screenHeight - rect.bottom;

                if (keypadHeight > screenHeight * 0.15) { // keyboard is open
                    if (!messageList.isEmpty()) {
                        messagesRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                    }
                }
            }
        });

    }

    private void checkAndSendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Check if either chat document exists
        DocumentReference chatRef = firestore.collection("groups").document(groupId);

        chatRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                sendMessage(chatRef, messageText);
            }else{
                Toast.makeText(this,"Failed! Chat not send",Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void sendMessage(DocumentReference chatRef, String messageText) {
        // Reference to the 'messages' subcollection
        if(messageText.equals("Accept") || messageText.equals("accept")){
            checkTypeAndSenderFood(chatRef);
        }
        // Firestore reference to the 'usernames' collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Fetch the username from the 'usernames' collection based on the userUid
        db.collection("usernames")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Find the document where the uid matches the userUid
                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().stream()
                            .filter(doc -> doc.getString("uid").equals(userUid))
                            .findFirst()
                            .orElse(null);

                    if (userDoc != null) {
                        // Get the username from the document name (document id)
                        String username = userDoc.getId();

                        // Reference to the 'messages' subcollection
                        CollectionReference messagesRef = chatRef.collection("messages");

                        // Prepare the message data, including the "sender" field
                        Map<String, Object> message = new HashMap<>();
                        message.put("text", messageText);
                        message.put("type", "text");
                        message.put("imageUrl", null);
                        message.put("timestamp", Timestamp.now());
                        message.put("senderName", username);  // Set senderName to the fetched username
                        message.put("sender", userUid);
                        message.put("status", null);

                        // Clear the input field after sending
                        messageInput.setText("");

                        // Add message to the messages subcollection
                        messagesRef.add(message).addOnSuccessListener(documentReference -> {
                            // Update lastMessage and lastMessage_timestamp in the main chat document
                            String lastMessage = messageText.length() > 35 ? messageText.substring(0, 35) + "..." : messageText;
                            updateLastMessage(chatRef, lastMessage, "text", "N/A");
                        }).addOnFailureListener(e -> {
                            // Handle the error (you might want to show a Toast or log the error)
                            Log.e("sendMessageError", "Error sending message: ", e);
                        });
                    } else {
                        // Handle the case when the user document is not found
                        Log.w("sendMessageError", "User not found for userUid: " + userUid);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle the error if there's an issue fetching the username
                    Log.e("sendMessageError", "Error fetching username: ", e);
                });
    }


    private void updateLastMessage(DocumentReference chatRef, String lastMessageText,String lastMessageType,String lastMessageFood) {
        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", lastMessageText);
        chatUpdate.put("lastMessage_timestamp", Timestamp.now());
        chatUpdate.put("lastMessageType", lastMessageType);
        chatUpdate.put("lastMessageSender", userUid);
        chatUpdate.put("lastMessageFood",lastMessageFood);

        // Update chat document with last message details
        chatRef.update(chatUpdate);
    }

    private void retrieveMessages() {

        // Check if either chat document exists
        DocumentReference chatRef = firestore.collection("groups").document(groupId);

        // Listen for messages collection
        chatRef.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.w("GroupChatInterfaceActivity", "Listen failed.", e);
                        return;
                    }

                    // Clear existing messages in the list
                    messageList.clear();

                    // If querySnapshot exists and has documents, process them
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            // Manually map the fields
                            String text = doc.getString("text");
                            String sender = doc.getString("sender");
                            String senderName = doc.getString("senderName");
                            String type = doc.getString("type");
                            Timestamp timestamp = doc.getTimestamp("timestamp");

                            // Log the values for debugging purposes
                            Log.d("GroupChatInterfaceActivity", "Text: " + text + ", Sender: " + sender + ", Type: " + type + ", Timestamp: " + timestamp);

                            // Create the message object manually
                            com.foodism.givegrub.group.messageAdapters.Message message = new com.foodism.givegrub.group.messageAdapters.Message(text, sender,senderName, type, timestamp);
                            messageList.add(message);
                        }
                    }

                    // Notify adapter to update the list
                    messageAdapter.notifyDataSetChanged();

                    // Scroll to the last message only if messageList is not empty
                    if (!messageList.isEmpty()) {
                        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void showAddUserDialog() {
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_group_add_chat, null);

        // Find the views in the custom layout
        EditText inputUsername = dialogView.findViewById(R.id.inputUsername);
        Button buttonAddUser = dialogView.findViewById(R.id.buttonAddUser);

        // Create the AlertDialog builder and set the custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Set up the button click listener
        buttonAddUser.setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            if (!username.isEmpty()) {
                // Check if the username exists in the usernames collection
                checkAndAddUserToGroup(groupId, username);
                dialog.dismiss(); // Dismiss the dialog after adding
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            }
        });

        // Show the dialog
        dialog.show();
    }

    private void checkAndAddUserToGroup(String groupId, String userName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Reference to the "usernames" collection, document with name as userName
        DocumentReference userRef = db.collection("usernames").document(userName);

        // Check if the username exists in the "usernames" collection
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Username exists, retrieve the UID
                String userUid = documentSnapshot.getString("uid");

                if (userUid != null) {
                    // UID retrieved, now add to the "members" array in the "groups" collection
                    DocumentReference groupRef = db.collection("groups").document(groupId);

                    groupRef.update("members", FieldValue.arrayUnion(userUid))
                            .addOnSuccessListener(aVoid -> {
                                // Successfully added user to the group
                                Toast.makeText(this, userName + " added to the group.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                // Failed to add user to the group
                                Toast.makeText(this, "Failed to add user to group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    // UID not found for the username
                    Toast.makeText(this, "UID not found for username.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Username does not exist in the "usernames" collection
                Toast.makeText(this, "Username not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            // Error fetching the document
            Toast.makeText(this, "Error checking username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }





    private void showFoodDetailsDialog() {
        // Inflate the dialog_food_details layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_food_details, null);

        // Initialize the EditTexts and Button in the dialog
        EditText foodEditText = dialogView.findViewById(R.id.foodEditText);
        EditText venueEditText = dialogView.findViewById(R.id.venueEditText);
        EditText peopleCountEditText = dialogView.findViewById(R.id.peopleCountEditText);
        Button sendButton = dialogView.findViewById(R.id.sendButton);

        // Build and show the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set the click listener for the send button inside the dialog
        sendButton.setOnClickListener(v -> {
            // Retrieve the entered data
            String food = foodEditText.getText().toString().trim();
            String venue = venueEditText.getText().toString().trim();
            String peopleCount = peopleCountEditText.getText().toString().trim();

            // Check if all fields are filled
            if (food.isEmpty() || venue.isEmpty() || peopleCount.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Perform the action with the input data (e.g., send it as a message)
            checkAndSendMessageWithFood(food, venue, peopleCount);

            // Dismiss the dialog
            dialog.dismiss();
        });
    }

    private void checkAndSendMessageWithFood(String food,String ven,String count) {
        String foodItem = food.trim();
        String venue = ven.trim();
        String peopleCount = count.trim();
        if (foodItem.isEmpty() || venue.isEmpty() || peopleCount.isEmpty()) {
            return;
        }

        // Check if either chat document exists
        DocumentReference chatRef = firestore.collection("groups").document(groupId);

        chatRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                sendMessageWithFood(chatRef, foodItem,venue,peopleCount);
            } else {
                Toast.makeText(this,"Failed! Try again",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessageWithFood(DocumentReference chatRef, String foodItem, String venue,String peopleCount) {

        // Firestore reference to the 'usernames' collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Fetch the username from the 'usernames' collection based on the userUid
        db.collection("usernames")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Find the document where the uid matches the userUid
                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().stream()
                            .filter(doc -> doc.getString("uid").equals(userUid))
                            .findFirst()
                            .orElse(null);
                    if (userDoc != null) {
                        // Get the username from the document name (document id)
                        String username = userDoc.getId();
                        // Reference to the 'messages' subcollection
                        CollectionReference messagesRef = chatRef.collection("messages");

                        // Prepare the message data, including the "sender" field
                        Map<String, Object> message = new HashMap<>();
                        message.put("text", foodItem + "#%" + venue + "#%" + peopleCount);
                        message.put("type", "food");
                        message.put("imageUrl", null);
                        message.put("timestamp", Timestamp.now());
                        message.put("senderName", username);
                        message.put("sender", userUid);
                        message.put("status", "pending");
                        // Clear the input field after sending
                        messageInput.setText("");

                        // Add message to the messages subcollection
                        messagesRef.add(message).addOnSuccessListener(documentReference -> {
                            // Update lastMessage and lastMessage_timestamp in the main chat document
                            String lastMessageFood = foodItem + "#%" + venue + "#%" + peopleCount;
                            String lastMessage = "\uD83C\uDF5C\uD83C\uDF2E Food Ordered from " + venue;
                            String lastMessages = lastMessage.length() > 35 ? lastMessage.substring(0, 35) + "..." : lastMessage;
                            updateLastMessage(chatRef, lastMessages, "food", lastMessageFood);
                        }).addOnFailureListener(e -> {
                            // Handle the error (you might want to show a Toast or log the error)
                        });
                    }else {
                        // Handle the case when the user document is not found
                        Log.w("sendMessageError", "User not found for userUid: " + userUid);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle the error if there's an issue fetching the username
                    Log.e("sendMessageError", "Error fetching username: ", e);
                });
    }

    public void checkTypeAndSenderFood(DocumentReference chatRef){
        // Retrieve the document from Firestore
        chatRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Get the document snapshot
                DocumentSnapshot documentSnapshot = task.getResult();

                if (documentSnapshot.exists()) {
                    // Access specific fields like 'lastMessageSender' and 'lastMessageType'
                    String lastMessageSender = documentSnapshot.getString("lastMessageSender");
                    String lastMessageType = documentSnapshot.getString("lastMessageType");
                    String lastMessageFood = documentSnapshot.getString("lastMessageFood");
                    Timestamp lastMessageTimestamp = documentSnapshot.getTimestamp("lastMessage_timestamp");

                    // Check if the last message sender is not the current user
                    if ("food".equals(lastMessageType) && !userUid.equals(lastMessageSender)) {
                        assert lastMessageFood != null;
                        updateOrderHistory(lastMessageFood,lastMessageSender,lastMessageTimestamp);
                    }
                    return;
                } else {
                    Log.d("GroupChatInterfaceActivity", "Document does not exist.");
                }
            } else {
                Log.w("GroupChatInterfaceActivity", "Failed to get document.", task.getException());
            }
        });
    }

    public void updateOrderHistory(String lastMessageFood, String lastMessageSender, Timestamp lastMessageTimestamp) {
        // Split the food message to extract individual pieces of information
        String[] food = lastMessageFood.split("#%");
        String foodItem = food[0];
        String venue = food[1];
        String peopleCount = food[2];

        // Firestore reference to the 'usernames' collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Retrieve the username for the lastMessageSender UID
        db.collection("usernames").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String username = queryDocumentSnapshots.getDocuments().stream()
                            .filter(document -> lastMessageSender.equals(document.getString("uid")))
                            .findFirst()
                            .map(DocumentSnapshot::getId)
                            .orElse(null);

                    if (username != null && !username.isEmpty()) {
                        // Create a reference to the 'orderhistory' subcollection under the current user document in 'users'
                        CollectionReference orderHistoryRef = db.collection("users")
                                .document(userUid)
                                .collection("orderhistory");

                        // Add a new document to 'orderhistory'
                        orderHistoryRef.add(new HashMap<String, Object>() {{
                            put("foodItem", foodItem);
                            put("venue", venue);
                            put("peopleCount", peopleCount);
                            put("foodDonor", lastMessageSender);
                            put("foodDonorName", username);
                            put("timestamp", lastMessageTimestamp);
                        }}).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("updateOrderHistory", "Order history updated successfully.");
                                Toast.makeText(this, "Order Accepted", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("updateOrderHistory", "Failed to update order history.", task.getException());
                            }
                        });
                    } else {
                        // Username not found for lastMessageSender UID
                        Toast.makeText(this, "Failed! Try Again", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure to retrieve username
                    Toast.makeText(this, "Failed to retrieve username", Toast.LENGTH_SHORT).show();
                });
    }
}