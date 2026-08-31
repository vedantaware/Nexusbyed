package com.ved.nexus;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.graphics.Color;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;

public class MainActivity extends Activity {
  private static final String NEXUS_URL = "https://nexusbyved.netlify.app/";
  private WebView webView;
  private ValueCallback<Uri[]> filePathCallback;
  private static final int FILE_CHOOSER_REQUEST = 4101;

  public class DownloadBridge {
    @JavascriptInterface
    public void saveBase64File(String fileName, String mimeType, String base64) {
      try {
        String safeName = fileName == null || fileName.trim().isEmpty() ? "nexus-file" : fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        byte[] bytes = Base64.getDecoder().decode(base64);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          ContentValues values = new ContentValues();
          values.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
          values.put(MediaStore.Downloads.MIME_TYPE, mimeType == null ? "application/octet-stream" : mimeType);
          values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NEXUS");
          values.put(MediaStore.Downloads.IS_PENDING, 1);
          uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
          if (uri == null) throw new Exception("Unable to create download");
          try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new Exception("Unable to open download");
            out.write(bytes);
          }
          values.clear();
          values.put(MediaStore.Downloads.IS_PENDING, 0);
          getContentResolver().update(uri, values, null, null);
        } else {
          File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/NEXUS");
          if (!dir.exists() && !dir.mkdirs()) throw new Exception("Unable to create Downloads folder");
          File file = new File(dir, safeName);
          try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); }
          uri = Uri.fromFile(file);
        }
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Saved to Downloads/NEXUS", Toast.LENGTH_SHORT).show());
      } catch (Exception e) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Unable to save file", Toast.LENGTH_SHORT).show());
      }
    }
  }

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
    webView.addJavascriptInterface(new DownloadBridge(), "NexusDownload");
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
      if (url != null && url.startsWith("blob:")) {
        String js = "(async()=>{try{const r=await fetch(" + jsQuote(url) + ");const b=await r.blob();const fr=new FileReader();fr.onloadend=()=>{const x=fr.result.split(',')[1];NexusDownload.saveBase64File(" + jsQuote(android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)) + "," + jsQuote(mimeType) + ",x)};fr.readAsDataURL(b)}catch(e){}})();";
        webView.evaluateJavascript(js, null);
        return;
      }
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

  private String jsQuote(String value) {
    if (value == null) return "null";
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r") + "'";
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
