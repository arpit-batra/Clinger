package com.example.clinger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    //Firebase Variables

    //Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference mReference;
    private FirebaseUser user;

    //Auth
    private FirebaseAuth mAuth;

    //Firebase Storage
    private StorageReference mStorageRef;

    //Layout Variables
    private TextView mDisplayName;
    private TextView mStatus;
    private CircleImageView mImage;
    private Button mStatusChange;
    private Button mImageChange;
    private ProgressBar mProgressBar;

    //Progress Dialog

    private ProgressDialog mProgressDialog;

    private static final int GALLERY_INT=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Finding Layouts
        mDisplayName = findViewById(R.id.settings_dispalayName);
        mStatus = findViewById(R.id.settings_status);
        mImage = findViewById(R.id.settings_pic);
        mStatusChange=(Button)findViewById(R.id.settings_changeStatus_btn);
        mImageChange = (Button)findViewById(R.id.settings_changeImage_btn);
        mProgressBar = (ProgressBar)findViewById(R.id.settings_progress_bar);


        //Firebase
        mAuth=FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        String uid = user.getUid();

        //Datbase
        mDatabase= FirebaseDatabase.getInstance();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mReference = mDatabase.getReference().child("Users").child(uid);
        mReference.keepSynced(true);
        mStorageRef= FirebaseStorage.getInstance().getReference();

        mProgressBar.setVisibility(ProgressBar.VISIBLE);

        ValueEventListener retrieveData = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                mDisplayName.setText(name);
                mStatus.setText(status);

                if(image.equals("default")){
                    mImage.setImageResource(R.drawable.default_pic);
                    mProgressBar.setVisibility(ProgressBar.GONE);
                }
                else {
                        Picasso.get().load(image).placeholder(R.drawable.default_pic).into(mImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                mProgressBar.setVisibility(ProgressBar.GONE);
                            }

                            @Override
                            public void onError(Exception e) {

                            }
                        });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mReference.addValueEventListener(retrieveData);


        //Changing Status
        mStatusChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String statusValue = mStatus.getText().toString();
                Intent intent = new Intent(SettingsActivity.this,StatusActivity.class);
                intent.putExtra("status",statusValue);
                startActivity(intent);
            }
        });


        //Changing Image
        mImageChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent,"Select Image"),GALLERY_INT);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mReference.child("online").setValue(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            mReference.child("online").setValue(ServerValue.TIMESTAMP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==GALLERY_INT && resultCode==RESULT_OK){
            Uri imageUri=data.getData();
            CropImage.activity(imageUri).setAspectRatio(1,1).start(this);
        }


        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                Uri resultUri = result.getUri();

                File originalFile = new File(resultUri.getPath());


                String imageName = user.getUid();
                Bitmap thumbImage;
                try {
                    thumbImage = new Compressor(this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(50)
                            .compressToBitmap(originalFile);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumbImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    final byte[] byteData = baos.toByteArray();



                    final StorageReference reference = mStorageRef.child("profile_images").child(imageName+".jpg");
                    final StorageReference thumbReference = mStorageRef.child("profile_images").child("thumbs").child(imageName+".jpg");

                    //ProgressDialog
                    mProgressDialog = new ProgressDialog(SettingsActivity.this);
                    mProgressDialog.setTitle("Uploading");
                    mProgressDialog.setMessage("Uploading Image");
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.show();

                    reference.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if(task.isSuccessful()){

                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(final Uri uri) {
                                        UploadTask uploadTask = thumbReference.putBytes(byteData);
                                        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                if(task.isSuccessful()) {
                                                    thumbReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                        @Override
                                                        public void onSuccess(final Uri thumburi) {
                                                            mReference.child("image").setValue(uri.toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        mReference.child("thumb_image").setValue(thumburi.toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if (task.isSuccessful()) {
                                                                                    mProgressDialog.dismiss();
                                                                                    Toast.makeText(SettingsActivity.this, "Image Uploaded Successfully", Toast.LENGTH_LONG).show();
                                                                                } else {
                                                                                    mProgressDialog.dismiss();
                                                                                    Toast.makeText(SettingsActivity.this, "Unable to upload thumbLink", Toast.LENGTH_LONG).show();
                                                                                }
                                                                            }
                                                                        });
                                                                    } else {
                                                                        mProgressDialog.dismiss();
                                                                        Toast.makeText(SettingsActivity.this, "Unable to uplaod image Link", Toast.LENGTH_LONG).show();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                                else{
                                                    mProgressDialog.dismiss();
                                                    Toast.makeText(SettingsActivity.this,"Cannot Uplaod Thumb image",Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                            else{
                                mProgressDialog.dismiss();
                                Toast.makeText(SettingsActivity.this,"Cant upload the image",Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                Toast.makeText(SettingsActivity.this,error.toString(),Toast.LENGTH_LONG).show();
            }
        }
    }

    public static String random(int StringLength){
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        char tempChar;
        for(int i=0;i<StringLength;i++){
            tempChar=(char)(generator.nextInt(96)+32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }
}
