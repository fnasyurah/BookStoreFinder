package com.example.bookstorefinder;

import android.content.Intent;
import android.os.Bundle;
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

        File storageDir = new File(getFilesDir(), "review_images");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("reviews");

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
                // Open Post Review Screen
                startActivity(new Intent(ReviewFeedActivity.this, PostReviewActivity.class));
            }
        });
    }

    private void loadReviews() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reviewList.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Review review = snapshot.getValue(Review.class);
                        if (review != null) {
                            review.setId(snapshot.getKey());
                            reviewList.add(review);
                        }
                    }

                    // Sort by timestamp (newest first)
                    Collections.sort(reviewList, new Comparator<Review>() {
                        @Override
                        public int compare(Review r1, Review r2) {
                            return Long.compare(r2.getTimestamp(), r1.getTimestamp());
                        }
                    });

                    // Update adapter
                    reviewAdapter.updateData(reviewList);
                }

                // Show/hide empty state
                if (reviewList.isEmpty()) {
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewReviews.setVisibility(View.GONE);
                    textViewEmpty.setText("No reviews yet.\nBe the first to share your experience!");
                } else {
                    textViewEmpty.setVisibility(View.GONE);
                    recyclerViewReviews.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ReviewFeedActivity.this,
                        "Failed to load reviews: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                textViewEmpty.setVisibility(View.VISIBLE);
                textViewEmpty.setText("Error loading reviews. Please check your connection.");
                recyclerViewReviews.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh reviews when returning from posting
        loadReviews();
    }
}