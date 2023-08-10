package com.cloud.gaming;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView capaImageView;
    private WebView webView;
    private boolean doubleBackToExitPressedOnce = false;
    private final Handler handler = new Handler();
    private boolean shouldInjectScript = true;
    private long lastBackPressedTime = 0;
    private boolean waitingForSecondBackPress = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUi();
        setContentView(R.layout.activity_main);
        initViews();
        setupSystemUiVisibilityListener();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }
    
    @Override
    public void onBackPressed() {
     if (webView != null && webView.getUrl().contains("/play/launch")) {
        long currentTime = System.currentTimeMillis();
        if (waitingForSecondBackPress && currentTime - lastBackPressedTime <= 2000) {
            webView.reload();
            waitingForSecondBackPress = false;
        } else {
            waitingForSecondBackPress = true;
            lastBackPressedTime = currentTime; 
            showToast("Aperte novamente para recarregar o jogo");
            handler.postDelayed(() -> {
                waitingForSecondBackPress = false;
            }, 2000);
        }
     } else {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            this.doubleBackToExitPressedOnce = true;
            showToast("Aperte novamente para sair");
            handler.postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        }
     }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            capaImageView.setImageResource(R.drawable.capa_retrato);
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            capaImageView.setImageResource(R.drawable.capa_paisagem);
        }
    }

    private void initViews() {
        capaImageView = findViewById(R.id.capaImageView);
        webView = findViewById(R.id.webView);
        capaImageView.setBackgroundColor(Color.TRANSPARENT);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            capaImageView.setImageResource(R.drawable.capa_retrato);
        } else {
            capaImageView.setImageResource(R.drawable.capa_paisagem);
        }
        webView.setVisibility(View.INVISIBLE);
        webView.setWebChromeClient(new WebChromeClientCustomPoster(20, 20, this));
        capaImageView.setVisibility(View.VISIBLE);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setWebViewClient(new WebViewClient() {
           @Override
           public void onPageFinished(WebView view, String url) {
               super.onPageFinished(view, url);
               
               handler.postDelayed(new Runnable() {
                @Override
                  public void run() {
                     capaImageView.setVisibility(View.GONE);
                     webView.setVisibility(View.VISIBLE);
                  }
               }, 8000);
               if (url.contains("/play/launch")) {
                   injectJavaScript(getUpdateButtonScript());
                   injectJavaScript(getUpdateButtonScript2());
               } else {
                   injectJavaScript(removefooter());
               }
                    
               hideSystemUi();
           }
    
           @Override
           public boolean shouldOverrideUrlLoading(WebView view, String url) {
               if (url.startsWith("https://www.xbox.com/") && url.contains("/play")) {
                   view.loadUrl(url);
               } else if (url.startsWith("https://login.live.com")) {
                   view.loadUrl(url);
               } else if (url.startsWith("https://login.microsoftonline.com")){
                   view.loadUrl(url);
               } else if (url.startsWith("https://www.xbox.com/") && url.contains("/auth")) {
                   view.loadUrl(url);
               } else {
                   Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                   startActivity(intent);
               }
               return true;
           }       
                
           @Override
           public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
               if (url.contains("/play/launch")) {
                   injectJavaScript(getUpdateButtonScript());
                   injectJavaScript(getUpdateButtonScript2());
                   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
               } else {
                   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                   injectJavaScript(removefooter());        
               }
               super.doUpdateVisitedHistory(view, url, isReload);
           }
        });
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setAllowContentAccess(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setEnableSmoothTransition(true);
        webSettings.setLoadsImagesAutomatically(true);
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 Edg/114.0.1823.90";
        webSettings.setUserAgentString(userAgent);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setForceDark(WebSettings.FORCE_DARK_OFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        } 
        webView.loadUrl("https://www.xbox.com/play");
    }
    
    private void injectJavaScript(String jsCode) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                }
            });
        } else {
            webView.loadUrl("javascript:" + jsCode);
        }
    }
    
    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY 
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION 
            | View.SYSTEM_UI_FLAG_FULLSCREEN 
            | View.SYSTEM_UI_FLAG_IMMERSIVE
            );
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setupSystemUiVisibilityListener() {
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                hideSystemUi();
            }
        });
    }
    
    private String getUpdateButtonScript() {
     return "function esconder() {\n" +
            "  if (\n" +
            "    document.querySelector(\"#StreamHud\").offsetLeft >= -200 &&\n" +
            "    document.querySelector(\"#StreamHud\").offsetLeft <= -120\n" +
            "  ) {\n" +
            "    document.querySelector(\"#StreamHud > button\").style.cssText =\n" +
            "      \"opacity: 0; pointer-events: none; transition: opacity 0.5s;\";\n" +
            "    document.querySelector(\"#StreamHud > div\").style.cssText =\n" +
            "      \"opacity: 0; pointer-events: none; transition: opacity 0.5s;\";\n" +
            "  }\n" +
            "}\n" +
            "function aparecer() {\n" +
            "  document.querySelector(\"#StreamHud > button\").style.cssText =\n" +
            "    \"opacity: 1; pointer-events: auto; transition: opacity 0.5s;\";\n" +
            "  document.querySelector(\"#StreamHud > div\").style.cssText =\n" +
            "    \"opacity: 1; pointer-events: auto; transition: opacity 0.5s;\";\n" +
            "  setTimeout(esconder, 3000);\n" +
            "}\n" +
            "document.addEventListener(\"click\", function () {\n" +
            "  aparecer();\n" +
            "});\n" +
            "esconder();";
    }
    
    private String removefooter() {
        return " var element = document.querySelector(\"#uhf-footer\");\n" +
               " if (element) {\n" +
               "    element.parentNode.removeChild(element);\n" +
               " }";
    }

    private String getUpdateButtonScript2() {
     return "var script = document.createElement('script');\n" +
            "script.src = 'https://code.jquery.com/jquery-3.6.0.min.js';\n" +
            "document.head.appendChild(script);\n" +
            "function botoes() {\n" +
            "    localStorage.setItem('videoButtonState', 'false');   \n" +
            "    localStorage.setItem('videoButtonState2', 'false');   \n" +
            "    const $1screen = document.querySelector('#PageContent section[class*=PureScreens]');\n" +
            "    if (!$1screen) {\n" +
            "        return;\n" +
            "    }\n" +
            "    if ($1screen.xObserving) {\n" +
            "        return;\n" +
            "    }\n" +
            "    $1screen.xObserving = true;\n" +
            "    const $1parent = $1screen.parentElement;\n" +
            "    const sharpnessValue = 1.5; \n" +
            "    const videoSelector = 'video';\n" +
            "    function addSharpnessFilter(sharpnessValue) {\n" +
            "        var svgFilter = '<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"display:none;\">' +\n" +
            "            '<filter id=\"sharpnessFilter\">' +\n" +
            "            '<feConvolveMatrix in=\"SourceGraphic\" order=\"3\" kernelMatrix=\"0  -1  0  -1  ' + (4 + sharpnessValue) + ' -1  0  -1  0\"/>' +\n" +
            "            '</filter>' +\n" +
            "            '</svg>';\n" +
            "        if ($('#sharpnessFilter').length === 0) {\n" +
            "            $('body').append(svgFilter);\n" +
            "        }\n" +
            "    }\n" +
            "    function applySharpnessToVideo(videoElement, sharpnessValue) {\n" +
            "        addSharpnessFilter(sharpnessValue);\n" +
            "        if (videoElement.style.filter === '') {\n" +
            "            videoElement.style.filter = 'url(#sharpnessFilter)';\n" +
            "        } else {\n" +
            "            videoElement.style.filter = '';\n" +
            "        }\n" +
            "    }\n" +
            "    const observer1 = new MutationObserver(mutationList1 => {\n" +
            "        mutationList1.forEach(item => {\n" +
            "            if (item.type !== 'childList') {\n" +
            "                return;\n" +
            "            }\n" +
            "            item.addedNodes.forEach(node => {\n" +
            "                if (!node.className || !node.className.startsWith('StreamMenu')) {\n" +
            "                    return;\n" +
            "                }\n" +
            "                const $1orgButton = node.querySelector('div > div > button');\n" +
            "                if (!$1orgButton) {\n" +
            "                    return;\n" +
            "                }\n" +
            "                const $1button = $1orgButton.cloneNode(true);\n" +
            "                $1button.setAttribute('aria-label', 'Video fullscreen');\n" +
            "                $1button.querySelector('div[class*=label]').textContent = 'Abrir Tela Cheia';\n" +
            "                $1button.querySelector(\"button:nth-child(1) > svg > path\").setAttribute('d', 'M1408 0h640v640h-128V219L218 1920h422v128H0v-640h128v422L1829 128h-421V0z');\n" +
            "                const $button = $1orgButton.cloneNode(true);\n" +
            "                $button.setAttribute('aria-label', 'Video settings');\n" +
            "                $button.querySelector('div[class*=label]').textContent = 'Habilitar o Clarity Boost';\n" +
            "                $button.querySelector(\"button:nth-child(1) > svg\").setAttribute('viewBox', '0 0 28 28');\n" +
            "                $button.querySelector(\"button:nth-child(1) > svg > path\").setAttribute('d', 'M25.0000005,3 C26.6569005,3 28.0000005,4.34315 28.0000005,6 L28.0000005,22 C28.0000005,23.6569 26.6569005,25 25.0000005,25 L7.00000048,25 C5.34310048,25 4.00000048,23.6569 4.00000048,22 L4.00000048,13 L4.02300048,13 C4.52730048,12.9964 5.01650048,12.8397 5.42720048,12.5536 C5.56450048,12.4581 5.69130048,12.3492 5.80590048,12.2287 L5.81640048,12.2175 C5.88200048,12.1475 5.94320048,12.0739 6.00000048,11.9973 L6.00000048,22 C6.00000048,22.5523 6.44770048,23 7.00000048,23 L25.0000005,23 C25.5523005,23 26.0000005,22.5523 26.0000005,22 L26.0000005,6 C26.0000005,5.44771 25.5523005,5 25.0000005,5 L8.98180048,5 C8.79600048,4.86398 8.59110048,4.75374 8.37250048,4.67337 L8.29170048,4.64366 L8.22950048,4.62671 L7.05750048,4.24605 C6.98670048,4.22216 6.92200048,4.18317 6.86790048,4.13188 C6.83400048,4.09524 6.80570048,4.05389 6.78380048,4.0091 L6.76240048,3.96533 L6.73880048,3.92265 C6.72940048,3.90559 6.72090048,3.88812 6.71320048,3.8703 L6.44680048,3.05089 C6.62610048,3.01747 6.81100048,3 7.00000048,3 L25.0000005,3 Z M13.8319005,10.0296 L13.9411005,10.0786 L19.6404005,13.3353 C19.7662005,13.4073 19.8683005,13.5238 19.9312005,13.6677 C20.0740005,13.994 19.9869005,14.383 19.7403005,14.5943 L19.6404005,14.6648 L13.9411005,17.9215 C13.8509005,17.9731 13.7512005,18.0000066 13.6503005,18.0000066 C13.3310005,18.0000066 13.0656005,17.7371 13.0105005,17.3904 L13.0000005,17.2568 L13.0000005,10.7434 C13.0000005,10.628 13.0235005,10.5142 13.0687005,10.411 C13.2115005,10.0847 13.5360005,9.93082 13.8319005,10.0296 Z M3.97600048,2.99999953 C4.07600048,2.99987 4.17350048,3.03104 4.25490048,3.08911 C4.31350048,3.13709 4.36010048,3.19812 4.39090048,3.26732 C4.39560048,3.27681 4.40300048,3.28459 4.41040048,3.29235 C4.41910048,3.30143 4.42770048,3.31045 4.43200048,3.32214 L4.82970048,4.54563 C4.87390048,4.66353 4.92660048,4.77808 4.98740048,4.88835 C5.10450048,5.12748 5.25880048,5.34649 5.44460048,5.53722 C5.72200048,5.8145 6.06060048,6.02302 6.43310048,6.14609 L7.65710048,6.54364 L7.68230048,6.5505 C7.77460048,6.58446 7.85440048,6.64585 7.91080048,6.72643 C7.96900048,6.80818 8.00010048,6.90602 8.00000024,7.00632 C8.00010048,7.10625 7.96900048,7.20371 7.91080048,7.28505 C7.85420048,7.36565 7.77450048,7.42734 7.68230048,7.46211 L6.45710048,7.85967 C6.08480048,7.98251 5.74660048,8.19109 5.46970048,8.46855 C5.19210048,8.74546 4.98310048,9.08348 4.85940048,9.45556 L4.46060048,10.679 L4.44910048,10.7087 C4.42670048,10.7609 4.39540048,10.8089 4.35660048,10.8504 C4.33470048,10.8734 4.31060048,10.8941 4.28460048,10.9121 C4.20380048,10.9686 4.10770048,10.9993 4.00910048,10.9999995 C3.90920048,11.0002 3.81160048,10.969 3.73030048,10.911 C3.64900048,10.8549 3.58720048,10.7751 3.55310048,10.6824 L3.15540048,9.45899 C3.06530048,9.18679 2.92970048,8.93185 2.75430048,8.70502 C2.68990048,8.62198 2.62040048,8.54301 2.54630048,8.46855 C2.46810048,8.39847 2.38560048,8.33322 2.29940048,8.2732 L2.29030048,8.26064 C2.06800048,8.0807 1.81680048,7.93971 1.54740048,7.84367 L0.32340048,7.44612 C0.22910048,7.41299 0.14730048,7.35154 0.0891004797,7.2702 C0.0310004797,7.18845 -9.95203489e-05,7.09062 2.39249232e-07,6.99032 C-9.95203489e-05,6.89038 0.0311004797,6.79291 0.0891004797,6.71158 C0.14750048,6.63022 0.22920048,6.56848 0.32340048,6.53451 L1.54740048,6.13696 C1.85990048,6.02516 2.14700048,5.85236 2.39200048,5.6286 C2.41030048,5.61232 2.42980048,5.59794 2.44930048,5.58365 C2.47480048,5.56486 2.50020048,5.5462 2.52230048,5.52351 C2.78710048,5.25423 2.98820048,4.92907 3.11090048,4.57191 L3.12110048,4.54107 L3.52000048,3.31758 C3.55400048,3.22525 3.61540048,3.14554 3.69600048,3.08911 C3.77780048,3.03101 3.87570048,2.99986 3.97600048,2.99999953 Z');\n" +
            "                const savedState1 = localStorage.getItem('videoButtonState2');\n" +
            "                if (savedState1 === null) {\n" +
            "                    localStorage.setItem('videoButtonState2', 'false');\n" +
            "                }\n" +
            "                const initialState1 = localStorage.getItem('videoButtonState2');\n" +
            "                $1button.setAttribute('aria-pressed', 'false');\n" +
            "               if (initialState1 === 'true') {\n" +
            "                $1button.setAttribute('aria-pressed', 'true');\n" +
            "                $1button.style.backgroundColor = '#FFF';\n" +
            "                $1button.style.color = '#000';\n" +
            "                $1button.setAttribute('aria-pressed', 'true');\n" +
            "                $1button.querySelector('div[class*=label]').textContent = 'Fechar Tela Cheia';\n" +
            "                $1button.querySelector(\"button:nth-child(1) > svg > path\").setAttribute('d', 'M256 1152h640v640H768v-421L93 2045l-90-90 674-675H256v-128zm1115-384h421v128h-640V256h128v421L1955 3l90 90-674 675z');\n" +
            "               } else {\n" +
            "                $1button.style.backgroundColor = '#000000b3';\n" +
            "                $1button.style.color = '#FFF';\n" +
            "                $button.setAttribute('aria-pressed', 'false');\n" +
            "                $1button.querySelector('div[class*=label]').textContent = 'Abrir Tela Cheia';\n" +
            "                $1button.querySelector(\"button:nth-child(1) > svg > path\").setAttribute('d', 'M1408 0h640v640h-128V219L218 1920h422v128H0v-640h128v422L1829 128h-421V0z');\n" +
            "               }\n" +
            "                $1button.addEventListener('click', e => {\n" +
            "                e.preventDefault();\n" +
            "                e.stopPropagation();\n" +
            "                const currentState1 = $1button.getAttribute('aria-pressed');\n" +
            "                const newState1 = currentState1 === 'true' ? 'false' : 'true';\n" +
            "                $1button.setAttribute('aria-pressed', newState1);\n" +
            "                localStorage.setItem('videoButtonState2', newState1);\n" +
            "                if (newState1 === 'true') {\n" +
            "                 const $1video = document.querySelector('#game-stream > div > video');\n" +
            "                 $1button.style.backgroundColor = '#FFF';\n" +
            "                 $1button.style.color = '#000';\n" +
            "                 $1button.querySelector('div[class*=label]').textContent = 'Fechar Tela Cheia';\n" +
            "                 isFullScreen = true;\n" +
            "                 $1video.style.objectFit = 'fill';\n" +
            "                 $1button.querySelector(\"button:nth-child(1) > svg > path\").setAttribute('d', 'M256 1152h640v640H768v-421L93 2045l-90-90 674-675H256v-128zm1115-384h421v128h-640V256h128v421L1955 3l90 90-674 675z');\n" +
            "                } else {\n" +
            "                 const $1video = document.querySelector('#game-stream > div > video');\n" +
            "                 $1button.style.backgroundColor = '#000000b3';\n" +
            "                 $1button.style.color = '#FFF';\n" +
            "                 $1button.querySelector('div[class*=label]').textContent = 'Abrir Tela Cheia';\n" +
            "                 isFullScreen = false;\n" +
            "                 $1video.style.objectFit = 'contain';\n" +
            "                 $1button.querySelector(\"button:nth-child(1) > svg > path\").setAttribute('d', 'M1408 0h640v640h-128V219L218 1920h422v128H0v-640h128v422L1829 128h-421V0z');\n" +
            "               }\n" +
            "           });\n" +
            "           $1orgButton.parentElement.insertBefore($1button, $1orgButton.parentElement.firstChild);\n" +
            "           const savedState = localStorage.getItem('videoButtonState');\n" +
            "           if (savedState === null) {\n" +
            "               localStorage.setItem('videoButtonState', 'false');\n" +
            "           }\n" +
            "           const initialState = localStorage.getItem('videoButtonState')\n" +
            "           $button.setAttribute('aria-pressed', 'false'); \n" +
            "           if (initialState === 'true') {\n" +
            "               $button.style.backgroundColor = '#FFF'; \n" +
            "               $button.style.color = '#000';\n" +
            "                $button.setAttribute('aria-pressed', 'true');\n" +
            "               $button.querySelector('div[class*=label]').textContent = 'Desabilitar O Clarity Boost';                \n" +
            "           } else {\n" +
            "               $button.style.backgroundColor = '#000000b3'; \n" +
            "               $button.style.color = '#FFF';\n" +
            "               $button.setAttribute('aria-pressed', 'false');\n" +
            "               $button.querySelector('div[class*=label]').textContent = 'Habilitar O Clarity Boost';\n" +
            "           } \n" +
            "           $button.addEventListener('click', e => {\n" +
            "                e.preventDefault();\n" +
            "                e.stopPropagation();\n" +
            "                const currentState = $button.getAttribute('aria-pressed');\n" +
            "                const newState = currentState === 'true' ? 'false' : 'true';\n" +
            "                $button.setAttribute('aria-pressed', newState);\n" +
            "                localStorage.setItem('videoButtonState', newState);   \n" +
            "                const videoElements = document.querySelectorAll(videoSelector);\n" +
            "                videoElements.forEach(videoElement => {\n" +
            "                   if (newState === 'true') {\n" +
            "                      applySharpnessToVideo(videoElement, sharpnessValue);\n" +
            "                   } else {\n" +
            "                      videoElement.style.filter = '';\n" +
            "                   }\n" +
            "                });\n" +
            "                if (newState === 'true') {\n" +
            "                   $button.style.backgroundColor = '#FFF';\n" +
            "                   $button.style.color = '#000';\n" +
            "                   $button.querySelector('div[class*=label]').textContent = 'Desabilitar O Clarity Boost';\n" +
            "                } else {\n" +
            "                   $button.style.backgroundColor = '#000000b3'; \n" +
            "                   $button.style.color = '#FFF';\n" +
            "                   $button.querySelector('div[class*=label]').textContent = 'Habilitar O Clarity Boost';\n" +
            "                   }\n" +
            "                }); \n" +
            "                $1orgButton.parentElement.insertBefore($button, $1orgButton.nextSibling);\n" +
            "            });\n" +
            "        });\n" +
            "    });\n" +
            "    observer1.observe($1screen, { subtree: true, childList: true });\n" +
            "}\n" +
             "function callInject(count) { if (count > 0) { botoes(); setTimeout(function() { callInject(count - 1); }, 2000); } } callInject(3);";
    }
}