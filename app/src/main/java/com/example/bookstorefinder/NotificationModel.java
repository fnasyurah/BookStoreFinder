package com.example.bookstorefinder;

import com.google.firebase.Timestamp;

public class NotificationModel {
    private String id;
    private String userId;
    private String title;
    private String message;
    private String reviewId;
    private String bookstoreName;
    private String senderName;
    private boolean isRead;
    private Timestamp timestamp;
    private String type;

    // Empty constructor for Firestore
    public NotificationModel() {}

    public NotificationModel(String id, String userId, String title, String message,
                             String reviewId, String bookstoreName, String senderName,
                             String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.reviewId = reviewId;
        this.bookstoreName = bookstoreName;
        this.senderName = senderName;
        this.isRead = false;
        this.timestamp = Timestamp.now();
        this.type = type;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getBookstoreName() { return bookstoreName; }
    public void setBookstoreName(String bookstoreName) { this.bookstoreName = bookstoreName; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}