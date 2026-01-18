package com.example.bookstorefinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScannerResultActivity extends AppCompatActivity {

    private TextView textViewIsbn, textViewTitle, textViewAuthor, textViewPublisher,
            textViewYear, textViewGenre, textViewSummary;
    private Button buttonSearchOnline, buttonBack;

    private RequestQueue requestQueue;
    private String scannedIsbn;

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

        // Initialize Volley for API requests
        requestQueue = Volley.newRequestQueue(this);

        // Initialize views
        initializeViews();

        // Search for book in Open Library API
        searchBookInOpenLibrary();

        // Setup button listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        textViewIsbn = findViewById(R.id.textViewIsbn);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewAuthor = findViewById(R.id.textViewAuthor);
        textViewPublisher = findViewById(R.id.textViewPublisher);
        textViewYear = findViewById(R.id.textViewYear);
        textViewGenre = findViewById(R.id.textViewGenre);
        textViewSummary = findViewById(R.id.textViewSummary);

        buttonSearchOnline = findViewById(R.id.buttonSearchOnline);
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

    private void searchBookInOpenLibrary() {
        // Use Open Library API (free, no API key needed)
        String url = "https://openlibrary.org/isbn/" + scannedIsbn + ".json";

        // Show loading
        textViewTitle.setText("Searching book info...");
        textViewSummary.setText("Please wait while we search Open Library...");

        // Make API request using Volley
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("BookScanner", "Open Library Response: " + response.toString());

                        // Parse book data from Open Library API
                        String title = response.optString("title", "Unknown Title");
                        String author = "Unknown Author";
                        String publisher = response.optString("publishers", "Unknown Publisher");
                        String publishDate = response.optString("publish_date", "Unknown Year");

                        // Get summary/description
                        String summary = "No summary available";
                        if (response.has("description")) {
                            Object descObj = response.get("description");
                            if (descObj instanceof String) {
                                summary = (String) descObj;
                            } else if (descObj instanceof JSONObject) {
                                summary = ((JSONObject) descObj).optString("value", "No summary available");
                            }
                        }

                        // Get author names if available
                        if (response.has("authors")) {
                            try {
                                JSONArray authors = response.getJSONArray("authors");
                                if (authors.length() > 0) {
                                    String authorKey = authors.getJSONObject(0).getString("key");
                                    // Fetch author details
                                    fetchAuthorDetails(authorKey, title, publisher, publishDate, summary);
                                    return;
                                }
                            } catch (Exception e) {
                                Log.e("BookScanner", "Error parsing authors: " + e.getMessage());
                            }
                        }

                        // If no author details to fetch, display what we have
                        displayBookInfo(title, author, publisher, publishDate, summary);

                    } catch (Exception e) {
                        Log.e("BookScanner", "Error parsing book data: " + e.getMessage());
                        displayBookNotFound();
                    }
                },
                error -> {
                    Log.e("BookScanner", "API Error: " + error.getMessage());
                    displayBookNotFound();
                }
        );

        // Set timeout for request (10 seconds)
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000, // 10 seconds timeout
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void fetchAuthorDetails(String authorKey, String title, String publisher,
                                    String publishDate, String summary) {
        String authorUrl = "https://openlibrary.org" + authorKey + ".json";

        JsonObjectRequest authorRequest = new JsonObjectRequest(
                Request.Method.GET, authorUrl, null,
                response -> {
                    try {
                        String authorName = response.optString("name", "Unknown Author");
                        displayBookInfo(title, authorName, publisher, publishDate, summary);
                    } catch (Exception e) {
                        Log.e("BookScanner", "Error parsing author: " + e.getMessage());
                        displayBookInfo(title, "Unknown Author", publisher, publishDate, summary);
                    }
                },
                error -> {
                    Log.e("BookScanner", "Author API Error: " + error.getMessage());
                    displayBookInfo(title, "Unknown Author", publisher, publishDate, summary);
                }
        );

        requestQueue.add(authorRequest);
    }

    private void displayBookInfo(String title, String author, String publisher,
                                 String year, String summary) {
        textViewTitle.setText(title);
        textViewAuthor.setText("Author: " + author);
        textViewPublisher.setText("Publisher: " + publisher);
        textViewYear.setText("Year: " + year);

        // Set summary (truncate if too long)
        if (summary.length() > 400) {
            summary = summary.substring(0, 400) + "...";
        }
        textViewSummary.setText(" " + summary);

        buttonSearchOnline.setEnabled(true);

        Toast.makeText(this, "✓ Book details loaded from Open Library", Toast.LENGTH_SHORT).show();
    }

    private void displayBookNotFound() {
        textViewTitle.setText("Book Not Found");
        textViewAuthor.setText("Author: Unknown");
        textViewPublisher.setText("Publisher: Unknown");
        textViewYear.setText("Year: Unknown");
        textViewGenre.setText("Genre: Unknown");
        textViewSummary.setText("This book was not found in Open Library database.\n\n" +
                "Possible reasons:\n" +
                "• Book is very rare or new\n" +
                "• ISBN might be incorrect\n" +
                "• Book not in digital databases\n\n" +
                "Click 'Search Online' to search on Google.");

        buttonSearchOnline.setEnabled(true);
    }

    private void setupButtonListeners() {
        // Search Online button (opens browser) - Lab 7 pattern
        buttonSearchOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBookOnline();
            }
        });

        // Back button - Fixed to go back to ScannerActivity
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to ScannerActivity (Lab 2 pattern - Intent navigation)
                Intent intent = new Intent(ScannerResultActivity.this, ScannerActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void searchBookOnline() {
        // Open browser to search for book by ISBN (Lab 7 pattern)
        try {
            String searchUrl = "https://www.google.com/search?q=" + scannedIsbn + "+book";
            Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(searchUrl));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open browser", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // When user presses device back button, go to ScannerActivity
        super.onBackPressed();
        Intent intent = new Intent(this, ScannerActivity.class);
        startActivity(intent);
        finish();
    }
}