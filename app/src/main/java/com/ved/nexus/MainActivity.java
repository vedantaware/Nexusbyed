package com.ved.nexus;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.graphics.Color;
import android.widget.Toast;

public class MainActivity extends Activity {
  private static final String NEXUS_URL = "https://nexusbyved.netlify.app/";
  private WebView webView;
  private ValueCallback<Uri[]> filePathCallback;
  private static final int FILE_CHOOSER_REQUEST = 4101;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    webView = new WebView(this);
    webView.setWebViewClient(new WebViewClient());
    WebSettings s = webView.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    s.setSupportZoom(false);
    s.setBuiltInZoomControls(false);
    s.setDisplayZoomControls(false);
    s.setLoadWithOverviewMode(false);
    s.setUseWideViewPort(false);

    webView.setBackgroundColor(Color.WHITE);
    webView.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (filePathCallback != null) filePathCallback.onReceiveValue(null);
        filePathCallback = callback;
        Intent intent = params.createIntent();
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
          startActivityForResult(intent, FILE_CHOOSER_REQUEST);
        } catch (ActivityNotFoundException e) {
          filePathCallback = null;
          Toast.makeText(MainActivity.this, "No file picker available", Toast.LENGTH_SHORT).show();
          return false;
        }
        return true;
      }
    });

    webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
      try {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType(mimeType);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("Downloading from NEXUS");
        request.setTitle(android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
            android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType));
        ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception ignored) { Toast.makeText(this, "Unable to download file", Toast.LENGTH_SHORT).show(); }
      }
    });

    setContentView(webView);
    webView.loadUrl(NEXUS_URL);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;
    Uri[] results = null;
    if (resultCode == RESULT_OK && data != null) {
      if (data.getClipData() != null) {
        int count = data.getClipData().getItemCount();
        results = new Uri[count];
        for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
      } else if (data.getData() != null) {
        results = new Uri[]{data.getData()};
      }
    }
    filePathCallback.onReceiveValue(results);
    filePathCallback = null;
  }

  @Override public void onBackPressed() {
    if (webView != null && webView.canGoBack()) webView.goBack();
    else super.onBackPressed();
  }
}
