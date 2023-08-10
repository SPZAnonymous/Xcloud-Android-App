package com.cloud.gaming;

import android.app.Activity;
import android.graphics.Bitmap;
import android.webkit.WebChromeClient;
import android.Manifest;
import android.content.pm.PackageManager;
import android.webkit.PermissionRequest;
import android.content.Context;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;


class WebChromeClientCustomPoster extends WebChromeClient {
    private int width;
    private int height;
    private Activity activity;
    private static final int REQUEST_MICROPHONE_PERMISSION = 123;
    private Bitmap defaultVideoPoster;
    
    public WebChromeClientCustomPoster(int width, int height, Activity activity) {
        this.width = width;
        this.height = height;
        this.activity = activity;
        this.defaultVideoPoster = null;
    }
    
    

    @Override
    public Bitmap getDefaultVideoPoster() {
        WebView webView = (WebView) activity.findViewById(R.id.webView);
        String url = webView.getUrl();
        
        if (url != null && url.contains("/play/launch")) {
            if (defaultVideoPoster == null) {
                defaultVideoPoster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
            return defaultVideoPoster;
        } else {
            return null;
        }
    }
    
    
    @Override
    public void onPermissionRequest(PermissionRequest request) {
     if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE_PERMISSION);
     } else {
        request.grant(request.getResources());
     }
    }
}
