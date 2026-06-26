// SPDX-License-Identifier: AGPL-3.0-only
package com.eeimo.sillytavern;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "RemoteNav")
public class RemoteNavPlugin extends Plugin {

    private static final String TAG = "RemoteNav";
    static final String SERVER_URL = "https://sillytavern.chat";
    private static String basicAuth = null;
    private static String username = null;
    private static String password = null;

    static String getBasicAuth() {
        return basicAuth;
    }

    static String getUsername() {
        return username;
    }

    static String getPassword() {
        return password;
    }

    @PluginMethod
    public void loadRemote(PluginCall call) {
        String url = call.getString("url");
        String auth = call.getString("basicAuth");
        if (url == null || url.isEmpty()) {
            call.reject("url is required");
            return;
        }
        basicAuth = auth;

        // Decode credentials for setHttpAuthUsernamePassword and fallback.
        if (auth != null && !auth.isEmpty()) {
            try {
                String decoded = new String(Base64.decode(auth, Base64.DEFAULT));
                int idx = decoded.indexOf(':');
                username = idx >= 0 ? decoded.substring(0, idx) : decoded;
                password = idx >= 0 ? decoded.substring(idx + 1) : "";
            } catch (Exception e) {
                Log.e(TAG, "decode basicAuth failed", e);
            }
        }

        final String finalUrl = url;
        final WebView webView = this.bridge.getWebView();
        new Handler(Looper.getMainLooper()).post(() -> {
            // Pre-store HTTP basic auth credentials so WebView sends them on the first request.
            try {
                Uri uri = Uri.parse(finalUrl);
                String host = uri.getHost();
                if (host != null && username != null) {
                    webView.setHttpAuthUsernamePassword(host, "SillyTavern", username, password);
                    Log.d(TAG, "setHttpAuthUsernamePassword host=" + host + " user=" + username);
                }
            } catch (Exception e) {
                Log.e(TAG, "setHttpAuthUsernamePassword failed", e);
            }
            webView.setWebViewClient(new StWebViewClient(this.bridge));
            // Send Authorization header on the main document request as a primary auth path.
            if (basicAuth != null && !basicAuth.isEmpty()) {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Authorization", "Basic " + basicAuth);
                webView.loadUrl(finalUrl, headers);
            } else {
                webView.loadUrl(finalUrl);
            }
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setOnRemote(true);
            }
        });
        call.resolve();
    }

    /** Load the remote server (used on app start and on "logout"). No JS bridge needed. */
    public void connect() {
        final WebView webView = this.bridge.getWebView();
        if (webView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            webView.setWebViewClient(new StWebViewClient(this.bridge));
            webView.loadUrl(SERVER_URL);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setOnRemote(true);
            }
        });
    }

    public void goLocal() {
        connect();
    }
}
