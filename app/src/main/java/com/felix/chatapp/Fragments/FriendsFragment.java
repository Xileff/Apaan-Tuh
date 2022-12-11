package com.felix.chatapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.felix.chatapp.Adapters.UserItemAdapter;
import com.felix.chatapp.MainActivity;
import com.felix.chatapp.MessageActivity;
import com.felix.chatapp.Models.User;
import com.felix.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FriendsFragment extends Fragment {

    private FirebaseUser fUser;
    private RecyclerView recyclerView;
    private UserItemAdapter userItemAdapter;
    private List<User> mUsers;
    private ArrayList<String> friendUidList;
    private EditText searchUsers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        searchFriends("");

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        if (isAdded() && getActivity() != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        fUser = FirebaseAuth.getInstance().getCurrentUser();
        mUsers = new ArrayList<>();
        readUsers();

        searchUsers = view.findViewById(R.id.search_users);
        searchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchFriends(charSequence.toString().toLowerCase(Locale.ROOT));
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchFriends(charSequence.toString().toLowerCase(Locale.ROOT));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return view;
    }

//  Invoked in create cycle, to store friends uid in a list
    private void readUsers() {
        Query qryFriends;
        friendUidList = new ArrayList<>();

        if (isAdded() && getActivity() != null) {
            qryFriends = FirebaseDatabase.getInstance(requireContext().getString(R.string.databaseURL)).getReference("Users").child(fUser.getUid()).child("friends");

            qryFriends.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        friendUidList.add(data.getKey());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d("(FriendsFragment)firebase error : ", error.getMessage());
                    Toast.makeText(requireContext(), "Failed retrieving data, please try again in a few minutes", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void searchFriends(String charSequence){
        if (!isAdded() && getActivity() == null) return;

        Log.d("Charsequence", charSequence);

        Query qrySearchUser = FirebaseDatabase.getInstance(requireContext().getString(R.string.databaseURL)).getReference("Users")
                .orderByChild("search")
                .startAt(charSequence)
                .endAt(charSequence + "\uf8ff");

        qrySearchUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mUsers.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    User user = data.getValue(User.class);
                    if (!user.getId().equals(fUser.getUid()) && friendUidList.contains(user.getId())) {
                        mUsers.add(user);
                    }
                }

                if (!isAdded() && getActivity() == null) return;
//              Update useritemadapter and recyclerview
                userItemAdapter = new UserItemAdapter(requireContext(), mUsers, false);
                recyclerView.setAdapter(userItemAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("(FriendsFragment)firebase error : ", error.getMessage());
                Toast.makeText(requireContext(), "Failed retrieving data, please try again in a few minutes", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        searchFriends("");
    }
}