package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

public class Config extends AppCompatActivity {
    private ImageView mBackground;
    private String sBackground = null;
    private EditText editServer, editPrinter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int i, j;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);
        mBackground = (ImageView) findViewById(R.id.configMain);
        editServer = (EditText) findViewById(R.id.editServer);
        editPrinter = (EditText) findViewById(R.id.editPrinter);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        getImage("config.png");
        editServer.setText(Variables.sWebServiceURL);
        editPrinter.setText(Variables.sPrinterURL);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
    }


    public void salirClick(View target)
    {
        Library.keybeep();
        onBackPressed();
    }

    public void grabarClick(View target)
    {
        Library.keybeep();
        Variables.sPrinterURL = editPrinter.getText().toString();
        Variables.sWebServiceURL = editServer.getText().toString();
        Misc.SaveConfig();
        onBackPressed();
    }

    private void getImage(String file)
    {
        if (!file.equals(sBackground)) {
            try {
                InputStream ims = getAssets().open(file);
                Drawable d = Drawable.createFromStream(ims, null);
                mBackground.setImageDrawable(d);
                ims.close();
            } catch (Exception e) {
            }
            mBackground.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            mBackground.setScaleType(ImageView.ScaleType.FIT_XY);
        }
        mBackground.setVisibility(ImageView.VISIBLE);
        sBackground = file;
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
        Intent intent = new Intent(this, Presentismo.class);
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
