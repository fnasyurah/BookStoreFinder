package com.example.bookstorefinder;

public class BookStore {
    private String id;
    private String name;
    private String address;
    private String phone;
    private double latitude;
    private double longitude;
    private String description;
    private String imageUrl;
    private float rating;
    private String category;
    private String openingHours;

    // Empty constructor (required for Firebase)
    public BookStore() {
    }

    // Constructor
    public BookStore(String id, String name, String address, String phone,
                     double latitude, double longitude, String description,
                     String imageUrl, float rating, String category, String openingHours) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.category = category;
        this.openingHours = openingHours;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    // Add this method to calculate distance
    public double calculateDistance(double userLat, double userLon) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(this.latitude - userLat);
        double lonDistance = Math.toRadians(this.longitude - userLon);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(this.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in kilometers
    }

    // Get formatted distance string
    public String getFormattedDistance(double userLat, double userLon) {
        double distance = calculateDistance(userLat, userLon);
        if (distance < 1) {
            return String.format("%.0f m", distance * 1000);
        } else {
            return String.format("%.1f km", distance);
        }
    }
}