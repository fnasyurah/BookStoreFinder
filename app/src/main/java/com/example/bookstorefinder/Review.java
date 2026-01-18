package com.example.bookstorefinder;

public class Review {
    private String id;
    private String userId;
    private String userEmail;
    private String userName;
    private String bookstoreId;
    private String bookstoreName;
    private String reviewText;
    private String imageBase64; // CHANGED FROM imageUrl TO imageBase64
    private float rating;
    private long timestamp;

    // Empty constructor required for Firebase
    public Review() {}

    // Constructor
    public Review(String id, String userId, String userName, String userEmail,
                  String bookstoreId, String bookstoreName, String reviewText,
                  String imageBase64, float rating, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.bookstoreId = bookstoreId;
        this.bookstoreName = bookstoreName;
        this.reviewText = reviewText;
        this.imageBase64 = imageBase64;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getBookstoreId() { return bookstoreId; }
    public void setBookstoreId(String bookstoreId) { this.bookstoreId = bookstoreId; }

    public String getBookstoreName() { return bookstoreName; }
    public void setBookstoreName(String bookstoreName) { this.bookstoreName = bookstoreName; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public String getImageBase64() { return imageBase64; } // CHANGED
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; } // CHANGED

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}