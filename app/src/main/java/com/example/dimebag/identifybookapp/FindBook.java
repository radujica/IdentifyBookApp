package com.example.dimebag.identifybookapp;

/*TODO:
    - copy OCR file with app
    - implement server on raspberry pi
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FindBook extends Activity {

    private static final String TAG = "FindBook";
    private static final String NOT_FOUND = "Could not find book. Please try again or use another search option";
    private static final int PERMISSION_CAMERA = 100000;
    private static final int REQUEST_CODE_BARCODE = 100;
    private static final int REQUEST_CODE_NFC_SCAN = 101;

    static final String INTENT_EXTRA_ISBN = "isbn";

    //server details; initially used for a given server but currently just placeholders
    static final String SERVER_CREDENTIALS = okhttp3.Credentials.basic("api_user","pass");
    private static final String STORE_INTERACTION_URL = "http://server.nl/bookInteraction/records.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_book);

        setButtonBarcodeScan();
        setButtonScanNFC();
        setButtonOCR();
        setButtonReverseImageSearch();
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

    private void setButtonBarcodeScan() {
        Button buttonScanISBN = (Button) findViewById(R.id.findbook_button_barcode);
        assert buttonScanISBN != null;
        buttonScanISBN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermission();
                //activity gets started in onRequestPermissionResult if successful
            }
        });
    }

    private void setButtonScanNFC() {
        Button buttonScanNFC = (Button) findViewById(R.id.findbook_button_nfc);
        assert buttonScanNFC != null;
        buttonScanNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(FindBook.this, ScanNFC.class), REQUEST_CODE_NFC_SCAN);
            }
        });
    }

    private void setButtonOCR() {
        Button buttonScanISBN = (Button) findViewById(R.id.findbook_button_ocr);
        assert buttonScanISBN != null;
        buttonScanISBN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createIntentTakePhoto(CROPPED_IMAGE_ISBN);
            }
        });
    }

    private void setButtonReverseImageSearch() {
        Button buttonTakePhoto = (Button) findViewById(R.id.findbook_button_reverse_image_search);
        assert buttonTakePhoto != null;
        buttonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createIntentTakePhoto(CROPPED_IMAGE_SEARCH);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_BARCODE:
                if (resultCode == RESULT_OK) {
                    String number = data.getExtras().getString(BarcodeScanner.INTENT_EXTRA_NUMBER);
                    String format = data.getExtras().getString(BarcodeScanner.INTENT_EXTRA_FORMAT);
                    if (checkProperISBNFromBarcode(number, format)) {
                        //storeInteractionOnServer(number, "barcode");           //TODO: remove comment when possible
                        returnResultToMain(number);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "canceled reading barcode ");
                } else {
                    Log.w(TAG, "failed getting barcode result");
                }
                break;
            case REQUEST_CODE_NFC_SCAN:
                if (resultCode == RESULT_OK) {
                    String number = data.getExtras().getString(ScanNFC.INTENT_EXTRA_NUMBER);
                    //storeInteractionOnServer(number, "nfc");                  //TODO: remove comment when possible
                    returnResultToMain(number);
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "canceled reading barcode ");
                } else {
                    Log.w(TAG, "failed getting barcode result");
                }
                break;
            default:
                Log.w(TAG, "weird requestCode captured in onActivityResult: " + requestCode);
                break;
        }
    }

    private boolean checkProperISBNFromBarcode(String number, String format) {
        if (number == null) {
            Toast.makeText(this,NOT_FOUND,Toast.LENGTH_LONG).show();
            Log.w(TAG, "ISBN from barcode is null");
            return false;
        }
        if (format.equals("EAN_13")) {
            return true;
        } else {
            Toast.makeText(FindBook.this,"Found different barcode type: " + format,Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void storeInteractionOnServer(String isbn, String interaction_type) {
        new UploadInteraction().execute(isbn,interaction_type);
    }

    /** Asynctask to upload interaction data */
    class UploadInteraction extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            OkHttpClient client = new OkHttpClient();

            Long timestamp = System.currentTimeMillis()/1000;
            JSONObject json = new JSONObject();
            try {
                JSONArray jsonArray = new JSONArray();
                JSONObject entry = new JSONObject();
                entry.put("timestamp",timestamp.toString());
                entry.put("android_id", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                entry.put("interaction_type",params[1]);
                entry.put("book_isbn",params[0]);
                entry.put("book_genre","nl");
                jsonArray.put(entry);
                json.put("book_interaction_records",jsonArray);

                Log.i(TAG,"book data: " + json.toString());
            } catch (JSONException e) {
                Log.e(TAG,"Error creating JSON while uploading interaction: " + e.getMessage());
            }

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),json.toString());

            Request request = new Request.Builder()
                    .header("Authorization", SERVER_CREDENTIALS)
                    .url(STORE_INTERACTION_URL)
                    .post(requestBody)
                    .build();

            Response response = null;
            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                Log.i(TAG,"data server response when uploading data: " + response.body().string());  //if removing log, close response body!
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"error uploading data to server");
                if (response != null) {
                    response.body().close();
                }
            }
            return null;
        }
    }

    private void returnResultToMain(String number) {
        Intent intent_returnToMain = new Intent();
        intent_returnToMain.putExtra(INTENT_EXTRA_ISBN, number);
        setResult(Activity.RESULT_OK, intent_returnToMain);
        finish();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(FindBook.this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(FindBook.this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CAMERA:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(FindBook.this, "This functionality requires the camera", Toast.LENGTH_LONG).show();
                    } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        startActivityForResult(new Intent(FindBook.this, BarcodeScanner.class),REQUEST_CODE_BARCODE);
                    } else {
                        Log.w(TAG,"something weird happened when requesting camera permission: " + Arrays.toString(grantResults));
                        finish();
                    }
                }
                break;
            default:
                Log.w(TAG,"some weird requestCode in permissions has been sent back: " + requestCode);
                finish();
                break;
        }
    }

}
