package com.example.clinger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {


    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private DatabaseReference mDatabaseRef;

    private FirebaseRecyclerAdapter mFirebaseRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);


        mToolbar=(Toolbar)findViewById(R.id.users_appBar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Users");


        mRecyclerView = (RecyclerView)findViewById(R.id.users_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));



        Query query = FirebaseDatabase.getInstance().getReference().child("Users");

        FirebaseRecyclerOptions<Users> options = new FirebaseRecyclerOptions.Builder<Users>().setQuery(query, new SnapshotParser<Users>() {
            @NonNull
            @Override
            public Users parseSnapshot(@NonNull DataSnapshot snapshot) {
                return new Users(snapshot.child("name").getValue().toString(),snapshot.child("image").getValue().toString(),snapshot.child("status").getValue().toString(),snapshot.child("thumb_image").getValue().toString());
            }
        }).build();

        mFirebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Users, UsersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder holder, final int position, @NonNull Users model) {
                holder.setTxtName(model.getName());
                holder.setTxtStatus(model.getStatus());
                holder.setImage(model.getThumb_image());
                final String userId = getRef(position).getKey();
                holder.root.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent profileIntent = new Intent(UsersActivity.this,ProfileActivity.class);
                        profileIntent.putExtra("User_Key",userId);
                        startActivity(profileIntent);
                    }
                });
            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_single_layout,viewGroup,false);
                return new UsersViewHolder(view);
            }
        };
        mRecyclerView.setAdapter(mFirebaseRecyclerAdapter);
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mDatabaseRef=FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Check","UserActivity Started");
        mFirebaseRecyclerAdapter.startListening();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mDatabaseRef.child("online").setValue(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFirebaseRecyclerAdapter.stopListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Check","UserActivity Stopped");
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
           mDatabaseRef.child("online").setValue(ServerValue.TIMESTAMP);
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder {

        public RelativeLayout root;
        public TextView txtName;
        public TextView txtStatus;
        public CircleImageView profileImage;
        public ImageView online;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);

            root = itemView.findViewById(R.id.users_single_root);
            txtName = itemView.findViewById(R.id.users_single_name);
            txtStatus = itemView.findViewById(R.id.users_single_status);
            profileImage = itemView.findViewById(R.id.users_single_image);
            online = itemView.findViewById(R.id.users_single_online);
            online.setVisibility(View.INVISIBLE);
        }

        public void setTxtName(String string){
            txtName.setText(string);
        }

        public void setTxtStatus(String string){
            txtStatus.setText(string);
        }

        public void setImage(String string){
            profileImage.setContentDescription(string);
            Picasso.get().load(string).into(profileImage);
        }
    }

}
