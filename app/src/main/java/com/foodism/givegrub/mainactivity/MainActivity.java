package com.foodism.givegrub.mainactivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.foodism.givegrub.R;
import com.foodism.givegrub.main_fragments.ChatFragment;
import com.foodism.givegrub.main_fragments.FeedsFragment;
import com.foodism.givegrub.main_fragments.GroupFragment;
import com.foodism.givegrub.orderHistory.OrderHistoryActivity;
import com.foodism.givegrub.profile.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageButton orderHistoryIcon,settingsIcon;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout mainContent;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.primary_the_darkest_color));
        getWindow().setNavigationBarColor(getColor(R.color.primary_the_darkest_color));
        setContentView(R.layout.activity_main);

        // Initialize FirebaseAuth and FirebaseStorage
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        settingsIcon = findViewById(R.id.settings_icon);
        orderHistoryIcon = findViewById(R.id.orderHistoryIcon);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        mainContent = findViewById(R.id.main_content);

        // Set up Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        loadFragment(new FeedsFragment());

        // Set Profile Icon click listener to open ProfileActivity
        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Set Notification Icon click listener (as previously done)
        orderHistoryIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrderHistoryActivity.class);
            startActivity(intent);
        });

        // Set Bottom Navigation Item Selected Listener
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_feeds) {
                loadFragment(new FeedsFragment());
                return true;
            } else if (itemId == R.id.nav_chats) {
                loadFragment(new ChatFragment());
                return true;
            } else if (itemId == R.id.nav_groups) {
                loadFragment(new GroupFragment());
                return true;
            } else {
                return false;
            }
        });
    }

    // Method to load the fragments
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_content, fragment)
                .commit();
    }
}