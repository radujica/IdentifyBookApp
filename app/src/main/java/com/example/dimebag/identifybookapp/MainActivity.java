package com.radujica.bscproject;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/* TODO
    MUST:
    - tag exceptions + rewrite
    - use internet check
    - picuri -> remove static; use onSaveInstance?
    - reorganize FindBook to return the number to be used with startActivityForResult

    MINOR/DETAILS:
    - WORK on S7 (API 23): NFC turned off/permissions
    - check internet connection
    - onSaveInstance for the static picUri & others
    - onTrimMemory for better memory management
    - shortcut on phone screen
    - transfer traineddata with app when installing
    - layout/colors/etc
    - manager switch to user -> auto start find book
    - switch from arrayadapter to something more efficient, assuming 10s of books

    EXTRA:
    - lookout table for already scanned books
    - store failed search attempts
 */

public class MainActivity extends AppCompatActivity {

    static final String ERROR_MESSAGE = "Something went wrong";
    static final int PERMISSION_CAMERA = 100;
    static final int PERMISSION_READ_EXTERNAL_STORAGE = 101;
    static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 102;

    private static final int LOCATE = 7;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setScanBookButton();
        setManagerModeButton();
        setButtonLocateBooks();

        //start scanning book by default
        Intent intent_findBook = new Intent(MainActivity.this, FindBook.class);
        startActivity(intent_findBook);
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
    protected void onDestroy() {
        super.onDestroy();
    }

    /** Set the manager mode button */
    private void setManagerModeButton() {
        Button buttonSwitchToManager = (Button) findViewById(R.id.button_switchToManager);
        assert buttonSwitchToManager != null;
        buttonSwitchToManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_switchToManager = new Intent(MainActivity.this, Manager.class);
                startActivity(intent_switchToManager);
            }
        });
    }

    /** Set the book scan button */
    private void setScanBookButton() {
        Button buttonScanBook = (Button) findViewById(R.id.button_mainScanBook);
        assert buttonScanBook != null;
        buttonScanBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_findBook = new Intent(MainActivity.this, FindBook.class);
                //intent_scanBook.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //TODO: don't start findBook multiple times
                startActivity(intent_findBook);
            }
        });
    }

    /** Set up the button to locate books based on their tag */
    private void setButtonLocateBooks() {
        Button buttonLocateBooks = (Button) findViewById(R.id.button_locateBooks);
        assert buttonLocateBooks != null;
        buttonLocateBooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                FindBook.picUri = Uri.fromFile(FindBook.getOutputMediaFile());
                intent_takePhoto.putExtra(MediaStore.EXTRA_OUTPUT, FindBook.picUri);
                startActivityForResult(intent_takePhoto, LOCATE);
                //startActivity(new Intent(FindBook.this,LocateBooks.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATE){
            if (resultCode == RESULT_OK) {
                startActivity(new Intent(MainActivity.this,LocateBooks.class));
            } else if (resultCode == RESULT_CANCELED) {
                Log.i(TAG, "canceled taking photo to locate books ");
            } else {
                Log.w(TAG, "taking photo to locate books");
            }
        }
        else {
            Log.w(TAG,"weird requestCode captured in " + TAG);
        }
    }
}
