package com.ved.nexus;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;

public class MainActivity extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    WebView w = new WebView(this);
    w.setWebViewClient(new WebViewClient());
    WebSettings s = w.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setSupportZoom(false);
    s.setBuiltInZoomControls(false);
    s.setDisplayZoomControls(false);
    w.loadUrl("https://nexusbyed.netlify.app/");
    setContentView(w);
  }
  @Override public void onBackPressed() {
    WebView w = (WebView) findViewById(android.R.id.content);
    super.onBackPressed();
  }
}
