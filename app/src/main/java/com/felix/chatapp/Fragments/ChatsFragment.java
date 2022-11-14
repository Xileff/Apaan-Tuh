package com.felix.chatapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.felix.chatapp.Adapters.UserAdapter;
import com.felix.chatapp.Models.Chat;
import com.felix.chatapp.Models.User;
import com.felix.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> mUsers; // Will contain all the users whom we have chat with

    FirebaseUser fUser;
    DatabaseReference reference;

    private List<String> chatUidList; // Will contain all id of the users whom we have chat with

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fUser = FirebaseAuth.getInstance().getCurrentUser();
        chatUidList = new ArrayList<>();

        try {
            reference = FirebaseDatabase.getInstance(getContext().getString(R.string.databaseURL)).getReference("Chats");
        } catch (Exception e) {
            reference = FirebaseDatabase.getInstance("https://chatapp-fc0be-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Chats");
        }
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatUidList.clear();

//              Save every chat sender's id related to current user in chatUidList
                for (DataSnapshot data : snapshot.getChildren()) {
                    Chat chat = data.getValue(Chat.class);

                    if (chat.getSender().equals(fUser.getUid())) {
                        chatUidList.add(chat.getReceiver());
                    }

                    if (chat.getReceiver().equals(fUser.getUid())) {
                        chatUidList.add(chat.getSender());
                    }

                    readChats();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return view;
    }

    private void readChats() {
        mUsers = new ArrayList<>();

        try {
            reference = FirebaseDatabase.getInstance(getContext().getString(R.string.databaseURL)).getReference("Users");
        } catch (Exception e) {
            reference = FirebaseDatabase.getInstance("https://chatapp-fc0be-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Users");
        }

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mUsers.clear();

//              Loop through all users and see if they have chat with us
                for (DataSnapshot data : snapshot.getChildren()) {
                    User user = data.getValue(User.class);
//                  If the user never chat with us, skip
                    if (!chatUidList.contains(user.getId())) continue;

//                  If had chat & mUsers is empty -> add him
                    if (mUsers.isEmpty()) {
                        mUsers.add(user);
                        continue;
                    }

//                  If had chat & musers isnt empty, make sure no duplicate
                    if (!mUsers.contains(user)) mUsers.add(user);
                }

                userAdapter = new UserAdapter(getContext(), mUsers);
                recyclerView.setAdapter(userAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}