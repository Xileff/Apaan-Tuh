package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Adapters.MessageAdapter;
import com.felix.chatapp.Models.Chat;
import com.felix.chatapp.Models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;

    FirebaseUser fUser;
    DatabaseReference reference;

    ImageButton btnSend;
    EditText textSend;

    MessageAdapter messageAdapter;
    List<Chat> mChats;

    RecyclerView recyclerView;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        recyclerView = findViewById(R.id.chats_recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        textSend = findViewById(R.id.text_send);
        btnSend = findViewById(R.id.btn_send);

//        intent from UserAdapter
        intent = getIntent();
        final String userId = intent.getStringExtra("userId");

//      Our profile and the receiver's contact
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users").child(userId);

//      Listen to changes in the database reference(receiver's data)
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//              When the app received the contact's data snapshot from Firebase
                User user = snapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")) {
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(MessageActivity.this).load(user.getImageURL()).into(profile_image);
                }

                readMessages(fUser.getUid(), userId, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = textSend.getText().toString();
                if (!message.equals("")) {
                    sendMessage(fUser.getUid(), userId, message);
                } else {
                    Toast.makeText(MessageActivity.this, "Can't send empty message", Toast.LENGTH_SHORT).show();
                }
                textSend.setText("");
            }
        });
    }

    private void sendMessage(String sender, String receiver, String message) {
        DatabaseReference reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference();
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);

        reference.child("Chats").push().setValue(hashMap);
    }

    private void readMessages(String myId, String userId, String imageUrl) {
        mChats = new ArrayList<>();

        reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChats.clear();
//              Load all chats from current user and receiver
                for (DataSnapshot data : snapshot.getChildren()) {
                    Chat chat = data.getValue(Chat.class);
                    if (chat.getReceiver().equals(myId) && chat.getSender().equals(userId) || chat.getReceiver().equals(userId) && chat.getSender().equals(myId)) {
                        mChats.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mChats, imageUrl);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}