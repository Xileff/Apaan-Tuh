package com.felix.chatapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.felix.chatapp.Adapters.UserItemAdapter;
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
    private UserItemAdapter userItemAdapter;
    private LinearLayout noChat;
    private List<User> mUsers; // Will contain all the user models whom we have chat with
    private List<String> chatUidList; // Will contain all id of the users whom we have chat with

    FirebaseUser fUser;
    DatabaseReference chatsReference, usersReference;

//  1. Collect all users id whom have chat with us -> chatUidList
//  2. Collect all users model whom have their id listed in chatUidList -> mUsers
//  3. Make userItemAdapter with mUsers
//  4. Use userItemAdapter as adapter for recyclerView

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        noChat = view.findViewById(R.id.noChats);

        if (isAdded() && getActivity() != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        fUser = FirebaseAuth.getInstance().getCurrentUser();
        chatUidList = new ArrayList<>();

        if (isAdded() && getActivity() != null) {
            chatsReference = FirebaseDatabase.getInstance(requireContext().getString(R.string.databaseURL)).getReference("Chats");
        }

        chatsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatUidList.clear();
//              Save every chat sender's id whom we have chat with
                for (DataSnapshot data : snapshot.getChildren()) {
                    Chat chat = data.getValue(Chat.class);
                    if (chat == null) return;

                    if (chat.getSender().equals(fUser.getUid())) {
                        chatUidList.add(chat.getReceiver());
                    } else if (chat.getReceiver().equals(fUser.getUid())) {
                        chatUidList.add(chat.getSender());
                    }

                    readChats();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("ChatsFragment", error.getMessage());
            }
        });

        return view;
    }

    private void readChats() {
        mUsers = new ArrayList<>();

        if (isAdded() && getActivity() != null) {
            usersReference = FirebaseDatabase.getInstance(requireContext().getString(R.string.databaseURL)).getReference("Users");
        }

        usersReference.addValueEventListener(new ValueEventListener() {
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

                if (isAdded() && getActivity() != null) {
                    userItemAdapter = new UserItemAdapter(requireContext(), mUsers, true);
                    recyclerView.setAdapter(userItemAdapter);
                }

//              Show no chat picture, if there is no chat
                if (mUsers.size() > 0) {
                    noChat.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    noChat.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("ChatsFragment", error.getMessage());
            }
        });
    }
}