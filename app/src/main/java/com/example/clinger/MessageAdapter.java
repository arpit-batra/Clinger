package com.example.clinger;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> mMessageList;
    private FirebaseAuth mAuth;
    private Context mContext;

    public MessageAdapter(){
        mAuth=FirebaseAuth.getInstance();
    }

    public MessageAdapter(Context context,List<Message> mMessageList){
        this.mMessageList = mMessageList;
        mAuth = FirebaseAuth.getInstance();
        this.mContext = context;
    }

    @NonNull
    @Override
    public MessageAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_layout,parent,false);
        v.setAlpha(0.5f);
        v.setY(50f);
        v.setScaleX(0.8f);
        v.setScaleY(0.8f);
        v.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f).start();
        return new MessageViewHolder(v);
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder{

        public TextView msgText;
        public TextView msgTime;
        public TextView msgTimeImage;
        public RelativeLayout msgRoot;
        public ImageView msgImage;
        public RelativeLayout rText;
        public RelativeLayout rImage;


        public MessageViewHolder(View view){
            super(view);
            msgRoot=view.findViewById(R.id.message_single_root);
            msgText=view.findViewById(R.id.message_single_text);
            msgTime=view.findViewById(R.id.message_single_time_text);
            msgTimeImage=view.findViewById(R.id.message_single_time_image);
            msgImage=view.findViewById(R.id.message_single_image);
            rText=view.findViewById(R.id.text_linear_layout);
            rImage=view.findViewById(R.id.image_linear_layout);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull  MessageViewHolder holder, int position) {
        Message c = mMessageList.get(position);
        String type = c.getType();
        if(type.equals("text")){
            holder.rText.setVisibility(View.VISIBLE);
            holder.rImage.setVisibility(View.INVISIBLE);
            holder.rImage.removeAllViews();
            holder.msgText.setText(c.getMessage());
            Timestamp timestamp = new Timestamp(Long.parseLong(c.getTime()));
            holder.msgTime.setText(timestamp.toString().substring(11,16));
            String fromId=mAuth.getCurrentUser().getUid();
            String msgFrom=c.getFrom();
            if(msgFrom.equals(fromId)){
                holder.msgText.setBackgroundResource(R.drawable.message_text_background_self);
                holder.msgRoot.setHorizontalGravity(Gravity.RIGHT);
                holder.rText.setHorizontalGravity(Gravity.RIGHT);
                holder.msgText.setTextColor(Color.BLACK);
                holder.msgTime.setTextColor(Color.BLACK);
            }
            else{
                holder.msgText.setBackgroundResource(R.drawable.message_text_background);
                holder.msgRoot.setHorizontalGravity(Gravity.LEFT);
                holder.rText.setHorizontalGravity(Gravity.LEFT);
                holder.msgText.setTextColor(Color.WHITE);
                holder.msgTime.setTextColor(Color.BLACK);
            }
        }
        else if(type.equals("image")){
            holder.rText.setVisibility(View.INVISIBLE);
            holder.rImage.setVisibility(View.VISIBLE);

            final String imageUri=c.getMessage();

            holder.msgImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext,Image.class);
                    intent.putExtra("image_link",imageUri);
                    mContext.startActivity(intent);

                }
            });


            Picasso.get().load(imageUri).resize(750,750).into(holder.msgImage);
            Timestamp timestamp = new Timestamp(Long.parseLong(c.getTime()));
            holder.msgTimeImage.setText(timestamp.toString().substring(11,16));
            String fromId=mAuth.getCurrentUser().getUid();
            String msgFrom=c.getFrom();
            if(msgFrom.equals(fromId)){
                holder.msgImage.setBackgroundResource(R.drawable.message_text_background_self);
                holder.msgRoot.setHorizontalGravity(Gravity.RIGHT);
                holder.rImage.setHorizontalGravity(Gravity.RIGHT);
                holder.msgTimeImage.setTextColor(Color.BLACK);

            }
            else{
                holder.msgImage.setBackgroundResource(R.drawable.message_text_background);
                holder.msgRoot.setHorizontalGravity(Gravity.LEFT);
                holder.rImage.setHorizontalGravity(Gravity.LEFT);
                holder.msgTimeImage.setTextColor(Color.BLACK);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size() ;
    }
}
