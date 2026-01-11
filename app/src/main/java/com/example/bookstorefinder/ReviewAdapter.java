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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
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
        holder.textViewUser.setText(review.getUserEmail());
        holder.textViewBookstore.setText("Bookstore: " + review.getBookstoreName());
        holder.textViewRating.setText(review.getRatingStars() + " (" + review.getRating() + ")");
        holder.textViewReview.setText(review.getReviewText());
        holder.textViewDate.setText(review.getFormattedDate());

        // Load image using Glide
        // In onBindViewHolder:
        if (review.getImageUrl() != null && !review.getImageUrl().isEmpty()) {
            // Try to load local image
            String localPath = review.getLocalImagePath(context);
            if (localPath != null) {
                holder.imageViewReview.setVisibility(View.VISIBLE);

                // Load from local storage (Lab 9 pattern)
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // Reduce size for memory
                Bitmap bitmap = BitmapFactory.decodeFile(localPath, options);

                if (bitmap != null) {
                    holder.imageViewReview.setImageBitmap(bitmap);
                } else {
                    holder.imageViewReview.setVisibility(View.GONE);
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