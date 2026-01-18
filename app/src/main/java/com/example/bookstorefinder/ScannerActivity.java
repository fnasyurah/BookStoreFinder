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

    private boolean isValidIsbn(String isbn) {
        String cleanIsbn = isbn.replaceAll("[^0-9Xx]", "");

        if (cleanIsbn.length() == 10) {
            // Validate ISBN-10
            return validateIsbn10(cleanIsbn);
        } else if (cleanIsbn.length() == 13) {
            // Validate ISBN-13
            return validateIsbn13(cleanIsbn);
        }
        return false;
    }

    private boolean validateIsbn10(String isbn) {
        try {
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += (i + 1) * Character.getNumericValue(isbn.charAt(i));
            }
            char lastChar = isbn.charAt(9);
            int lastDigit = (lastChar == 'X' || lastChar == 'x') ? 10 : Character.getNumericValue(lastChar);
            sum += 10 * lastDigit;
            return (sum % 11 == 0);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateIsbn13(String isbn) {
        try {
            int sum = 0;
            for (int i = 0; i < 13; i++) {
                int digit = Character.getNumericValue(isbn.charAt(i));
                sum += (i % 2 == 0) ? digit : digit * 3;
            }
            return (sum % 10 == 0);
        } catch (Exception e) {
            return false;
        }
    }

    private void processScannedIsbn(String isbn) {
        // Clean the ISBN
        String cleanIsbn = isbn.replaceAll("[^0-9Xx]", "");

        // Validate ISBN
        if (isValidIsbn(cleanIsbn)) {
            // Valid ISBN, go to result screen
            Intent intent = new Intent(this, ScannerResultActivity.class);
            intent.putExtra("SCANNED_ISBN", cleanIsbn);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this,
                    "Invalid ISBN format. Please scan a valid book barcode.",
                    Toast.LENGTH_LONG).show();
            // Restart scanner
            startScanner();
        }
    }
}