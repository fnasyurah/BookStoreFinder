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

        // Load and display Base64 image
        String base64Image = review.getImageBase64();
        if (base64Image != null && !base64Image.isEmpty() && base64Image.length() > 100) {
            Log.d(TAG, "Decoding Base64 image, length: " + base64Image.length());
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            if (bitmap != null) {
                holder.imageViewReview.setImageBitmap(bitmap);
                holder.imageViewReview.setVisibility(View.VISIBLE);
                Log.d(TAG, "Image displayed successfully");
            } else {
                holder.imageViewReview.setVisibility(View.GONE);
                Log.e(TAG, "Failed to decode Base64 image");
            }
        } else {
            holder.imageViewReview.setVisibility(View.GONE);
            Log.d(TAG, "No image or empty Base64 string");
        }
    }

    // Convert Base64 string to Bitmap
    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            // Remove data URL prefix if present
            if (base64String.contains(",")) {
                base64String = base64String.substring(base64String.indexOf(",") + 1);
            }

            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding Base64: " + e.getMessage());
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
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