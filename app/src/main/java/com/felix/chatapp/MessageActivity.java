package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    CircleImageView profileImage;
    TextView name;
    ImageButton btnSend;
    EditText textSend;

    FirebaseUser fUser;
    DatabaseReference reference;

    MessageAdapter messageAdapter;
    List<Chat> mChats;

    RecyclerView recyclerView;

    Intent intent;

    ValueEventListener seenListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        recyclerView = findViewById(R.id.chats_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));

        profileImage = findViewById(R.id.profile_image);
        name = findViewById(R.id.name);
        textSend = findViewById(R.id.text_send);
        btnSend = findViewById(R.id.btn_send);

//      Intent from clicking an userAdapter
        intent = getIntent();
        final String userId = intent.getStringExtra("userId");

//      Our and receiver's reference in firebase
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users").child(userId);

//      Listen to changes in the database reference(receiver's data)
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//              Set receiver's profile image and name
                User user = snapshot.getValue(User.class);
                name.setText(user.getName());
                if (user.getImageURL().equals("default")) {
                    profileImage.setImageResource(R.mipmap.ic_launcher);
                } else {
                    if (!MessageActivity.this.isFinishing()) {
                        Glide.with(MessageActivity.this).load(user.getImageURL()).into(profileImage);
                    }
                }

//              Automatically read receiver's message
                readMessages(fUser.getUid(), userId, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("(MessageActivity)firebase error : ", error.getMessage());
                Toast.makeText(MessageActivity.this, "Failed retrieving data, please try again in a few minutes", Toast.LENGTH_LONG).show();
            }
        });

        btnSend.setOnClickListener(view -> {
            String message = textSend.getText().toString();
            if (message.equals("")) {
                Toast.makeText(MessageActivity.this, "Can't send empty message", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage(fUser.getUid(), userId, message);
            textSend.setText("");
        });
    }

    private void sendMessage(String sender, String receiver, String message) {
        DatabaseReference reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference();
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("isSeen", false);

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
                    recyclerView.scrollToPosition(mChats.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("(MessageActivity)firebase error : ", error.getMessage());
                Toast.makeText(MessageActivity.this, "Failed retrieving data, please try again in a few minutes", Toast.LENGTH_LONG).show();
            }
        });

        seenMessage(userId);
    }

    private void seenMessage(String userId) {
        reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Chat chat = data.getValue(Chat.class);

                    if (chat.getReceiver().equals(fUser.getUid()) && chat.getSender().equals(userId) && !isFinishing()) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isSeen", true);
                        data.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("(MessageActivity)firebase error : ", error.getMessage());
                Toast.makeText(MessageActivity.this, "Failed retrieving data, please try again in a few minutes", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateStatus(String status) {
        reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users").child(fUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        updateStatus("offline");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        reference.removeEventListener(seenListener);
        updateStatus("offline");
    }
}