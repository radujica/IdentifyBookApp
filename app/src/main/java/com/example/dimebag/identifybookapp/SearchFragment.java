package com.example.dimebag.identifybookapp;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SearchFragment extends Fragment {

    private static final String TAG = "Search";
    private static final String AMAZON = "amazon";
    private static final String BOL = "bol.com";
    private static final String ERROR_MESSAGE = "Error. Please try again.";

    private int requestCode;
    private String website;
    private Listener mListener;
    private WebView webView;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_fragment, container, false);
        //set up the webview and the client to extract the html
        webView = (WebView) view.findViewById(R.id.fragment_webView);
        assert webView != null;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");

        //to enable extracting the html
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webView.loadUrl("javascript:window.HTMLOUT.showHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
            }
        });

        Bundle extras = getArguments();
        if (extras != null) {
            requestCode = extras.getInt(FindBookActivity.INTENT_EXTRA_REQUEST_CODE);
            webView.loadUrl(extras.getString(FindBookActivity.INTENT_EXTRA_URL));
        } else {
            Log.w(TAG,"extras is null");
            Toast.makeText(getContext(), ERROR_MESSAGE, Toast.LENGTH_SHORT).show();
        }

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /** Javascript interface to process the html data extracted from the webview */
    class MyJavaScriptInterface
    {
        @JavascriptInterface
        public void showHTML(String html) {
            switch (requestCode) {
                case FindBookActivity.SEARCH_GOOGLE:
                    findLinkOnGoogle(html);
                    break;
                case FindBookActivity.SEARCH_AMAZON:
                    switch (website) {
                        case AMAZON:
                            findISBNOnAmazon(html);
                            break;
                        case BOL:
                            findISBNOnBol(html);
                            break;
                        default:
                            mListener.error("Wrong website in switch method");
                            break;
                    }
                    break;
                default:
                    mListener.error("Wrong requestCode in SearchFragment");
                    break;
            }
        }
    }

    /** Return whether a link has been found on google for the book. If yes,
     * create intent and return to FindBook */
    private void findLinkOnGoogle(String html) {
        Document doc = Jsoup.parse(html);
        Elements links = doc.select("a[href]");
        String result;

        for (Element e : links) {
            result = e.attr("href");
            if (result.contains(AMAZON)) {
                Log.i(TAG, result);         //now result contains the best amazon guess
                reloadUrl(result,AMAZON);
                return;
            }
            else if (result.contains(BOL)) {
                Log.i(TAG, result);         //now result contains the best bol guess
                reloadUrl(result,BOL);
                return;
            }
        }
        mListener.error("No suitable link found on Google");
        mListener.deleteUpload();
    }

    /* Reloads webview url, i.e. amazon or bol, for the second step of the search */
    private void reloadUrl(final String url, String website) {
        requestCode = FindBookActivity.SEARCH_AMAZON;
        this.website = website;
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

    /** Return whether an ISBN has been found on Amazon. If yes, create intent
     * and return to FindBook */
    private void findISBNOnAmazon(String html) {
        Document doc = Jsoup.parse(html);
        Elements links = doc.select("tr:contains(ISBN-13)");    //div#detail_bullets_id for desktop
        String result;

        for (Element e : links) {
            result = e.text();
            if (result.contains("ISBN-13")) {
                result = result.substring(7);
                Log.i(TAG, "amazon: " + result);
                mListener.urlFound(FindBookActivity.SEARCH_GOOGLE,result);
                return;
            }
        }
        mListener.error("No suitable ISBN found on amazon");
    }

    /** Return whether an ISBN has been found on Bol. If yes, create intent
     * and return to FindBookActivity */
    private void findISBNOnBol(String html) {
        Document doc = Jsoup.parse(html);
        Elements links = doc.select("tr:contains(ISBN13)");
        String result;

        for (Element e : links) {
            result = e.text();
            if (result.contains("ISBN13")) {
                result = result.substring(9);
                Log.i(TAG, "bol: " + result);
                mListener.urlFound(FindBookActivity.SEARCH_GOOGLE,result);
                return;
            }
        }
        mListener.error("No suitable link found on bol.com");
    }

    /** Interface used to communicate with FindBookActivity */
    interface Listener {
        void urlFound(int requestCode, String url);
        void error(String errorMessage);
        void deleteUpload();
    }

    /** Method to set the listener allowing communication with FindBookActivity */
    public void setListener(Listener listener) {
        mListener = listener;
    }

}
