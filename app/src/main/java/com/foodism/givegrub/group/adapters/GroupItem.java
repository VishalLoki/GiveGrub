package com.foodism.givegrub.group.adapters;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GroupItem {
    private String groupId;
    private String groupName;
    private String groupDescription;
    private String profileImageUrl;
    private String lastMessage;
    private String lastMessageSender;
    private String lastMessageType;
    private Timestamp lastMessageTimestamp;
    private int unreadCount;

    // Constructor
    public GroupItem(String groupId, String groupName, String groupDescription, String lastMessage, String lastMessageSender, String lastMessageType, Timestamp lastMessageTimestamp, String profileImageUrl, int unreadCount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.profileImageUrl = profileImageUrl;
        this.lastMessage = lastMessage;
        this.lastMessageSender = lastMessageSender;
        this.lastMessageType = lastMessageType;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
    }

    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public String getProfileImageUrl() { return profileImageUrl; }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getLastMessageSender() {
        return lastMessageSender;
    }

    public String getLastMessageType(){ return lastMessageType; }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
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
