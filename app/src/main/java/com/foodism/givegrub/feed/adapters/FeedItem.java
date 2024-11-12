package com.foodism.givegrub.feed.adapters;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FeedItem {
    private String username;
    private String description;
    private String foodItem;
    private String venue;
    private String peopleCount;
    private String imageUrl;
    private String imageName;
    private Timestamp timestamp;
    private String uid;
    private String documentId;

    public FeedItem(String username, String description, String foodItem, String venue, String peopleCount, String imageUrl, String imageName, Timestamp timestamp, String uid, String documentId) {
        this.username = username;
        this.description = description;
        this.foodItem = foodItem;
        this.venue = venue;
        this.peopleCount = peopleCount;
        this.imageUrl = imageUrl;
        this.imageName = imageName;
        this.timestamp = timestamp;
        this.uid = uid;
        this.documentId = documentId;
    }

    // Getters and setters for each field
    public String getUsername() {
        return username;
    }

    public String getDescription() {
        return description;
    }

    public String getFoodItem() {
        return foodItem;
    }

    public String getVenue() {
        return venue;
    }

    public String getPeopleCount() {
        return peopleCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImageName() {
        return imageName;
    }

    public String getTimestamp() {
        return formatTimestamp(timestamp);
    }

    public String getUid() {
        return uid;
    }

    public String getDocumentId(){ return documentId; }

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
