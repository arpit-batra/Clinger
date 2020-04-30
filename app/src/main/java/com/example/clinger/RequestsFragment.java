package com.example.clinger;


import android.os.Bundle;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private RecyclerView mRequestRecyclerView;
    private DatabaseReference mDatabase;
    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mReqDatabase;
    private FirebaseAuth mAuth;
    private String mCurrentUser;
    private FirebaseRecyclerAdapter<Request,RequestViewHolder> firebaseRecyclerAdapter;
    private String type;

    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_requests, container, false);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFriendsDatabase=mDatabase.child("Friends");
        mReqDatabase=mDatabase.child("Friend_req");
        mRequestRecyclerView = view.findViewById(R.id.request_recycler_view);
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser=mAuth.getCurrentUser().getUid();

        mRequestRecyclerView.setHasFixedSize(true);
        mRequestRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Query query = mDatabase.child("Friend_req").child(mCurrentUser);

        FirebaseRecyclerOptions<Request> options = new FirebaseRecyclerOptions.Builder<Request>().setQuery(query, new SnapshotParser<Request>() {
            @NonNull
            @Override
            public Request parseSnapshot(@NonNull DataSnapshot snapshot) {
                return new Request(snapshot.getKey(),snapshot.child("request_type").getValue().toString());
            }
        }).build();

        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Request, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull Request model) {
                final String uid=model.getFrom();
                type = model.getType();
                holder.checkType(type);
                Log.d("infoo",uid+" "+type);
                if(type.equals("r")){
                    mDatabase.child("Users").child(uid).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Log.d("infoo",dataSnapshot.child("name").getValue().toString());
                            holder.setReqName(dataSnapshot.child("name").getValue().toString());
                            holder.setReqStatus(dataSnapshot.child("status").getValue().toString());
                            holder.setReqImage(dataSnapshot.child("thumb_image").getValue().toString());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    holder.accept.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final String currDate = DateFormat.getDateTimeInstance().format(new Date());
                            mFriendsDatabase.child(mCurrentUser).child(uid).child("date").setValue(currDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mFriendsDatabase.child(uid).child(mCurrentUser).child("date").setValue(currDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mReqDatabase.child(mCurrentUser).child(uid).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mReqDatabase.child(uid).child(mCurrentUser).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Toast.makeText(getContext(),"Your are Now Friends",Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });

                    holder.decline.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mDatabase.child("Friend_req").child(mCurrentUser).child(uid).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mDatabase.child("Friend_req").child(uid).child(mCurrentUser).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(getContext(),"Request Declined ",Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_single_layout,parent,false);
                return new RequestViewHolder(view);
            }
        };

        mRequestRecyclerView.setAdapter(firebaseRecyclerAdapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseRecyclerAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseRecyclerAdapter.stopListening();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {

        public TextView nameview;
        public TextView statusview;
        public CircleImageView cimageview;
        public Button accept;
        public Button decline;
        public ConstraintLayout root;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.request_single_root);
            nameview = itemView.findViewById(R.id.request_single_name);
            statusview = itemView.findViewById(R.id.request_single_status);
            cimageview = itemView.findViewById(R.id.request_single_image);
            accept = itemView.findViewById(R.id.request_single_accept_button);
            decline = itemView.findViewById(R.id.request_single_decline_button);
        }

        public void checkType(String type){
            if(type.equals("s")){
                root.setVisibility(View.GONE);
            }
            else{
                root.setVisibility(View.VISIBLE);
            }
        }


        public void setReqName(String name){
            nameview.setText(name);
        }

        public void setReqStatus(String status){
            statusview.setText(status);
        }

        public void setReqImage(String img){
            Picasso.get().load(img).placeholder(R.drawable.default_pic).into(cimageview);
        }
    }
}
