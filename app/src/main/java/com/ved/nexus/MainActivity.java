package com.ved.nexus;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.view.View;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;

public class MainActivity extends Activity {
  private static final String NEXUS_URL = "https://nexusbyved.netlify.app/";
  private WebView webView;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    webView = new WebView(this);
    webView.setWebViewClient(new WebViewClient());
    WebSettings s = webView.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setSupportZoom(false);
    s.setBuiltInZoomControls(false);
    s.setDisplayZoomControls(false);
    s.setLoadWithOverviewMode(false);
    s.setUseWideViewPort(false);

    webView.setBackgroundColor(Color.WHITE);
    webView.setWebChromeClient(new android.webkit.WebChromeClient());
    setContentView(webView);
    webView.loadUrl(NEXUS_URL);
  }

  @Override public void onBackPressed() {
    if (webView != null && webView.canGoBack()) {
      webView.goBack();
    } else {
      super.onBackPressed();
    }
  }
}
