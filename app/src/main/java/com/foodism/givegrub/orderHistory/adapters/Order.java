package com.foodism.givegrub.orderHistory.adapters;

import com.google.firebase.Timestamp;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Order {
    private String foodDonor;
    private String foodItem;
    private String venue;
    private String peopleCount;
    private Timestamp timestamp;
    private String foodDonorName;

    // Constructor
    public Order(String foodDonor, String foodDonorName, String foodItem, String venue, String peopleCount, Timestamp timestamp) {
        this.foodDonor = foodDonor;
        this.foodDonorName = foodDonorName;
        this.foodItem = foodItem;
        this.venue = venue;
        this.peopleCount = peopleCount;
        this.timestamp = timestamp;
    }

    // Getter methods
    public String getFoodDonor() {
        return foodDonor;
    }

    public String getFoodDonorName(){ return foodDonorName; }

    public String getFoodItem() {
        return foodItem;
    }

    public String getVenue() {
        return venue;
    }

    public String getPeopleCount() {
        return peopleCount;
    }

    public String getTimestamp() {
        return formatTimestamp(timestamp);
    }

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
