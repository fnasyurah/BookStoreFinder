package com.example.bookstorefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearbyStoresActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;
    private List<BookStore> bookStoreList;
    private Map<Marker, BookStore> markerBookStoreMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Components
    private EditText editTextSearch;
    private Button btnSearch, btnMyLocation, btnNearbyBookstores;
    private LatLng currentUserLocation;
    private double userLat = 0, userLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_stores);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("bookstores");
        bookStoreList = new ArrayList<>();
        markerBookStoreMap = new HashMap<>();

        // Initialize UI components
        editTextSearch = findViewById(R.id.editTextSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        btnNearbyBookstores = findViewById(R.id.btnNearbyBookstores);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load all bookstores initially
        loadAllBookStores();

        // Set up button listeners
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Search button
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMapSearch();
            }
        });

        // My Location button
        btnMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });

        // Nearby Bookstores button (REPLACED Back button)
        btnNearbyBookstores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNearbyBookstores();
            }
        });
    }

    private void loadAllBookStores() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookStoreList.clear();
                markerBookStoreMap.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    BookStore store = snapshot.getValue(BookStore.class);
                    if (store != null) {
                        // Set the Firebase key as ID
                        store.setId(snapshot.getKey());
                        bookStoreList.add(store);
                    }
                }

                // Show initial toast about loaded bookstores
                Toast.makeText(NearbyStoresActivity.this,
                        "Database loaded: " + bookStoreList.size() + " bookstores available",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NearbyStoresActivity.this,
                        "Failed to load bookstores: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        // Set map type (like Lab 8)
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
                                    "Location found! Click 'Nearby Bookstores' to see bookstores",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Use default location for emulator (Kuala Lumpur)
                            currentUserLocation = new LatLng(3.1390, 101.6869);
                            userLat = 3.1390;
                            userLng = 101.6869;

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 12));
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentUserLocation)
                                    .title("Kuala Lumpur (Default)")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                    .snippet("Emulator location"));

                            Toast.makeText(NearbyStoresActivity.this,
                                    "Using default location. Click 'Nearby Bookstores'",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Search function (following Lab 8)
    private void onMapSearch() {
        String location = editTextSearch.getText().toString();
        List<Address> addressList = null;

        if (location != null && !location.isEmpty()) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng searchLatLng = new LatLng(address.getLatitude(), address.getLongitude());

                // Move camera to searched location
                if (mMap != null) {
                    // Clear and add search marker
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions()
                            .position(searchLatLng)
                            .title("Search: " + location)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchLatLng, 15));

                    Toast.makeText(this,
                            "Search location found. Click 'Nearby Bookstores' to see bookstores",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
        }
    }

    // NEW METHOD: Show nearby bookstores (NO RADIUS FILTER)
    private void showNearbyBookstores() {
        if (mMap == null) return;

        if (bookStoreList.isEmpty()) {
            Toast.makeText(this,
                    "No bookstores in database yet. Please add bookstores to Firebase.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Clear map but keep current location marker
        mMap.clear();
        markerBookStoreMap.clear();

        // Add current location marker back if available
        if (currentUserLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(currentUserLocation)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .snippet("Your current location"));
        }

        // Add ALL bookstore markers from database (NO RADIUS FILTER)
        for (BookStore store : bookStoreList) {
            LatLng storeLocation = new LatLng(store.getLatitude(), store.getLongitude());

            // Calculate distance if we have user location
            String distanceInfo = "";
            if (userLat != 0 && userLng != 0) {
                double distance = calculateDistance(userLat, userLng,
                        store.getLatitude(), store.getLongitude());
                distanceInfo = String.format(" (%.1f km away)", distance);
            }

            // Create orange marker for bookstores
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(storeLocation)
                    .title(store.getName())  // Shows name when cursor hovers
                    .snippet("⭐ " + store.getRating() + distanceInfo)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

            // Store mapping between marker and bookstore
            if (marker != null) {
                markerBookStoreMap.put(marker, store);
            }
        }

        // Show success message
        Toast.makeText(this,
                "Showing " + bookStoreList.size() + " bookstores from database",
                Toast.LENGTH_SHORT).show();
    }

    // Helper method to calculate distance
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        BookStore store = markerBookStoreMap.get(marker);
        if (store != null) {
            // Show info window with bookstore details
            marker.showInfoWindow();

            // Zoom to the clicked marker
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15));

            // Open BookStoreDetailActivity when marker is clicked
            Intent intent = new Intent(this, BookStoreDetailActivity.class);
            intent.putExtra("BOOKSTORE_ID", store.getId());
            startActivity(intent);

            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    enableMyLocation();
                }
            } else {
                Toast.makeText(this,
                        "Location permission denied. Bookstores will still be shown from database.",
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