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

    CircleImageView profileImage;
    TextView name;
    ImageButton btnSend;
    EditText textSend;

    FirebaseUser fUser;
    DatabaseReference reference, backgroundReference;

    MessageAdapter messageAdapter;
    List<Chat> mChats;
    private String userId;

    RecyclerView recyclerView;

    Intent intent;

    ValueEventListener seenListener;

    //  Firebase Storage implementation
    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        storageReference = FirebaseStorage.getInstance().getReference("Uploads/backgrounds");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        recyclerView = findViewById(R.id.messageRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));

        profileImage = findViewById(R.id.activityMessageProfileImage);
        name = findViewById(R.id.profileName);
        textSend = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);

//      Intent from clicking an userAdapter
        intent = getIntent();
        userId = intent.getStringExtra("userId");

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
                    Log.d("imageURL", user.getImageURL());
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
                Log.d("(MessageActivity)firebase error : ", error.getMessage());
                Toast.makeText(MessageActivity.this, "Failed retrieving data, please try again in a few minutes", Toast.LENGTH_LONG).show();
            }
        });

//      Background reference
        backgroundReference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users").child(fUser.getUid()).child("friends").child(userId).child("backgroundUri");
        backgroundReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                ImageView backgroundImage = findViewById(R.id.messageBackground);
                String bgUri = snapshot.getValue(String.class);
                Glide.with(MessageActivity.this).load(bgUri).into(backgroundImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        reference.removeEventListener(seenListener);
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
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

        if (imageUri != null) {
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

                        reference = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users").child(fUser.getUid()).child("friends").child(userId);
                        deleteFile(reference, "backgroundUri");

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("backgroundUri", mUri);
                        reference.updateChildren(map);

                        pd.dismiss();
                    } else {
                        Toast.makeText(MessageActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        } else {
            Toast.makeText(MessageActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(MessageActivity.this, "Upload in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
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
        DatabaseReference previousImage = reference.child(key);
        previousImage.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                StorageReference previousImageLocation = FirebaseStorage.getInstance().getReferenceFromUrl(snapshot.getValue(String.class));
                previousImageLocation.delete();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("Delete error : ", error.getMessage());
            }
        });
    }
}