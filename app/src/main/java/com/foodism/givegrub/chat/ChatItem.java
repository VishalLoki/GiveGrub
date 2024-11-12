package com.foodism.givegrub.chat;

public class ChatItem {
    private String profileUrl;
    private String username;
    private String uid2;
    private String lastMessage;
    private String timestamp;
    private int unreadMessages;

    // Constructor
    public ChatItem(String profileUrl, String username,String uid2, String lastMessage, String timestamp, int unreadMessages) {
        this.profileUrl = profileUrl;
        this.username = username;
        this.uid2 = uid2;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadMessages = unreadMessages;
    }

    // Getters
    public String getProfileUrl() { return profileUrl; }
    public String getUsername() { return username; } // New getter
    public String getLastMessage() { return lastMessage; }
    public String getTimestamp() { return timestamp; }
    public int getUnreadMessages() { return unreadMessages; }
    public String getUid2(){ return uid2; }
}
