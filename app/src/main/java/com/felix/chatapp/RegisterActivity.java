package com.felix.chatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {
    private MaterialEditText inputUsername, inputName, inputEmail, inputPassword;
    private Button btnRegister;
    private FirebaseAuth fAuth;
    private FirebaseUser fUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        setupToolbar("Register");
        setupViews();

        fAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(view -> {
            String name = inputName.getText().toString();
            String username = inputUsername.getText().toString();
            String email = inputEmail.getText().toString();
            String password = inputPassword.getText().toString();

            if (username.equals("") || password.equals("") || email.equals("")) {
                Toast.makeText(RegisterActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 8) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            } else if (!username.matches("[a-z0-9]{8,20}")) {
                Toast.makeText(RegisterActivity.this, "Username must be between 8-20 characters, and contain lowercase alphabet with numbers", Toast.LENGTH_SHORT).show();
            } else {

//                  todo : Make username unique

                register(name, username, email, password);
            }
        });
    }

    private void register(String name, String username, String email, String password) {
        fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(RegisterActivity.this, "You can't register with this email", Toast.LENGTH_SHORT).show();
                return;
            }

            fUser = fAuth.getCurrentUser();
            String userId = fUser.getUid();

            DatabaseReference userReference = FirebaseDatabase.getInstance(getString(R.string.databaseURL))
                    .getReference("Users")
                    .child(userId);

            HashMap<String, String> newUser = new HashMap<>();
            newUser.put("id", userId);
            newUser.put("name", name);
            newUser.put("username", username);
            newUser.put("search", username.toLowerCase(Locale.ROOT));
            newUser.put("imageURL", "default");
            newUser.put("friends", "");
            newUser.put("status", "");
            newUser.put("bio", "");

            userReference.setValue(newUser).addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }

    private void setupToolbar(String title){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }

    private void setupViews(){
        inputUsername = findViewById(R.id.inputUsername);
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnRegister = findViewById(R.id.btnRegister);
    }
}