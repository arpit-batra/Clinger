package com.example.clinger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class Image extends AppCompatActivity {

    private ImageView mImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        String url = getIntent().getStringExtra("image_link");
        mImageView = findViewById(R.id.message_image);
        Picasso.get().load(url).into(mImageView);


    }
}
