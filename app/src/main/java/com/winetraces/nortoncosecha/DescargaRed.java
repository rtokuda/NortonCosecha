package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.InputStream;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DescargaRed extends AppCompatActivity {
    private ImageView mBackground;
    private String sBackground = null;
    private TextView vLog;
    int WaitTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int i, j;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_descarga_red);
        mBackground = (ImageView) findViewById(R.id.DescargaRedMain);
        vLog = (TextView)findViewById(R.id.detalle);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        getImage("webconnect.png");
        Variables.msgTxt = "Conectando...\r\n";
        Thread th = new Thread(new Runnable() {
            String lastMsg = "";
            @Override
            public void run() {
                while (true) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!Variables.msgTxt.equals(lastMsg)) {
                                vLog.setText(Variables.msgTxt);
                                lastMsg = Variables.msgTxt;
                            }
                        }
                    });
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                    }
                }
            }
        });
        th.start();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                Variables.ws.SendData();
                Variables.ws.GetConfig();
                Variables.ws.GetPrograma();
            }
        });
        th.start();
    }


    public void salirClick(View target)
    {
        Library.keybeep();
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