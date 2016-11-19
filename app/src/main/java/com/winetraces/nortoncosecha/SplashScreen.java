package com.winetraces.nortoncosecha;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashScreen extends Activity {
    private WebView mSplashView;
    private final Handler mHideHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mSplashView = (WebView) findViewById(R.id.splashScreen);
        mSplashView.setVisibility(View.VISIBLE);
        mSplashView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!Variables.mifareEnable) {
            Variables.mainEnable = true;
            Variables.mifareEnable = true;
            onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PendingIntent pendingIntent = this.createPendingIntent();
        // Enable NFC adapter
        MifareIO.enable(this, pendingIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disable NFC adapter
        MifareIO.disable(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mSplashView.loadUrl("file:///android_asset/splash.html");
        mSplashView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                mHideHandler.removeCallbacks(mHideRunnable);
                mainEnable();
                return false;
            }
        });
        delayedHide(4000);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mHideHandler.removeCallbacks(mHideRunnable);
            mainEnable();
        }
    };

    protected void mainEnable()
    {
        mSplashView.loadUrl("file:///android_asset/wait_card.html");
        mSplashView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return false;
            }
        });
        PendingIntent pendingIntent = this.createPendingIntent();
        // Enable NFC adapter
        Variables.keyEnable = true;
        MifareIO.enable(this, pendingIntent);
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(this, SplashScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }
}
