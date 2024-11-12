package com.foodism.givegrub.chatinterface.adapters;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Message {
    private String text;
    private String sender;
    private String type;
    private Timestamp timestamp;

    public Message() {}  // Default constructor required for Firebase

    public Message(String text, String sender, String type, Timestamp timestamp) {
        this.text = text;
        this.sender = sender;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getText() { return text; }
    public String getSender() { return sender; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getTypes() { return type; }

    // Format the timestamp based on the current date, week, or older
    public String formatTimestamp(Timestamp timestamp) {
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
