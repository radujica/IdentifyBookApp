package com.example.dimebag.identifybookapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//TODO: switch from arrayAdapter

public class ActivityDisplay extends AppCompatActivity {

    private static final String TAG = "DisplayInfo";
    private static final String SERVER_RESULT_TAG = "result";
    private static final int REQUEST_CODE_FIND_BOOK = 0;

    private String displayName;
    private DisplayRecordDBHandler db;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        db = new DisplayRecordDBHandler(this);

        getIntentData();
        setListView();
        setEditText();
        setFinishDisplayButton();
        setFAB();
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
        displayName = null;
        adapter = null;
        db = null;
    }

    /** Get the extras from the intent (if any) and store */
    private void getIntentData() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            displayName = "";
        } else {
            displayName = extras.getString(ActivityManager.INTENT_EXTRA_DISPLAY_NAME);
        }
    }

    private void setListView() {
        adapter = new ArrayAdapter<>(this, R.layout.activity_display_info_list_view, db.getBookISBNs(displayName));

        ListView listView = (ListView) findViewById(R.id.listView_books);
        assert listView != null;
        listView.setAdapter(adapter);
    }

    private void setEditText() {
        final EditText editText = (EditText) findViewById(R.id.editText_displayName);
        assert editText != null;
        editText.setText(displayName);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    displayName = editText.getText().toString();
                    //hide the keyboard
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    editText.setCursorVisible(false);
                    handled = true;
                }
                return handled;
            }
        });
    }

    private void setFinishDisplayButton() {
        Button finishDisplay = (Button) findViewById(R.id.button_finishDisplay);
        assert finishDisplay != null;
        finishDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setFAB() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent_addBook = new Intent(ActivityDisplay.this,ActivityFindBook.class);
                startActivityForResult(intent_addBook, REQUEST_CODE_FIND_BOOK);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FIND_BOOK) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                String isbn = extras.getString(ActivityFindBook.INTENT_EXTRA_ISBN);
                new UploadDisplay().execute(isbn);
            } else if (resultCode == RESULT_CANCELED) {
                Log.i(TAG,"canceled getting book isbn for display use");
            } else {
                Log.w(TAG,"failed getting book isbn for display use with resultCode: " + resultCode);
            }
        }
    }

    class UploadDisplay extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            OkHttpClient client = new OkHttpClient();

            Long timestamp = System.currentTimeMillis()/1000;
            JSONObject json = new JSONObject();
            try {
                JSONObject entry = new JSONObject();

                entry.put(DisplayRecordDBHandler.COLUMN_NAME_DISPLAY_NAME, displayName);
                entry.put(DisplayRecordDBHandler.COLUMN_NAME_BOOK_ISBN, params[0]);
                entry.put(DisplayRecordDBHandler.COLUMN_NAME_TIMESTAMP, timestamp.toString());
                entry.put(DisplayRecordDBHandler.COLUMN_NAME_ANDROID_ID, Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

                json.put("display",entry);

                Log.i(TAG,"displayInfo to server:  " + json.toString());
            } catch (JSONException e) {
                Log.e(TAG,"Error creating JSONObject to UploadDisplay: " + e.getMessage());
            }

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),json.toString());

            Request request = new Request.Builder()
                    .header("Authorization", ActivityFindBook.SERVER_CREDENTIALS)
                    .url(ActivityManager.SERVER_DISPLAY_DB_URL)
                    .post(requestBody)
                    .build();

            Response response = null;
            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                String resp = response.body().string();
                response.body().close();
                return resp;
            } catch (IOException e) {
                Log.e(TAG,"error uploading displayInfo to server: " + e.getMessage());
                if (response != null) response.body().close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                DisplayRecord record = createDisplayRecord(s);
                if (record != null) {
                    db.addDisplayRecord(record);
                    adapter.add(record.getBookISBN());
                }
            }
        }

        private DisplayRecord createDisplayRecord(String s) {
            DisplayRecord record = new DisplayRecord();
            try {
                JSONObject json = new JSONObject(s);
                JSONObject result = new JSONObject(json.getString(SERVER_RESULT_TAG));
                if (result.length() != 5) throw new JSONException("wrong json data length");
                if (!result.has("id") || !result.has(DisplayRecordDBHandler.COLUMN_NAME_DISPLAY_NAME) ||
                        !result.has(DisplayRecordDBHandler.COLUMN_NAME_BOOK_ISBN) ||
                        !result.has(DisplayRecordDBHandler.COLUMN_NAME_TIMESTAMP) ||
                        !result.has(DisplayRecordDBHandler.COLUMN_NAME_ANDROID_ID)) {
                    throw new JSONException("json does not contain all the information");
                }
                record.setId(result.getInt("id"));
                record.setDisplayName(result.getString(DisplayRecordDBHandler.COLUMN_NAME_DISPLAY_NAME));
                record.setBookISBN(result.getString(DisplayRecordDBHandler.COLUMN_NAME_BOOK_ISBN));
                record.setTimestamp(result.getString(DisplayRecordDBHandler.COLUMN_NAME_TIMESTAMP));
                record.setAndroidId(result.getString(DisplayRecordDBHandler.COLUMN_NAME_ANDROID_ID));
            } catch (JSONException e) {
                Log.e(TAG,"Server success response after uploading record is wrong: " + e.getMessage());
            }
            return record;
        }
    }
}
