package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private boolean runFlag, cancelFlag;
    static boolean asyncEnd;
    private AsyncNet as;
    private ProgressDialog wProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int i, j;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_descarga_red);
        mBackground = (ImageView) findViewById(R.id.DescargaRedMain);
        vLog = (TextView)findViewById(R.id.detalle);
        vLog.setMovementMethod(new ScrollingMovementMethod());

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        getImage("uploadNET.png");
        Variables.msgTxt = "Conectando...\r\n";

        wProgress = new ProgressDialog(this);
        wProgress.setCancelable(false);
        wProgress.setMessage("...Desconectando");
        wProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        wProgress.setIndeterminate(true);

        runFlag = true;
        cancelFlag = false;
        asyncEnd = false;

        Thread th = new Thread(new Runnable() {
            String lastMsg = "";
            @Override
            public void run() {
                while (true) {
                    if (!runFlag)
                        break;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!Variables.msgTxt.equals(lastMsg)) {
                                vLog.setText(Variables.msgTxt);
                                lastMsg = Variables.msgTxt;
                            }
                            if (cancelFlag) {
                                if (!wProgress.isShowing())
                                    wProgress.show();
                                if (asyncEnd)
                                {
                                    runFlag = false;
                                    onBackPressed();
                                }
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
        as = new AsyncNet();
        as.execute();
    }


    public void salirClick(View target)
    {
        endActivity();
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
    public boolean dispatchKeyEvent (KeyEvent event)
    {
        Intent intent;

        if (event.getAction() == KeyEvent.ACTION_DOWN)
        {
            int key = event.getKeyCode();
            switch(key)
            {
                case KeyEvent.KEYCODE_BACK:
                    endActivity();
                    return true;
            }
        }
        return false;
    }

    private void endActivity()
    {
        Library.keybeep();
        if (asyncEnd) {
            as.cancel(true);
            runFlag = false;
            onBackPressed();
        }
        else
        {
            cancelFlag = true;
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

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(this, DescargaRed.class);
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

class AsyncNet extends AsyncTask<Void, String, Integer>
{
    private ProgressDialog wProgress;

    @Override
    protected void onPreExecute()
    {
        DescargaRed.asyncEnd = false;
    }

    @Override
    protected Integer doInBackground(Void...arg0) {
        WebService ws = new WebService();

        if (isCancelled())
            return 0;
        ws.SendData();
        if (isCancelled())
            return 0;
        ws.GetConfig();
        if (isCancelled())
            return 0;
        ws.GetPrograma();
        ws = null;
        return 0;
    }

    @Override
    protected void onCancelled()
    {
    }

    @Override
    protected void onProgressUpdate(String...msg){
    }

    @Override
    protected void onPostExecute(Integer arg0) {
        DescargaRed.asyncEnd = true;
        super.onPostExecute(arg0);
    }
}
