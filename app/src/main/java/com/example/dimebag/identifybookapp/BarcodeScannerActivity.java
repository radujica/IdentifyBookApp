package com.example.dimebag.identifybookapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class BarcodeScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private final static String TAG = "BarcodeScannerActivity";

    static final String INTENT_EXTRA_NUMBER = "number";
    static final String INTENT_EXTRA_FORMAT = "format";

    private ZXingScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScannerView = null;
    }

    @Override
    public void handleResult(Result result) {
        Log.i(TAG, result.getText());                        // Prints scan results
        Log.i(TAG, result.getBarcodeFormat().toString());    // Prints the scan format (qrcode, pdf417 etc.)
        returnResultToFindBook(result.getText(),result.getBarcodeFormat().toString());
    }

    /** Create intent that returns the barcode */
    private void returnResultToFindBook(String number, String format) {
        Intent intent_returnBarcode = new Intent();
        intent_returnBarcode.putExtra(INTENT_EXTRA_NUMBER, number);
        intent_returnBarcode.putExtra(INTENT_EXTRA_FORMAT, format);
        setResult(Activity.RESULT_OK, intent_returnBarcode);
        finish();
    }
}
