package com.example.bookstorefinder;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FirestoreNotificationService {

    private static final String TAG = "FirestoreNotifService";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public FirestoreNotificationService() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // Create a notification for all users when a review is posted
    public void createNewReviewNotification(String reviewId, String bookstoreName,
                                            String reviewText, String senderName,
                                            String senderId) {

        Log.d(TAG, "Creating notification for review: " + reviewId);

        // First, save the review to Firestore users collection for the sender
        saveUserReviewToFirestore(senderId, reviewId);

        // Then get all users except the sender
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int userCount = task.getResult().size();
                        Log.d(TAG, "Found " + userCount + " users in Firestore");

                        int notificationCount = 0;
                        for (DocumentSnapshot document : task.getResult()) {
                            String userId = document.getId();

                            // Skip the user who posted the review
                            if (userId.equals(senderId)) {
                                continue;
                            }

                            // Create notification for this user
                            createNotificationForUser(userId, reviewId, bookstoreName, reviewText, senderName);
                            notificationCount++;
                        }

                        Log.d(TAG, "✅ Created " + notificationCount + " notifications in Firestore");

                    } else {
                        Log.e(TAG, "❌ Error getting users: " + task.getException());
                        // If no users found, create at least one notification for testing
                        createTestNotification(reviewId, bookstoreName, reviewText, senderName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get users: " + e.getMessage());
                    createTestNotification(reviewId, bookstoreName, reviewText, senderName);
                });
    }

    // Save review to sender's user document in Firestore
    private void saveUserReviewToFirestore(String userId, String reviewId) {
        Map<String, Object> reviewRef = new HashMap<>();
        reviewRef.put("reviewId", reviewId);
        reviewRef.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users")
                .document(userId)
                .collection("reviews")
                .document(reviewId)
                .set(reviewRef)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "✅ Review saved to user's collection: " + userId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error saving review to user: " + e.getMessage()));
    }

    // Create notification for a specific user
    private void createNotificationForUser(String userId, String reviewId,
                                           String bookstoreName, String reviewText,
                                           String senderName) {

        String notificationId = db.collection("notifications").document().getId();
        String title = "📚 New Review: " + bookstoreName;
        String message = senderName + ": " +
                (reviewText.length() > 50 ? reviewText.substring(0, 50) + "..." : reviewText);

        // Create notification data
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", notificationId);
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("reviewId", reviewId);
        notification.put("bookstoreName", bookstoreName);
        notification.put("senderName", senderName);
        notification.put("isRead", false);
        notification.put("type", "new_review");
        notification.put("timestamp", new Date());
        notification.put("createdAt", FieldValue.serverTimestamp());

        // Save to Firestore notifications collection
        db.collection("notifications")
                .document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Notification saved for user: " + userId);

                    // Also save a reference in user's notifications subcollection
                    saveNotificationToUserCollection(userId, notificationId, notification);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error saving notification: " + e.getMessage());
                });
    }

    // Save notification reference to user's collection
    private void saveNotificationToUserCollection(String userId, String notificationId,
                                                  Map<String, Object> notificationData) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .set(notificationData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "✅ Notification saved to user's collection: " + userId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error saving to user's collection: " + e.getMessage()));
    }

    // Create a test notification if no users found (for testing)
    private void createTestNotification(String reviewId, String bookstoreName,
                                        String reviewText, String senderName) {

        String notificationId = db.collection("notifications").document().getId();
        String title = "📚 Test Notification: " + bookstoreName;
        String message = senderName + ": " + reviewText.substring(0, Math.min(50, reviewText.length()));

        Map<String, Object> notification = new HashMap<>();
        notification.put("id", notificationId);
        notification.put("userId", "test_user_123");
        notification.put("title", title);
        notification.put("message", message);
        notification.put("reviewId", reviewId);
        notification.put("bookstoreName", bookstoreName);
        notification.put("senderName", senderName);
        notification.put("isRead", false);
        notification.put("type", "new_review");
        notification.put("timestamp", new Date());

        db.collection("notifications")
                .document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "✅ Test notification created"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error creating test notification: " + e.getMessage()));
    }

    // Create a user in Firestore if not exists
    public void createUserIfNotExists(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        String userId = firebaseUser.getUid();
        String userEmail = firebaseUser.getEmail();
        String userName = firebaseUser.getDisplayName();

        if (userName == null || userName.isEmpty()) {
            if (userEmail != null && userEmail.contains("@")) {
                userName = userEmail.split("@")[0];
            } else {
                userName = "User_" + userId.substring(0, 6);
            }
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", userId);
        userData.put("email", userEmail);
        userData.put("name", userName);
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("lastLogin", new Date());

        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "✅ User created/updated in Firestore: " + userId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Error creating user in Firestore: " + e.getMessage()));
    }

    // Get unread notification count for a user
    public void getUnreadNotificationCount(String userId, NotificationCountCallback callback) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        callback.onCountReceived(count);
                    } else {
                        Log.e(TAG, "Error getting notification count: " + task.getException());
                        callback.onCountReceived(0);
                    }
                });
    }

    public interface NotificationCountCallback {
        void onCountReceived(int count);
    }
}