package com.example.bookstorefinder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ReviewFeedActivity extends AppCompatActivity {

    private RecyclerView recyclerViewReviews;
    private TextView textViewEmpty;
    private FloatingActionButton fabAddReview;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    // Firebase Database (Realtime Database for reviews)
    private DatabaseReference databaseReference;

    // Firebase Firestore (for notifications - optional)
    private FirebaseFirestore firestoreDb;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private ListenerRegistration firestoreListener;

    // Notification-related variables
    private String notificationReviewId = null;
    private boolean fromNotification = false;
    private int previousReviewCount = -1;
    private ValueEventListener reviewListener;
    private ChildEventListener childEventListener;

    private static final String TAG = "ReviewFeedActivity";
    private static final String CHANNEL_ID = "reviews_channel";
    private static final int NOTIFICATION_ID_BASE = 1000;
    private int notificationCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_feed);

        Log.d(TAG, "=== ReviewFeedActivity STARTED ===");

        // Check if opened from notification
        handleNotificationIntent();

        firestore = FirebaseFirestore.getInstance();

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize Firebase Firestore (for notifications)
        firestoreDb = FirebaseFirestore.getInstance();

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

        // Initialize Firebase Database (Realtime Database)
        databaseReference = FirebaseDatabase.getInstance().getReference("reviews");
        Log.d(TAG, "Firebase Database reference: " + databaseReference.toString());

        // Create notification channel (required for Android 8+)
        createNotificationChannel();
        
        // Request notification permission (Android 13+)
        requestNotificationPermissionIfNeeded();

        // ==================== HIGHLIGHT NOTIFICATION REVIEW START ====================
        // If opened from notification, load reviews and then highlight
        if (fromNotification && notificationReviewId != null) {
            // Show message that we're opening from notification
            showNotificationToast();

            // Load reviews first, then highlight
            loadReviewsWithNotification();
        } else {
            // Normal load (not from notification)
            loadReviews();
        }
        // ==================== HIGHLIGHT NOTIFICATION REVIEW END ====================

        // FAB click listener
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

    // ==================== NOTIFICATION HANDLING METHODS START ====================

    /**
     * Handle notification intent - check if activity was opened from notification
     */
    private void handleNotificationIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            // Check for notification review ID
            if (intent.hasExtra("notification_review_id")) {
                notificationReviewId = intent.getStringExtra("notification_review_id");
                fromNotification = true;
                Log.d(TAG, "Opened from notification with review ID: " + notificationReviewId);

                // Clear the intent extra so it doesn't trigger again on resume
                intent.removeExtra("notification_review_id");
            }

            // Also check for Firestore notification data (if using Firestore notifications)
            if (intent.hasExtra("reviewId")) {
                notificationReviewId = intent.getStringExtra("reviewId");
                fromNotification = true;
                Log.d(TAG, "Opened from Firestore notification with review ID: " + notificationReviewId);

                // Clear the intent extra
                intent.removeExtra("reviewId");
            }
        }
    }

    /**
     * Show toast message when opened from notification
     */
    private void showNotificationToast() {
        if (fromNotification) {
            Toast.makeText(this,
                    "📢 Opening review from notification",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load reviews with notification highlighting
     * This loads reviews and then tries to find/highlight the notification review
     */
    private void loadReviewsWithNotification() {
        Log.d(TAG, "Loading reviews with notification highlighting for review ID: " + notificationReviewId);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange called (with notification)");
                reviewList.clear();

                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Data exists. Children count: " + dataSnapshot.getChildrenCount());

                    int reviewCount = 0;
                    int notificationReviewPosition = -1;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Log.d(TAG, "Processing snapshot key: " + snapshot.getKey());

                        Review review = snapshot.getValue(Review.class);
                        if (review != null) {
                            Log.d(TAG, "Review loaded: " + review.getBookstoreName() + " by " + review.getUserName());
                            reviewList.add(review);
                            reviewCount++;

                            // Check if this is the notification review
                            if (notificationReviewId != null &&
                                    notificationReviewId.equals(review.getId())) {
                                notificationReviewPosition = reviewList.size() - 1;
                                Log.d(TAG, "Found notification review at position: " + notificationReviewPosition);
                            }
                        } else {
                            Log.e(TAG, "Failed to convert snapshot to Review object");
                        }
                    }

                    Log.d(TAG, "Total reviews loaded: " + reviewCount);

                    // Show/hide empty state
                    if (reviewCount > 0) {
                        textViewEmpty.setVisibility(View.GONE);
                        recyclerViewReviews.setVisibility(View.VISIBLE);

                        // Highlight notification review if found
                        if (notificationReviewPosition != -1) {
                            highlightNotificationReview(notificationReviewPosition);
                        } else {
                            // If review not found, scroll to top
                            recyclerViewReviews.scrollToPosition(0);
                        }
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

                // Reset notification flag after processing
                fromNotification = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase error: " + databaseError.getMessage());

                // Handle error
                textViewEmpty.setText("Error loading reviews: " + databaseError.getMessage());
                textViewEmpty.setVisibility(View.VISIBLE);
                recyclerViewReviews.setVisibility(View.GONE);

                // Reset notification flag
                fromNotification = false;
            }
        });
    }

    /**
     * Highlight the review that came from notification
     * @param position Position of the review in the list
     */
    private void highlightNotificationReview(int position) {
        // Scroll to the notification review
        recyclerViewReviews.scrollToPosition(position);


        Log.d(TAG, "Scrolled to notification review at position: " + position);

        // Optional: Show a longer toast
        Toast.makeText(this,
                "📖 Showing review from notification",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Mark notification as read in Firestore (if using Firestore notifications)
     */
    private void markNotificationAsRead() {
        if (notificationReviewId != null) {
            // Get current user
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                // Query Firestore for notifications for this user and this review
                firestoreDb.collection("notifications")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("reviewId", notificationReviewId)
                        .whereEqualTo("isRead", false)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                // Mark all matching notifications as read
                                for (int i = 0; i < task.getResult().size(); i++) {
                                    String notificationId = task.getResult().getDocuments().get(i).getId();
                                    firestoreDb.collection("notifications")
                                            .document(notificationId)
                                            .update("isRead", true)
                                            .addOnSuccessListener(aVoid ->
                                                    Log.d(TAG, "Marked notification as read: " + notificationId))
                                            .addOnFailureListener(e ->
                                                    Log.e(TAG, "Error marking notification as read: " + e.getMessage()));
                                }
                            }
                        });
            }
        }
    }

    // ==================== NOTIFICATION HANDLING METHODS END ====================

    /**
     * Original loadReviews method (without notification handling)
     * Used when not opened from notification
     */
    private void loadReviews() {
        Log.d(TAG, "Loading reviews from Firebase Database...");

        // Remove existing listener if any
        if (reviewListener != null) {
            databaseReference.removeEventListener(reviewListener);
        }
        if (childEventListener != null) {
            databaseReference.removeEventListener(childEventListener);
        }

        // Use ChildEventListener to detect NEW reviews being added
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Log.d(TAG, "New review added: " + snapshot.getKey());
                Review review = snapshot.getValue(Review.class);
                if (review != null) {
                    // Check if this is not the current user's review
                    if (currentUser != null && review.getUserId() != null && 
                        !review.getUserId().equals(currentUser.getUid())) {
                        // Show notification for new review
                        showNewReviewNotification(review);
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                // Handle review updates if needed
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // Handle review removal if needed
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // Handle review move if needed
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "ChildEventListener cancelled: " + error.getMessage());
            }
        };
        databaseReference.addChildEventListener(childEventListener);

        // Use ValueEventListener to load all reviews
        reviewListener = new ValueEventListener() {
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
                            review.setId(snapshot.getKey());
                            Log.d(TAG, "Review loaded: " + review.getBookstoreName() + " by " + review.getUserName());
                            reviewList.add(review);
                            reviewCount++;
                        } else {
                            Log.e(TAG, "Failed to convert snapshot to Review object");
                        }
                    }

                    // Sort by timestamp (newest first)
                    reviewList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

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
        };
        databaseReference.addValueEventListener(reviewListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== ReviewFeedActivity RESUMED ===");

        // Refresh data when returning to this activity
        if (reviewAdapter != null) {
            reviewAdapter.notifyDataSetChanged();
        }

        // Check if we need to mark notification as read
        if (fromNotification && notificationReviewId != null) {
            markNotificationAsRead();
        }

        // Check intent again in case notification came while activity was running
        handleNotificationIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");

        // Update the current intent
        setIntent(intent);

        // Handle notification intent again
        handleNotificationIntent();

        // If we got a new notification while activity was open, reload
        if (fromNotification && notificationReviewId != null) {
            showNotificationToast();
            loadReviewsWithNotification();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "=== ReviewFeedActivity PAUSED ===");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== ReviewFeedActivity DESTROYED ===");
        
        // Remove listeners to prevent memory leaks
        if (reviewListener != null && databaseReference != null) {
            databaseReference.removeEventListener(reviewListener);
        }
        if (childEventListener != null && databaseReference != null) {
            databaseReference.removeEventListener(childEventListener);
        }
    }

    /**
     * Create notification channel (required for Android 8+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "New Reviews";
            String description = "Notifications when a new review is posted";
            int importance = NotificationManager.IMPORTANCE_HIGH; // HIGH importance to show in notification bar
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
            }
        }
    }

    /**
     * Request notification permission for Android 13+
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }

    /**
     * Show notification for a newly added review
     */
    private void showNewReviewNotification(Review review) {
        Log.d(TAG, "Showing notification for review: " + review.getBookstoreName());
        
        Context context = this;

        // Create intent to open ReviewFeedActivity when notification is tapped
        Intent intent = new Intent(context, ReviewFeedActivity.class);
        intent.putExtra("notification_review_id", review.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationCounter++,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification with HIGH priority and visibility
        String title = "📝 New review for " + review.getBookstoreName();
        String content = (review.getUserName() != null ? review.getUserName() : "Someone") 
                + ": " + (review.getReviewText() != null && review.getReviewText().length() > 50 
                    ? review.getReviewText().substring(0, 50) + "..." 
                    : review.getReviewText());

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(
                                (review.getUserName() != null ? review.getUserName() : "Someone") 
                                + ": " + review.getReviewText()))
                        .setPriority(NotificationCompat.PRIORITY_HIGH) // HIGH priority
                        .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setCategory(NotificationCompat.CATEGORY_SOCIAL);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Use unique notification ID for each notification
            int notificationId = NOTIFICATION_ID_BASE + notificationCounter;
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification shown with ID: " + notificationId);
        } else {
            Log.e(TAG, "NotificationManager is null!");
        }
    }
}