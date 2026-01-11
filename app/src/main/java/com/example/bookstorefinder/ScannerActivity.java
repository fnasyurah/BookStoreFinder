package com.example.bookstorefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, start scanner
            startScanner();
        }
    }

    private void startScanner() {
        try {
            // Initialize ZXing scanner
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan a book ISBN barcode");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.setCaptureActivity(CaptureActivityPortrait.class);
            integrator.initiateScan();
        } catch (Exception e) {
            Toast.makeText(this, "Scanner error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start scanner
                startScanner();
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission is required to scan barcodes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                // Scan cancelled
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // Scan successful
                String scannedIsbn = result.getContents();
                processScannedIsbn(scannedIsbn);
            }
        }
    }

    private void processScannedIsbn(String isbn) {
        // Clean the ISBN (remove any non-digit characters except +)
        String cleanIsbn = isbn.replaceAll("[^0-9]", "");

        // Validate ISBN length
        if (cleanIsbn.length() == 10 || cleanIsbn.length() == 13) {
            // Valid ISBN, go to result screen
            Intent intent = new Intent(this, ScannerResultActivity.class);
            intent.putExtra("SCANNED_ISBN", cleanIsbn);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this,
                    "Invalid ISBN format. Got " + cleanIsbn.length() + " digits. Need 10 or 13.",
                    Toast.LENGTH_LONG).show();
            // Restart scanner
            startScanner();
        }
    }
}