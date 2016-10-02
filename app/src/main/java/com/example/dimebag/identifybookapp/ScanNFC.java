package com.example.dimebag.identifybookapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

//TODO: automatically start scanning; display result

public class ScanNFC extends Activity {

    private static final String TAG = "ScanNFC";
    static final String INTENT_EXTRA_NUMBER = "number";

    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;
    private IntentFilter intentFilter[];
    private String techLists[][];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_nfc);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(checkNFCAvailability()) {
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, intentFilter, techLists);
        } else {
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nfcAdapter = null;
        nfcPendingIntent = null;
        intentFilter = null;
        techLists = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            //get tag data and id
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] id = tag.getId();

            NfcV nfcvTag = NfcV.get(tag);
            try {
                nfcvTag.connect();

                byte[] cmd = createNFCCommand(id);
                String response = byteArrayToHex(nfcvTag.transceive(cmd));

                //note that the rfid contains much more data, but this number is written on the
                //tag and is used to uniquely identify a book in the DB
                String number = response.substring(4, 12) + response.substring(14, 20);

                //create intent and return
                Intent intent_returnNumberToFindBook = new Intent();
                intent_returnNumberToFindBook.putExtra(INTENT_EXTRA_NUMBER, number);
                setResult(Activity.RESULT_OK, intent_returnNumberToFindBook);

                nfcvTag.close();
            } catch (IOException e) {
                Log.e(TAG,"something went wrong when scanning the nfc tag: " + e.getMessage());
                Toast.makeText(this,"Error reading the tag. Please try again or use another option.",Toast.LENGTH_LONG).show();
            } finally {
                finish();
            }
        }
    }

    /** Create the command that needs to be sent to the rfid tag */
    private byte[] createNFCCommand(byte[] id) {
        int offset = 0;  // offset of first block to read
        int blocks = 2;  // number of blocks to read
        byte[] cmd = new byte[]{
                (byte)0x60,                  // flags: addressed (= UID field present)
                (byte)0x23,                  // command: READ MULTIPLE BLOCKS
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  // placeholder for tag UID
                (byte)(offset & 0x0ff),      // first block number
                (byte)((blocks - 1) & 0x0ff) // number of blocks (-1 as 0x00 means one block)
        };
        //copy id into command
        System.arraycopy(id, 0, cmd, 2, 8);
        return cmd;
    }

    /** Convert byte[] to hex. Slow implementation */
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    /** Check whether NFC is available and act accordingly */
    private boolean checkNFCAvailability() {
        PackageManager pm = getPackageManager();

        //not available
        if(!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Toast.makeText(this, "NFC not supported!", Toast.LENGTH_SHORT).show();
            return false;
        }
        else {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);

            //available but disabled
            if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                Toast.makeText(this, "Please enable NFC and press Back to return to the app!", Toast.LENGTH_SHORT).show();
                return false;
            }

            //available and enabled
            else {
                nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
                intentFilter = new IntentFilter[] {ndef,};
                techLists = new String[][] { new String[] { android.nfc.tech.NfcV.class.getName() } };
            }
        }
        return true;
    }
}
