package com.example.bookstorefinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is already logged in, go to MainActivity
            startActivity(new Intent(AuthActivity.this, MainActivity.class));
            finish();
        } else {
            // User is not logged in, show login UI
            // For now, redirect to a simple login screen
            // TODO: Replace with your actual login UI
            Toast.makeText(this, "Redirecting to login...", Toast.LENGTH_SHORT).show();

            // Create a simple login intent
            // In your actual app, this would open your LoginFragment/LoginActivity
            Intent loginIntent = new Intent(AuthActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }
    }
}