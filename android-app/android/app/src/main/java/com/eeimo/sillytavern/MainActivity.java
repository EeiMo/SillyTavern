// SPDX-License-Identifier: AGPL-3.0-only
package com.eeimo.sillytavern;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.PluginHandle;

public class MainActivity extends BridgeActivity {

    private boolean onRemote = false;
    private boolean initialConnected = false;
    private View loadingOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(RemoteNavPlugin.class);
        super.onCreate(savedInstanceState);
        setupFullscreen();
        applyKeyboardInsets();
        setupLoadingOverlay();
    }

    /** Truly hide the status bar (and nav bar) — immersive sticky fullscreen. */
    private void setupFullscreen() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());

        View root = window.getDecorView();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            v.setPadding(0, 0, 0, 0);
            // Apply IME (keyboard) height as bottom padding on the WebView so its content
            // stays visible above the soft keyboard while keeping fullscreen immersive.
            if (this.bridge != null) {
                android.webkit.WebView wv = this.bridge.getWebView();
                if (wv != null) {
                    int imeH = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                    int navH = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    int bottom = Math.max(imeH, navH);
                    wv.setPadding(0, 0, 0, bottom);
                }
            }
            return ViewCompat.onApplyWindowInsets(v, insets);
        });
    }

    /** Ensure the WebView gets insets applied once the bridge is ready. */
    private void applyKeyboardInsets() {
        if (this.bridge == null) return;
        android.webkit.WebView wv = this.bridge.getWebView();
        if (wv != null) {
            wv.requestApplyInsets();
        }
    }

    private void rehideBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    private int statusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return getResources().getDimensionPixelSize(resId);
        }
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
    }

    private void setupLoadingOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xFF202124);
        overlay.setVisibility(View.GONE);

        FrameLayout box = new FrameLayout(this);
        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        boxLp.gravity = Gravity.CENTER;
        box.setLayoutParams(boxLp);

        ProgressBar bar = new ProgressBar(this);
        bar.setIndeterminate(true);
        bar.getIndeterminateDrawable().setTint(0xFF8AB4F8);

        FrameLayout.LayoutParams textLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        textLp.topMargin = dp(70);

        TextView text = new TextView(this);
        text.setText("正在连接服务器…");
        text.setTextColor(0xFFE8EAED);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        box.addView(bar);
        textLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        textLp.topMargin = dp(70);
        box.addView(text, textLp);

        overlay.addView(box);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addContentView(overlay, lp);
        loadingOverlay = overlay;
    }

    void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(show && onRemote ? View.VISIBLE : View.GONE);
            }
        });
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    void setOnRemote(boolean v) {
        this.onRemote = v;
        runOnUiThread(() -> {
            if (loadingOverlay != null && !v) {
                loadingOverlay.setVisibility(View.GONE);
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onResume() {
        super.onResume();
        rehideBars();
        applyKeyboardInsets();
        if (!initialConnected && this.bridge != null) {
            initialConnected = true;
            PluginHandle handle = this.bridge.getPlugin("RemoteNav");
            if (handle != null) {
                ((RemoteNavPlugin) handle.getInstance()).connect();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            rehideBars();
        }
    }
}
