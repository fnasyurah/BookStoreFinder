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

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView welcomeText;
    private Button btnNearby, btnScanner, btnReviews, btnLogout, btnAbout, btnBookStores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        welcomeText = findViewById(R.id.textWelcome);
        btnNearby = findViewById(R.id.btnNearby);
        btnScanner = findViewById(R.id.btnScanner);
        btnReviews = findViewById(R.id.btnReviews);
        btnLogout = findViewById(R.id.btnLogout);


        ImageView btnAbout = findViewById(R.id.btnAbout);

        if (currentUser != null) {
            String email = currentUser.getEmail();
            welcomeText.setText("Welcome, " + email + "!");
        }

        btnNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change from Toast to actual activity
                startActivity(new Intent(MainActivity.this, NearbyStoresActivity.class));
            }
        });
        // In MainActivity.java, update the scanner button onClickListener:
        btnScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Open Scanner Activity
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
                // Change from Toast to open ReviewFeedActivity
                startActivity(new Intent(MainActivity.this, ReviewFeedActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "About button clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, AboutUsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("MainActivity", "Error starting AboutUsActivity: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}