package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;

public class Menu extends AppCompatActivity {
    private ImageView mBackground;
    private String sBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_menu);
        mBackground = (ImageView) findViewById(R.id.menuMain);
        getImage("menu_main.png");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );


    }

    private void getImage(String file)
    {
        try {
            InputStream ims = getAssets().open(file);
            Drawable d = Drawable.createFromStream(ims, null);
            mBackground.setImageDrawable(d);
            ims.close();
        }catch (Exception e){}
        mBackground.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mBackground.setScaleType(ImageView.ScaleType.FIT_XY);
        mBackground.setVisibility(ImageView.VISIBLE);
        sBackground = file;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

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

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(this, Menu.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        MifareIO.disconnect();
    }
}
