package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
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

    private MaterialEditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView txtForgotPassword;
    private SignInButton btnLoginGoogle;

    private FirebaseAuth fAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final int RC_SIGN_IN = 2;
    private final String GOOGLE_SIGN_IN_TAG = "GOOGLE_SIGN_IN";
    private final String ACTIVITY_TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupToolbar("Login");
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        txtForgotPassword = findViewById(R.id.forgotPassword);

        fAuth = FirebaseAuth.getInstance();

//      Default login
        btnLogin.setOnClickListener(view -> {
            String email = inputEmail.getText().toString();
            String password = inputPassword.getText().toString();

            if (email.equals("") || password.equals("")) {
                Toast.makeText(LoginActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            fAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        intentMainActivity();
                    });
        });

//      Forgot password
        txtForgotPassword.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class)));

//      Google login
        btnLoginGoogle.setOnClickListener(view -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(GOOGLE_SIGN_IN_TAG, "Google Auth:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                Log.d(GOOGLE_SIGN_IN_TAG, e.getMessage());
            }
        }
     }

//  Google authentication
    private void firebaseAuthWithGoogle(String idToken) {
         AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
         fAuth.signInWithCredential(credential)
                 .addOnCompleteListener(this, task -> {
                     if (!task.isSuccessful()) {
                         Log.d(GOOGLE_SIGN_IN_TAG, task.getException().toString());
                         Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                         return;
                     }

                     FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                     DatabaseReference fUserRef = FirebaseDatabase.getInstance(getString(R.string.databaseURL))
                             .getReference("Users")
                             .child(fUser.getUid());

//                   Prevent sign in if account hasnt been registered yet
                     fUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                         @Override
                         public void onDataChange(@NonNull DataSnapshot snapshot) {
                             User user = snapshot.getValue(User.class);
                             if (user == null) {
                                 fUser.delete().addOnCompleteListener(task -> {
                                     if (task.isSuccessful()) {
                                         fAuth.signOut();
                                         mGoogleSignInClient.signOut();
                                         Toast.makeText(LoginActivity.this, "This gmail is not registered in Apaan Tuh", Toast.LENGTH_SHORT).show();
                                     }
                                 });
                             } else {
                                 intentMainActivity();
                             }
                         }

                         @Override
                         public void onCancelled(@NonNull DatabaseError error) {
                             Log.d(ACTIVITY_TAG, error.getMessage());
                         }
                     });
                 });
     }

    private void intentMainActivity(){
        // Go to MainActivity and set it as new root activity
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void setupToolbar(String title){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> startActivity(new Intent(LoginActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }
}