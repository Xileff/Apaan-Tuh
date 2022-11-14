package com.felix.chatapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Models.Chat;
import com.felix.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<Chat> mChats;
    private String imageUrl;

    FirebaseUser fUser;

    public MessageAdapter(Context mContext, List<Chat> mChats, String imageUrl) {
        this.mContext = mContext; //Context depends from the activity which calls this constructor
        this.mChats = mChats;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
//    The data type 'ViewHolder' refers to the inner class below
//    onCreateViewHolder is used to inflate the chat_item_<direction>.xml layout
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int direction = viewType == MSG_TYPE_RIGHT ? R.layout.chat_item_right : R.layout.chat_item_left;
        View chatBubble = LayoutInflater.from(mContext).inflate(direction, parent, false);

        return new MessageAdapter.ViewHolder(chatBubble);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = mChats.get(position);
        holder.showMessage.setText(chat.getMessage());

        if (imageUrl.equals("default")) {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(imageUrl).into(holder.profile_image);
        }
    }

    @Override
    public int getItemCount() {
        return mChats.size();
    }

    @Override
    public int getItemViewType(int position) {
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mChats.get(position).getSender().equals(fUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    //    Used to hold user_item.xml
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView showMessage;
        public ImageView profile_image;

        public ViewHolder(View itemView) {
            super(itemView);
            showMessage = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
        }
    }
}
