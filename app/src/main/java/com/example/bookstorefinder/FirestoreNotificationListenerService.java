package com.example.bookstorefinder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Date;

public class FirestoreNotificationListenerService {
    private static final String TAG = "NotifListenerService";
    private static FirestoreNotificationListenerService instance;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationListener;
    private Context context;

    private static final String CHANNEL_ID = "bookstore_notifications";
    private static final String CHANNEL_NAME = "BookStore Finder Notifications";
    private static final String CHANNEL_DESC = "Notifications for new reviews and updates";

    private int notificationId = 1000;

    private FirestoreNotificationListenerService(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        createNotificationChannel();
    }

    public static synchronized FirestoreNotificationListenerService getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreNotificationListenerService(context);
        }
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 100, 200});
            channel.setShowBadge(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Notification channel created");
            } else {
                Log.e(TAG, "❌ Cannot get NotificationManager");
            }
        }
    }

    public void startListening() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot start listener: No user logged in");
            showToast("❌ Please login to receive notifications");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Starting notification listener for user: " + userId);
        showToast("🔔 Starting notification listener...");

        // Stop any existing listener
        stopListening();

        // Listen for NEW notifications for THIS USER only (unread only)
        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", userId)  // CRITICAL: Only this user's notifications
                .whereEqualTo("isRead", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Listen failed: " + error.getMessage());
                        showToast("❌ Notification error: " + error.getMessage());
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d(TAG, "📡 Listener triggered. Documents: " + snapshots.size());

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            Log.d(TAG, "📄 Change type: " + dc.getType() + ", Doc ID: " + dc.getDocument().getId());

                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                // This is a NEW notification
                                String title = dc.getDocument().getString("title");
                                String message = dc.getDocument().getString("message");
                                String reviewId = dc.getDocument().getString("reviewId");
                                String bookstoreName = dc.getDocument().getString("bookstoreName");
                                String senderName = dc.getDocument().getString("senderName");
                                String docId = dc.getDocument().getId();

                                Log.d(TAG, "🎯 NEW NOTIFICATION!");
                                Log.d(TAG, "   Title: " + title);
                                Log.d(TAG, "   Message: " + message);
                                Log.d(TAG, "   From: " + senderName);

                                // Show system notification
                                showSystemNotification(
                                        docId,
                                        title,
                                        message,
                                        reviewId,
                                        bookstoreName,
                                        senderName
                                );

                                // Mark as read in Firestore (optional)
                                markNotificationAsRead(docId);
                            }
                        }
                    } else {
                        Log.d(TAG, "📭 No notifications found for user: " + userId);
                    }
                });

        Log.d(TAG, "✅ Notification listener started for user: " + userId);
        showToast("✅ Notification listener started");
    }

    private void showSystemNotification(String notificationId, String title, String message,
                                        String reviewId, String bookstoreName, String senderName) {

        Log.d(TAG, "🔔 Attempting to show system notification: " + title);

        // Create intent to open ReviewFeedActivity when notification is tapped
        Intent intent = new Intent(context, ReviewFeedActivity.class);

        // Add extras if available
        if (reviewId != null) {
            intent.putExtra("notification_review_id", reviewId);
        }
        if (bookstoreName != null) {
            intent.putExtra("bookstore_name", bookstoreName);
        }
        if (senderName != null) {
            intent.putExtra("sender_name", senderName);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                this.notificationId++,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.bookstorefinder)
                .setContentTitle(title != null ? title : "New Review")
                .setContentText(message != null ? message : "Tap to view")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{100, 200, 100, 200})
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setGroup("bookstore_reviews")
                .setGroupSummary(true);

        // Show the notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            int uniqueId = (int) System.currentTimeMillis();
            notificationManager.notify(uniqueId, builder.build());
            Log.d(TAG, "✅ System notification shown with ID: " + uniqueId);
            showToast("📱 Notification shown: " + title);
        } else {
            Log.e(TAG, "❌ Cannot show notification: NotificationManager is null");
            showToast("❌ Cannot show notification");
        }
    }

    private void markNotificationAsRead(String notificationId) {
        db.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "✅ Marked notification as read: " + notificationId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Failed to mark as read: " + e.getMessage()));
    }

    public void stopListening() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
            Log.d(TAG, "🛑 Notification listener stopped");
        }
    }

    private void showToast(String message) {
        // Only show toast if context is an Activity
        try {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + e.getMessage());
        }
    }

    // ==================== DEBUGGING METHODS ====================

    public void checkExistingNotifications() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot check notifications: No user logged in");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Checking existing notifications for user: " + userId);

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        Log.d(TAG, "📊 Found " + count + " unread notifications");
                        showToast("📊 Found " + count + " unread notifications");

                        if (count > 0) {
                            // Show the latest one
                            String title = task.getResult().getDocuments().get(0).getString("title");
                            String message = task.getResult().getDocuments().get(0).getString("message");
                            showToast("Latest: " + title);
                        }
                    } else {
                        Log.e(TAG, "❌ Error checking notifications: " + task.getException());
                        showToast("❌ Error: " + task.getException().getMessage());
                    }
                });
    }

    public void createTestNotification() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Cannot create test: No user logged in");
            return;
        }

        String userId = currentUser.getUid();
        String notificationId = db.collection("notifications").document().getId();

        java.util.Map<String, Object> notification = new java.util.HashMap<>();
        notification.put("id", notificationId);
        notification.put("userId", userId);
        notification.put("title", "🔔 Test Notification");
        notification.put("message", "This is a test notification sent at " + new Date());
        notification.put("reviewId", "test_review_" + System.currentTimeMillis());
        notification.put("bookstoreName", "Test Bookstore");
        notification.put("senderName", "Test System");
        notification.put("isRead", false);
        notification.put("type", "test");
        notification.put("timestamp", new Date());

        db.collection("notifications")
                .document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Test notification created: " + notificationId);
                    showToast("✅ Test notification created");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to create test: " + e.getMessage());
                    showToast("❌ Error: " + e.getMessage());
                });
    }

    public void testDirectNotification() {
        Log.d(TAG, "🧪 Testing direct notification");
        showSystemNotification(
                "direct_test_" + System.currentTimeMillis(),
                "🧪 Direct Test",
                "This notification was triggered directly",
                null,
                null,
                null
        );
    }
}