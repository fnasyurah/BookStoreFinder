package com.example.bookstorefinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BookStoreDetailActivity extends AppCompatActivity {

    private TextView textViewName, textViewAddress, textViewPhone, textViewDescription,
            textViewRating, textViewOpeningHours;
    private Button buttonCall, buttonBack;

    // Store the place details
    private String placeName, placeAddress, placePhone, placeLatitude, placeLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_store_detail);

        // Get place data from intent
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("PLACE_NAME")) {
            Toast.makeText(this, "No bookstore data received!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Extract data from intent
        placeName = intent.getStringExtra("PLACE_NAME");
        placeAddress = intent.getStringExtra("PLACE_ADDRESS");
        placePhone = intent.getStringExtra("PLACE_PHONE");
        String description = intent.getStringExtra("PLACE_DESCRIPTION");
        String rating = intent.getStringExtra("PLACE_RATING");
        String openingHours = intent.getStringExtra("PLACE_HOURS");
        placeLatitude = intent.getStringExtra("PLACE_LATITUDE");
        placeLongitude = intent.getStringExtra("PLACE_LONGITUDE");

        // Initialize views
        textViewName = findViewById(R.id.textViewName);
        textViewAddress = findViewById(R.id.textViewAddress);
        textViewPhone = findViewById(R.id.textViewPhone);
        textViewRating = findViewById(R.id.textViewRating);
        textViewOpeningHours = findViewById(R.id.textViewOpeningHours);
        buttonCall = findViewById(R.id.buttonCall);
        buttonBack = findViewById(R.id.buttonBack);

        // Display bookstore details
        displayBookStoreDetails(placeName, placeAddress, placePhone,
                rating, openingHours);

        // Set up button listeners
        setupButtonListeners();
    }

    private void displayBookStoreDetails(String name, String address, String phone, String rating, String hours) {
        // Store name
        textViewName.setText(name);

        // Make address clickable for navigation (like Lab 8)
        if (address != null && !address.isEmpty()) {
            textViewAddress.setText("📍 " + address);
            textViewAddress.setTextColor(getResources().getColor(R.color.blue_600));
            textViewAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openGoogleMapsNavigation();
                }
            });
        } else {
            textViewAddress.setText("📍 Address not available");
            textViewAddress.setTextColor(getResources().getColor(R.color.gray_600));
        }

        // Phone number
        if (phone != null && !phone.isEmpty() && !phone.equals("null")) {
            textViewPhone.setText("📞 " + phone);
            textViewPhone.setTextColor(getResources().getColor(R.color.blue_600));
            textViewPhone.setClickable(true);
            textViewPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makePhoneCall();
                }
            });
            buttonCall.setEnabled(true);
            buttonCall.setAlpha(1.0f); // Fully visible
        } else {
            textViewPhone.setText("📞 Phone not available");
            textViewPhone.setTextColor(getResources().getColor(R.color.gray_600));
            textViewPhone.setClickable(false);
            buttonCall.setEnabled(false);
            buttonCall.setAlpha(0.5f); // Dimmed
        }

        // Rating
        if (rating != null && !rating.isEmpty()) {
            textViewRating.setText("⭐ " + rating);
        } else {
            textViewRating.setText("⭐ No rating");
        }

        // Opening hours
        if (hours != null && !hours.isEmpty()) {
            textViewOpeningHours.setText("🕒 " + hours);
        } else {
            textViewOpeningHours.setText("🕒 Hours not available");
        }
    }

    private void setupButtonListeners() {
        // Call button
        buttonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makePhoneCall();
            }
        });

        // Back button
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void makePhoneCall() {
        if (placePhone != null && !placePhone.isEmpty() && !placePhone.equals("null")) {
            // Clean phone number (remove non-numeric characters except +)
            String phoneNumber = placePhone.replaceAll("[^0-9+]", "");

            if (!phoneNumber.isEmpty()) {
                try {
                    // Check if phone number starts with country code
                    if (!phoneNumber.startsWith("+") && !phoneNumber.startsWith("0")) {
                        // Assume it's a local number, add Malaysian country code
                        phoneNumber = "+60" + phoneNumber;
                    } else if (phoneNumber.startsWith("0")) {
                        // Replace leading 0 with +60 for Malaysia
                        phoneNumber = "+60" + phoneNumber.substring(1);
                    }

                    // Create the call intent
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Error making call: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Invalid phone number format", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Open Google Maps navigation (like Lab 8 output)
    private void openGoogleMapsNavigation() {
        if (placeLatitude != null && placeLongitude != null &&
                !placeLatitude.isEmpty() && !placeLongitude.isEmpty()) {

            try {
                double lat = Double.parseDouble(placeLatitude);
                double lng = Double.parseDouble(placeLongitude);

                // Create Google Maps navigation URI (exactly like Lab 8)
                String uri = "google.navigation:q=" + lat + "," + lng + "&mode=d";

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");

                // Check if Google Maps is installed
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    // Fallback: Open in browser Google Maps
                    String mapsUrl = "https://www.google.com/maps/dir/?api=1" +
                            "&destination=" + lat + "," + lng +
                            "&travelmode=driving";

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(mapsUrl));
                    startActivity(browserIntent);
                }

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Location coordinates not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}