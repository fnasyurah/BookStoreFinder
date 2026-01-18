package com.example.bookstorefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PostReviewActivity extends AppCompatActivity {

    // Camera-related variables
    private static final String TAG = "PostReviewActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private String currentPhotoPath;
    private File photoFile;

    // UI Components
    private ImageView imageViewPreview;
    private Button btnTakePhoto, btnPostReview;
    private EditText editTextBookstore, editTextReview;
    private RatingBar ratingBar;
    private TextView textViewPreviewLabel;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_review);

        Log.d(TAG, "=== PostReviewActivity STARTED ===");

        // Create necessary directories
        createAppDirectories();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("reviews");
        Log.d(TAG, "Database reference: " + databaseReference.toString());

        // Initialize UI components
        imageViewPreview = findViewById(R.id.imageViewPreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnPostReview = findViewById(R.id.btnPostReview);
        editTextBookstore = findViewById(R.id.editTextBookstore);
        editTextReview = findViewById(R.id.editTextReview);
        ratingBar = findViewById(R.id.ratingBar);
        textViewPreviewLabel = findViewById(R.id.textViewPreviewLabel);

        // Initially disable post button (wait for photo)
        btnPostReview.setEnabled(false);
        btnPostReview.setAlpha(0.5f);

        // Get bookstore name from intent (if provided)
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("BOOKSTORE_NAME")) {
            String bookstoreName = intent.getStringExtra("BOOKSTORE_NAME");
            if (bookstoreName != null && !bookstoreName.isEmpty()) {
                editTextBookstore.setText(bookstoreName);
                Log.d(TAG, "Received bookstore name: " + bookstoreName);
            }
        }

        // Set up button listeners
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Camera button clicked");
                takePhoto();
            }
        });

        btnPostReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Post Review button clicked");
                postReview();
            }
        });
    }

    private void createAppDirectories() {
        try {
            // Create Pictures directory for camera photos
            File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (picturesDir != null && !picturesDir.exists()) {
                boolean created = picturesDir.mkdirs();
                Log.d(TAG, "Pictures directory created: " + created + " at " + picturesDir.getAbsolutePath());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating directories: " + e.getMessage());
        }
    }

    private void takePhoto() {
        Log.d(TAG, "takePhoto() called");

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting camera permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Camera permission already granted");
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent() called");

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Check if there's a camera app available
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Log.d(TAG, "Camera app found");

            // Create the File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    Log.d(TAG, "Image file created at: " + photoFile.getAbsolutePath());
                }
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Continue only if File was created successfully
            if (photoFile != null) {
                try {
                    // Get URI using FileProvider
                    Uri photoURI = FileProvider.getUriForFile(this,
                            getPackageName() + ".provider",
                            photoFile);

                    Log.d(TAG, "Photo URI: " + photoURI.toString());

                    // Set the output file and start camera
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                    Log.d(TAG, "Camera intent started successfully");

                } catch (Exception e) {
                    Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
                    Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Failed to create image file");
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "No camera app available");
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    // Create image file
    private File createImageFile() throws IOException {
        // Create image file name with timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // Get directory for saving photos
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }

        // Create directory if it doesn't exist
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            Log.d(TAG, "Storage directory created: " + created);
        }

        // Create temp file
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save path for later use
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "Image path saved: " + currentPhotoPath);

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Camera returned OK");
                // Display preview
                displayImagePreview();

                // Enable post button now that we have a photo
                btnPostReview.setEnabled(true);
                btnPostReview.setAlpha(1.0f);
                Toast.makeText(this, "✓ Photo captured successfully!", Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Camera cancelled by user");
                Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Camera returned unknown result: " + resultCode);
                Toast.makeText(this, "Camera operation failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayImagePreview() {
        if (currentPhotoPath != null) {
            try {
                Log.d(TAG, "Displaying image preview from: " + currentPhotoPath);

                // Load and display image with reduced size for memory efficiency
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4; // Reduce size by factor of 4
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, options);

                if (bitmap != null) {
                    imageViewPreview.setImageBitmap(bitmap);
                    imageViewPreview.setVisibility(View.VISIBLE);
                    textViewPreviewLabel.setText("📷 Photo Preview (Tap to retake)");

                    // Enable retake functionality
                    imageViewPreview.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "Retake photo clicked");
                            takePhoto();
                        }
                    });

                    Log.d(TAG, "Image displayed successfully");
                } else {
                    Log.e(TAG, "Failed to decode bitmap");
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: " + e.getMessage(), e);
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "No photo path available for preview");
            Toast.makeText(this, "No photo available for preview", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted by user");
                // Permission granted, start camera
                dispatchTakePictureIntent();
            } else {
                Log.d(TAG, "Camera permission denied by user");
                Toast.makeText(this, "Camera permission is required to take photos",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Convert image to Base64 string
    private String convertImageToBase64() {
        if (currentPhotoPath == null) {
            Log.e(TAG, "No photo to convert");
            return "";
        }

        try {
            // Read the image file
            File imageFile = new File(currentPhotoPath);
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file doesn't exist: " + currentPhotoPath);
                return "";
            }

            // Check file size (Base64 increases size by ~33%)
            long fileSize = imageFile.length();
            Log.d(TAG, "Original image size: " + fileSize + " bytes");

            // Realtime Database has limits, so compress heavily
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8; // Heavy compression for Base64

            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, options);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap for Base64");
                return "";
            }

            // Compress to very small size for Realtime Database
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos); // 40% quality
            byte[] imageBytes = baos.toByteArray();

            // Convert to Base64
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            Log.d(TAG, "Base64 string length: " + base64Image.length() + " characters");
            Log.d(TAG, "Compressed size: " + imageBytes.length + " bytes");

            // Check if it's too large for Realtime Database (max ~10MB per node)
            if (base64Image.length() > 5000000) { // 5MB limit for safety
                Log.e(TAG, "Base64 string too large for Realtime Database");
                Toast.makeText(this, "Image too large. Please take a smaller photo.", Toast.LENGTH_LONG).show();
                return "";
            }

            return base64Image;

        } catch (Exception e) {
            Log.e(TAG, "Error converting image to Base64: " + e.getMessage(), e);
            return "";
        }
    }

    private void postReview() {
        Log.d(TAG, "postReview() called");

        // Get input values
        String bookstoreName = editTextBookstore.getText().toString().trim();
        String reviewText = editTextReview.getText().toString().trim();
        float rating = ratingBar.getRating();

        Log.d(TAG, "Validating inputs:");
        Log.d(TAG, "  Bookstore: " + bookstoreName);
        Log.d(TAG, "  Review text length: " + reviewText.length());
        Log.d(TAG, "  Rating: " + rating);

        // Validate inputs
        if (TextUtils.isEmpty(bookstoreName)) {
            editTextBookstore.setError("Bookstore name is required");
            editTextBookstore.requestFocus();
            Log.e(TAG, "Validation failed: Bookstore name empty");
            return;
        }

        if (TextUtils.isEmpty(reviewText)) {
            editTextReview.setError("Review text is required");
            editTextReview.requestFocus();
            Log.e(TAG, "Validation failed: Review text empty");
            return;
        }

        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Validation failed: Rating is 0");
            return;
        }

        if (currentPhotoPath == null) {
            Toast.makeText(this, "Please take a photo first", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Validation failed: No photo taken");
            return;
        }

        // Disable button to prevent multiple clicks
        btnPostReview.setEnabled(false);
        btnPostReview.setText("Processing...");

        // Convert image to Base64 and save review
        processAndSaveReview(bookstoreName, reviewText, rating);
    }

    private void processAndSaveReview(String bookstoreName, String reviewText, float rating) {
        // Convert image to Base64
        String base64Image = convertImageToBase64();

        if (base64Image.isEmpty()) {
            Toast.makeText(this, "Failed to process image. Try again.", Toast.LENGTH_SHORT).show();
            resetPostButton();
            return;
        }

        // Update button text
        btnPostReview.setText("Posting...");

        // Save to database with Base64 image
        saveReviewToDatabase(bookstoreName, reviewText, rating, base64Image);
    }

    private void saveReviewToDatabase(String bookstoreName, String reviewText,
                                      float rating, String base64Image) {
        // Generate unique review ID using Firebase push()
        String reviewId = databaseReference.push().getKey();

        if (reviewId == null) {
            Log.e(TAG, "Failed to generate review ID");
            Toast.makeText(this, "Failed to create review", Toast.LENGTH_SHORT).show();
            resetPostButton();
            return;
        }

        // Get user info
        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail();
        String userName = currentUser.getDisplayName();

        // If display name is null, use email username
        if (userName == null || userName.isEmpty()) {
            if (userEmail != null && userEmail.contains("@")) {
                userName = userEmail.split("@")[0]; // Get part before @
            } else {
                userName = "Anonymous";
            }
        }

        Log.d(TAG, "=== SAVING TO FIREBASE DATABASE WITH BASE64 IMAGE ===");
        Log.d(TAG, "Review ID: " + reviewId);
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "User Name: " + userName);
        Log.d(TAG, "User Email: " + userEmail);
        Log.d(TAG, "Bookstore: " + bookstoreName);
        Log.d(TAG, "Rating: " + rating);
        Log.d(TAG, "Base64 image length: " + (base64Image != null ? base64Image.length() : 0) + " chars");
        Log.d(TAG, "Timestamp: " + System.currentTimeMillis());

        // Create Review object
        Review review = new Review();
        review.setId(reviewId);
        review.setUserId(userId);
        review.setUserName(userName);
        review.setUserEmail(userEmail);
        review.setBookstoreId("unknown");
        review.setBookstoreName(bookstoreName);
        review.setReviewText(reviewText);
        review.setImageBase64(base64Image); // Store Base64 string
        review.setRating(rating);
        review.setTimestamp(System.currentTimeMillis());

        // Save to Firebase Database under "reviews/{reviewId}"
        databaseReference.child(reviewId).setValue(review)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ SUCCESS: Review with image saved to Firebase Database!");
                            Log.d(TAG, "Review path: reviews/" + reviewId);

                            Toast.makeText(PostReviewActivity.this,
                                    "✅ Review with photo posted successfully!\nIt will appear in the community feed.",
                                    Toast.LENGTH_LONG).show();

                            // Clear form
                            clearForm();

                            // Go back to ReviewFeedActivity
                            Intent intent = new Intent(PostReviewActivity.this,
                                    ReviewFeedActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            Log.e(TAG, "❌ FAILED to save review: " + task.getException());
                            if (task.getException() != null) {
                                Log.e(TAG, "Error details: ", task.getException());
                            }

                            Toast.makeText(PostReviewActivity.this,
                                    "Failed to post review: " +
                                            (task.getException() != null ?
                                                    task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                            resetPostButton();
                        }
                    }
                });
    }

    private void resetPostButton() {
        btnPostReview.setEnabled(true);
        btnPostReview.setText("POST REVIEW");
        btnPostReview.setAlpha(1.0f);
    }

    private void clearForm() {
        editTextBookstore.setText("");
        editTextReview.setText("");
        ratingBar.setRating(0);
        imageViewPreview.setVisibility(View.GONE);
        textViewPreviewLabel.setText("Photo Preview (Take a photo first)");
        currentPhotoPath = null;
        photoFile = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== PostReviewActivity RESUMED ===");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "=== PostReviewActivity PAUSED ===");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== PostReviewActivity DESTROYED ===");
    }
}
