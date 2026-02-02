package com.example.bookstorefinder;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private Context context;
    private List<NotificationModel> notificationList;
    private static final String TAG = "NotificationAdapter";

    public NotificationAdapter(Context context, List<NotificationModel> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
        Log.d(TAG, "Adapter created with " + notificationList.size() + " notifications");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);
        Log.d(TAG, "Binding notification at position " + position + ": " + notification.getTitle());

        // Set notification data
        holder.textTitle.setText(notification.getTitle());
        holder.textMessage.setText(notification.getMessage());

        // Set sender info if available
        if (notification.getSenderName() != null && !notification.getSenderName().isEmpty()) {
            holder.textSender.setText("From: " + notification.getSenderName());
        } else {
            holder.textSender.setText("");
        }

        // Set timestamp
        if (notification.getTimestamp() != null) {
            try {
                Date date = notification.getTimestamp().toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                holder.textTimestamp.setText(sdf.format(date));
            } catch (Exception e) {
                Log.e(TAG, "Error formatting timestamp: " + e.getMessage());
                holder.textTimestamp.setText("Recently");
            }
        } else {
            holder.textTimestamp.setText("Just now");
        }

        // Set icon based on notification type
        switch (notification.getType()) {
            case "new_review":
                holder.imageIcon.setImageResource(R.drawable.bookstorefinder);
                break;
            default:
                holder.imageIcon.setImageResource(R.drawable.bookstorefinder);
        }

        // Show unread indicator - FIXED: Use proper boolean check
        if (!notification.isRead()) {
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
            Log.d(TAG, "Notification is unread - showing indicator");
        } else {
            holder.viewUnreadIndicator.setVisibility(View.GONE);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Notification clicked: " + notification.getTitle());

            // Handle notification click
            if ("new_review".equals(notification.getType()) && notification.getReviewId() != null) {
                // Open review in ReviewFeedActivity
                Intent intent = new Intent(context, ReviewFeedActivity.class);
                intent.putExtra("notification_review_id", notification.getReviewId()); // FIXED: Changed key to match
                intent.putExtra("bookstore_name", notification.getBookstoreName());
                intent.putExtra("sender_name", notification.getSenderName());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Mark as read when clicked
                markNotificationAsRead(notification.getId());
                notification.setRead(true);
                notifyItemChanged(position); // Update UI

                context.startActivity(intent);
            }
        });
    }

    // Method to mark notification as read
    private void markNotificationAsRead(String notificationId) {
        // This should update in Firestore - you need to implement this
        // For now, just log it
        Log.d(TAG, "Marking notification as read: " + notificationId);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    // Update method to refresh data
    public void updateNotifications(List<NotificationModel> newNotifications) {
        this.notificationList.clear();
        this.notificationList.addAll(newNotifications);
        notifyDataSetChanged();
        Log.d(TAG, "Notifications updated: " + newNotifications.size() + " items");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textMessage, textSender, textTimestamp;
        ImageView imageIcon;
        View viewUnreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textMessage = itemView.findViewById(R.id.textMessage);
            textSender = itemView.findViewById(R.id.textSender);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            imageIcon = itemView.findViewById(R.id.imageIcon);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
        }
    }
}