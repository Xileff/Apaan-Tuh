package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Adapters.MessageAdapter;
import com.felix.chatapp.Models.Chat;
import com.felix.chatapp.Models.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    private String userId;

    private CircleImageView profileImage;
    private TextView name;
    private ImageButton btnSend;
    private EditText inputMessage;

    private FirebaseUser fUser;
    private FirebaseDatabase db;
    private DatabaseReference receiverReference, chatReference, backgroundReference;

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Chat> mChats;

    private Intent intent;

    private ValueEventListener seenListener;

    //  Firebase Storage
    private StorageReference storageReference;
    private static final int REQUEST_CODE_IMAGE = 1;
    private Uri imageUri;
    private StorageTask uploadTask;

    private final String ACTIVITY_TAG = "MessageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

//      Intent from clicking UserItemAdapter
        intent = getIntent();
        userId = intent.getStringExtra("userId");
        fUser = FirebaseAuth.getInstance().getCurrentUser();

        db = FirebaseDatabase.getInstance(getString(R.string.databaseURL));
        receiverReference = db.getReference("Users").child(userId);
        storageReference = FirebaseStorage.getInstance().getReference("Uploads/backgrounds");
        chatReference = db.getReference("Chats");

        setupToolbar();
        setupViews();

//      Get receiver profile and read all messages
        receiverReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//              Set receiver's profile image and name
                User user = snapshot.getValue(User.class);
                name.setText(user.getName());
                if (user.getImageURL().equals("default")) {
                    profileImage.setImageResource(R.drawable.nophoto_white);
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
                Log.d(ACTIVITY_TAG, error.getMessage());
                Toast.makeText(MessageActivity.this, "Failed retrieving data, please try again in a few minutes", Toast.LENGTH_LONG).show();
            }
        });

//      Get background
        backgroundReference = FirebaseDatabase.getInstance(getString(R.string.databaseURL))
                .getReference("Users")
                .child(fUser.getUid())
                .child("friends")
                .child(userId)
                .child("backgroundUri");
        backgroundReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                ImageView backgroundImage = findViewById(R.id.messageBackground);
                String bgUri = snapshot.getValue(String.class);

                if (!MessageActivity.this.isFinishing()) {
                    Glide.with(MessageActivity.this).load(bgUri).into(backgroundImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(ACTIVITY_TAG, error.getMessage());
            }
        });

//      If we click on receiver's name or image, show their complete profile
        for (View v : new View[]{profileImage, name}) {
            v.setOnClickListener(view -> startActivity(new Intent(MessageActivity.this, UserProfile.class).putExtra("userId", userId)));
        }

//      Send message
        btnSend.setOnClickListener(view -> {
            String message = inputMessage.getText().toString();
            if (message.equals("")) {
                Toast.makeText(MessageActivity.this, "Can't send empty message", Toast.LENGTH_SHORT).show();
                return;
            }

            sendMessage(fUser.getUid(), userId, message);
            inputMessage.setText("");
        });
    }

    private void sendMessage(String sender, String receiver, String message) {
        HashMap<String, Object> chat = new HashMap<>();

        chat.put("sender", sender);
        chat.put("receiver", receiver);
        chat.put("message", message);
        chat.put("isSeen", false);

        chatReference.push().setValue(chat);
    }

    private void readMessages(String myId, String userId, String imageUrl) {
        mChats = new ArrayList<>();

        chatReference.addValueEventListener(new ValueEventListener() {
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
        seenListener = chatReference.addValueEventListener(new ValueEventListener() {
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
                Log.d(ACTIVITY_TAG, error.getMessage());
                Toast.makeText(MessageActivity.this, "Failed retrieving data, please try again in a few minutes", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        chatReference.removeEventListener(seenListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatReference.removeEventListener(seenListener);
    }

    private void openImage() {
        Intent requestImage = new Intent();
        requestImage.setType("image/*");
        requestImage.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(requestImage, REQUEST_CODE_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(MessageActivity.this, "Upload in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage() {
        final ProgressDialog pd = new ProgressDialog(MessageActivity.this);
        pd.setMessage("Uploading");
        pd.show();

        if (imageUri == null) {
            Toast.makeText(MessageActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
            pd.dismiss();
            return;
        }

        final StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
        uploadTask = fileReference.putFile(imageUri);
        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                return fileReference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    String mUri = downloadUri.toString();

                    DatabaseReference myFriendUidRef = db.getReference("Users")
                                                        .child(fUser.getUid())
                                                        .child("friends")
                                                        .child(userId);
                    deleteFile(myFriendUidRef, "backgroundUri");

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("backgroundUri", mUri);
                    myFriendUidRef.updateChildren(map);

                    pd.dismiss();
                } else {
                    Log.d(ACTIVITY_TAG, task.getException().toString());
                    Toast.makeText(MessageActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(ACTIVITY_TAG, e.getMessage());
                Toast.makeText(MessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_message, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_change_background:
                openImage();
                return true;
        }

        return false;
    }

    public void deleteFile(DatabaseReference reference, String key) {
        DatabaseReference previousImageUri = reference.child(key);
        previousImageUri.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                StorageReference previousImage = FirebaseStorage.getInstance().getReferenceFromUrl(snapshot.getValue(String.class));
                previousImage.delete();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(ACTIVITY_TAG, error.getMessage());
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.messageRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));

        profileImage = findViewById(R.id.activityMessageProfileImage);
        name = findViewById(R.id.profileName);
        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
    }
}