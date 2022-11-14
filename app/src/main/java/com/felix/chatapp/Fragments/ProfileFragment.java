package com.felix.chatapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Models.User;
import com.felix.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    CircleImageView imageProfile;
    TextView username;

    DatabaseReference reference;
    FirebaseUser fUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imageProfile = view.findViewById(R.id.profile_image);
        username = view.findViewById(R.id.username);

        fUser = FirebaseAuth.getInstance().getCurrentUser();

//        try {
//            reference = FirebaseDatabase.getInstance(getContext().getString(R.string.databaseURL)).getReference("Users").child(fUser.getUid());
//        } catch (Exception e) {
//            reference = FirebaseDatabase.getInstance("https://chatapp-fc0be-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users").child(fUser.getUid());
//        }

        reference = FirebaseDatabase.getInstance(requireContext().getString(R.string.databaseURL)).getReference("Users").child(fUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);

                username.setText(user.getUsername());

                if (user.getImageURL().equals("default")) {
                    imageProfile.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(requireContext()).load(user.getImageURL()).into(imageProfile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return view;
    }
}