package com.foodism.givegrub.main_fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foodism.givegrub.R;
import com.foodism.givegrub.group.adapters.GroupItem;
import com.foodism.givegrub.group.adapters.GroupListAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageButton addGroupButton;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private RecyclerView groupChatRecyclerView;
    private GroupListAdapter groupListAdapter;
    private List<GroupItem> groupItemList;
    private Uri groupImageUri;
    private FirebaseStorage storage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        groupChatRecyclerView = view.findViewById(R.id.groupChatRecyclerView);
        groupItemList = new ArrayList<>();
        groupListAdapter = new GroupListAdapter(getContext(), groupItemList);
        groupChatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        groupChatRecyclerView.setAdapter(groupListAdapter);

        addGroupButton = view.findViewById(R.id.addGroupButton);
        addGroupButton.setOnClickListener(v -> showAddGroupDialog());

        loadGroups();

        return view;
    }

    private void showAddGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_group, null);
        builder.setView(dialogView);

        final EditText groupNameInput = dialogView.findViewById(R.id.groupNameEditText);
        final EditText groupDescriptionInput = dialogView.findViewById(R.id.groupDescriptionEditText);
        final ImageView groupImageView = dialogView.findViewById(R.id.groupImageView);
        Button createGroupButton = dialogView.findViewById(R.id.createGroupButton);

        groupImageView.setOnClickListener(v -> openImagePicker());

        AlertDialog dialog = builder.create();
        createGroupButton.setOnClickListener(v -> {
            String groupName = groupNameInput.getText().toString().trim();
            String groupDescription = groupDescriptionInput.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a group name", Toast.LENGTH_SHORT).show();
                return;
            }
            createGroup(groupName, groupDescription, dialog);
        });

        dialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            groupImageUri = data.getData();
        }
    }

    private void createGroup(String groupName, String groupDescription, AlertDialog dialog) {
        String userId = auth.getCurrentUser().getUid();
        String groupId = firestore.collection("groups").document().getId();

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("groupName", groupName);
        groupData.put("groupDescription", groupDescription);
        groupData.put("createdBy", userId);
        groupData.put("createdAt", Timestamp.now());
        groupData.put("members", List.of(userId));
        groupData.put("lastMessage", "");
        groupData.put("lastMessageSender", "");
        groupData.put("lastMessageType", "");
        groupData.put("lastMessageFood","");
        groupData.put("lastMessage_timestamp", Timestamp.now());

        firestore.collection("groups")
                .document(groupId)
                .set(groupData)
                .addOnSuccessListener(aVoid -> {
                    if (groupImageUri != null) {
                        uploadGroupImage(groupImageUri, groupId, dialog);
                    } else {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Group created successfully!", Toast.LENGTH_SHORT).show();
                        loadGroups();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to create group", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadGroupImage(Uri imageUri, String groupId, AlertDialog dialog) {
        StorageReference imageRef = storage.getReference("group_images/" + groupId + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    firestore.collection("groups")
                            .document(groupId)
                            .update("profileImageUrl", imageUrl)
                            .addOnSuccessListener(aVoid -> {
                                dialog.dismiss();
                                Toast.makeText(getContext(), "Group created successfully with image!", Toast.LENGTH_SHORT).show();
                                loadGroups();
                            });
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadGroups() {
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("groups")
                .whereArrayContains("members", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        groupItemList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String groupId = document.getId();
                            String groupName = document.getString("groupName");
                            String groupDescription = document.getString("groupDescription");
                            String lastMessage = document.getString("lastMessage");
                            String lastMessageSender = document.getString("lastMessageSender");
                            String lastMessageType = document.getString("lastMessageType");
                            String profileImageUrl = document.getString("profileImageUrl");
                            Timestamp lastMessageTimestamp = document.getTimestamp("lastMessage_timestamp");

                            if (lastMessage == null || lastMessage.isEmpty()) {
                                lastMessage = "Start the conversation...";
                            }

                            groupItemList.add(new GroupItem(groupId, groupName, groupDescription, lastMessage, lastMessageSender, lastMessageType, lastMessageTimestamp, profileImageUrl,0));
                        }
                        groupListAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error loading groups", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
