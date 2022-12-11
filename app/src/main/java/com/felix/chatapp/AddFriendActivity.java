package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddFriendActivity extends AppCompatActivity {

    private Button btnSearch, btnAddFriend;
    private EditText searchUsers;
    private TextView name, status, bio;
    private RelativeLayout layoutFound, layoutNotFound;
    private CircleImageView profileImage;
    private String uid, search;
    private FirebaseUser fUser;
    private boolean alreadyAdded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add friend");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(AddFriendActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        btnSearch = findViewById(R.id.btnSearch);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        searchUsers = findViewById(R.id.inputSearch);
        name = findViewById(R.id.profileName);
        status = findViewById(R.id.profileStatus);
        bio = findViewById(R.id.profileBio);
        layoutFound = findViewById(R.id.profileContainer);
        layoutNotFound = findViewById(R.id.layoutNotFound);
        profileImage = findViewById(R.id.profileImage);

        fUser = FirebaseAuth.getInstance().getCurrentUser();

        btnSearch.setOnClickListener(view -> {
            search = searchUsers.getText().toString().toLowerCase(Locale.ROOT);
            if (search.equals("")) {
                Toast.makeText(AddFriendActivity.this, "Please type the username to search", Toast.LENGTH_SHORT).show();
                return;
            }

            Query query = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users")
                    .orderByChild("search")
                    .limitToFirst(1)
                    .equalTo(search);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        showLayoutNotFound();
                        return;
                    }

                    User user = snapshot.getChildren().iterator().next().getValue(User.class);

//                  If we search ourselves
                    if (user.getId().equals(fUser.getUid())) {
                        showLayoutNotFound();
                        Toast.makeText(AddFriendActivity.this, "You can't add yourself", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    uid = user.getId();
                    name.setText(user.getName());
                    status.setText(user.getStatus());
                    bio.setText(user.getBio());

//                  Logic if the user is already added or not
                    Query checkUser = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users").child(fUser.getUid()).child("friends").child(uid);
                    checkUser.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                alreadyAdded = true;
                                btnAddFriend.setText("Chat");
                                showLayoutFound();
                                Toast.makeText(AddFriendActivity.this, search + " is already your friend", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            alreadyAdded = false;
                            btnAddFriend.setText("Add Friend");
                            if (user.getImageURL().equals("default")) {
                                profileImage.setImageResource(R.drawable.nophoto);
                            } else {
                                Glide.with(AddFriendActivity.this).load(user.getImageURL()).into(profileImage);
                            }
                            showLayoutFound();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        });

//      Listener btnAddFriend
        btnAddFriend.setOnClickListener(view -> {
            if (alreadyAdded) {
                Intent intent = new Intent(AddFriendActivity.this, MessageActivity.class);
                intent.putExtra("userId", uid);
                startActivity(intent);
            } else {
                DatabaseReference reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users").child(fUser.getUid()).child("friends").child(uid);
                HashMap<String, String> friendData = new HashMap<>();
                friendData.put("id", uid);
                friendData.put("backgroundUri", "");

                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            reference.setValue(friendData).addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) return;

                                alreadyAdded = true;
                                btnAddFriend.setText("Chat");
                                Toast.makeText(AddFriendActivity.this, search + " added succesfully.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
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

    @Override
    protected void onDestroy() {
        finish();
        super.onDestroy();
    }
}