package com.felix.chatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {
    private EditText inputEmail;
    private Button btnReset;
    private FirebaseAuth fAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        setupToolbar("Reset Password");
        setupViews();

        fAuth = FirebaseAuth.getInstance();
        btnReset.setOnClickListener(view -> {
            String email = inputEmail.getText().toString();
            if (email.equals("")) {
                Toast.makeText(ResetPasswordActivity.this, "Email can't be blank", Toast.LENGTH_SHORT).show();
                return;
            }

            fAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(ResetPasswordActivity.this, "Please check your email", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
            });
        });
    }

    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }

    private void setupViews(){
        inputEmail = findViewById(R.id.inputEmail);
        btnReset = findViewById(R.id.btnResetPassword);
    }
}
////