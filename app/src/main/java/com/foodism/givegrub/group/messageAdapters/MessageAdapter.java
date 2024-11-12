package com.foodism.givegrub.group.messageAdapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.foodism.givegrub.R;
import com.foodism.givegrub.group.messageAdapters.Message;

import org.w3c.dom.Text;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_OUTGOING_TEXT = 1;
    private static final int VIEW_TYPE_INCOMING_TEXT = 2;
    private static final int VIEW_TYPE_OUTGOING_FOOD = 3;
    private static final int VIEW_TYPE_INCOMING_FOOD = 4;

    private List<com.foodism.givegrub.group.messageAdapters.Message> messageList;
    private String currentUserUid;

    public MessageAdapter(List<com.foodism.givegrub.group.messageAdapters.Message> messageList, String currentUserUid) {
        this.messageList = messageList;
        this.currentUserUid = currentUserUid;
    }

    @Override
    public int getItemViewType(int position) {
        com.foodism.givegrub.group.messageAdapters.Message message = messageList.get(position);

        if (message == null) {
            // Log message as null for debugging if needed
            return VIEW_TYPE_INCOMING_TEXT;  // Default as incoming text
        }

        String type = message.getTypes();
        String sender = message.getSender();

        // Check if type is null or empty
        if ("text".equals(type)) {
            return currentUserUid.equals(sender) ? VIEW_TYPE_OUTGOING_TEXT : VIEW_TYPE_INCOMING_TEXT;
        } else if ("food".equals(type)) {
            return currentUserUid.equals(sender) ? VIEW_TYPE_OUTGOING_FOOD : VIEW_TYPE_INCOMING_FOOD;
        } else {
            // Log type and sender for debugging
            Log.e("check null","Unknown type or sender is null: Type = " + type + ", Sender = " + sender);
            return VIEW_TYPE_INCOMING_TEXT;  // Default to incoming text
        }
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_OUTGOING_TEXT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_message_outgoing, parent, false);
                return new com.foodism.givegrub.group.messageAdapters.MessageAdapter.TextMessageViewHolder(view);
            case VIEW_TYPE_INCOMING_TEXT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_message_incoming, parent, false);
                return new com.foodism.givegrub.group.messageAdapters.MessageAdapter.TextMessageViewHolder(view);
            case VIEW_TYPE_OUTGOING_FOOD:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_message_outgoing_food, parent, false);
                return new com.foodism.givegrub.group.messageAdapters.MessageAdapter.FoodMessageViewHolder(view);
            case VIEW_TYPE_INCOMING_FOOD:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_message_incoming_food, parent, false);
                return new com.foodism.givegrub.group.messageAdapters.MessageAdapter.FoodMessageViewHolder(view);
            default:
                // Should never reach here if getItemViewType is implemented correctly
                throw new IllegalStateException("Unexpected view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        String type = message != null ? message.getTypes() : null;

        if (holder instanceof com.foodism.givegrub.group.messageAdapters.MessageAdapter.TextMessageViewHolder && "text".equals(type)) {
            com.foodism.givegrub.group.messageAdapters.MessageAdapter.TextMessageViewHolder textHolder = (com.foodism.givegrub.group.messageAdapters.MessageAdapter.TextMessageViewHolder) holder;
            if (message != null && message.getText() != null) {
                textHolder.messageText.setText(message.getText());
            } else {
                textHolder.messageText.setText(""); // Default text if null
            }
            if (textHolder.timestamp != null && message != null) {
                textHolder.timestamp.setText(message.formatTimestamp(message.getTimestamp()));
            }
            if (textHolder.userName != null && message != null) {
                textHolder.userName.setText(message.getSenderName());
            }
        } else if (holder instanceof com.foodism.givegrub.group.messageAdapters.MessageAdapter.FoodMessageViewHolder && "food".equals(type)) {
            com.foodism.givegrub.group.messageAdapters.MessageAdapter.FoodMessageViewHolder foodHolder = (com.foodism.givegrub.group.messageAdapters.MessageAdapter.FoodMessageViewHolder) holder;
            if (message != null && message.getText() != null) {
                String[] messageText = message.getText().split("#%");
                foodHolder.foodItem.setText(messageText.length > 0 ? "\uD83C\uDF5BFood : "+messageText[0] : "N/A");
                foodHolder.venue.setText(messageText.length > 1 ? "\uD83D\uDCCDLocation : "+messageText[1] : "N/A");
                foodHolder.peopleCount.setText(messageText.length > 2 ? "\uD83D\uDE4D\uD83C\uDFFBPeople : "+messageText[2]+" serves" : "N/A");
            }
            if (foodHolder.timestamp != null && message != null) {
                foodHolder.timestamp.setText(message.formatTimestamp(message.getTimestamp()));
            }
            if (foodHolder.userName != null && message != null) {
                foodHolder.userName.setText(message.getSenderName());
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    static class TextMessageViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView messageText;
        TextView timestamp;

        TextMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            messageText = itemView.findViewById(R.id.messageText);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }

    static class FoodMessageViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView foodItem;
        TextView venue;
        TextView peopleCount;
        TextView timestamp;

        FoodMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            foodItem = itemView.findViewById(R.id.foodItem);
            venue = itemView.findViewById(R.id.venue);
            peopleCount = itemView.findViewById(R.id.peopleCount);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}

