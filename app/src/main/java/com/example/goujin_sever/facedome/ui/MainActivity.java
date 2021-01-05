package com.example.goujin_sever.facedome.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.goujin_sever.facedome.R;
import com.example.goujin_sever.facedome.capture.CircleImageView;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public final static int REQUEST_CODE_SCAN = 100;
    private ImageView imageView11;
    private CircleImageView imageView22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView getImageView = findViewById(R.id.get_image_view);
        imageView11 = findViewById(R.id.image_11);
        imageView22 = findViewById(R.id.image_22);
        getImageView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.get_image_view:
                Intent intent = new Intent(MainActivity.this, FaceCaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SCAN);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SCAN:
                    if (data != null) {
                        Bundle b = data.getExtras();
                        Bitmap bmp = b.getParcelable("bitmap");
                        displayView(bmp);
                    }
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void displayView(Bitmap bmp) {
        imageView11.setImageBitmap(bmp);
        imageView22.setImageBitmap(bmp);
    }
}
