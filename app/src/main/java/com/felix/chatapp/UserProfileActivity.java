package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView name, status, bio;
    private DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        String userId = getIntent().getStringExtra("userId");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> {
            startActivity(new Intent(UserProfileActivity.this, MessageActivity.class)
                            .putExtra("userId", userId)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });

        profileImage = findViewById(R.id.profileImage);
        name = findViewById(R.id.profileName);
        status = findViewById(R.id.profileStatus);
        bio = findViewById(R.id.profileBio);

        reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL))
                                    .getReference("Users")
                                    .child(userId);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                getSupportActionBar().setTitle(user.getName());

                if (!user.getImageURL().equals("default")) {
                    Glide.with(UserProfileActivity.this).load(user.getImageURL()).into(profileImage);
                }
                name.setText(user.getName());
                status.setText(user.getStatus());
                bio.setText(user.getBio());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("UserProfileActivity", error.getMessage());
            }
        });
    }
}