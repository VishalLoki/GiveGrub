package com.foodism.givegrub.chatinterface;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.foodism.givegrub.chatinterface.adapters.MessageAdapter;
import com.foodism.givegrub.chatinterface.adapters.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatInterfaceActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private String participantUid;
    private String participantUsername;
    private String currentUserUid;
    private EditText messageInput;
    private ImageView sendButton, diningButton;
    private TextView contactName;
    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().setNavigationBarColor(getColor(R.color.primary_the_darkest_color));
        getWindow().setStatusBarColor(getColor(R.color.primary_the_darkest_color));
        setContentView(R.layout.activity_chat_interface);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestore = FirebaseFirestore.getInstance();
        participantUid = getIntent().getStringExtra("participant_uid");
        currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        participantUsername = getIntent().getStringExtra("participant_username");

        messageInput = findViewById(R.id.messageInput);
        contactName = findViewById(R.id.contactName);
        sendButton = findViewById(R.id.sendButton);
        diningButton = findViewById(R.id.diningButton);

        ImageView backBtn = findViewById(R.id.backButton);
        if (backBtn != null) {
            backBtn.setOnClickListener(view -> onBackPressed());
        }
        contactName.setText(participantUsername);

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

        // RecyclerView setup
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserUid);
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

        // Generate possible chat document IDs
        String chatId1 = currentUserUid + "_" + participantUid;
        String chatId2 = participantUid + "_" + currentUserUid;

        // Check if either chat document exists
        DocumentReference chatRef1 = firestore.collection("chats").document(chatId1);
        DocumentReference chatRef2 = firestore.collection("chats").document(chatId2);

        chatRef1.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                sendMessage(chatRef1, messageText);
            } else {
                // If first chat document does not exist, check the alternative document
                chatRef2.get().addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful() && task2.getResult().exists()) {
                        sendMessage(chatRef2, messageText);
                    } else {
                        // If neither document exists, consider creating the chat document if needed
                        sendMessage(chatRef1, messageText);  // Use chatRef1 by default for new chat
                    }
                });
            }
        });
    }

    private void sendMessage(DocumentReference chatRef, String messageText) {
        // Reference to the 'messages' subcollection
        if(messageText.equals("Accept") || messageText.equals("accept")){
            checkTypeAndSenderFood(chatRef);
        }
        CollectionReference messagesRef = chatRef.collection("messages");

        // Prepare the message data, including the "sender" field
        Map<String, Object> message = new HashMap<>();
        message.put("text", messageText);
        message.put("type", "text");
        message.put("imageUrl", null);
        message.put("timestamp", Timestamp.now());
        message.put("sender", currentUserUid);
        message.put("status",null);
        // Clear the input field after sending
        messageInput.setText("");

        // Add message to the messages subcollection
        messagesRef.add(message).addOnSuccessListener(documentReference -> {
            // Update lastMessage and lastMessage_timestamp in the main chat document
            String lastMessage = messageText.length() > 35 ? messageText.substring(0, 35) + "..." : messageText;
            updateLastMessage(chatRef, lastMessage,"text","N/A");
        }).addOnFailureListener(e -> {
            // Handle the error (you might want to show a Toast or log the error)
        });
    }

    private void updateLastMessage(DocumentReference chatRef, String lastMessageText,String lastMessageType,String lastMessageFood) {
        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", lastMessageText);
        chatUpdate.put("lastMessage_timestamp", Timestamp.now());
        chatUpdate.put("lastMessageType", lastMessageType);
        chatUpdate.put("lastMessageSender", currentUserUid);
        chatUpdate.put("lastMessageFood",lastMessageFood);

        // Update chat document with last message details
        chatRef.update(chatUpdate);
    }

    private void retrieveMessages() {
        String chatId1 = currentUserUid + "_" + participantUid;
        String chatId2 = participantUid + "_" + currentUserUid;

        firestore.collection("chats").document(chatId1).get().addOnCompleteListener(task -> {
            DocumentReference chatRef = task.isSuccessful() && task.getResult().exists()
                    ? firestore.collection("chats").document(chatId1)
                    : firestore.collection("chats").document(chatId2);

            // Listen for messages collection
            chatRef.collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (e != null) {
                            Log.w("ChatInterfaceActivity", "Listen failed.", e);
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
                                String type = doc.getString("type");
                                Timestamp timestamp = doc.getTimestamp("timestamp");

                                // Log the values for debugging purposes
                                Log.d("ChatInterfaceActivity", "Text: " + text + ", Sender: " + sender + ", Type: " + type + ", Timestamp: " + timestamp);

                                // Create the message object manually
                                Message message = new Message(text, sender, type, timestamp);
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
                Toast.makeText(ChatInterfaceActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
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

        // Generate possible chat document IDs
        String chatId1 = currentUserUid + "_" + participantUid;
        String chatId2 = participantUid + "_" + currentUserUid;

        // Check if either chat document exists
        DocumentReference chatRef1 = firestore.collection("chats").document(chatId1);
        DocumentReference chatRef2 = firestore.collection("chats").document(chatId2);

        chatRef1.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                sendMessageWithFood(chatRef1, foodItem,venue,peopleCount);
            } else {
                // If first chat document does not exist, check the alternative document
                chatRef2.get().addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful() && task2.getResult().exists()) {
                        sendMessageWithFood(chatRef2, foodItem,venue,peopleCount);
                    } else {
                        // If neither document exists, consider creating the chat document if needed
                        sendMessageWithFood(chatRef1, foodItem,venue,peopleCount);  // Use chatRef1 by default for new chat
                    }
                });
            }
        });
    }

    private void sendMessageWithFood(DocumentReference chatRef, String foodItem, String venue,String peopleCount) {
        // Reference to the 'messages' subcollection
        CollectionReference messagesRef = chatRef.collection("messages");

        // Prepare the message data, including the "sender" field
        Map<String, Object> message = new HashMap<>();
        message.put("text", foodItem+"#%"+venue+"#%"+peopleCount);
        message.put("type", "food");
        message.put("imageUrl", null);
        message.put("timestamp", Timestamp.now());
        message.put("sender", currentUserUid);
        message.put("status","pending");
        // Clear the input field after sending
        messageInput.setText("");

        // Add message to the messages subcollection
        messagesRef.add(message).addOnSuccessListener(documentReference -> {
            // Update lastMessage and lastMessage_timestamp in the main chat document
            String lastMessageFood = foodItem+"#%"+venue+"#%"+peopleCount;
            String lastMessage = "\uD83C\uDF5C\uD83C\uDF2E Food Ordered from "+venue;
            String lastMessages = lastMessage.length() > 35 ? lastMessage.substring(0, 35) + "..." : lastMessage;
            updateLastMessage(chatRef, lastMessages,"food",lastMessageFood);
        }).addOnFailureListener(e -> {
            // Handle the error (you might want to show a Toast or log the error)
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
                    if ("food".equals(lastMessageType) && !currentUserUid.equals(lastMessageSender)) {
                        assert lastMessageFood != null;
                        updateOrderHistory(lastMessageFood,lastMessageSender,lastMessageTimestamp);
                    }
                    return;
                } else {
                    Log.d("ChatInterfaceActivity", "Document does not exist.");
                }
            } else {
                Log.w("ChatInterfaceActivity", "Failed to get document.", task.getException());
            }
        });
    }

    public void updateOrderHistory(String lastMessageFood, String lastMessageSender, Timestamp lastMessageTimestamp) {
        // Split the food message to extract individual pieces of information
        String[] food = lastMessageFood.split("#%");
        String foodItem = food[0];
        String venue = food[1];
        String peopleCount = food[2];

        // Get the current user's UID (assuming currentUserUid is available)
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

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
                                .document(currentUserUid)
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