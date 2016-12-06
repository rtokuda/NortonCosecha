package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.winetraces.recordstore.RecordStore;

import java.io.InputStream;


public class NortonCosecha extends AppCompatActivity {

    private View mMainView;
    private ImageView mBackground;
    private TextView mNombre, mCount, mLegajo;
    private TextView count1, count2, count3, count4, count5, count6;
    private Button setPrograma;
    private ProgressBar mCosechadorTimer;
    private Handler handler = new Handler();
    private String sBackground = "";
    private int prgInx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nortoncosecha);
        mMainView = findViewById(R.id.mainScreen);
        mBackground = (ImageView) findViewById(R.id.background);
        getImage("wait_programa.png");
        Variables.currView = mBackground;

        mNombre = (TextView) findViewById(R.id.cosechador_nombre);
        mCount = (TextView) findViewById(R.id.cosechador_count);
        mLegajo = (TextView) findViewById(R.id.cosechador_legajo);

        count1 = (TextView) findViewById(R.id.Count1);
        count2 = (TextView) findViewById(R.id.Count2);
        count3 = (TextView) findViewById(R.id.Count3);
        count4 = (TextView) findViewById(R.id.Count4);
        count5 = (TextView) findViewById(R.id.Count5);
        count6 = (TextView) findViewById(R.id.Count6);
        setPrograma = (Button)findViewById(R.id.programa);
        mCosechadorTimer = (ProgressBar)findViewById(R.id.cosechadorTimer);

        Variables.bFlagCosecha = false;
        Variables.mifareEnable = false;
        Variables.mainEnable = false;
        Variables.keyEnable = false;

        mBackground.setVisibility(View.INVISIBLE);
        mMainView.setVisibility(View.INVISIBLE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        RecordStore.initializeRecordStore(this, false);
        Variables.DeviceID = Build.SERIAL.hashCode(); //5DBE1EDE, 1572740830
        String s = Build.SERIAL; //7C8912B5  2089358005
        Misc.SetPrograma(this);

        Variables.sWebServiceURL = "norton.fundacionadabyron.org";

        Variables.ErrNum = 0;
        prgInx = 0;

        mCosechadorTimer.setVisibility(View.INVISIBLE);
        mCosechadorTimer.setMax(Variables.DiffTime);

            Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                     while (true)
                     {
                         if(mCosechadorTimer.getVisibility()==View.VISIBLE)
                         {
                             int horaAct;
                             final int diff;
                             boolean fDisplay = false;
                             if (Variables.CosechadorLastTime > 0) {
                                 horaAct = Misc.GetClock();
                                 int tmp = (horaAct % 86400) - ((int) (Variables.CosechadorLastTime % 86400L));
                                 if (tmp > Variables.DiffTime)
                                 {
                                     Variables.CosechadorLastTime = -Variables.CosechadorLastTime;
                                     tmp = Variables.DiffTime;
                                 }
                                 diff = tmp;

                                 runOnUiThread(new Runnable() {
                                     @Override
                                     public void run() {
                                         handler.post(new Runnable() {
                                             @Override
                                             public void run() {
                                                 mCosechadorTimer.setProgress(diff);
                                                 if (RecordStore.ChannelCount!=0) {
                                                     Toast.makeText(NortonCosecha.this, "Error Canal Recordstore", Toast.LENGTH_SHORT).show();
                                                 }
                                             }
                                         });
                                     }
                                 });

                             }

                         }
                         try {
                             Thread.sleep(3000);
                         } catch (Exception e) {
                         }
                     }
                }
            });
            th.start();
        Variables.ws = new WebService();
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

    public void programaClick(View target)
    {
        Intent intent = new Intent(this, Programa.class);
        Library.keybeep();
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Variables.mainEnable) {
            if (mMainView.getVisibility() == View.INVISIBLE)
                mMainView.setVisibility(View.VISIBLE);
            setBackground();
            refreshScreenCosecha();
        }
        mMainView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        PendingIntent pendingIntent = this.createPendingIntent();
        // Enable NFC adapter
        MifareIO.enable(this, pendingIntent);
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(this, NortonCosecha.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disable NFC adapter
        MifareIO.disable(this);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Variables.bFlagCosecha && MifareIO.connect(this, intent)) {
            if (Cosecha.CardProcess(this, intent)) {
                switch (Variables.CardType)
                {
                    case Defines.T_BIN:
                    case Defines.T_CHANGE:
                    case Defines.T_CAMION:
                        Library.alert(this,"Información", Variables.msg, android.R.drawable.ic_dialog_info);
                        break;
                    case Defines.T_COSECHADOR:
                        refreshScreenCosecha();
                        break;
                    case Defines.T_PROGRAMA:
                        break;
                }
                Library.goodBeep();
            }
            else { Library.badBeep(); }
        }
        else {
            Library.badBeep();
        }
        MifareIO.disconnect();
    }

    public void setBackground()
    {
        if (Variables.ProgSel) {
            if (!Variables.bFlagCosecha)
            {
                Variables.bFlagCosecha = true;
                mBackground.setVisibility(View.VISIBLE);
            }
            if (Variables.ModoCosecha == Defines.MODO_TACHO) {
                getImage("cosechando_tacho.png");
                mBackground.setVisibility(View.VISIBLE);
            }
            else {
                getImage("cosechando_caja.png");
                mBackground.setVisibility(View.VISIBLE);
            }
            setPrograma.setText(Variables.Programa);
        }
        else {
            getImage("wait_programa.png");
            mBackground.setVisibility(View.VISIBLE);
            setPrograma.setText(R.string.program_name);
        }
    }

    public void refreshScreenCosecha()
    {
        if (!Variables.bFlagCosecha || !Variables.ProgSel) {
            Variables.CosechadorLastTime = -1;
            mCosechadorTimer.setVisibility(View.INVISIBLE);
            mNombre.setText(" ");
            mCount.setText(" ");
            mLegajo.setText(" ");
            count1.setText(" ");
            count2.setText(" ");
            count3.setText(" ");
            count4.setText(" ");
            count5.setText(" ");
            count6.setText(" ");
            return;
        }
        mNombre.setText(Variables.Cosechador_name);
        mCount.setText(Variables.Cosechador_count);
        mLegajo.setText(Variables.Cosechador_legajo);
        count1.setText(Library.padNum(Variables.TachoCajaCnt,3));
        count3.setText(Library.padNum(Variables.TotalTachos,3));
        count4.setText(Library.padNum(Variables.CamionCnt,3));
        count6.setText(Library.padNum(Variables.BinCnt,3));
        if (Variables.ModoCosecha == Defines.MODO_TACHO)
        {
            count2.setText(Library.padNum(Variables.Tachos4Bin,3));
            count5.setText(Library.padNum(Variables.Bin4Camion,3));
        }
        else
        {
            count2.setText(Library.padNum(Variables.Cajas4Pallet,3));
            count5.setText(Library.padNum(Variables.Pallet4Camion,3));
        }
        setPrograma.setText(Variables.Programa);
        if  (Variables.CosechadorLastTime > 0) {
            if (mCosechadorTimer.getVisibility() == View.INVISIBLE)
                mCosechadorTimer.setVisibility(View.VISIBLE);
            int horaAct = Misc.GetClock();
            int diff = (horaAct % 86400) - ((int) (Variables.CosechadorLastTime % 86400L));
            mCosechadorTimer.setProgress(diff);
        }
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
                case KeyEvent.KEYCODE_SEARCH:
                    if  (Variables.keyEnable) {
                        intent = new Intent(this, Programa.class);
                        Library.keybeep();
                        startActivity(intent);
                    }
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    return true;
                case KeyEvent.KEYCODE_MENU:
                    if  (Variables.keyEnable) {
                        intent = new Intent(this, Menu.class);
                        Library.keybeep();
                        startActivity(intent);
                    }
                    return true;
            }
        }
        return false;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Intent intent = new Intent(this, SplashScreen.class);
        startActivity(intent);
    }

    protected void mainEnable()
    {
        PendingIntent pendingIntent = this.createPendingIntent();
        // Enable NFC adapter
        Variables.keyEnable = true;
        MifareIO.enable(this, pendingIntent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public void badBeep2()
    {
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
        tone.startTone(ToneGenerator.TONE_PROP_NACK);
        try {
            Thread.sleep(500);
        }catch (InterruptedException e)
        {
            return;
        }
        tone.release();
    }
}