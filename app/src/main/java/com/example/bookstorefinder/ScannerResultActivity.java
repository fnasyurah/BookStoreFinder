package com.example.bookstorefinder;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
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
import java.util.HashMap;
import java.util.Map;

public class ScannerResultActivity extends AppCompatActivity {

    private TextView textViewIsbn, textViewTitle, textViewAuthor, textViewPublisher,
            textViewYear, textViewPrice, textViewGenre, textViewDescription;
    private Button buttonSearchOnline, buttonAddToDatabase, buttonBack;

    private DatabaseReference databaseReference;
    private String scannedIsbn;
    private Book foundBook = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_result);

        // Get scanned ISBN from intent
        scannedIsbn = getIntent().getStringExtra("SCANNED_ISBN");
        if (scannedIsbn == null || scannedIsbn.isEmpty()) {
            Toast.makeText(this, "No ISBN data received", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // In ScannerResultActivity, add this:
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        Log.d(TAG, "Database URL: " + database.getReference().toString());

        // Initialize views
        initializeViews();

        // Setup Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("books");

        // Search for book in database
        searchBookInDatabase();

        // Setup button listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        textViewIsbn = findViewById(R.id.textViewIsbn);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewAuthor = findViewById(R.id.textViewAuthor);
        textViewPublisher = findViewById(R.id.textViewPublisher);
        textViewYear = findViewById(R.id.textViewYear);
        textViewPrice = findViewById(R.id.textViewPrice);
        textViewGenre = findViewById(R.id.textViewGenre);
        textViewDescription = findViewById(R.id.textViewDescription);

        buttonSearchOnline = findViewById(R.id.buttonSearchOnline);
        buttonAddToDatabase = findViewById(R.id.buttonAddToDatabase);
        buttonBack = findViewById(R.id.buttonBack);

        // Display scanned ISBN
        textViewIsbn.setText("ISBN: " + formatIsbn(scannedIsbn));
    }

    private String formatIsbn(String isbn) {
        // Format ISBN for display (add hyphens for readability)
        String cleanIsbn = isbn.replaceAll("[\\s-]", "");

        if (cleanIsbn.length() == 10) {
            // ISBN-10 format: 0-123-45678-9
            return cleanIsbn.substring(0, 1) + "-" +
                    cleanIsbn.substring(1, 4) + "-" +
                    cleanIsbn.substring(4, 9) + "-" +
                    cleanIsbn.substring(9);
        } else if (cleanIsbn.length() == 13) {
            // ISBN-13 format: 978-0-123-45678-9
            return cleanIsbn.substring(0, 3) + "-" +
                    cleanIsbn.substring(3, 4) + "-" +
                    cleanIsbn.substring(4, 7) + "-" +
                    cleanIsbn.substring(7, 12) + "-" +
                    cleanIsbn.substring(12);
        }
        return isbn; // Return original if not standard length
    }

    private void searchBookInDatabase() {
        // Query Firebase for book with this ISBN
        databaseReference.orderByChild("isbn").equalTo(scannedIsbn)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Book found in database
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                foundBook = snapshot.getValue(Book.class);
                                if (foundBook != null) {
                                    foundBook.setId(snapshot.getKey());
                                    displayBookInfo(foundBook);
                                    break;
                                }
                            }
                        } else {
                            // Book not found in database
                            displayBookNotFound();
                            Toast.makeText(ScannerResultActivity.this,
                                    "Book not found in database. You can add it manually.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(ScannerResultActivity.this,
                                "Database error: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        displayBookNotFound();
                    }
                });
    }

    private void displayBookInfo(Book book) {
        textViewTitle.setText(book.getTitle());
        textViewAuthor.setText("Author: " + book.getAuthor());
        textViewPublisher.setText("Publisher: " + book.getPublisher());
        textViewYear.setText("Year: " + book.getYear());
        textViewPrice.setText("Price: " + book.getFormattedPrice());
        textViewGenre.setText("Genre: " + book.getGenre());
        textViewDescription.setText(book.getDescription());

        // Hide "Add to Database" button since book already exists
        buttonAddToDatabase.setVisibility(View.GONE);
        buttonSearchOnline.setEnabled(true);
    }

    private void displayBookNotFound() {
        textViewTitle.setText("Book Not Found in Database");
        textViewAuthor.setText("Author: Unknown");
        textViewPublisher.setText("Publisher: Unknown");
        textViewYear.setText("Year: Unknown");
        textViewPrice.setText("Price: RM 0.00");
        textViewGenre.setText("Genre: Unknown");
        textViewDescription.setText("This book is not in our database. You can add it manually or search online.");

        // Show "Add to Database" button
        buttonAddToDatabase.setVisibility(View.VISIBLE);
        buttonSearchOnline.setEnabled(true);
    }

    private void setupButtonListeners() {
        // Search Online button (opens browser)
        buttonSearchOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBookOnline();
            }
        });

        // Add to Database button
        buttonAddToDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBookToDatabase();
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

    private void searchBookOnline() {
        // Open browser to search for book by ISBN
        try {
            String searchUrl = "https://www.google.com/search?q=ISBN+" + scannedIsbn + "+book";
            android.content.Intent browserIntent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(searchUrl));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open browser", Toast.LENGTH_SHORT).show();
        }
    }

    private void addBookToDatabase() {
        // Create a simple dialog or start a new activity to add book details
        // For now, we'll add a placeholder book
        String bookId = databaseReference.push().getKey();

        Map<String, Object> bookData = new HashMap<>();
        bookData.put("isbn", scannedIsbn);
        bookData.put("title", "Unknown Title");
        bookData.put("author", "Unknown Author");
        bookData.put("publisher", "Unknown Publisher");
        bookData.put("year", "Unknown");
        bookData.put("price", 0.0);
        bookData.put("genre", "General");
        bookData.put("description", "Added via scanner");

        if (bookId != null) {
            databaseReference.child(bookId).setValue(bookData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ScannerResultActivity.this,
                                    "Book added to database (placeholder data)",
                                    Toast.LENGTH_SHORT).show();
                            buttonAddToDatabase.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(ScannerResultActivity.this,
                                    "Failed to add book: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}