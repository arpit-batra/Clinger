package com.example.clinger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    //Layouts
    private ImageView mProfileImage;
    private TextView mProfileName;
    private TextView mProfileStatus;
    private TextView mProfileFriendCount;
    private Button mProfileSendRequest;
    private Button mProfileDeclineBtn;
    private ProgressBar mProgressBar;

    //Database References
    private DatabaseReference mProfileDatabase;
    private DatabaseReference mReqDatabase;
    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mNotifications;
    private DatabaseReference mUserRef;


    private int mRelationStatus;
    // 0 - Not Friends now can send request
    // 1 - Request Sent now can cancel
    // 2 - Request Recieved now can accept or reject
    // 3 - Friends now can unfriend

    private FirebaseUser mCurrUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String userKey = getIntent().getStringExtra("User_Key");

        //Layouts
        mProfileImage = (ImageView)findViewById(R.id.profile_image);
        mProfileName = (TextView)findViewById(R.id.profile_name);
        mProfileStatus = (TextView)findViewById(R.id.profile_status);
        mProfileFriendCount = (TextView)findViewById(R.id.profile_friend_count);
        mProfileSendRequest = (Button) findViewById(R.id.profile_request_button);
        mProfileDeclineBtn = (Button)findViewById(R.id.profile_declinebtn);

        ConstraintLayout layout = findViewById(R.id.profile_root);

        //ProgressBar
        mProgressBar = (ProgressBar)findViewById(R.id.profile_progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);

        //Getting Current User
        mCurrUser = FirebaseAuth.getInstance().getCurrentUser();


        //Getting References for databases
        mProfileDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userKey);
        mReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotifications = FirebaseDatabase.getInstance().getReference().child("Notifications");

        mProfileSendRequest.setVisibility(Button.GONE);
        mProfileSendRequest.setEnabled(false);

        mProfileDeclineBtn.setVisibility(Button.GONE);
        mProfileDeclineBtn.setEnabled(false);

        mProfileDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mProfileName.setText(dataSnapshot.child("name").getValue().toString());
                mProfileStatus.setText(dataSnapshot.child("status").getValue().toString());
                Picasso.get().load(dataSnapshot.child("image").getValue().toString()).into(mProfileImage,new com.squareup.picasso.Callback(){
                    @Override
                    public void onSuccess(){
                        mProgressBar.setVisibility(View.GONE);
                        //---------------------------Retrieving Current Relation----------------------------------

                        mProfileSendRequest.setEnabled(true);
                        mProfileSendRequest.setVisibility(Button.VISIBLE);

                        mReqDatabase.child(mCurrUser.getUid()).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.hasChild(userKey)){
                                    String status = dataSnapshot.child(userKey).child("request_type").getValue().toString();
//                                    Toast.makeText(ProfileActivity.this,status,Toast.LENGTH_LONG).show();
                                    if(status.equals("r")){
                                        mRelationStatus=2; // Recieving Request

                                        //Accept Button
                                        mProfileSendRequest.setText("Accept Request");

                                        //Decline Button
                                        mProfileDeclineBtn.setEnabled(true);
                                        mProfileDeclineBtn.setVisibility(Button.VISIBLE);
                                        mProfileDeclineBtn.setText("Decline Request");
                                    }
                                    else if(status.equals("s")){
                                        mRelationStatus=1;//Request_Sent now can cancel
                                        mProfileSendRequest.setText("Cancel Request");
                                        mProfileDeclineBtn.setVisibility(Button.GONE);
                                        mProfileDeclineBtn.setEnabled(false);
                                    }
                                }
                                else{
                                    mFriendsDatabase.child(mCurrUser.getUid()).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.hasChild(userKey)){
                                                mRelationStatus=3; //Friends
                                                mProfileSendRequest.setText("Unfriend");
                                                mProfileDeclineBtn.setVisibility(Button.GONE);
                                                mProfileDeclineBtn.setEnabled(false);
                                            }
                                            else{
                                                mRelationStatus=0; //Not Friends
                                                mProfileSendRequest.setText("Send Friend Request");
                                                mProfileDeclineBtn.setVisibility(Button.GONE);
                                                mProfileDeclineBtn.setEnabled(false);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ProfileActivity.this,"Unable to load image",Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mProfileSendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProfileSendRequest.setEnabled(false);

                //--------------------- Not Friends State 0 --------------------------

                if(mRelationStatus==0){
                    mReqDatabase.child(mCurrUser.getUid()).child(userKey).child("request_type").setValue("s").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                mReqDatabase.child(userKey).child(mCurrUser.getUid()).child("request_type").setValue("r").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        HashMap<String,String> notificationData = new HashMap<>();

                                        notificationData.put("from",mCurrUser.getUid());
                                        notificationData.put("type","request");

                                        mNotifications.child(userKey).push().setValue(notificationData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                mProfileSendRequest.setEnabled(true);
                                                mRelationStatus=1;//Request_Sent
                                                mProfileSendRequest.setText("Cancel Request");
                                                Toast.makeText(ProfileActivity.this,"Request Sent",Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                });
                            }
                            else{
                                   Toast.makeText(ProfileActivity.this,"Error!!!  Try again Later",Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }

                //-----------------------Request Sent Now Cancelling 1 ------------------------------------------

                if(mRelationStatus==1){
                    mReqDatabase.child(mCurrUser.getUid()).child(userKey).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mReqDatabase.child(userKey).child(mCurrUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendRequest.setEnabled(true);
                                    mRelationStatus=0;//Not Friends
                                    mProfileSendRequest.setText("Send Friend Request");
                                    Toast.makeText(ProfileActivity.this,"Request Cancelled",Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }

                //--------------------------------Request Recieved Accepting or Declining 2 ----------------------

                if(mRelationStatus==2){
                    final String currDate = DateFormat.getDateTimeInstance().format(new Date());
                    mFriendsDatabase.child(mCurrUser.getUid()).child(userKey).child("date").setValue(currDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendsDatabase.child(userKey).child(mCurrUser.getUid()).child("date").setValue(currDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mReqDatabase.child(mCurrUser.getUid()).child(userKey).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mReqDatabase.child(userKey).child(mCurrUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mProfileSendRequest.setEnabled(true);
                                                    mRelationStatus=3;   // Friends
                                                    mProfileSendRequest.setText("Unfriend");
                                                    Toast.makeText(ProfileActivity.this,"Your are Now Friends",Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }

                // ------------------------- Friends now unfriending -------------------------------

                if(mRelationStatus == 3){
                    mFriendsDatabase.child(mCurrUser.getUid()).child(userKey).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendsDatabase.child(userKey).child(mCurrUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendRequest.setEnabled(true);
                                    mRelationStatus=0; //Not Friends
                                    mProfileSendRequest.setText("Send Friend Request");
                                    Toast.makeText(ProfileActivity.this,"Unfriended",Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            }
        });

        mProfileDeclineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mReqDatabase.child(mCurrUser.getUid()).child(userKey).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mReqDatabase.child(userKey).child(mCurrUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mProfileSendRequest.setEnabled(true);
                                mRelationStatus=0;//Not Friends
                                mProfileSendRequest.setText("Send Friend Request");
                                Toast.makeText(ProfileActivity.this,"Request Declined ",Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mUserRef= FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrUser.getUid());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mUserRef.child("online").setValue(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mUserRef.child("online").setValue(ServerValue.TIMESTAMP);
    }
}
