package com.felix.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.core.UserWriteRecord;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddFriendActivity extends AppCompatActivity {

    Button btnSearch, btnAddFriend;
    EditText searchUsers;
    TextView username;
    RelativeLayout layoutFound, layoutNotFound;
    CircleImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        btnSearch = findViewById(R.id.btn_search);
        btnAddFriend = findViewById(R.id.btn_add_friend);
        searchUsers = findViewById(R.id.search_users);
        username = findViewById(R.id.username);
        layoutFound = findViewById(R.id.profile_container);
        layoutNotFound = findViewById(R.id.not_found);
        profileImage = findViewById(R.id.profile_image);

        btnSearch.setOnClickListener(view -> {
            String search = searchUsers.getText().toString();
//            UserWriteRecord userRecord = FirebaseAuth.getInstance().getUserByEmail();
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