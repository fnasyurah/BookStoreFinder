package com.example.bookstorefinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
                mAuth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
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
    }
}