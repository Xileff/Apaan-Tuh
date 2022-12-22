package com.felix.chatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

public class AboutDevActivity extends AppCompatActivity {

    private ImageButton btnLinkedIn;
    final String url = "https://www.linkedin.com/in/felixsavero/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_dev);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("About developer");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(
                                                    AboutDevActivity.this,
                                                                MainActivity.class)
                                                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        btnLinkedIn = findViewById(R.id.btn_dev_linkedin);

        btnLinkedIn.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
    }
}