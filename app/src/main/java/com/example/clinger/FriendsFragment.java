package com.example.clinger;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {

    private RecyclerView mFriendsList;
    private DatabaseReference mDataBaseReference;
    private FirebaseAuth mAuth;
    private View mView;
    private FirebaseRecyclerAdapter<Friends,FriendsViewHolder> friendsRecylerView;
    String mCurrentUser;

    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_friends, container, false);

        mFriendsList = mView.findViewById(R.id.friends_view);
        mAuth = FirebaseAuth.getInstance();

        mCurrentUser=mAuth.getCurrentUser().getUid();

        mDataBaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mDataBaseReference.keepSynced(true);

        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        Query query = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrentUser);

        FirebaseRecyclerOptions<Friends> options = new FirebaseRecyclerOptions.Builder<Friends>().setQuery(query, new SnapshotParser<Friends>() {
            @NonNull
            @Override
            public Friends parseSnapshot(@NonNull DataSnapshot snapshot) {
                return new Friends(snapshot.child("date").getValue().toString());
            }
        }).build();

         friendsRecylerView = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, int position, @NonNull Friends model) {
                holder.setDate(model.getDate());
                final String userId = getRef(position).getKey();
                mDataBaseReference.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String online = dataSnapshot.child("online").getValue().toString();

                        holder.setTxtName(userName);
                        holder.setImage(dataSnapshot.child("thumb_image").getValue().toString());
                        holder.setUserOnline(online);

                        holder.root.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CharSequence options[] = new CharSequence[]{"Open profile","Send Message"};

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle("Select Options");
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if(i==0){
                                            Intent profileIntent = new Intent(getContext(),ProfileActivity.class);
                                            profileIntent.putExtra("User_Key",userId);
                                            startActivity(profileIntent);
                                        }
                                        else if(i==1){
                                            Intent chatIntent = new Intent(getContext(),ChatActivity.class);
                                            chatIntent.putExtra("User_Key",userId);
                                            chatIntent.putExtra("User_Name",userName);
                                            startActivity(chatIntent);
                                        }
                                    }
                                });
                                builder.show();
                            }
                        });



                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_single_layout,parent,false);
                return new FriendsViewHolder(view);
            }
        };
        mFriendsList.setAdapter(friendsRecylerView);
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        friendsRecylerView.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        friendsRecylerView.stopListening();
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder{
        public RelativeLayout root;
        public TextView txtName;
        public TextView txtStatus;
        public CircleImageView profileImage;
        public ImageView onlineImage;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            root = itemView.findViewById(R.id.users_single_root);
            txtName = itemView.findViewById(R.id.users_single_name);
            txtStatus = itemView.findViewById(R.id.users_single_status);
            profileImage = itemView.findViewById(R.id.users_single_image);
            onlineImage = itemView.findViewById(R.id.users_single_online);
        }

        public void setTxtName(String string){
            txtName.setText(string);
        }

//        public void setTxtStatus(String string){
//            txtStatus.setText(string);
//        }


        public void setImage(String string){
            profileImage.setContentDescription(string);
            Picasso.get().load(string).into(profileImage);
        }

        public void setDate(String string){
            txtStatus.setText(string);
        }

        public void setUserOnline(String status){
            if(status.equals("true")){
                onlineImage.setVisibility(View.VISIBLE);
            }
            else{
                onlineImage.setVisibility(View.INVISIBLE);
            }
        }
    }
}


