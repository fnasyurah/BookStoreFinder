package com.example.bookstorefinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AboutUsActivity extends AppCompatActivity {

    private ImageView imageMember1, imageMember2, imageMember3, imageMember4;
    private TextView textName1, textName2, textName3, textName4;
    private TextView textMatric1, textMatric2, textMatric3, textMatric4;
    private TextView textPosition1, textPosition2, textPosition3, textPosition4;
    private Button btnGitHub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        // Initialize views
        initializeViews();

        // Set member information
        setMemberInfo();

        // Set up GitHub button (optional - remove if not needed)
        btnGitHub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replace with your actual GitHub repository URL
                String githubUrl = "https://github.com/fnasyurah/BookStoreFinder.git";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl));
                startActivity(intent);
            }
        });
    }

    private void initializeViews() {
        // ImageViews
        imageMember1 = findViewById(R.id.imageMember1);
        imageMember2 = findViewById(R.id.imageMember2);
        imageMember3 = findViewById(R.id.imageMember3);
        imageMember4 = findViewById(R.id.imageMember4);

        // Names
        textName1 = findViewById(R.id.textName1);
        textName2 = findViewById(R.id.textName2);
        textName3 = findViewById(R.id.textName3);
        textName4 = findViewById(R.id.textName4);

        // Matric numbers
        textMatric1 = findViewById(R.id.textMatric1);
        textMatric2 = findViewById(R.id.textMatric2);
        textMatric3 = findViewById(R.id.textMatric3);
        textMatric4 = findViewById(R.id.textMatric4);

        // Positions
        textPosition1 = findViewById(R.id.textPosition1);
        textPosition2 = findViewById(R.id.textPosition2);
        textPosition3 = findViewById(R.id.textPosition3);
        textPosition4 = findViewById(R.id.textPosition4);

        // GitHub button
        btnGitHub = findViewById(R.id.btnGitHub);
    }

    private void setMemberInfo() {
        // Member 1
        textName1.setText("Nur Fatin Nasyurah");
        textMatric1.setText("2023638086");
        textPosition1.setText("Mobile Technology and Development");

        // Member 2
        textName2.setText("Nurul Farhana");
        textMatric2.setText("2023276818");
        textPosition2.setText("Mobile Technology and Development");

        // Member 3
        textName3.setText("Aina Syazliana");
        textMatric3.setText("2023261126");
        textPosition3.setText("Mobile Technology and Development");

        // Member 4
        textName4.setText("Siti Aisyah");
        textMatric4.setText("2023837654");
        textPosition4.setText("Mobile Technology and Development");
        //Set member image
        imageMember1.setImageResource(R.drawable.member1);
        imageMember2.setImageResource(R.drawable.member2);
        imageMember3.setImageResource(R.drawable.member3);
        imageMember4.setImageResource(R.drawable.member4);
    }
}