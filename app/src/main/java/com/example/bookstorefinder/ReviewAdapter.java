package com.example.bookstorefinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        Log.d(TAG, "ReviewAdapter created with " + reviewList.size() + " reviews");
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

        if (review == null) {
            Log.w(TAG, "Review is null at position " + position);
            return;
        }

        // Set user info with null checks
        holder.textUserName.setText(review.getUserName() != null ? review.getUserName() : "Anonymous");
        holder.textUserEmail.setText(review.getUserEmail() != null ? review.getUserEmail() : "");

        // Set bookstore info
        if (review.getBookstoreName() != null && !review.getBookstoreName().isEmpty()) {
            holder.textStoreName.setText("📍 " + review.getBookstoreName());
            holder.textStoreName.setVisibility(View.VISIBLE);
        } else {
            holder.textStoreName.setText("📍 Unknown Bookstore");
            holder.textStoreName.setVisibility(View.VISIBLE);
        }

        // Set review text
        if (review.getReviewText() != null && !review.getReviewText().isEmpty()) {
            holder.textReview.setText(review.getReviewText());
            holder.textReview.setVisibility(View.VISIBLE);
        } else {
            holder.textReview.setText("No review text provided");
            holder.textReview.setVisibility(View.VISIBLE);
        }

        // Set rating - ensure it's within 0-5 range
        float rating = review.getRating();
        if (rating < 0) rating = 0;
        if (rating > 5) rating = 5;
        holder.ratingBar.setRating(rating);

        // Format timestamp
        if (review.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(review.getTimestamp()));
            holder.textTimestamp.setText(formattedDate);
        } else {
            holder.textTimestamp.setText("Just now");
        }

        // Load and display Base64 image
        String base64Image = review.getImageBase64();
        if (base64Image != null && !base64Image.isEmpty() && base64Image.length() > 100) {
            Log.d(TAG, "Decoding Base64 image, length: " + base64Image.length());
            try {
                Bitmap bitmap = decodeBase64ToBitmap(base64Image);
                if (bitmap != null) {
                    holder.imageViewReview.setImageBitmap(bitmap);
                    holder.imageViewReview.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Image displayed successfully");
                } else {
                    holder.imageViewReview.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to decode Base64 image - bitmap is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error decoding image: " + e.getMessage());
                holder.imageViewReview.setVisibility(View.GONE);
            }
        } else {
            holder.imageViewReview.setVisibility(View.GONE);
            Log.d(TAG, "No image or empty Base64 string");
        }
    }

    // Convert Base64 string to Bitmap with better error handling
    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            if (base64String == null || base64String.isEmpty()) {
                Log.e(TAG, "Base64 string is null or empty");
                return null;
            }

            // Remove data URL prefix if present
            if (base64String.contains(",")) {
                base64String = base64String.substring(base64String.indexOf(",") + 1);
            }

            // Decode base64
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

            // Check if decoded bytes are valid
            if (decodedBytes == null || decodedBytes.length == 0) {
                Log.e(TAG, "Decoded bytes are null or empty");
                return null;
            }

            // Decode bitmap with options to prevent OutOfMemoryError
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2; // Reduce image size
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory

            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid Base64 string: " + e.getMessage());
            return null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory decoding image: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error decoding Base64: " + e.getMessage());
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    // Update method to refresh data
    public void updateReviews(List<Review> newReviews) {
        this.reviewList.clear();
        this.reviewList.addAll(newReviews);
        notifyDataSetChanged();
        Log.d(TAG, "Reviews updated: " + newReviews.size() + " items");
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName, textUserEmail, textStoreName, textReview, textTimestamp;
        RatingBar ratingBar;
        ImageView imageViewReview;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.textUserName);
            textUserEmail = itemView.findViewById(R.id.textUserEmail);
            textStoreName = itemView.findViewById(R.id.textStoreName);
            textReview = itemView.findViewById(R.id.textReview);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            imageViewReview = itemView.findViewById(R.id.imageViewReview);
        }
    }
}