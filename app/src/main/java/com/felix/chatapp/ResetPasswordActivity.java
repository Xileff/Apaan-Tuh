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

    EditText sendEmail;
    Button btnReset;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Reset Password");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sendEmail = findViewById(R.id.inputEmail);
        btnReset = findViewById(R.id.btnResetPassword);

        firebaseAuth = FirebaseAuth.getInstance();

        btnReset.setOnClickListener(view -> {
            String email = sendEmail.getText().toString();

            if (email.equals("")) {
                Toast.makeText(ResetPasswordActivity.this, "Email can't be blank", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    String error = task.getException().getMessage();
                    Toast.makeText(ResetPasswordActivity.this, error, Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(ResetPasswordActivity.this, "Please check your email", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
            });
        });
    }
}