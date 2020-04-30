package com.example.clinger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    //Strings
    private String mChatUser;
    private String mUserName;
    private String mCurrentUser;
    private String prevKey;

    //Ints
    private static final int TOTAL_ITEMS_TO_LOAD = 11;
    private int pos=0;
    //for sending image
    private static final int IMAGE_SEND_INT=1;

    //For displaying messages
    private final List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter messageAdapter;

    //Layout
    private TextView mTitleView;
    private TextView mLastSeen;
    private CircleImageView mProfileImage;
    private ImageButton mAddButton;
    private ImageButton mSendButton;
    private EditText mChatMessageView;
    private Toolbar mChatToolbar;
    private RecyclerView mMessageList;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    //Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private StorageReference mStorRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Getting From Previous Activity
        mChatUser = getIntent().getStringExtra("User_Key");
        mUserName = getIntent().getStringExtra("User_Name");

        //DatbaseReference
        mDatabase= FirebaseDatabase.getInstance().getReference();

        //StorageFirebase
        mStorRef = FirebaseStorage.getInstance().getReference();

        //Getting Current User
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser().getUid();

        //Toolbar
        mChatToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(mChatToolbar);

        //ActionBar
        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle(mUserName);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar,null);
        actionBar.setCustomView(action_bar_view);

        //Finding Layouts
        mTitleView = findViewById(R.id.custom_bar_name);
        mLastSeen = findViewById(R.id.custom_bar_LastSeen);
        mProfileImage = findViewById(R.id.custom_bar_image);
        mAddButton = findViewById(R.id.chat_add_button);
        mSendButton = findViewById(R.id.chat_send_button);
        mChatMessageView = findViewById(R.id.chat_message_input);
        mMessageList = findViewById(R.id.chat_messages_list);
        mSwipeRefreshLayout = findViewById(R.id.chat_swipe_refresh);

        mTitleView.setText(mUserName);

        //Linear Layout that shows messages
        mLinearLayout = new LinearLayoutManager(this);
        mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLinearLayout);
        messageAdapter=new MessageAdapter(ChatActivity.this,messageList);
        mMessageList.setAdapter(messageAdapter);

        loadMessages();

        //This is to display info in the toolbar
        mDatabase.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String online = dataSnapshot.child("online").getValue().toString();
                String image = dataSnapshot.child("thumb_image").getValue().toString();

                if(online.equals("true")){
                    mLastSeen.setText("online");
                }
                else
                {
                    GetTimeAgo timeAgo = new GetTimeAgo();
                    String time = timeAgo.getTimeAgo(Long.parseLong(online),getApplicationContext());
                    mLastSeen.setText(time);
                }

                Picasso.get().load(image).placeholder(R.drawable.default_pic).into(mProfileImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });




        //ToAddChat
        mDatabase.child("Chat").child(mCurrentUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild(mChatUser)){
                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen",false);
                    chatAddMap.put("timestamp",ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/"+mCurrentUser+"/"+mChatUser,chatAddMap);
                    chatUserMap.put("Chat/"+mChatUser+"/"+mCurrentUser,chatAddMap);

                    mDatabase.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if(databaseError !=null){
                                Log.d("Chat error",databaseError.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Click Listener for send button
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        //Retrieve Old messages when swipe refresh is called
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("dekh","List cleared");
                loadMoreMessages();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });


        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select image"),IMAGE_SEND_INT);
            }
        });


    //OnCreate Ending Bracket
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == IMAGE_SEND_INT && resultCode==RESULT_OK){
            Uri Imageuri = data.getData();
            Log.d("image",Imageuri.toString());

            DatabaseReference imageMessageReference = mDatabase.child("messages").child(mCurrentUser).child(mChatUser).push();
            final String push_id=imageMessageReference.getKey();

            final StorageReference imageRef = mStorRef.child("message_images").child(push_id+".jpg");
            imageRef.putFile(Imageuri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()){
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                Map message = new HashMap();
                                message.put("message",uri.toString());
                                message.put("seen",false);
                                message.put("type","image");
                                message.put("time",ServerValue.TIMESTAMP);
                                message.put("from",mCurrentUser);

                                Map messageUserMap = new HashMap();
                                messageUserMap.put("messages/"+mCurrentUser+"/"+mChatUser+"/"+push_id,message);
                                messageUserMap.put("messages/"+mChatUser+"/"+mCurrentUser+"/"+push_id,message);

                                mDatabase.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                        if(databaseError!=null){
                                            Log.d("messageSend",databaseError.getMessage());
                                        }
                                        else{
                                            Log.d("messageSend","Message Sent");
                                        }
                                    }
                                });
                                mChatMessageView.setText("");
                                mMessageList.scrollToPosition(messageList.size());

                            }
                        });
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"Image Cant be uploaded",Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }

    }

    private void loadMoreMessages() {
        pos=0;
        DatabaseReference messageRef=mDatabase.child("messages").child(mCurrentUser).child(mChatUser);
        Query messageQuery = messageRef.orderByKey().endAt(prevKey).limitToLast(TOTAL_ITEMS_TO_LOAD+1);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message=new Message(dataSnapshot.child("message").getValue().toString(),dataSnapshot.child("seen").getValue().toString(),dataSnapshot.child("time").getValue().toString(),dataSnapshot.child("type").getValue().toString(),dataSnapshot.child("from").getValue().toString());
                if(pos==0 && dataSnapshot.getKey().equals(prevKey)){
                    mSwipeRefreshLayout.setEnabled(false);
                }
                else{
                    if(pos!=TOTAL_ITEMS_TO_LOAD)
                        messageList.add(pos,message);
                    Log.d("dekh",message.getMessage()+" added");
                    //Set that message has been seen
                    mDatabase.child("Chat").child(mCurrentUser).child(mChatUser).child("seen").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("seen","seen true done");
                        }
                    });
                    messageAdapter.notifyDataSetChanged();
                    pos++;
                    if(pos==1){
                        prevKey=dataSnapshot.getKey();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private ChildEventListener mChildEventListener;

    private void loadMessages(){

        Log.d("dekh","load message called");

        DatabaseReference messageRef=mDatabase.child("messages").child(mCurrentUser).child(mChatUser);

        Query messageQuery = messageRef.limitToLast(TOTAL_ITEMS_TO_LOAD);

        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message=new Message(dataSnapshot.child("message").getValue().toString(),dataSnapshot.child("seen").getValue().toString(),dataSnapshot.child("time").getValue().toString(),dataSnapshot.child("type").getValue().toString(),dataSnapshot.child("from").getValue().toString());
                messageList.add(message);
                Log.d("dekh",message.getMessage()+" added");
                //Set that message has been seen
                mDatabase.child("Chat").child(mCurrentUser).child(mChatUser).child("seen").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("seen","seen true done");
                    }
                });
                messageAdapter.notifyDataSetChanged();
                pos++;
                if(pos==1){
                    prevKey = dataSnapshot.getKey();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        messageQuery.addChildEventListener(mChildEventListener);
    }

    //On Start of Activity

    @Override
    protected void onStart() {
        super.onStart();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null){
            sendToStart();
        }
        else{
            mDatabase.child("Users").child(user.getUid()).child("online").setValue(true);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        DatabaseReference messageRef=mDatabase.child("messages").child(mCurrentUser).child(mChatUser);
        Query messageQuery = messageRef.limitToLast(TOTAL_ITEMS_TO_LOAD);
        messageQuery.removeEventListener(mChildEventListener);
        mDatabase.child("Users").child(user.getUid()).child("online").setValue(ServerValue.TIMESTAMP);
    }

    private void sendToStart() {
        Intent startIntent  = new Intent(ChatActivity.this, StartActivity.class);
        startActivity(startIntent);
        finish();
    }


    //Send Message Function
    private void sendMessage(){
        String message = mChatMessageView.getText().toString();

        if(!TextUtils.isEmpty(message)){
            DatabaseReference uniqueMessage = mDatabase.child("messages").child(mCurrentUser).child(mChatUser).push();
            String push_id = uniqueMessage.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message",message);
            messageMap.put("seen",false);
            messageMap.put("type","text");
            messageMap.put("time",ServerValue.TIMESTAMP);
            messageMap.put("from",mCurrentUser);

            Map messageUserMap = new HashMap();
            messageUserMap.put("messages/"+mCurrentUser+"/"+mChatUser+"/"+push_id,messageMap);
            messageUserMap.put("messages/"+mChatUser+"/"+mCurrentUser+"/"+push_id,messageMap);

            mDatabase.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if(databaseError!=null){
                        Log.d("messageSend",databaseError.getMessage());
                    }
                    else{
                        Log.d("messageSend","Message Sent");
                    }
                }
            });

            DatabaseReference myChat = mDatabase.child("Chat").child(mCurrentUser).child(mChatUser).child("seen");
            final DatabaseReference hisChat = mDatabase.child("Chat").child(mChatUser).child(mCurrentUser).child("seen");
            myChat.setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    hisChat.setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("seen","Seen info added");
                        }
                    });

                }
            });

            mChatMessageView.setText("");
            mMessageList.scrollToPosition(messageList.size());

        }
        else{
            Toast.makeText(this,"Kuch likh to lo bhai/behen",Toast.LENGTH_LONG);
        }
    }
}
