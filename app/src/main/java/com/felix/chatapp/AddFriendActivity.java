package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.UserWriteRecord;

import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddFriendActivity extends AppCompatActivity {

    Button btnSearch, btnAddFriend;
    EditText searchUsers;
    TextView name;
    RelativeLayout layoutFound, layoutNotFound;
    CircleImageView profileImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add friend");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        btnSearch = findViewById(R.id.btn_search);
        btnAddFriend = findViewById(R.id.btn_add_friend);
        searchUsers = findViewById(R.id.search_users);
        name = findViewById(R.id.name);
        layoutFound = findViewById(R.id.profile_container);
        layoutNotFound = findViewById(R.id.not_found);
        profileImage = findViewById(R.id.profile_image);

        btnSearch.setOnClickListener(view -> {
            String search = searchUsers.getText().toString().toLowerCase(Locale.ROOT);
            if (search.equals("")) {
                Toast.makeText(AddFriendActivity.this, "Please type the username to search", Toast.LENGTH_SHORT).show();
                return;
            }

            Query query = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users")
                    .orderByChild("username")
                    .limitToFirst(1)
                    .equalTo(search);

            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        showLayoutNotFound();
                        return;
                    }

                    User user = snapshot.getChildren().iterator().next().getValue(User.class);

                    if (user.getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        showLayoutNotFound();
                        Toast.makeText(AddFriendActivity.this, "You can't add yourself", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    name.setText(user.getName());
                    if (user.getImageURL().equals("default")) {
                        profileImage.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        Glide.with(AddFriendActivity.this).load(user.getImageURL()).into(profileImage);
                    }

                    showLayoutFound();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        });
    }

    private void showLayoutFound(){
        layoutFound.setVisibility(View.VISIBLE);
        layoutNotFound.setVisibility(View.GONE);
    }

    private void showLayoutNotFound() {
        layoutFound.setVisibility(View.GONE);
        layoutNotFound.setVisibility(View.VISIBLE);
    }
}