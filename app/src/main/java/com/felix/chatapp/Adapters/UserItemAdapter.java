package com.felix.chatapp.Adapters;

import android.content.Context;
import android.content.Intent;
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
    private boolean isChat;

    private String theLastMessage;

    public UserItemAdapter(Context mContext, List<User> mUsers, boolean isChat) {
        this.mUsers = mUsers;
        this.mContext = mContext; //Context depends from the activity which calls this constructor
        this.isChat = isChat;
    }

    @NonNull
    @Override
//    The data type 'ViewHolder' refers to the inner class below
//    onCreateViewHolder is used to inflate the user_item.xml layout
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new UserItemAdapter.ViewHolder(view);
    }

    @Override
//    For binding each contact(user_item.xml) on the RecyclerView
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        User user = mUsers.get(position);
        ViewHolder userItem = (ViewHolder) holder;
        // The data type 'ViewHolder' refers to the inner class below

        userItem.name.setText(user.getName());
        if (user.getImageURL().equals("default")) {
            (userItem).profileImage.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(userItem.profileImage);
        }

        if (isChat) {
            showLastMessage(user.getId(), ((ViewHolder) holder).lastMessage, ((ViewHolder) holder).badge);

            if (user.getStatus().equals("online")) {
                ((ViewHolder) holder).imgOnline.setVisibility(View.VISIBLE);
                ((ViewHolder) holder).imgOffline.setVisibility(View.GONE);
            } else {
                ((ViewHolder) holder).imgOnline.setVisibility(View.GONE);
                ((ViewHolder) holder).imgOffline.setVisibility(View.VISIBLE);
            }
        } else {
            ((ViewHolder) holder).imgOnline.setVisibility(View.GONE);
            ((ViewHolder) holder).imgOffline.setVisibility(View.GONE);
            ((ViewHolder) holder).lastMessage.setVisibility(View.GONE);
        }

//      When a contact is clicked, go to MessageActivity with the contact's detail
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userId", user.getId());
//                Buggy?
                mContext.startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

//    Used to hold user_item.xml
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView name, lastMessage;
        public ImageView profileImage, imgOnline, imgOffline;
        public CircleImageView badge;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            profileImage = itemView.findViewById(R.id.profile_image);
            imgOnline = itemView.findViewById(R.id.img_online);
            imgOffline = itemView.findViewById(R.id.img_offline);
            lastMessage = itemView.findViewById(R.id.last_message);
            badge = itemView.findViewById(R.id.img_badge);
        }
    }

    private void showLastMessage(String userId, TextView txtLastMessage, CircleImageView badge) {
        theLastMessage = "default";
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance("https://chatapp-fc0be-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Chat c = data.getValue(Chat.class);

//                    Prevent crashing after logout
                    if (fUser == null) return;

                    if (c.getReceiver().equals(fUser.getUid()) && c.getSender().equals(userId) || c.getSender().equals(fUser.getUid()) && c.getReceiver().equals(userId)) {
                        theLastMessage = c.getMessage();
                    }

                    if (c.getReceiver().equals(fUser.getUid()) && c.getSender().equals(userId)) {
                        badge.setVisibility(c.getIsSeen() ? View.GONE : View.VISIBLE);
                    }
                }

                txtLastMessage.setText(theLastMessage.equals("default") ? "No message" : theLastMessage);
                theLastMessage = "default";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
