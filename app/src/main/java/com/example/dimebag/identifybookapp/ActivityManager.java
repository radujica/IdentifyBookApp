package com.example.dimebag.identifybookapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//TODO: add loading while getting displays from server

public class ActivityManager extends AppCompatActivity {

    private static final String TAG = "ActivityManager";
    private static final String DIALOG_TITLE = "title";
    private static final String SERVER_RESULT_TAG = "result";
    private static final String DISPLAY_CHOOSER_FRAG_TAG = "choose_display";

    static final String INTENT_EXTRA_DISPLAY_NAME = "displayName";
    static final String SERVER_DISPLAY_DB_URL = "http://server.nl/display_record/records.json";

    private DisplayRecordDBHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        db = new DisplayRecordDBHandler(this);

        getDisplaysFromServer();

        setSwitchToUserButton();
        setCreateDisplayButton();
        setViewDisplaysButton();
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
        db = null;
    }

    private void setSwitchToUserButton() {
        Button buttonUserMode = (Button) findViewById(R.id.manager_button_switch_to_user);
        assert buttonUserMode != null;
        buttonUserMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setCreateDisplayButton() {
        Button buttonCreateDisplay = (Button) findViewById(R.id.manager_button_create_display);
        assert buttonCreateDisplay != null;
        buttonCreateDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_createDisplay = new Intent(ActivityManager.this, ActivityDisplay.class);
                startActivity(intent_createDisplay);
            }
        });
    }

    private void setViewDisplaysButton() {
        Button button = (Button) findViewById(R.id.manager_button_view_displays);
        assert button != null;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (db.getDisplayRecordCount() != 0) {
                    FragmentManager fm = getSupportFragmentManager();
                    PickDisplayDialogFragment alertDialog = PickDisplayDialogFragment.newInstance("Displays",db.getDisplayNames());
                    alertDialog.show(fm, DISPLAY_CHOOSER_FRAG_TAG);
                } else {
                    Toast.makeText(ActivityManager.this,"No displays available",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void getDisplaysFromServer() {
        new GetDisplays().execute();
    }

    class GetDisplays extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .header("Authorization", ActivityFindBook.SERVER_CREDENTIALS)
                    .url(SERVER_DISPLAY_DB_URL)
                    .get()
                    .build();

            Response response = null;
            String resp = null;
            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                resp = response.body().string();
            } catch (IOException e) {
                Log.e(TAG,"error requesting data from server: " + e.getMessage());
                if (response != null) response.body().close();
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                processDisplaysData(s,db);
            } else {
                Log.w(TAG,"error processing data from server: response is null");
            }
        }
    }

    private void processDisplaysData(String stringDisplays, DisplayRecordDBHandler db) {
        try {
            JSONObject json = new JSONObject(stringDisplays);
            JSONArray jsonArray = new JSONArray(json.getString(SERVER_RESULT_TAG));
            int numberOfDisplays = jsonArray.length();
            for (int i=0;i<numberOfDisplays;i++) {
                db.addDisplayRecord(createDisplayRecord(jsonArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG,"error reading json: " + e.getMessage());
        }
    }

    private DisplayRecord createDisplayRecord(JSONObject data) throws JSONException {
        if (data.length() != 5) throw new JSONException("wrong json data length");
        if (!data.has("id") || !data.has(DisplayRecordDBHandler.COLUMN_NAME_DISPLAY_NAME) ||
                !data.has(DisplayRecordDBHandler.COLUMN_NAME_BOOK_ISBN) ||
                !data.has(DisplayRecordDBHandler.COLUMN_NAME_TIMESTAMP) ||
                !data.has(DisplayRecordDBHandler.COLUMN_NAME_ANDROID_ID)) {
            throw new JSONException("json does not contain all the information");
        }

        return new DisplayRecord(data.getInt("id"),
                data.getString(DisplayRecordDBHandler.COLUMN_NAME_DISPLAY_NAME),
                data.getString(DisplayRecordDBHandler.COLUMN_NAME_BOOK_ISBN),
                data.getString(DisplayRecordDBHandler.COLUMN_NAME_TIMESTAMP),
                data.getString(DisplayRecordDBHandler.COLUMN_NAME_ANDROID_ID));
    }

    /** Dialog class to allow choosing a display to view */
    public static class PickDisplayDialogFragment extends DialogFragment {

        private static final String BUNDLE_ARGUMENT_LIST = "displayNames";

        public PickDisplayDialogFragment() {
            // Empty constructor required for DialogFragment
        }

        /** Create a new instance of the DialogFragment */
        public static PickDisplayDialogFragment newInstance(String title, List<String> displayNames) {
            PickDisplayDialogFragment frag = new PickDisplayDialogFragment();
            Bundle args = new Bundle();
            args.putString(DIALOG_TITLE, title);
            args.putStringArrayList(BUNDLE_ARGUMENT_LIST, new ArrayList<>(displayNames));
            frag.setArguments(args);
            return frag;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString(DIALOG_TITLE);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(title);
            List<String> displayNamesList = getArguments().getStringArrayList(BUNDLE_ARGUMENT_LIST);
            assert displayNamesList != null;
            final String[] displayNames = (String[]) displayNamesList.toArray(); //createDisplayNamesArray(displayNamesList);
            alertDialogBuilder.setItems(displayNames, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    createIntentViewDisplay(displayNames, which);
                }
            });
            return alertDialogBuilder.create();
        }

        /*
        private String[] createDisplayNamesArray(List<String> displayNamesList) {
            final String[] displayNames = new String[displayNamesList.size()];
            for (int i=0;i<displayNamesList.size();i++) {
                displayNames[i] = displayNamesList.get(i);
            }
            return displayNames;
        } */

        /** create the intent and start the activity to view display @param which */
        private void createIntentViewDisplay(String[] displayNames, int which) {
            Intent intent_viewDisplay = new Intent(getContext(), ActivityDisplay.class);
            intent_viewDisplay.putExtra(INTENT_EXTRA_DISPLAY_NAME, displayNames[which]);
            startActivity(intent_viewDisplay);
        }
    }

}
