package com.example.clinger;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatsFragment extends Fragment {
   public ChatsFragment() {
        // Required empty public constructor
    }

    private String mCurrentUser;
    private List<Chat> mChatList;

    //Layout
    private RecyclerView mChatListView;

    //Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersReference;
    private DatabaseReference mMessageReference;
    private DatabaseReference mChatReference;

    private FirebaseRecyclerAdapter<Chat,ChatViewHolder> mChatAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mview= inflater.inflate(R.layout.fragment_chats, container, false);
        Log.d("logss","hello");
        //finding layout
        mChatListView  = mview.findViewById(R.id.chats_recycler_view);

        //Getting UserId
        mAuth=FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser().getUid();

        LinearLayoutManager linearLayoutManager =new LinearLayoutManager(getContext());
        mChatListView.setLayoutManager(linearLayoutManager);
        mChatListView.setHasFixedSize(true);

        //Setting database references
        mUsersReference= FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersReference.keepSynced(true);
        mMessageReference=FirebaseDatabase.getInstance().getReference().child("messages");
        mChatReference = FirebaseDatabase.getInstance().getReference().child("Chat");

        Query query = FirebaseDatabase.getInstance().getReference().child("Chat").child(mCurrentUser);

        FirebaseRecyclerOptions<Chat> options = new FirebaseRecyclerOptions.Builder<Chat>().setQuery(query, new SnapshotParser<Chat>() {
            @NonNull
            @Override
            public Chat parseSnapshot(@NonNull DataSnapshot snapshot) {
                return new Chat(snapshot.child("seen").getValue().toString(),snapshot.child("timestamp").getValue().toString());
            }
        }).build();

        mChatAdapter = new FirebaseRecyclerAdapter<Chat,ChatViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatViewHolder holder, int position, @NonNull Chat model) {
                final Boolean seen = Boolean.parseBoolean(model.getSeen());
                final String friendUserId = getRef(position).getKey();
                Log.d("logss",friendUserId);
                mUsersReference.child(friendUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String friendName = dataSnapshot.child("name").getValue().toString();
                        holder.setFriendName(friendName,seen);
                        holder.setImage(dataSnapshot.child("thumb_image").getValue().toString());
                        holder.setLastSeen(dataSnapshot.child("online").getValue().toString());
                        holder.root.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {


                                Intent intent = new Intent(getContext(),ChatActivity.class);
                                intent.putExtra("User_Key",friendUserId);
                                intent.putExtra("User_Name",friendName);
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                Query q=mMessageReference.child(mCurrentUser).child(friendUserId).limitToLast(1);
                q.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        String type = dataSnapshot.child("type").getValue().toString();
                        if(type.equals("image")){
                            holder.setLastMessage(" * Image *",seen);
                        }
                        else{
                            String lastMessage = dataSnapshot.child("message").getValue().toString();
                            holder.setLastMessage(lastMessage,seen);
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

            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_single_layout,parent,false);
                return new ChatViewHolder(view);
            }
        };
        mChatListView.setAdapter(mChatAdapter);
       return mview;
   }

    @Override
    public void onStart() {
        super.onStart();
        mChatAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        mChatAdapter.stopListening();
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder{

        public CircleImageView circleImageView;
        public TextView friendName;
        public TextView lastMessage;
        public ImageView online;
        public RelativeLayout root;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            circleImageView=itemView.findViewById(R.id.users_single_image);
            friendName = itemView.findViewById(R.id.users_single_name);
            lastMessage = itemView.findViewById(R.id.users_single_status);
            online = itemView.findViewById(R.id.users_single_online);
            root = itemView.findViewById(R.id.users_single_root);
        }

        public void setFriendName(String string, Boolean seen){
            if(seen==true){
                friendName.setTypeface(Typeface.DEFAULT);
            }
            else{
                friendName.setTypeface(Typeface.DEFAULT_BOLD);
            }
            friendName.setText(string);
        }

        public void setLastMessage(String string, Boolean seen){
            if(seen==true){
                lastMessage.setTypeface(Typeface.DEFAULT);
            }
            else{
                lastMessage.setTypeface(Typeface.DEFAULT_BOLD);
            }
            lastMessage.setText(string);
        }

        public void setImage(String string){
            Picasso.get().load(string).into(circleImageView);
        }

        public void setLastSeen(String string){
            if(string.equals("true")){
                online.setVisibility(View.VISIBLE);
            }
            else{
                online.setVisibility(View.INVISIBLE);
            }
        }
    }
}
