package com.example.bookstorefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView welcomeText;
    private Button btnNearby, btnScanner, btnReviews, btnAbout;
    private ImageView btnLogoutIcon;
    private DatabaseReference userRef;

    // CHANGED: Using FirestoreNotificationListenerService instead of FirestoreNotificationService
    private FirestoreNotificationListenerService notificationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize views
        welcomeText = findViewById(R.id.textWelcome);
        btnNearby = findViewById(R.id.btnNearby);
        btnScanner = findViewById(R.id.btnScanner);
        btnReviews = findViewById(R.id.btnReviews);
        btnAbout = findViewById(R.id.btnAbout);
        btnLogoutIcon = findViewById(R.id.btnLogout);

        // ==================== NOTIFICATION FIXES START ====================
        // CHANGED: Get instance of FirestoreNotificationListenerService
        notificationService = FirestoreNotificationListenerService.getInstance(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Start listening for Firestore notifications
            notificationService.startListening();
            Log.d("MainActivity", "✅ Notification listener started for user: " + currentUser.getUid());

            // Check and request notification permission for Android 13+
            checkNotificationPermission();
        }
        // ==================== NOTIFICATION FIXES END ====================

        // Check if username was passed from LoginActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("username")) {
            String username = intent.getStringExtra("username");
            if (username != null && !username.isEmpty()) {
                welcomeText.setText("Welcome, " + username + "!");
            } else {
                loadUserDataFromDatabase();
            }
        } else {
            loadUserDataFromDatabase();
        }

        // Button listeners
        btnNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NearbyStoresActivity.class));
            }
        });

        btnScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Scanner error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReviewFeedActivity.class));
            }
        });

        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "About Us button clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, AboutUsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("MainActivity", "Error starting AboutUsActivity: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Logout icon click listener
        btnLogoutIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop listening for notifications
                if (notificationService != null) {
                    notificationService.stopListening();
                    Log.d("MainActivity", "✅ Notification listener stopped");
                }

                mAuth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
        // Find the notification button
        ImageView btnNotification = findViewById(R.id.btnNotification); // Add this to your layout
        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
                startActivity(intent);
            }
        });

    }

    // ==================== NEW NOTIFICATION PERMISSION METHOD ====================
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                // Show explanation dialog before requesting
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {

                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Notification Permission")
                            .setMessage("This app needs notification permission to alert you when other users post reviews.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                // Request permission after explanation
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                        101);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    // Directly request permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            101);
                }
            } else {
                Log.d("MainActivity", "Notification permission already granted");
            }
        }
    }

    // ==================== HANDLE PERMISSION RESULT ====================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Notification permission granted", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Notification permission granted by user");
            } else {
                Toast.makeText(this, "❌ Notification permission denied", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Notification permission denied by user");
            }
        }
    }

    private void loadUserDataFromDatabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            userRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Get username from database
                        String username = dataSnapshot.child("username").getValue(String.class);
                        if (username != null && !username.isEmpty()) {
                            welcomeText.setText("Welcome, " + username + "!");
                        } else {
                            // Fallback to email if username not found
                            String email = currentUser.getEmail();
                            if (email != null) {
                                welcomeText.setText("Welcome, " + email + "!");
                            }
                        }
                    } else {
                        // User data not found in database, use email
                        String email = currentUser.getEmail();
                        if (email != null) {
                            welcomeText.setText("Welcome, " + email + "!");
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                    Log.e("MainActivity", "Database error: " + databaseError.getMessage());
                    String email = currentUser.getEmail();
                    if (email != null) {
                        welcomeText.setText("Welcome, " + email + "!");
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when activity resumes
        loadUserDataFromDatabase();

        // ==================== RESTART NOTIFICATION LISTENER ====================
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && notificationService != null) {
            notificationService.startListening();
            Log.d("MainActivity", "Notification listener restarted on resume");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up notification listener
        if (notificationService != null) {
            notificationService.stopListening();
            Log.d("MainActivity", "Notification listener stopped on destroy");
        }
    }
}