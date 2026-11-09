package com.patel.chashmghar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ComponentActivity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private PermissionRequest pendingWebPermission;
    private static final int FILE_CHOOSER_REQUEST = 2001;
    private static final int MEDIA_PERMISSION_REQUEST = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String url = r.getUrl().toString();
                if (url.startsWith("https://appassets.androidplatform.net/")) return false;
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:") || url.startsWith("https://") || url.startsWith("http://")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                    catch (Exception e) { Toast.makeText(MainActivity.this, "App nahi mila", Toast.LENGTH_SHORT).show(); }
                    return true;
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try { startActivityForResult(p.createIntent(), FILE_CHOOSER_REQUEST); return true; }
                catch (Exception e) { filePathCallback = null; return false; }
            }
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    List<String> need = new ArrayList<>();
                    for (String res : request.getResources()) {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.CAMERA);
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECORD_AUDIO);
                    }
                    if (need.isEmpty()) request.grant(request.getResources());
                    else { pendingWebPermission = request; ActivityCompat.requestPermissions(MainActivity.this, need.toArray(new String[0]), MEDIA_PERMISSION_REQUEST); }
                });
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack(); else finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        super.onActivityResult(rc, res, data);
        if (rc == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            filePathCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] grants) {
        super.onRequestPermissionsResult(rc, perms, grants);
        if (rc == MEDIA_PERMISSION_REQUEST && pendingWebPermission != null) {
            boolean ok = grants.length > 0;
            for (int g : grants) ok &= g == PackageManager.PERMISSION_GRANTED;
            if (ok) pendingWebPermission.grant(pendingWebPermission.getResources()); else pendingWebPermission.deny();
            pendingWebPermission = null;
        }
    }

    @Override
    protected void onDestroy() { if (webView != null) { webView.destroy(); webView = null; } super.onDestroy(); }
}
