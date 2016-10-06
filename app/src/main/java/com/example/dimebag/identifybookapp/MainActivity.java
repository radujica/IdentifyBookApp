package com.example.dimebag.identifybookapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/*Main TODO:
 * - copy OCR file with app; AssetManager?
 * - allow user to delete all saved images in 1 tap
 * - add voice recog option
 * - remove search fragment; should use APIs
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_FIND_BOOK = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setFindBookButton();
        setManagerModeButton();
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
    }

    private void setFindBookButton() {
        Button buttonScanBook = (Button) findViewById(R.id.main_button_findbook);
        assert buttonScanBook != null;
        buttonScanBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_findBook = new Intent(MainActivity.this, FindBookActivity.class);
                //intent_scanBook.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //TODO: don't start findBook multiple times
                startActivityForResult(intent_findBook, REQUEST_CODE_FIND_BOOK);
            }
        });
    }

    private void setManagerModeButton() {
        Button buttonSwitchToManager = (Button) findViewById(R.id.main_button_manager);
        assert buttonSwitchToManager != null;
        buttonSwitchToManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_switchToManager = new Intent(MainActivity.this, ManagerActivity.class);
                startActivity(intent_switchToManager);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FIND_BOOK){
            if (resultCode == RESULT_OK) {
                String isbn = data.getExtras().getString(FindBookActivity.INTENT_EXTRA_ISBN);
                Intent intentViewBookOnAmazon = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.amazon.com/s/field-keywords=" + isbn));
                startActivity(intentViewBookOnAmazon);
            } else if (resultCode == RESULT_CANCELED) {
                Log.i(TAG, "canceled finding book");
            } else {
                Log.w(TAG, "weird result code received: " + resultCode);
            }
        }
        else {
            Log.w(TAG,"weird requestCode captured:  " + requestCode);
        }
    }
}
