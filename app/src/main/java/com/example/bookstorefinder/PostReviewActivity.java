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
    private static final String TAG = "PostReview";
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
            }
        }

        // Set up button listeners
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        btnPostReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postReview();
            }
        });
    }

    private void createAppDirectories() {
        try {
            // Create Pictures directory for camera photos
            File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (picturesDir != null && !picturesDir.exists()) {
                picturesDir.mkdirs();
            }
        } catch (Exception e) {
            // Silent fail - directory creation error
        }
    }

    private void takePhoto() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Check if there's a camera app available
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
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

                    // Set the output file and start camera
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                } catch (Exception e) {
                    Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        } else {
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
            storageDir.mkdirs();
        }

        // Create temp file
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save path for later use
        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                // Display preview
                displayImagePreview();

                // Enable post button now that we have a photo
                btnPostReview.setEnabled(true);
                btnPostReview.setAlpha(1.0f);
                Toast.makeText(this, "✓ Photo captured successfully!", Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera operation failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayImagePreview() {
        if (currentPhotoPath != null) {
            try {
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
                            takePhoto();
                        }
                    });
                } else {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No photo available for preview", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start camera
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Convert image to Base64 string
    private String convertImageToBase64() {
        if (currentPhotoPath == null) {
            return "";
        }

        try {
            // Read the image file
            File imageFile = new File(currentPhotoPath);
            if (!imageFile.exists()) {
                return "";
            }

            // Check file size
            long fileSize = imageFile.length();

            // Realtime Database has limits, so compress heavily
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8; // Heavy compression for Base64

            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, options);
            if (bitmap == null) {
                return "";
            }

            // Compress to very small size for Realtime Database - INCREASED COMPRESSION
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos); // REDUCED TO 25% quality
            byte[] imageBytes = baos.toByteArray();

            // Convert to Base64
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Check if it's too large for Realtime Database (max ~10MB per node)
            if (base64Image.length() > 5000000) { // 5MB limit for safety
                Toast.makeText(this, "Image too large. Please take a smaller photo.", Toast.LENGTH_LONG).show();
                return "";
            }

            return base64Image;

        } catch (Exception e) {
            return "";
        }
    }

    private void postReview() {
        // Get input values
        String bookstoreName = editTextBookstore.getText().toString().trim();
        String reviewText = editTextReview.getText().toString().trim();
        float rating = ratingBar.getRating();

        // Validate inputs
        if (TextUtils.isEmpty(bookstoreName)) {
            editTextBookstore.setError("Bookstore name is required");
            editTextBookstore.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(reviewText)) {
            editTextReview.setError("Review text is required");
            editTextReview.requestFocus();
            return;
        }

        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPhotoPath == null) {
            Toast.makeText(this, "Please take a photo first", Toast.LENGTH_SHORT).show();
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
}