package de.battlerap.tvapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private static final String HOME = "https://www.battlerap.de/browse";
    private static final int BASE_STEP = 24;
    private static final long CURSOR_HIDE_MS = 4000;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient());

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
                web.setVisibility(View.INVISIBLE); // kein GONE: sonst verliert das Fullscreen-Video seine Zielgroesse
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
        }
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
        if (e.getAction() == KeyEvent.ACTION_DOWN || e.getAction() == KeyEvent.ACTION_MULTIPLE) {
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
        int step = BASE_STEP * (1 + Math.min(repeat, 20) / 4);
        float nx = cursorX + dx * step;
        float ny = cursorY + dy * step;
        boolean edge = false;
        int w = cursor.getWidth(), h = cursor.getHeight();
        if (w > 0 && h > 0) {
            if (nx < 0) {
                nx = 0;
                edge = true;
            } else if (nx > w) {
                nx = w;
                edge = true;
            }
            if (ny < 0) {
                ny = 0;
                edge = true;
            } else if (ny > h) {
                ny = h;
                edge = true;
            }
        }
        cursorX = nx;
        cursorY = ny;
        cursor.setPos(nx, ny);
        if (edge && repeat > 1 && customView == null) {
            web.scrollBy(dx * 80, dy * 80);
        } else {
            long now = SystemClock.uptimeMillis();
            MotionEvent ev = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_HOVER_MOVE, nx, ny, 0);
            ev.setSource(InputDevice.SOURCE_CLASS_POINTER);
            touchTarget().dispatchGenericMotionEvent(ev);
            ev.recycle();
        }
    }

    private void tapDown() {
        tapDownTime = SystemClock.uptimeMillis();
        MotionEvent ev = MotionEvent.obtain(tapDownTime, tapDownTime,
                MotionEvent.ACTION_DOWN, cursorX, cursorY, 0);
        touchTarget().dispatchTouchEvent(ev);
        ev.recycle();
    }

    private void tapUp() {
        long now = SystemClock.uptimeMillis();
        MotionEvent ev = MotionEvent.obtain(tapDownTime, now,
                MotionEvent.ACTION_UP, cursorX, cursorY, 0);
        touchTarget().dispatchTouchEvent(ev);
        ev.recycle();
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            chrome.onHideCustomView();
        } else if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            if (web.getParent() != null) {
                ((ViewGroup) web.getParent()).removeView(web);
            }
            web.destroy();
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
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(5);
            ring.setColor(0xFFE60039);
            dot.setStyle(Paint.Style.FILL);
            dot.setColor(0xFFFFFFFF);
        }

        void setPos(float x, float y) {
            this.x = x;
            this.y = y;
            invalidate();
        }

        void setCursorVisible(boolean v) {
            visible = v;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (!visible) {
                return;
            }
            canvas.drawCircle(x, y, 26, ring);
            canvas.drawCircle(x, y, 6, dot);
        }
    }
}
