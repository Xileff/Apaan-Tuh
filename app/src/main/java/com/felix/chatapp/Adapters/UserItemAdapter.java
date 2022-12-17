package com.felix.chatapp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.felix.chatapp.MessageActivity;
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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserItemAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private List<User> mUsers;
    private boolean isChat; // to determine if this is ChatsFragment or no
    private String theLastMessage;

    public UserItemAdapter(Context mContext, List<User> mUsers, boolean isChat) {
        this.mUsers = mUsers;
        this.mContext = mContext;
        this.isChat = isChat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new UserItemAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        User user = mUsers.get(position);
        ViewHolder userItem = (ViewHolder) holder;

        userItem.name.setText(user.getName());
        if (user.getImageURL().equals("default")) {
            (userItem).profileImage.setImageResource(R.drawable.nophoto);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(userItem.profileImage);
        }

        if (isChat) {
            ((ViewHolder) holder).lastMessage.setVisibility(View.VISIBLE);
            showLastMessage(user.getId(), ((ViewHolder) holder).lastMessage, ((ViewHolder) holder).badge);
        }

//      When a contact is clicked, go to MessageActivity with the contact's detail
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, MessageActivity.class);
            intent.putExtra("userId", user.getId());
            mContext.startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name, lastMessage;
        public ImageView profileImage;
        public CircleImageView badge;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.profileName);
            profileImage = itemView.findViewById(R.id.profileImage);
            lastMessage = itemView.findViewById(R.id.last_message);
            badge = itemView.findViewById(R.id.img_badge);
        }
    }

    private void showLastMessage(String userId, TextView txtLastMessage, CircleImageView badge) {
        theLastMessage = "";
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference chatsReference = FirebaseDatabase.getInstance("https://chatapp-fc0be-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Chats");

        chatsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (fUser == null) return;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Chat c = data.getValue(Chat.class);
                    if (c.getReceiver().equals(fUser.getUid()) && c.getSender().equals(userId) || c.getSender().equals(fUser.getUid()) && c.getReceiver().equals(userId)) {
                        theLastMessage = c.getMessage();
                    }
                    if (c.getReceiver().equals(fUser.getUid()) && c.getSender().equals(userId)) {
                        badge.setVisibility(c.getIsSeen() ? View.GONE : View.VISIBLE);
                    }
                }
                txtLastMessage.setText(theLastMessage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("UserItemAdapter", error.getMessage());
            }
        });
    }
}
