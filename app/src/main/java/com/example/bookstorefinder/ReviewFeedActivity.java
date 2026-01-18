package com.example.bookstorefinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReviewFeedActivity extends AppCompatActivity {

    private static final String TAG = "ReviewFeedActivity";

    private RecyclerView recyclerViewReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private TextView textViewEmpty;
    private FloatingActionButton fabAddReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_feed);

        Log.d(TAG, "=== ReviewFeedActivity STARTED ===");

        // Create review_images directory if it doesn't exist
        File storageDir = new File(getFilesDir(), "review_images");
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            Log.d(TAG, "Created review_images directory: " + created);
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("reviews");
        Log.d(TAG, "Database reference path: " + databaseReference.toString());

        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Direct database access successful");
                DataSnapshot snapshot = task.getResult();
                Log.d(TAG, "Total reviews in database: " + snapshot.getChildrenCount());
            } else {
                Log.e(TAG, "Direct database access failed: " + task.getException());
            }
        });

        // Initialize views
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        fabAddReview = findViewById(R.id.fabAddReview);

        // Setup RecyclerView
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReviews.setAdapter(reviewAdapter);

        // Load reviews
        loadReviews();

        // Set up Floating Action Button
        fabAddReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if user is logged in before posting
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(ReviewFeedActivity.this,
                            "Please login first to post reviews",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Open Post Review Screen
                Intent intent = new Intent(ReviewFeedActivity.this, PostReviewActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadReviews() {
        Log.d(TAG, "=== Loading reviews from Firebase ===");

        // Show loading state
        textViewEmpty.setText("Loading reviews...");
        textViewEmpty.setVisibility(View.VISIBLE);
        recyclerViewReviews.setVisibility(View.GONE);

        // Remove existing listener first to avoid duplicates
        if (databaseReference != null) {
            databaseReference.removeEventListener(valueEventListener);
        }

        // Add new listener
        databaseReference.addValueEventListener(valueEventListener);
    }

    // Create the ValueEventListener as a class field
    private ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Log.d(TAG, "=== onDataChange called ===");
            Log.d(TAG, "DataSnapshot exists: " + dataSnapshot.exists());
            Log.d(TAG, "DataSnapshot children count: " + dataSnapshot.getChildrenCount());

            reviewList.clear();

            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                int reviewCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Log.d(TAG, "Processing snapshot key: " + snapshot.getKey());

                        // Get the review data
                        Review review = snapshot.getValue(Review.class);
                        if (review != null) {
                            // Set the ID from Firebase key
                            review.setId(snapshot.getKey());

                            // Log review details for debugging
                            Log.d(TAG, "Review " + reviewCount + ":");
                            Log.d(TAG, "  - Bookstore: " + review.getBookstoreName());
                            Log.d(TAG, "  - User: " + review.getUserEmail());
                            Log.d(TAG, "  - Rating: " + review.getRating());

                            reviewList.add(review);
                            reviewCount++;
                        } else {
                            Log.e(TAG, "Review object is null for key: " + snapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing review: " + e.getMessage(), e);
                    }
                }

                Log.d(TAG, "=== Total reviews loaded: " + reviewCount + " ===");

                // Sort by timestamp (newest first)
                if (!reviewList.isEmpty()) {
                    Collections.sort(reviewList, new Comparator<Review>() {
                        @Override
                        public int compare(Review r1, Review r2) {
                            return Long.compare(r2.getTimestamp(), r1.getTimestamp());
                        }
                    });

                    // Update adapter
                    reviewAdapter.updateData(reviewList);

                    // Show reviews
                    textViewEmpty.setVisibility(View.GONE);
                    recyclerViewReviews.setVisibility(View.VISIBLE);

                    Toast.makeText(ReviewFeedActivity.this,
                            "Loaded " + reviewList.size() + " reviews",
                            Toast.LENGTH_SHORT).show();
                } else {
                    showEmptyState("No reviews found in database");
                }
            } else {
                Log.d(TAG, "No reviews found in database or database is empty");
                showEmptyState("No reviews yet.\nBe the first to share your experience!");
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.e(TAG, "=== DATABASE ERROR ===");
            Log.e(TAG, "Code: " + databaseError.getCode());
            Log.e(TAG, "Message: " + databaseError.getMessage());
            Log.e(TAG, "Details: " + databaseError.getDetails());

            Toast.makeText(ReviewFeedActivity.this,
                    "Database error: " + databaseError.getMessage(),
                    Toast.LENGTH_LONG).show();
            showEmptyState("Error loading reviews. Please check your connection.");
        }
    };

    private void showEmptyState(String message) {
        textViewEmpty.setText(message);
        textViewEmpty.setVisibility(View.VISIBLE);
        recyclerViewReviews.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== onResume - refreshing reviews ===");
        // Refresh reviews when returning to this activity
        loadReviews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener to prevent memory leaks
        if (databaseReference != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "=== onStart ===");

    }
}