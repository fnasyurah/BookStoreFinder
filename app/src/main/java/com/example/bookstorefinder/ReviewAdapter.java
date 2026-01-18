package com.example.bookstorefinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;
    private RecyclerView recyclerViewReviews; // Add this reference

    // Constructor with RecyclerView reference
    public ReviewAdapter(Context context, List<Review> reviewList, RecyclerView recyclerView) {
        this.context = context;
        this.reviewList = reviewList;
        this.recyclerViewReviews = recyclerView;
    }

    // Keep the old constructor for backward compatibility
    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
        this.recyclerViewReviews = null;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Set review data
        if (review.getUserEmail() != null && !review.getUserEmail().isEmpty()) {
            holder.textViewUser.setText(review.getUserEmail());
        } else {
            holder.textViewUser.setText("Anonymous User");
        }

        holder.textViewBookstore.setText("Bookstore: " + review.getBookstoreName());
        holder.textViewRating.setText(review.getRatingStars() + " (" + String.format("%.1f", review.getRating()) + "/5)");
        holder.textViewReview.setText(review.getReviewText());
        holder.textViewDate.setText(review.getFormattedDate());

        // Load image if exists
        if (review.getImageUrl() != null && !review.getImageUrl().isEmpty() && !review.getImageUrl().equals("null")) {
            String localPath = review.getLocalImagePath(context);
            if (localPath != null) {
                try {
                    // Option 1: Use Glide (recommended)
                    Glide.with(context)
                            .load(new File(localPath))
                            .placeholder(android.R.drawable.ic_menu_gallery) // Use system icon if custom not available
                            .error(android.R.drawable.ic_menu_report_image)
                            .centerCrop()
                            .into(holder.imageViewReview);

                    holder.imageViewReview.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    // Option 2: Fallback to BitmapFactory if Glide fails
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        Bitmap bitmap = BitmapFactory.decodeFile(localPath, options);
                        if (bitmap != null) {
                            holder.imageViewReview.setImageBitmap(bitmap);
                            holder.imageViewReview.setVisibility(View.VISIBLE);
                        } else {
                            holder.imageViewReview.setVisibility(View.GONE);
                        }
                    } catch (Exception e2) {
                        holder.imageViewReview.setVisibility(View.GONE);
                    }
                }
            } else {
                holder.imageViewReview.setVisibility(View.GONE);
            }
        } else {
            holder.imageViewReview.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void updateData(List<Review> newReviews) {
        reviewList.clear();
        reviewList.addAll(newReviews);
        notifyDataSetChanged();
    }

    public void addReview(Review review) {
        reviewList.add(0, review); // Add at beginning for latest first
        notifyItemInserted(0);

        // Scroll to top if we have the RecyclerView reference
        if (recyclerViewReviews != null) {
            recyclerViewReviews.smoothScrollToPosition(0);
        }
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUser, textViewBookstore, textViewRating,
                textViewReview, textViewDate;
        ImageView imageViewReview;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUser = itemView.findViewById(R.id.textViewUser);
            textViewBookstore = itemView.findViewById(R.id.textViewBookstore);
            textViewRating = itemView.findViewById(R.id.textViewRating);
            textViewReview = itemView.findViewById(R.id.textViewReview);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            imageViewReview = itemView.findViewById(R.id.imageViewReview);
        }
    }
}