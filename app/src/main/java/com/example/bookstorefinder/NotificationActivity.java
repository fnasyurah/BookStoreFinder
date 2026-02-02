package com.example.bookstorefinder;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    private TextView textViewEmpty;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, notificationList);
        recyclerView.setAdapter(adapter);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Load notifications
        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            textViewEmpty.setText("Please login to view notifications");
            textViewEmpty.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            return;
        }

        String userId = currentUser.getUid();

        // Query notifications for current user, ordered by timestamp
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        notificationList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NotificationModel notification = document.toObject(NotificationModel.class);
                            notification.setId(document.getId());
                            notificationList.add(notification);
                        }

                        // Update UI
                        if (notificationList.isEmpty()) {
                            textViewEmpty.setText("No notifications yet");
                            textViewEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            textViewEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();

                            // Mark all as read
                            markAllAsRead(userId);
                        }
                    } else {
                        Log.e("NotificationActivity", "Error loading notifications", task.getException());
                        textViewEmpty.setText("Error loading notifications");
                        textViewEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void markAllAsRead(String userId) {
        // Mark all notifications as read when user opens the activity
        for (NotificationModel notification : notificationList) {
            if (!notification.isRead()) {
                db.collection("notifications")
                        .document(notification.getId())
                        .update("isRead", true)
                        .addOnSuccessListener(aVoid ->
                                Log.d("NotificationActivity", "Marked as read: " + notification.getId()));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notifications when returning to activity
        loadNotifications();
    }
}