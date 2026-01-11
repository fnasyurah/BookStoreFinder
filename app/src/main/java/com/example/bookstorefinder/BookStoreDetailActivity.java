package com.example.bookstorefinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BookStoreDetailActivity extends AppCompatActivity {

    private TextView textViewName, textViewAddress, textViewPhone, textViewDescription,
            textViewRating, textViewCategory, textViewOpeningHours;
    private Button buttonCall, buttonBack;
    private DatabaseReference databaseReference;
    private String bookstoreId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_store_detail);

        // Get bookstore ID from intent
        bookstoreId = getIntent().getStringExtra("BOOKSTORE_ID");
        if (bookstoreId == null) {
            Toast.makeText(this, "Bookstore not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        textViewName = findViewById(R.id.textViewName);
        textViewAddress = findViewById(R.id.textViewAddress);
        textViewPhone = findViewById(R.id.textViewPhone);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewRating = findViewById(R.id.textViewRating);
        textViewCategory = findViewById(R.id.textViewCategory);
        textViewOpeningHours = findViewById(R.id.textViewOpeningHours);
        buttonCall = findViewById(R.id.buttonCall);
        buttonBack = findViewById(R.id.buttonBack);

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("bookstores").child(bookstoreId);

        // Load bookstore details
        loadBookStoreDetails();

        // Set up button listeners
        buttonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makePhoneCall();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadBookStoreDetails() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    BookStore bookStore = dataSnapshot.getValue(BookStore.class);
                    if (bookStore != null) {
                        displayBookStoreDetails(bookStore);
                    }
                } else {
                    Toast.makeText(BookStoreDetailActivity.this,
                            "Bookstore not found in database",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(BookStoreDetailActivity.this,
                        "Failed to load bookstore details",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBookStoreDetails(BookStore bookStore) {
        textViewName.setText(bookStore.getName());
        textViewAddress.setText(bookStore.getAddress());
        textViewPhone.setText(bookStore.getPhone());
        textViewDescription.setText(bookStore.getDescription());
        textViewRating.setText("⭐ Rating: " + bookStore.getRating());
        textViewCategory.setText("Category: " + bookStore.getCategory());
        textViewOpeningHours.setText("⏰ Hours: " + bookStore.getOpeningHours());

        // Enable/disable call button based on phone number
        if (bookStore.getPhone() != null && !bookStore.getPhone().isEmpty()) {
            buttonCall.setEnabled(true);
            buttonCall.setText("📞 Call: " + bookStore.getPhone());
        } else {
            buttonCall.setEnabled(false);
            buttonCall.setText("No Phone Number");
        }
    }

    private void makePhoneCall() {
        String phoneNumber = textViewPhone.getText().toString();
        // Extract only numbers from phone number
        phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }
}