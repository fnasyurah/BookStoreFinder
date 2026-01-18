package com.example.bookstorefinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ReviewFeedActivity extends AppCompatActivity {

    private RecyclerView recyclerViewReviews;
    private TextView textViewEmpty;
    private FloatingActionButton fabAddReview;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private DatabaseReference databaseReference;
    private static final String TAG = "ReviewFeedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_feed);

        Log.d(TAG, "=== ReviewFeedActivity STARTED ===");

        // Initialize views
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        fabAddReview = findViewById(R.id.fabAddReview);

        // Setup RecyclerView
        recyclerViewReviews.setHasFixedSize(true);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        recyclerViewReviews.setAdapter(reviewAdapter);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("reviews");
        Log.d(TAG, "Firebase reference: " + databaseReference.toString());

        // Load reviews from Firebase
        loadReviews();

        // FAB click listener - FIXED: Uncommented and implemented
        fabAddReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "FAB clicked - Opening PostReviewActivity");

                // Open PostReviewActivity
                Intent intent = new Intent(ReviewFeedActivity.this, PostReviewActivity.class);
                startActivity(intent);

                // Optional: Add animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void loadReviews() {
        Log.d(TAG, "Loading reviews from Firebase...");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange called");
                reviewList.clear();

                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Data exists. Children count: " + dataSnapshot.getChildrenCount());

                    int reviewCount = 0;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Log.d(TAG, "Processing snapshot key: " + snapshot.getKey());

                        Review review = snapshot.getValue(Review.class);
                        if (review != null) {
                            Log.d(TAG, "Review loaded: " + review.getBookstoreName() + " by " + review.getUserName());
                            reviewList.add(review);
                            reviewCount++;
                        } else {
                            Log.e(TAG, "Failed to convert snapshot to Review object");
                        }
                    }

                    Log.d(TAG, "Total reviews loaded: " + reviewCount);

                    // Show/hide empty state
                    if (reviewCount > 0) {
                        textViewEmpty.setVisibility(View.GONE);
                        recyclerViewReviews.setVisibility(View.VISIBLE);
                    } else {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerViewReviews.setVisibility(View.GONE);
                    }
                } else {
                    // No reviews found
                    Log.d(TAG, "No reviews found in database");
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewReviews.setVisibility(View.GONE);
                }

                reviewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase error: " + databaseError.getMessage());

                // Handle error
                textViewEmpty.setText("Error loading reviews: " + databaseError.getMessage());
                textViewEmpty.setVisibility(View.VISIBLE);
                recyclerViewReviews.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== ReviewFeedActivity RESUMED ===");

        // Refresh data when returning to this activity
        if (reviewAdapter != null) {
            reviewAdapter.notifyDataSetChanged();
        }
    }
}