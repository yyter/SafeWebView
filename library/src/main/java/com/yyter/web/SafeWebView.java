package com.yyter.web;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;

/**
 * <p>一个安全的WebView.可以避免js反射带来的安全问题.</p>
 * 注意使用{@link #addJavascriptInvoker(Object, String)} 代替 {@link #addJavascriptInterface(Object, String)}<br/>
 * 其他和WebView使用方法一致
 * Created by liyang on 5/12/16.
 */
public class SafeWebView extends WebView {
    public static final String JSINVOKER_PREFIX = "JsInvoker::";
    private WebChromeClient client;
    private JsInvoker jsInvoker;
    public SafeWebView(Context context) {
        super(context);
        init();
    }

    public SafeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SafeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public SafeWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @TargetApi(11)
    public SafeWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        init();
    }

    private void init() {
        super.setWebChromeClient(new SafeWebChromeClient());
    }

    /**
     * 代替 {@link android.webkit.WebView#addJavascriptInterface(Object, String)}方法,
     * 用法和其一致.<br/>
     * <font color=red>Call from UI thread</font>
     */
    public void addJavascriptInvoker(Object javaBridge, String identifier) {
        if(jsInvoker == null) {
            jsInvoker = new JsInvoker();
        }
        jsInvoker.registerJavaBridge(javaBridge, identifier);
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        this.client = client;
    }

    private final class SafeWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if(client != null) {
                client.onProgressChanged(view, newProgress);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if(client != null) {
                client.onReceivedTitle(view, title);
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            if(client != null) {
                client.onReceivedIcon(view, icon);
            }
        }

        @Override
        public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
            super.onReceivedTouchIconUrl(view, url, precomposed);
            if(client != null) {
                client.onReceivedTouchIconUrl(view, url, precomposed);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            if(client != null) {
                client.onShowCustomView(view, callback);
            }
        }

        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            super.onShowCustomView(view, requestedOrientation, callback);
            if(client != null) {
                if(Build.VERSION.SDK_INT >= 14) {
                    client.onShowCustomView(view, requestedOrientation, callback);
                }
            }
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if(client != null) {
                client.onHideCustomView();
            }
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            if(client != null) {
                return client.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
            }
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
        }

        @Override
        public void onRequestFocus(WebView view) {
            super.onRequestFocus(view);
            if(client != null) {
                client.onRequestFocus(view);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            if(client != null) {
                client.onCloseWindow(window);
            }
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if(client != null) {
                return client.onJsAlert(view, url, message, result);
            }
            return super.onJsAlert(view, url, message, result);
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            if(client != null) {
                return client.onJsConfirm(view, url, message, result);
            }
            return super.onJsConfirm(view, url, message, result);
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            if (message != null && message.startsWith(JSINVOKER_PREFIX)) {
                //invoke java method
                String javaStatement = message.substring(JSINVOKER_PREFIX.length());
                try {
                    Object r = jsInvoker.invoke(javaStatement);
                    if (r != null) {
                        result.confirm(r.toString());
                    } else {
                        result.confirm();
                    }
                } catch (JsInvoker.SyntaxException e) {
                    e.printStackTrace();
                    result.cancel();
                }
                return true;
            }
            return client != null && client.onJsPrompt(view, url, message, defaultValue, result);
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            if(client != null) {
                return client.onJsBeforeUnload(view, url, message, result);
            }
            return super.onJsBeforeUnload(view, url, message, result);
        }

        @Override
        public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long estimatedDatabaseSize, long totalQuota, WebStorage.QuotaUpdater quotaUpdater) {
            super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
            if(client != null) {
                client.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
            }
        }

        @Override
        public void onReachedMaxAppCacheSize(long requiredStorage, long quota, WebStorage.QuotaUpdater quotaUpdater) {
            super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
            if(client != null) {
                client.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            super.onGeolocationPermissionsShowPrompt(origin, callback);
            if(client != null) {
                client.onGeolocationPermissionsShowPrompt(origin, callback);
            }
        }

        @Override
        public void onGeolocationPermissionsHidePrompt() {
            super.onGeolocationPermissionsHidePrompt();
            if(client != null) {
                client.onGeolocationPermissionsHidePrompt();
            }
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            super.onPermissionRequest(request);
            if(client != null) {
                if(Build.VERSION.SDK_INT >= 21) {
                    client.onPermissionRequest(request);
                }
            }
        }

        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            super.onPermissionRequestCanceled(request);
            if(client != null) {
                if(Build.VERSION.SDK_INT >= 21) {
                    client.onPermissionRequestCanceled(request);
                }
            }
        }

        @Override
        public boolean onJsTimeout() {
            if(client != null) {
                return client.onJsTimeout();
            }
            return super.onJsTimeout();
        }

        @Override
        public void onConsoleMessage(String message, int lineNumber, String sourceID) {
            super.onConsoleMessage(message, lineNumber, sourceID);
            if(client != null) {
                client.onConsoleMessage(message, lineNumber, sourceID);
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if(client != null) {
                return client.onConsoleMessage(consoleMessage);
            }
            return super.onConsoleMessage(consoleMessage);
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            if(client != null) {
                return client.getDefaultVideoPoster();
            }
            return super.getDefaultVideoPoster();
        }

        @Override
        public View getVideoLoadingProgressView() {
            if(client != null) {
                return client.getVideoLoadingProgressView();
            }
            return super.getVideoLoadingProgressView();
        }

        @Override
        public void getVisitedHistory(ValueCallback<String[]> callback) {
            super.getVisitedHistory(callback);
            if(client != null) {
                client.getVisitedHistory(callback);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if(client != null) {
                if(Build.VERSION.SDK_INT >= 21) {
                    client.onShowFileChooser(webView, filePathCallback, fileChooserParams);
                }
            }
            return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
        }
    }
}
