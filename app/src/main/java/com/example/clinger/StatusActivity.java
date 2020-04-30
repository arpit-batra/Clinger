package com.example.clinger;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class StatusActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextInputLayout mStatusText;
    private Button mButton;

    //Firebase
    private DatabaseReference mStatusDatabase;
    private DatabaseReference mUserRef;
    private FirebaseUser mUser;


    //ProgressDialog
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);


        mToolbar = (Toolbar)findViewById(R.id.status_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Account Status");

        mStatusText = (TextInputLayout)findViewById(R.id.status_input);
        mButton = (Button)findViewById(R.id.status_button);

        //To display current status
        String status = getIntent().getStringExtra("status");
        mStatusText.getEditText().setText(status);



        //Firebase
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = mUser.getUid();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mStatusDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);




        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Progress
                mProgress=new ProgressDialog(StatusActivity.this);
                mProgress.setTitle("Updating Status");
                mProgress.setMessage("Your Status is being changed");

                mProgress.show();
                String status = mStatusText.getEditText().getText().toString();
                mStatusDatabase.child("status").setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            mProgress.dismiss();
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"Unable to save changes", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mStatusDatabase.child("online").setValue(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mStatusDatabase.child("online").setValue(ServerValue.TIMESTAMP);
    }
}
