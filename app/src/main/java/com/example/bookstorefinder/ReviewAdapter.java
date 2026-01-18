package com.example.bookstorefinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;
    private static final String TAG = "ReviewAdapter";

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Set user info
        holder.textUserName.setText(review.getUserName());
        holder.textUserEmail.setText(review.getUserEmail());

        // Set bookstore info
        if (review.getBookstoreName() != null && !review.getBookstoreName().isEmpty()) {
            holder.textStoreName.setText("📍 " + review.getBookstoreName());
        } else {
            holder.textStoreName.setText("📍 Unknown Bookstore");
        }

        // Set review text
        holder.textReview.setText(review.getReviewText());

        // Set rating
        holder.ratingBar.setRating(review.getRating());

        // Format timestamp
        if (review.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(review.getTimestamp()));
            holder.textTimestamp.setText(formattedDate);
        } else {
            holder.textTimestamp.setText("Just now");
        }

        // Load and display image if available
        loadReviewImage(holder.imageViewReview, review.getImageUrl());
    }

    private void loadReviewImage(ImageView imageView, String imageFileName) {
        if (imageFileName != null && !imageFileName.isEmpty()) {
            try {
                Log.d(TAG, "Loading image: " + imageFileName);

                // Get the file from internal storage
                File storageDir = new File(context.getFilesDir(), "review_images");
                File imageFile = new File(storageDir, imageFileName);

                Log.d(TAG, "Image file path: " + imageFile.getAbsolutePath());
                Log.d(TAG, "Image file exists: " + imageFile.exists());
                Log.d(TAG, "Image file size: " + imageFile.length() + " bytes");

                if (imageFile.exists()) {
                    // Decode and display image
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2; // Reduce size for better performance

                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
                    if (bitmap != null) {
                        Log.d(TAG, "Bitmap loaded successfully. Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                    } else {
                        Log.e(TAG, "Failed to decode bitmap");
                        imageView.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Image file does not exist");
                    imageView.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: " + e.getMessage(), e);
                imageView.setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "No image file name provided");
            imageView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName, textUserEmail, textStoreName, textReview, textTimestamp;
        RatingBar ratingBar;
        ImageView imageViewReview; // Add this

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.textUserName);
            textUserEmail = itemView.findViewById(R.id.textUserEmail);
            textStoreName = itemView.findViewById(R.id.textStoreName);
            textReview = itemView.findViewById(R.id.textReview);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            imageViewReview = itemView.findViewById(R.id.imageViewReview); // Add this line
        }
    }
}