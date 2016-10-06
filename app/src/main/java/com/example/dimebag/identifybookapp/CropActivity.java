package com.example.dimebag.identifybookapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.isseiaoki.simplecropview.CropImageView;
import com.isseiaoki.simplecropview.callback.CropCallback;
import com.isseiaoki.simplecropview.callback.LoadCallback;
import com.isseiaoki.simplecropview.callback.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CropActivity extends AppCompatActivity {

    private static final int IMAGE_COMPRESSION_QUALITY = 90;
    private static final int IMAGE_SIZE = 800;              //usually width
    private static final int MIN_FRAME_SIZE = 30;           //dp
    private static final String TAG = "Crop";
    private static final String ERROR_MESSAGE = "Error cropping image. Please re-take picture.";

    private CropImageView cropImageView;
    private Uri picUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        getPicUri();
        setCropImageView();
        setOKButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cropImageView = null;
    }

    private void getPicUri() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.w(TAG,"extras is null");
            Toast.makeText(CropActivity.this, "Error cropping image", Toast.LENGTH_SHORT).show();
        } else {
            picUri = Uri.parse(extras.getString(FindBookActivity.INTENT_EXTRA_PIC_URI));
        }
    }

    private void setOKButton() {
        Button buttonOK = (Button) findViewById(R.id.crop_button);
        assert buttonOK != null;
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImageView.startCrop(picUri,
                        new CropCallback() {
                            @Override
                            public void onSuccess(Bitmap cropped) {
                                saveCroppedImage(cropped);
                            }
                            @Override
                            public void onError() {
                                handleError("error starting crop");
                            }
                        },
                        new SaveCallback() {
                            @Override
                            public void onSuccess(Uri outputUri) {
                                setResult(Activity.RESULT_OK, new Intent());
                                finish();
                            }
                            @Override
                            public void onError() {
                                handleError("error saving callback crop");
                            }
                        }
                );
            }
        });
    }

    private void saveCroppedImage(Bitmap cropped) {
        // save uri to rewrite file to it
        String uri = picUri.getPath();

        //delete previous image
        File f = new File(uri);
        if (f.delete()) {
            Log.i(TAG, "success deleting previous image");
        } else {
            Log.w(TAG, "fail deleting previous image");
        }

        //create file on same uri and get the byte[] to write on it
        File imageFile = new File(uri);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        cropped.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, stream);
        byte[] byteArray = stream.toByteArray();

        //write the file
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(imageFile);
            fos.write(byteArray);
            fos.close();
        } catch (IOException e) {
            Log.e(TAG,"Error saving cropped image: " + e.getMessage());
        }

        //make sure it shows in the gallery
        updateGallery();
    }

    /** To update the phone gallery with the just cropped picture */
    private void updateGallery() {
        if (Build.VERSION.SDK_INT >= 19) {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, picUri));
        }
        else {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
        }
    }

    private void setCropImageView() {
        cropImageView = (CropImageView) findViewById(R.id.cropImageView);
        assert cropImageView != null;
        cropImageView.setCropMode(CropImageView.CropMode.FREE);
        cropImageView.setOutputMaxSize(IMAGE_SIZE, IMAGE_SIZE);
        cropImageView.setMinFrameSizeInDp(MIN_FRAME_SIZE);
        cropImageView.startLoad(picUri, new LoadCallback() {
            @Override
            public void onSuccess() { }
            @Override
            public void onError() {
                handleError("error starting load");
            }
        });
    }

    /** Inform user if there's an error and end activity */
    void handleError(String message) {
        Log.e(TAG, message);
        Toast.makeText(CropActivity.this,ERROR_MESSAGE,Toast.LENGTH_SHORT).show();
        finish();
    }
}
