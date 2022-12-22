package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

    private Button btnSearch, btnAddFriend, btnChat;
    private EditText searchUsers;
    private TextView name, status, bio;
    private RelativeLayout layoutFound, layoutNotFound;
    private CircleImageView profileImage;

    private FirebaseAuth fAuth;
    private FirebaseUser fUser;

    private String uid, search;

    private final String ACTIVITY_TAG = "AddFriendActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        setupToolbar("Add Friend");
        setupViews();

        fAuth = FirebaseAuth.getInstance();
        fUser = fAuth.getCurrentUser();
        btnSearch.setOnClickListener(view -> {
            search = searchUsers.getText().toString().toLowerCase(Locale.ROOT);
            if (search.equals("")) {
                Toast.makeText(AddFriendActivity.this, "Please type the username to search", Toast.LENGTH_SHORT).show();
                return;
            }

            Query query = FirebaseDatabase.getInstance(getString(R.string.databaseURL))
                    .getReference("Users")
                    .orderByChild("username")
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

                    if (user.getId().equals(fUser.getUid())) {
                        Toast.makeText(AddFriendActivity.this, "You wanna add yourself? That's illogical, sir. Perhaps you're lonely?", Toast.LENGTH_LONG).show();
                        return;
                    }

                    uid = user.getId();
                    name.setText(user.getName());
                    status.setText(user.getStatus());
                    bio.setText(user.getBio());
                    if (!user.getImageURL().equals("default")) {
                        Glide.with(AddFriendActivity.this).load(user.getImageURL()).into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.nophoto_white);
                    }
                    showLayoutFound();

                    Query qryUser = FirebaseDatabase.getInstance(getString(R.string.databaseURL))
                                        .getReference("Users")
                                        .child(fUser.getUid())
                                        .child("friends")
                                        .child(uid);

                    qryUser.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(AddFriendActivity.this, search + " is already your friend", Toast.LENGTH_SHORT).show();
                                showChatButton();
                            }

                            else {
                                showAddButton();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.d(ACTIVITY_TAG, error.getMessage());
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(ACTIVITY_TAG, error.getMessage());
                }
            });
        });

        btnAddFriend.setOnClickListener(view -> {
            DatabaseReference newFriendRef = FirebaseDatabase.getInstance(getString(R.string.databaseURL))
                    .getReference("Users")
                    .child(fUser.getUid())
                    .child("friends")
                    .child(uid);

            HashMap<String, String> friendData = new HashMap<>();
            friendData.put("id", uid);
            friendData.put("backgroundUri", "");

            newFriendRef.setValue(friendData).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(AddFriendActivity.this, "Couldn't add " + search + ", please try again shortly", Toast.LENGTH_SHORT).show();
                    Log.d(ACTIVITY_TAG, task.getException().toString());
                }

                Toast.makeText(AddFriendActivity.this, search + " added succesfully.", Toast.LENGTH_SHORT).show();
                showChatButton();
            });
        });

        btnChat.setOnClickListener(view -> {
            Intent intent = new Intent(AddFriendActivity.this, MessageActivity.class);
            intent.putExtra("userId", uid);
            startActivity(intent);
        });
    }

    private void showLayoutFound(){
        layoutFound.setVisibility(View.VISIBLE);
        layoutNotFound.setVisibility(View.GONE);
        closeKeyboard();
    }

    private void showLayoutNotFound() {
        layoutFound.setVisibility(View.GONE);
        layoutNotFound.setVisibility(View.VISIBLE);
        closeKeyboard();
    }

    private void setupViews() {
        btnSearch = findViewById(R.id.btnSearch);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        btnChat = findViewById(R.id.btnChat);
        searchUsers = findViewById(R.id.inputSearch);
        name = findViewById(R.id.profileName);
        status = findViewById(R.id.profileStatus);
        bio = findViewById(R.id.profileBio);
        layoutFound = findViewById(R.id.layoutFound);
        layoutNotFound = findViewById(R.id.layoutNotFound);
        profileImage = findViewById(R.id.profileImage);
    }

    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add friend");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(
                                        AddFriendActivity.this,
                                                    MainActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }

    private void showAddButton() {
        btnAddFriend.setVisibility(View.VISIBLE);
        btnChat.setVisibility(View.GONE);
    }

    private void showChatButton() {
        btnAddFriend.setVisibility(View.GONE);
        btnChat.setVisibility(View.VISIBLE);
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager manager = (InputMethodManager)  getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}