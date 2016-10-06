package com.example.dimebag.identifybookapp;

/*TODO:
 *   - implement server on raspberry pi
 *
 * Possible:
 *   - switch to instance id? String iid = InstanceID.getInstance(context).getId()
 *   - perhaps upload interactions in batches, e.g. when app onStop
 */

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FindBookActivity extends AppCompatActivity implements SearchFragment.Listener {

    private static final String TAG = "FindBookActivity";
    private static final String FRAGMENT_TAG = "SearchFragment";
    private static final String ERROR_NOT_FOUND = "Could not find book. Please try again or use another search option";
    private static final String ERROR_REVERSE_IMAGE_SEARCH = "Error connecting to the internet. Please try again";
    private static final int PERMISSION_CAMERA = 100000;
    private static final int REQUEST_CODE_BARCODE = 100;
    private static final int REQUEST_CODE_NFC_SCAN = 101;
    private static final int REQUEST_CODE_TAKE_PHOTO = 102;
    private static final int REQUEST_CODE_CROP_IMAGE = 103;
    private static final int OCR = 104;
    private static final int ISBN_LENGTH = 13;
    private static final int REVERSE_IMAGE_SEARCH = 105;
    static final int SEARCH_GOOGLE = 106;
    static final int SEARCH_AMAZON = 107;
    static final String INTENT_EXTRA_ISBN = "isbn";
    static final String INTENT_EXTRA_PIC_URI = "picUri";
    static final String INTENT_EXTRA_URL = "url";
    static final String INTENT_EXTRA_REQUEST_CODE = "requestCode";
    static final String INTENT_EXTRA_WEBSITE = "website";

    //server details; initially used for a given server but currently just placeholders
    static final String SERVER_CREDENTIALS = okhttp3.Credentials.basic("api_user","pass");
    private static final String STORE_INTERACTION_URL = "http://server.nl/bookInteraction/records.json";

    //google and imgur details
    private static final String GOOGLE_IMAGE_SEARCH_PREFIX = "https://www.google.com/searchbyimage?site=search&sa=X&image_url=";
    private static final String IMGUR_CLIENT_ID = "id";
    private static final String IMGUR_LINK = "https://api.imgur.com/3/image/";
    private static final int IMAGE_DELETE_DELAY = 10000;    //ms
    private String deleteHash;

    private Uri picUri;
    private int croppedImageNextStep;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_book);

        progressDialog = new ProgressDialog(this);

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
                startActivityForResult(new Intent(FindBookActivity.this, ScanNFCActivity.class), REQUEST_CODE_NFC_SCAN);
            }
        });
    }

    private void setButtonOCR() {
        Button buttonScanISBN = (Button) findViewById(R.id.findbook_button_ocr);
        assert buttonScanISBN != null;
        buttonScanISBN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createIntentTakePhoto(OCR);
            }
        });
    }

    private void setButtonReverseImageSearch() {
        Button buttonTakePhoto = (Button) findViewById(R.id.findbook_button_reverse_image_search);
        assert buttonTakePhoto != null;
        buttonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createIntentTakePhoto(REVERSE_IMAGE_SEARCH);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_BARCODE:
                if (resultCode == RESULT_OK) {
                    String number = data.getExtras().getString(BarcodeScannerActivity.INTENT_EXTRA_NUMBER);
                    String format = data.getExtras().getString(BarcodeScannerActivity.INTENT_EXTRA_FORMAT);
                    if (checkProperISBNFromBarcode(number, format)) {
                        //storeInteractionOnServer(number, "barcode");           //TODO: remove comment when possible
                        returnResultToActivity(number);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "canceled reading barcode ");
                } else {
                    Log.w(TAG, "failed getting barcode result");
                }
                break;
            case REQUEST_CODE_NFC_SCAN:
                if (resultCode == RESULT_OK) {
                    String number = data.getExtras().getString(ScanNFCActivity.INTENT_EXTRA_NUMBER);
                    //storeInteractionOnServer(number, "nfc");                  //TODO: remove comment when possible
                    returnResultToActivity(number);
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "canceled reading barcode");
                } else {
                    Log.w(TAG, "failed getting barcode result");
                }
                break;
            case REQUEST_CODE_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    Intent intentCropImage = new Intent(FindBookActivity.this, CropActivity.class);
                    intentCropImage.putExtra(INTENT_EXTRA_PIC_URI, picUri.toString());
                    startActivityForResult(intentCropImage, REQUEST_CODE_CROP_IMAGE);
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "canceled taking photo");
                } else {
                    Log.w(TAG, "failed taking photo");
                }
                break;
            case REQUEST_CODE_CROP_IMAGE:
                if (resultCode == RESULT_OK) {
                    switch (croppedImageNextStep) {
                        case OCR:
                            findISBNWithOCR();
                            break;
                        case REVERSE_IMAGE_SEARCH:
                            if (isOnline()) reverseImageSearch();
                            break;
                        default:
                            Log.w(TAG, "weird nextStep code captured in CROP_IMAGE: " + croppedImageNextStep);
                            break;
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG,"canceled cropping image");
                } else {
                    Log.w(TAG,"failed cropping image");
                }
                break;
            default:
                Log.w(TAG, "weird requestCode captured in onActivityResult: " + requestCode);
                break;
        }
    }

    private boolean checkProperISBNFromBarcode(String number, String format) {
        if (number == null) {
            Toast.makeText(this, ERROR_NOT_FOUND,Toast.LENGTH_LONG).show();
            Log.w(TAG, "ISBN from barcode is null");
            return false;
        }
        if (format.equals("EAN_13")) {
            return true;
        } else {
            Toast.makeText(FindBookActivity.this,"Found different barcode type: " + format,Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    /** Apply OCR on the image and extract the isbn */
    private void findISBNWithOCR() {
        TessBaseAPI baseAPI = new TessBaseAPI();
        baseAPI.init(Environment.getExternalStorageDirectory().getPath() + "/tesseract/", "eng");
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picUri);
            baseAPI.setImage(bitmap);
            baseAPI.setVariable("tessedit_char_whitelist", "0123456789-ISBN");

            extractISBNFromOCRString(baseAPI.getUTF8Text());
        } catch (IOException e) {
            Log.e(TAG, "failed getting bitmap in OCR: " + e.getMessage());
        }
    }

    /** Search for ISBN in the string returned by OCR */
    private void extractISBNFromOCRString(String s) {
        if (s == null) {
            Toast.makeText(this, ERROR_NOT_FOUND,Toast.LENGTH_LONG).show();
            Log.w(TAG, "ISBN from OCR is null");
            return;
        }
        int index = s.toLowerCase().indexOf("isbn");
        if (index == -1) {
            Toast.makeText(this, ERROR_NOT_FOUND,Toast.LENGTH_LONG).show();
            Log.w(TAG, "extractISBNFromOCRString has not found index of isbn ~ -1");
        }
        else {
            s = s.substring(index+4);
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for(int i=0;i<s.length();i++) {
                if(Character.isDigit(s.charAt(i))) {
                    sb = sb.append(s.charAt(i));
                    count++;
                }
                if (count == ISBN_LENGTH) break;
            }
            if (count != ISBN_LENGTH) {
                Log.w(TAG,"ISBN from OCR is shorter than the standard of 13 digits");
                Toast.makeText(this,"ISBN from OCR is shorter than the standard of 13 digits",Toast.LENGTH_SHORT).show();
            } else {
                s = sb.toString();
                Log.i(TAG,"ISBN from OCR: " + s);
                //storeInteraction(s, "OCR");            //TODO: remove comment
                returnResultToActivity(s);
            }
        }
    }

    private void storeInteractionOnServer(String isbn, String interaction_type) {
        /* Example JSON data sent to server:
         * {"book_interaction_records": [
         *       {
         *           "timestamp" : 1460973019,
         *           "android_id" : "Android device id",
         *           "interaction_type" : "NFC",
         *           "book_isbn" : "9789460038549",
         *           "book_genre" : "thriller"
         *       }
         *   ]}
         */
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
                response.body().close();
            } catch (IOException e) {
                Log.e(TAG,"error uploading data to server: " + e.getMessage());
                if (response != null) {
                    response.body().close();
                }
            }
            return null;
        }
    }

    private void returnResultToActivity(String number) {
        Intent intent_returnToActivity = new Intent();
        intent_returnToActivity.putExtra(INTENT_EXTRA_ISBN, number);
        setResult(Activity.RESULT_OK, intent_returnToActivity);
        finish();
    }

    private void createIntentTakePhoto(int requestCode) {
        this.croppedImageNextStep = requestCode;
        Intent intent_takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        picUri = Uri.fromFile(getOutputMediaFile());
        intent_takePhoto.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
        startActivityForResult(intent_takePhoto, requestCode);
    }

    /** Returns a file to save the picture in */
    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(FindBookActivity.this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(FindBookActivity.this,
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
                        Toast.makeText(FindBookActivity.this, "This functionality requires the camera", Toast.LENGTH_LONG).show();
                    } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        startActivityForResult(new Intent(FindBookActivity.this, BarcodeScannerActivity.class),REQUEST_CODE_BARCODE);
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

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }

    private void reverseImageSearch() {
        new UploadImgur().execute();
        progressDialog.setMessage(getResources().getString(R.string.searching));
        progressDialog.show();
    }

    /** AsyncTask to handle image upload and the process further */
    class UploadImgur extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            File imageFile = new File(picUri.getPath());
            //start a httpclient
            OkHttpClient client = new OkHttpClient();
            //create multipart body with image and title
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"title\""),
                            RequestBody.create(null, imageFile.getName()))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"image\""),
                            RequestBody.create(MediaType.parse("image/jpeg"), imageFile))
                    .build();
            //build upload/post request
            Request request = new Request.Builder()
                    .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                    .url(IMGUR_LINK)
                    .post(requestBody)
                    .build();

            Response response = null;
            String imageLink, finalURL = null;

            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                //process imgur response to obtain link
                JSONObject json = new JSONObject(response.body().string());
                json = new JSONObject(json.getString("data"));
                imageLink = json.getString("link");
                deleteHash = json.getString("deletehash");  // to handle deletion later

                finalURL = GOOGLE_IMAGE_SEARCH_PREFIX + imageLink;
            } catch (IOException | JSONException e) {
                Log.e(TAG,"Error uploading to imgur: " + e.getMessage());
                if (response != null) response.body().close();
            }
            return finalURL;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url == null) {
                Log.e(TAG,"error communicating with imgur");
                Toast.makeText(FindBookActivity.this,ERROR_REVERSE_IMAGE_SEARCH,Toast.LENGTH_SHORT).show();
                return;
            }
            //get amazon/bol link
            startFragmentSearch(SEARCH_GOOGLE,url,null);
        }
    }

    private void startFragmentSearch(int requestCode, String url, String website) {
        SearchFragment searchFragment = new SearchFragment();
        searchFragment.setListener(this);
        Bundle args = new Bundle();
        args.putInt(INTENT_EXTRA_REQUEST_CODE,requestCode);
        args.putString(INTENT_EXTRA_URL,url);
        args.putString(INTENT_EXTRA_WEBSITE,website);
        searchFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, searchFragment, FRAGMENT_TAG).commit();
    }

    @Override
    public void urlFound(int requestCode, String url) {
        switch (requestCode) {
            case SEARCH_GOOGLE:
                getSupportFragmentManager().beginTransaction().
                        remove(getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG)).commit();
                //storeInteraction(isbn, "image_search");                    //TODO: remove comment when possible
                returnResultToActivity(url);
                deleteUploadedImage();
                break;
            default:
                Log.w(TAG, "weird requestCode captured in urlFound " + requestCode);
                progressDialog.cancel();
                break;
        }
    }

    @Override
    public void error(String errorMessage) {
        progressDialog.cancel();
        Log.e(TAG, errorMessage);
        Toast.makeText(FindBookActivity.this,errorMessage,Toast.LENGTH_SHORT).show();
        getSupportFragmentManager().beginTransaction().
                remove(getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG)).commit();
    }

    @Override
    public void deleteUpload() {
        deleteUploadedImage();
    }

    /** Launch delayed async task to request image deletion on imgur */
    private void deleteUploadedImage() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new DeleteImgur().execute();
            }
        }, IMAGE_DELETE_DELAY);
    }

    /** Asynctask to request imgur to delete image */
    class DeleteImgur extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                    .url(IMGUR_LINK + deleteHash)
                    .delete()
                    .build();

            Response response = null;
            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                response.body().close();
            } catch (IOException e) {
                Log.e(TAG,"error deleting from imgur: " + e.getMessage());
                if (response != null) {
                    response.body().close();
                }
            }
            return null;
        }
    }
}
