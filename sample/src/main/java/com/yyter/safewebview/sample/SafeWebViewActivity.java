package com.yyter.safewebview.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.yyter.web.JavascriptInvoker;
import com.yyter.web.SafeWebView;

import java.io.File;

/**
 * Created by liyang on 5/14/16.
 */
public class SafeWebViewActivity extends Activity {
    private SafeWebView safeWebView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        safeWebView = new SafeWebView(this.getApplicationContext());
        configureWebView();
        setWebChromeClient();
        setContentView(safeWebView);

        addJavascriptInvokers();

        //load assets html file
        safeWebView.loadUrl("file:///android_asset/web/index.html");
    }

    private void addJavascriptInvokers() {
        safeWebView.addJavascriptInvoker(new JavaBridge(this), "Android");
    }

    private static class JavaBridge {
        private final Context context;
        JavaBridge(Context context) {
            this.context = context.getApplicationContext();
        }

        @JavascriptInvoker
        public void showToast(String data) {
            Toast.makeText(context, data, Toast.LENGTH_LONG).show();
        }

        @JavascriptInvoker
        public String getCpuAbi() {
            return Build.CPU_ABI;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        //configure
        WebSettings ws = safeWebView.getSettings();

        //js
        ws.setJavaScriptEnabled(true);
        if(Build.VERSION.SDK_INT >= 11) {
            safeWebView.removeJavascriptInterface("searchBoxJavaBridge_");
        }

        //h5 application cache
        ws.setAppCacheEnabled(true);
        File h5AppCachePath = getExternalFilesDir("h5_app_cache");
        if(h5AppCachePath != null) {
            ws.setAppCachePath(h5AppCachePath.getAbsolutePath());
        }

        //h5 session store & local store
        ws.setDomStorageEnabled(true);

        //js read & write db
        ws.setDatabaseEnabled(true);
        File jsDbPath = getExternalFilesDir("js_db");
        if(jsDbPath != null) {
            ws.setDatabasePath(jsDbPath.getAbsolutePath());
        }

        //viewport
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);

        //render
        ws.setRenderPriority(WebSettings.RenderPriority.HIGH);

        //scale
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        if(Build.VERSION.SDK_INT >= 11) {
            ws.setDisplayZoomControls(false);
        }
    }

    private void setWebChromeClient() {
        safeWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.i("WebView", "js_log: " + consoleMessage.message() + ", lineNumber: " + consoleMessage.lineNumber());
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(SafeWebViewActivity.this)
                        .setTitle("JsAlert")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
                final EditText et = new EditText(SafeWebViewActivity.this);
                et.setText(defaultValue);
                new AlertDialog.Builder(SafeWebViewActivity.this)
                        .setTitle(message)
                        .setView(et)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm(et.getText().toString());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        if(Build.VERSION.SDK_INT >= 11) {
            safeWebView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= 11) {
            safeWebView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        safeWebView.loadUrl("about:blank");//清除资源
        ViewGroup vg = (ViewGroup) safeWebView.getParent();
        vg.removeView(safeWebView);
        safeWebView.removeAllViews();
        safeWebView.destroy();
        super.onDestroy();
    }
}
