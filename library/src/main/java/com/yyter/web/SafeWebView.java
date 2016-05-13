package com.yyter.web;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
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
    }
}
