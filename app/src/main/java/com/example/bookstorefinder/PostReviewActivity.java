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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PostReviewActivity extends AppCompatActivity {

    // Camera-related variables (following Lab 9)
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

        // Initialize Firebase (NO STORAGE - just database like Lab 3/4)
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("reviews");

        // Initialize UI components
        imageViewPreview = findViewById(R.id.imageViewPreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnPostReview = findViewById(R.id.btnPostReview);
        editTextBookstore = findViewById(R.id.editTextBookstore);
        editTextReview = findViewById(R.id.editTextReview);
        ratingBar = findViewById(R.id.ratingBar);
        textViewPreviewLabel = findViewById(R.id.textViewPreviewLabel);

        // Enable post button (can post without photo like Lab 9)
        btnPostReview.setEnabled(true);

        // Get bookstore name from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("BOOKSTORE_NAME")) {
            String bookstoreName = intent.getStringExtra("BOOKSTORE_NAME");
            editTextBookstore.setText(bookstoreName);
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

    private void takePhoto() {
        // Check camera permission (exactly like Lab 9 page 384)
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

        // Ensure there's a camera activity (Lab 9 page 384)
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go (Lab 9)
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Continue only if File was created (Lab 9)
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // Create image file (following Lab 9 pattern)
    private File createImageFile() throws IOException {
        // Create image file name (Lab 9)
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // Get directory (like Lab 9)
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }

        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Save path for later use (Lab 9)
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Display preview (Lab 9)
            displayImagePreview();
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayImagePreview() {
        if (currentPhotoPath != null) {
            try {
                // Load and display image (Lab 9)
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, options);

                if (bitmap != null) {
                    imageViewPreview.setImageBitmap(bitmap);
                    imageViewPreview.setVisibility(View.VISIBLE);
                    textViewPreviewLabel.setText("Photo Preview");

                    // Enable retake
                    imageViewPreview.setOnClickListener(v -> takePhoto());
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Save image locally (Lab 9 pattern - NO Firebase Storage)
    private String saveImageToLocalStorage() {
        if (currentPhotoPath == null || photoFile == null) {
            return null;
        }

        try {
            // Create unique filename
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "review_" + timeStamp + ".jpg";

            // Save to app's internal storage (Lab 9)
            File storageDir = new File(getFilesDir(), "review_images");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            File destinationFile = new File(storageDir, imageFileName);

            // Copy the file
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (bitmap != null) {
                FileOutputStream fos = new FileOutputStream(destinationFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                fos.close();

                // Return file name (not full path for security)
                return imageFileName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void postReview() {
        // Validate inputs
        String bookstoreName = editTextBookstore.getText().toString().trim();
        String reviewText = editTextReview.getText().toString().trim();
        float rating = ratingBar.getRating();

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

        if (currentUser == null) {
            Toast.makeText(this, "Please log in to post reviews", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button
        btnPostReview.setEnabled(false);
        btnPostReview.setText("Posting...");

        // Save image locally (optional - Lab 9 style)
        String localImageName = null;
        if (currentPhotoPath != null) {
            localImageName = saveImageToLocalStorage();
        }

        // Save to Firebase Database (Lab 3/4 pattern)
        saveReviewToDatabase(bookstoreName, reviewText, rating, localImageName);
    }

    private void saveReviewToDatabase(String bookstoreName, String reviewText,
                                      float rating, String imageFileName) {
        String reviewId = databaseReference.push().getKey();
        if (reviewId == null) {
            Toast.makeText(this, "Failed to create review", Toast.LENGTH_SHORT).show();
            btnPostReview.setEnabled(true);
            btnPostReview.setText("POST REVIEW");
            return;
        }

        // Create review (store only filename, not full path)
        Review review = new Review(
                reviewId,
                currentUser.getUid(),
                currentUser.getEmail(),
                "unknown",
                bookstoreName,
                reviewText,
                imageFileName, // Store only filename
                rating,
                System.currentTimeMillis()
        );

        // Save to Firebase Database (Lab 3)
        databaseReference.child(reviewId).setValue(review)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Send simple notification (Lab 6)
                            sendNotification(bookstoreName);

                            Toast.makeText(PostReviewActivity.this,
                                    "✅ Review posted!",
                                    Toast.LENGTH_SHORT).show();

                            // Return to feed
                            Intent intent = new Intent(PostReviewActivity.this,
                                    ReviewFeedActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(PostReviewActivity.this,
                                    "Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            btnPostReview.setEnabled(true);
                            btnPostReview.setText("POST REVIEW");
                        }
                    }
                });
    }

    // Simple notification (Lab 6 style)
    private void sendNotification(String bookstoreName) {
        Toast.makeText(this,
                "📢 New review for " + bookstoreName,
                Toast.LENGTH_LONG).show();

        // You can add actual notification here following Lab 6
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission required",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}