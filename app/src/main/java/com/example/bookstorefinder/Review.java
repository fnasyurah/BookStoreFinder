package com.example.bookstorefinder;

import android.content.Context;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Review {
    private String id;
    private String userId;
    private String userEmail;
    private String bookstoreId;
    private String bookstoreName;
    private String reviewText;
    private String imageUrl;
    private float rating;
    private long timestamp;

    // Empty constructor for Firebase (REQUIRED)
    public Review() {
        // Firebase needs this empty constructor
    }

    // Full constructor
    public Review(String id, String userId, String userEmail, String bookstoreId,
                  String bookstoreName, String reviewText, String imageUrl,
                  float rating, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.bookstoreId = bookstoreId;
        this.bookstoreName = bookstoreName;
        this.reviewText = reviewText;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    // Getters and Setters (ALL REQUIRED for Firebase)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getBookstoreId() {
        return bookstoreId;
    }

    public void setBookstoreId(String bookstoreId) {
        this.bookstoreId = bookstoreId;
    }

    public String getBookstoreName() {
        return bookstoreName;
    }

    public void setBookstoreName(String bookstoreName) {
        this.bookstoreName = bookstoreName;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Helper method to format date
    public String getFormattedDate() {
        if (timestamp == 0) {
            return "Just now";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Helper method to get rating stars
    public String getRatingStars() {
        StringBuilder stars = new StringBuilder();
        int fullStars = (int) rating;
        boolean halfStar = (rating - fullStars) >= 0.5f;

        for (int i = 0; i < fullStars; i++) {
            stars.append("★");
        }
        if (halfStar) {
            stars.append("½");
        }
        for (int i = stars.length(); i < 5; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }

    // Get local image path
    public String getLocalImagePath(Context context) {
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            File storageDir = new File(context.getFilesDir(), "review_images");
            File imageFile = new File(storageDir, imageUrl);
            return imageFile.exists() ? imageFile.getAbsolutePath() : null;
        }
        return null;
    }
}