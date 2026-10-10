package de.battlerap.tvapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private static final String HOME = "https://www.battlerap.de/browse";
    private static final String ALLOWED_HOST = "battlerap.de";
    private static final int BASE_STEP = 24;
    private static final int EDGE_SCROLL_PX = 80;
    private static final long CURSOR_HIDE_MS = 4000;
    private static final int CURSOR_RING_RADIUS = 26;
    private static final int CURSOR_DOT_RADIUS = 6;
    private static final int CURSOR_RING_WIDTH = 5;
    private static final int CURSOR_RING_COLOR = 0xFFE60039;
    private static final int CURSOR_DOT_COLOR = 0xFFFFFFFF;
    private static final int CURSOR_DIRTY = 32;

    private WebView web;
    private WebChromeClient chrome;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private CursorView cursor;
    private float cursorX, cursorY;
    private boolean cursorInit;
    private long tapDownTime;
    private final Runnable hideCursor = () -> {
        if (cursor != null) {
            cursor.setCursorVisible(false);
        }
    };

    static int stepForRepeat(int repeat) {
        return (int) (BASE_STEP * (1 + Math.min(Math.max(repeat, 0), 20) / 4.0f));
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setupWebView();
        setContentView(web);
        cursor = new CursorView(this);
        addContentView(cursor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        cursor.setCursorVisible(false);
        web.requestFocus();
        if (savedInstanceState == null) {
            web.loadUrl(HOME);
        } else {
            web.restoreState(savedInstanceState);
            if (web.copyBackForwardList().getSize() == 0) {
                web.loadUrl(HOME);
            }
        }
    }

    private void setupWebView() {
        if (customView != null) {
            if (customView.getParent() != null) {
                ((ViewGroup) customView.getParent()).removeView(customView);
            }
            customView = null;
            customCallback = null;
        }
        if (web != null) {
            if (web.getParent() != null) {
                ((ViewGroup) web.getParent()).removeView(web);
            }
            web.destroy();
        }
        web = new WebView(this);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSafeBrowsingEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        // Third-party cookies stay on: the video CDN sets them during playback.
        cookies.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }

            private boolean handleUrl(Uri uri) {
                String scheme = uri.getScheme();
                String host = uri.getHost();
                boolean http = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
                if (http && host != null
                        && (host.equals(ALLOWED_HOST) || host.endsWith("." + ALLOWED_HOST))) {
                    return false;
                }
                if (http && host != null) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException e) {
                        // No browser on TV, stay in app.
                    }
                }
                return true;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                setupWebView();
                setContentView(web);
                web.requestFocus();
                web.loadUrl(HOME);
                return true;
            }
        });

        chrome = new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customCallback = callback;
                addContentView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                web.setVisibility(View.INVISIBLE); // never GONE: fullscreen video loses its target size
                cursor.bringToFront();
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }
                ((ViewGroup) customView.getParent()).removeView(customView);
                customView = null;
                web.setVisibility(View.VISIBLE);
                if (customCallback != null) {
                    customCallback.onCustomViewHidden();
                }
            }
        };
        web.setWebChromeClient(chrome);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_BUTTON_A) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
                ensureCursorInit();
                showCursor();
                tapDown();
            } else if (e.getAction() == KeyEvent.ACTION_UP) {
                tapUp();
            }
            return true;
        }
        int dx = 0, dy = 0;
        if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
            dx = -1;
        } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            dx = 1;
        } else if (code == KeyEvent.KEYCODE_DPAD_UP) {
            dy = -1;
        } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            dy = 1;
        } else {
            return super.dispatchKeyEvent(e);
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            moveCursor(dx, dy, Math.max(e.getRepeatCount(), 0));
        }
        return true;
    }

    private View touchTarget() {
        return customView != null ? customView : web;
    }

    private void ensureCursorInit() {
        if (cursorInit) {
            return;
        }
        int w = cursor.getWidth(), h = cursor.getHeight();
        cursorX = (w > 0 ? w : getResources().getDisplayMetrics().widthPixels) / 2f;
        cursorY = (h > 0 ? h : getResources().getDisplayMetrics().heightPixels) / 2f;
        cursorInit = true;
    }

    private void showCursor() {
        cursor.setCursorVisible(true);
        cursor.removeCallbacks(hideCursor);
        cursor.postDelayed(hideCursor, CURSOR_HIDE_MS);
    }

    private void moveCursor(int dx, int dy, int repeat) {
        ensureCursorInit();
        showCursor();
        int step = stepForRepeat(repeat);
        float rawX = cursorX + dx * step;
        float rawY = cursorY + dy * step;
        float nx = rawX, ny = rawY;
        boolean edge = false;
        int w = cursor.getWidth(), h = cursor.getHeight();
        if (w > 0 && h > 0) {
            nx = clamp(rawX, 0, w);
            ny = clamp(rawY, 0, h);
            edge = nx != rawX || ny != rawY;
        }
        cursorX = nx;
        cursorY = ny;
        cursor.setPos(nx, ny);
        if (edge && repeat > 1 && customView == null && web != null) {
            web.scrollBy(dx * EDGE_SCROLL_PX, dy * EDGE_SCROLL_PX);
        } else {
            long now = SystemClock.uptimeMillis();
            MotionEvent ev = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_HOVER_MOVE, nx, ny, 0);
            ev.setSource(InputDevice.SOURCE_MOUSE);
            touchTarget().dispatchGenericMotionEvent(ev);
            ev.recycle();
        }
    }

    private void tapDown() {
        tapDownTime = SystemClock.uptimeMillis();
        MotionEvent ev = MotionEvent.obtain(tapDownTime, tapDownTime,
                MotionEvent.ACTION_DOWN, cursorX, cursorY, 0);
        ev.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        touchTarget().dispatchTouchEvent(ev);
        ev.recycle();
    }

    private void tapUp() {
        if (tapDownTime == 0) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        MotionEvent ev = MotionEvent.obtain(tapDownTime, now,
                MotionEvent.ACTION_UP, cursorX, cursorY, 0);
        ev.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        touchTarget().dispatchTouchEvent(ev);
        ev.recycle();
        tapDownTime = 0;
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            chrome.onHideCustomView();
        } else if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) {
            web.saveState(outState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cursor != null) {
            cursor.removeCallbacks(hideCursor);
        }
        if (web != null) {
            web.onPause();
            web.pauseTimers();
        }
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            web.resumeTimers();
        }
    }

    @Override
    protected void onDestroy() {
        if (cursor != null) {
            cursor.removeCallbacks(hideCursor);
        }
        if (web != null) {
            if (web.getParent() != null) {
                ((ViewGroup) web.getParent()).removeView(web);
            }
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    private static class CursorView extends View {
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float x, y;
        private boolean visible;

        CursorView(Context c) {
            super(c);
            setFocusable(false);
            setClickable(false);
            setVisibility(View.GONE);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(CURSOR_RING_WIDTH);
            ring.setColor(CURSOR_RING_COLOR);
            dot.setStyle(Paint.Style.FILL);
            dot.setColor(CURSOR_DOT_COLOR);
        }

        void setPos(float x, float y) {
            float ox = this.x, oy = this.y;
            this.x = x;
            this.y = y;
            invalidate((int) Math.min(ox, x) - CURSOR_DIRTY, (int) Math.min(oy, y) - CURSOR_DIRTY,
                    (int) Math.max(ox, x) + CURSOR_DIRTY, (int) Math.max(oy, y) + CURSOR_DIRTY);
        }

        void setCursorVisible(boolean v) {
            visible = v;
            setVisibility(v ? View.VISIBLE : View.GONE);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (!visible) {
                return;
            }
            canvas.drawCircle(x, y, CURSOR_RING_RADIUS, ring);
            canvas.drawCircle(x, y, CURSOR_DOT_RADIUS, dot);
        }
    }
}
