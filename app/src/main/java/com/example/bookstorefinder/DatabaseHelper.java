package com.example.bookstorefinder;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    private DatabaseReference databaseReference;
    private Context context;

    public DatabaseHelper(Context context) {
        this.context = context;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("bookstores");
    }

    // Add a new bookstore
    public void addBookStore(BookStore bookStore) {
        String id = databaseReference.push().getKey();
        if (id != null) {
            bookStore.setId(id);
            databaseReference.child(id).setValue(bookStore)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Bookstore added successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to add bookstore", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Update a bookstore
    public void updateBookStore(String id, BookStore bookStore) {
        databaseReference.child(id).setValue(bookStore)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Bookstore updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to update bookstore", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Delete a bookstore
    public void deleteBookStore(String id) {
        databaseReference.child(id).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Bookstore deleted successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to delete bookstore", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Get all bookstores
    public void getAllBookStores(OnDataLoadedListener listener) {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<BookStore> bookStoreList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    BookStore bookStore = snapshot.getValue(BookStore.class);
                    if (bookStore != null) {
                        bookStoreList.add(bookStore);
                    }
                }
                listener.onDataLoaded(bookStoreList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Failed to load bookstores: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Get nearby bookstores (within radius - simplified version)
    public void getNearbyBookStores(double userLat, double userLon, double radiusKm, OnDataLoadedListener listener) {
        getAllBookStores(new OnDataLoadedListener() {
            @Override
            public void onDataLoaded(List<BookStore> allBookStores) {
                List<BookStore> nearbyBookStores = new ArrayList<>();

                for (BookStore store : allBookStores) {
                    double distance = calculateDistance(userLat, userLon,
                            store.getLatitude(), store.getLongitude());
                    if (distance <= radiusKm) {
                        nearbyBookStores.add(store);
                    }
                }
                listener.onDataLoaded(nearbyBookStores);
            }
        });
    }

    // Calculate distance between two coordinates (in kilometers)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    }

    // Interface for callback
    public interface OnDataLoadedListener {
        void onDataLoaded(List<BookStore> bookStoreList);
    }
}