package com.felix.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Adapters.ViewPagerAdapter;
import com.felix.chatapp.Fragments.ChatsFragment;
import com.felix.chatapp.Fragments.ProfileFragment;
import com.felix.chatapp.Fragments.FriendsFragment;
import com.felix.chatapp.Models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView name;

    private FirebaseAuth fAuth;
    private FirebaseUser fUser;

    private GoogleSignInOptions gso;
    private GoogleSignInClient mGoogleSignInClient;

    private DatabaseReference fUserReference;

    private final String ACTIVITY_TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupViews();

        fAuth = FirebaseAuth.getInstance();
        fUser = fAuth.getCurrentUser();
        fUserReference = FirebaseDatabase.getInstance(getString(R.string.databaseURL))
                .getReference("Users")
                .child(fUser.getUid());
        fUserReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);

                name.setText(user.getName());
                if (user.getImageURL().equals("default")) {
                    profileImage.setImageResource(R.drawable.nophoto_white);
                    return;
                }

//              Prevent crashing
                if (!MainActivity.this.isFinishing()){
                    Glide.with(MainActivity.this).load(user.getImageURL()).into(profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(ACTIVITY_TAG, error.getMessage());
            }
        });

//      Fragments
        setupFragments();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            fAuth.signOut();
            mGoogleSignInClient.signOut();
            finish();
            return true;
        }

        else if (item.getItemId() == R.id.menu_about_dev) {
            startActivity(new Intent(MainActivity.this, AboutDevActivity.class));
            return true;
        }

        else if (item.getItemId() == R.id.menu_add_friend) {
            startActivity(new Intent(MainActivity.this, AddFriendActivity.class));
            return true;
        }

        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
    }

    private void setupViews() {
        profileImage = findViewById(R.id.profileImage);
        name = findViewById(R.id.profileName);
    }

    private void setupFragments() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager viewPager = findViewById(R.id.viewPager);
//      Initially, there's no fragment in viewpager. Check the layout to see

//      Add fragments to viewPagerAdapter
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new ChatsFragment(), "Chats");
        viewPagerAdapter.addFragment(new FriendsFragment(), "Friends");
        viewPagerAdapter.addFragment(new ProfileFragment(), "Profile");

//      Combine tabLayout and viewPager
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }
}
//