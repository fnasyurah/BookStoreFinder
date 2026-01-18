package com.example.bookstorefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearbyStoresActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Google Places API Key (Use your Maps API key)
    private static final String PLACES_API_KEY = "AIzaSyBSZ5z9Ke3YiiaCKo_ZHGxtQVdbVGiXY2o";

    // UI Components
    private EditText editTextSearch;
    private Button btnSearch, btnMyLocation, btnFindBookstores;
    private LatLng currentUserLocation;
    private double userLat = 0, userLng = 0;

    // For Volley requests
    private RequestQueue requestQueue;

    // Store marker data
    private Map<Marker, String> markerPlaceMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_stores);

        // Initialize Volley for network requests
        requestQueue = Volley.newRequestQueue(this);

        // Initialize marker map
        markerPlaceMap = new HashMap<>();

        // Initialize UI components
        editTextSearch = findViewById(R.id.editTextSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        btnFindBookstores = findViewById(R.id.btnFindBookstores);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up button listeners
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Search button (for location search)
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLocation();
            }
        });

        // My Location button
        btnMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });

        // Find Bookstores button (Uses Google Places API)
        btnFindBookstores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNearbyBookstores();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        // Set map type
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Enable UI controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        // Get current location
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null && mMap != null) {
                            userLat = location.getLatitude();
                            userLng = location.getLongitude();
                            currentUserLocation = new LatLng(userLat, userLng);

                            // Move camera to current location
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 13));

                            // Add blue marker for current location
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentUserLocation)
                                    .title("You are here")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                    .snippet("Your current location"));

                            Toast.makeText(NearbyStoresActivity.this,
                                    "Location found! Click 'Find Bookstores' to search",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Use default location for emulator (Kuala Lumpur)
                            currentUserLocation = new LatLng(3.1390, 101.6869);
                            userLat = 3.1390;
                            userLng = 101.6869;

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 12));
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentUserLocation)
                                    .title("Default Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                    .snippet("Kuala Lumpur"));

                            Toast.makeText(NearbyStoresActivity.this,
                                    "Using default location. Click 'Find Bookstores' to search",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Search for a specific location
    private void searchLocation() {
        String location = editTextSearch.getText().toString().trim();

        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(this);

        try {
            addressList = geocoder.getFromLocationName(location, 1);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (addressList != null && !addressList.isEmpty()) {
            Address address = addressList.get(0);
            LatLng searchLatLng = new LatLng(address.getLatitude(), address.getLongitude());

            // Update user location
            userLat = address.getLatitude();
            userLng = address.getLongitude();
            currentUserLocation = searchLatLng;

            // Clear map and add search marker
            if (mMap != null) {
                mMap.clear();
                markerPlaceMap.clear();

                mMap.addMarker(new MarkerOptions()
                        .position(searchLatLng)
                        .title("Search: " + location)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchLatLng, 15));

                Toast.makeText(this,
                        "Search complete. Click 'Find Bookstores' to search near " + location,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location not found: " + location, Toast.LENGTH_SHORT).show();
        }
    }

    // MAIN METHOD: Find nearby bookstores using Google Places API
    private void findNearbyBookstores() {
        if (currentUserLocation == null) {
            Toast.makeText(this,
                    "Please get your location or search for a location first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Build Google Places API URL
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + userLat + "," + userLng +
                "&radius=5000" + // 5km radius
                "&type=book_store" + // Only bookstores
                "&key=" + PLACES_API_KEY;

        Log.d("BookstoreFinder", "Searching bookstores at: " + userLat + "," + userLng);

        // Make API request using Volley
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        // Clear previous bookstore markers
                        for (Marker marker : markerPlaceMap.keySet()) {
                            marker.remove();
                        }
                        markerPlaceMap.clear();

                        // Add current location marker back
                        if (currentUserLocation != null) {
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentUserLocation)
                                    .title("You are here")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                    .snippet("Your current location"));
                        }

                        // Parse response
                        String status = response.getString("status");

                        if (status.equals("OK")) {
                            JSONArray results = response.getJSONArray("results");

                            int bookstoreCount = Math.min(results.length(), 20); // Limit to 20

                            for (int i = 0; i < bookstoreCount; i++) {
                                JSONObject place = results.getJSONObject(i);
                                String name = place.getString("name");

                                // Get location
                                JSONObject location = place.getJSONObject("geometry")
                                        .getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                LatLng bookstoreLocation = new LatLng(lat, lng);

                                // Get rating if available
                                String rating = "No rating";
                                if (place.has("rating")) {
                                    double ratingValue = place.getDouble("rating");
                                    rating = "⭐ " + String.format("%.1f", ratingValue) + "/5";
                                }

                                // Get address
                                String address = place.optString("vicinity", "Address not available");

                                // Check if open now
                                String openStatus = "";
                                if (place.has("opening_hours")) {
                                    JSONObject openingHours = place.getJSONObject("opening_hours");
                                    boolean isOpen = openingHours.optBoolean("open_now", false);
                                    openStatus = isOpen ? "🟢 Open Now" : "🔴 Closed";
                                }

                                // Get phone number (if available)
                                String phone = "";
                                if (place.has("formatted_phone_number")) {
                                    phone = place.getString("formatted_phone_number");
                                } else if (place.has("international_phone_number")) {
                                    phone = place.getString("international_phone_number");
                                } else if (place.has("phone_number")) {
                                    phone = "+" + place.getString("phone_number"); // Add + prefix for international format
                                }

                                // Calculate distance from user
                                String distanceInfo = "";
                                if (userLat != 0 && userLng != 0) {
                                    double distance = calculateDistance(userLat, userLng, lat, lng);
                                    if (distance < 1) {
                                        distanceInfo = String.format(" (%.0f m away)", distance * 1000);
                                    } else {
                                        distanceInfo = String.format(" (%.1f km away)", distance);
                                    }
                                }

                                // Create marker for bookstore
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(bookstoreLocation)
                                        .title(name)
                                        .snippet(rating + distanceInfo)
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_ORANGE)));

                                // Store bookstore info with marker for detail view
                                if (marker != null) {
                                    String bookstoreInfo = String.format(
                                            "%s||%s||%s||%.6f||%.6f||%s||%s",
                                            name, rating, address, lat, lng, openStatus, phone
                                    );
                                    markerPlaceMap.put(marker, bookstoreInfo);
                                }
                            }

                            // Show results
                            if (bookstoreCount > 0) {
                                Toast.makeText(this,
                                        "Found " + bookstoreCount + " bookstores nearby!",
                                        Toast.LENGTH_SHORT).show();

                                // Zoom to show all markers
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 13));
                            } else {
                                Toast.makeText(this,
                                        "No bookstores found nearby",
                                        Toast.LENGTH_SHORT).show();
                            }

                        } else if (status.equals("ZERO_RESULTS")) {
                            Toast.makeText(this,
                                    "No bookstores found in this area",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    "API Error: " + status,
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Log.e("BookstoreFinder", "Error parsing results: " + e.getMessage());
                        Toast.makeText(this, "Error loading bookstores", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("BookstoreFinder", "Request failed: " + error.getMessage());
                    Toast.makeText(this, "Network error. Check your internet connection.", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }

    // Helper method to calculate distance between two coordinates
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in kilometers
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        String bookstoreInfo = markerPlaceMap.get(marker);

        if (bookstoreInfo != null) {
            // Show info window
            marker.showInfoWindow();

            // Parse the stored info
            String[] parts = bookstoreInfo.split("\\|\\|");
            if (parts.length >= 5) {
                String name = parts[0];
                String rating = parts[1];
                String address = parts[2];
                String lat = parts[3];
                String lng = parts[4];

                // Get open status if available (part 5 if exists)
                String openStatus = "";
                if (parts.length >= 6) {
                    openStatus = parts[5];
                }

                // Get phone if available (part 6 if exists)
                String phone = "";
                if (parts.length >= 7) {
                    phone = parts[6];
                }

                // Format opening hours message
                String hours = "Check Google Maps for hours";
                if (!openStatus.isEmpty()) {
                    hours = openStatus;
                }

                // Open BookStoreDetailActivity with place data
                Intent intent = new Intent(this, BookStoreDetailActivity.class);
                intent.putExtra("PLACE_NAME", name);
                intent.putExtra("PLACE_ADDRESS", address);
                intent.putExtra("PLACE_PHONE", phone);
                intent.putExtra("PLACE_DESCRIPTION", "Bookstore found via Google Places API");
                intent.putExtra("PLACE_RATING", rating);
                intent.putExtra("PLACE_HOURS", hours);
                intent.putExtra("PLACE_LATITUDE", lat);
                intent.putExtra("PLACE_LONGITUDE", lng);

                startActivity(intent);
            }

            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    enableMyLocation();
                }
            } else {
                Toast.makeText(this,
                        "Location permission denied. You can still search for locations manually.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Handle back button press
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}