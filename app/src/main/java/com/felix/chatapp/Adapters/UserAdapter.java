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
import com.felix.chatapp.Models.User;
import com.felix.chatapp.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private List<User> mUsers;

    public UserAdapter(Context mContext, List<User> mUsers) {
        this.mUsers = mUsers;
        this.mContext = mContext; //Context depends from the activity which calls this constructor
    }

    @NonNull
    @Override
//    The data type 'ViewHolder' refers to the inner class below
//    onCreateViewHolder is used to inflate the user_item.xml layout
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new UserAdapter.ViewHolder(view);
    }

    @Override
//    For binding each contact(user_item.xml) on the RecyclerView
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        User user = mUsers.get(position);
        ViewHolder userItem = (ViewHolder) holder;
        // The data type 'ViewHolder' refers to the inner class below

        userItem.username.setText(user.getUsername());
        if (user.getImageURL().equals("default")) {
            (userItem).profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(userItem.profile_image);
        }

//      When a contact is clicked, go to MessageActivity with the contact's detail
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userId", user.getId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

//    Used to hold user_item.xml
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView username;
        public ImageView profile_image;

        public ViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            profile_image = itemView.findViewById(R.id.profile_image);
        }
    }
}
