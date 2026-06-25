// SPDX-License-Identifier: AGPL-3.0-only
package com.eeimo.sillytavern;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.webkit.HttpAuthHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeWebViewClient;

public class StWebViewClient extends BridgeWebViewClient {

    private static final String TAG = "StWebViewClient";
    private final Bridge bridgeRef;

    public StWebViewClient(Bridge bridge) {
        super(bridge);
        this.bridgeRef = bridge;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        // Keep remote server URLs inside this WebView instead of launching the system browser.
        Uri url = request.getUrl();
        String host = url.getHost();
        if (host != null && (host.equals("47.74.57.111") || host.equals("localhost") || host.equals("127.0.0.1"))) {
            return false;
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri u = Uri.parse(url);
        String host = u.getHost();
        if (host != null && (host.equals("47.74.57.111") || host.equals("localhost") || host.equals("127.0.0.1"))) {
            return false;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        String user = RemoteNavPlugin.getUsername();
        String pass = RemoteNavPlugin.getPassword();
        Log.d(TAG, "onReceivedHttpAuthRequest host=" + host + " realm=" + realm + " user=" + user);
        if (user != null && !user.isEmpty()) {
            handler.proceed(user, pass != null ? pass : "");
        } else {
            handler.cancel();
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Activity a = bridgeRef.getActivity();
        if (a instanceof MainActivity) {
            ((MainActivity) a).showLoading(true);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Activity a = bridgeRef.getActivity();
        if (a instanceof MainActivity) {
            ((MainActivity) a).showLoading(false);
        }
    }
}
