package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felix.chatapp.Models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

public class LoginActivity extends AppCompatActivity {

    private MaterialEditText email, password;
    private Button btnLogin, btnLoginGooglee;
    private TextView txtForgotPassword;

    private FirebaseAuth auth;
    private GoogleSignInClient mGoogleSignInClient;
    private SignInButton btnLoginGoogle;
    private int RC_SIGN_IN = 2;
    private final String GoogleSignInTag = "Google Sign In";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        email = findViewById(R.id.inputEmail);
        password = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        txtForgotPassword = findViewById(R.id.forgotPassword);

//      Default login
        btnLogin.setOnClickListener(view -> {
            String txt_email = email.getText().toString();
            String txt_password = password.getText().toString();

            if (TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password)) {
                Toast.makeText(LoginActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
            } else {
                auth.signInWithEmailAndPassword(txt_email, txt_password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                goToMainActivity();
                            } else {
                                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

//      Google login
        btnLoginGoogle.setOnClickListener(view -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignIn();
        });

//      Forgot password
        txtForgotPassword.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
        });
    }

    private void googleSignIn(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(GoogleSignInTag, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                Log.d(GoogleSignInTag, e.getMessage());
            }
        }
     }

//   Last method
     private void firebaseAuthWithGoogle(String idToken) {
         AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
         auth.signInWithCredential(credential)
                 .addOnCompleteListener(this, task -> {
                     if (task.isSuccessful()) {
                         FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                         DatabaseReference firebaseUserRef = FirebaseDatabase.getInstance(getString(R.string.databaseURL)).getReference("Users").child(firebaseUser.getUid());

//                       Prevent sign in if account hasnt been registered yet
                         firebaseUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                             @Override
                             public void onDataChange(@NonNull DataSnapshot snapshot) {
                                 User user = snapshot.getValue(User.class);
                                 if (user == null) {
                                     firebaseUser.delete().addOnCompleteListener(task -> {
                                         if (task.isSuccessful()) {
                                             auth.signOut();
                                             mGoogleSignInClient.signOut();
                                             Toast.makeText(LoginActivity.this, "This gmail is not registered in Apaan Tuh", Toast.LENGTH_SHORT).show();
                                         }
                                     });
                                 }
                                 else {
                                     goToMainActivity();
                                 }

                             }

                             @Override
                             public void onCancelled(@NonNull DatabaseError error) {

                             }
                         });
                     } else {
                         Log.d(GoogleSignInTag, "signInWithCredential:failed", task.getException());
                         Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                     }
                 });
     }

    @Override
    protected void onStart() {
        super.onStart();
        auth = FirebaseAuth.getInstance();
    }

    private void goToMainActivity(){
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}