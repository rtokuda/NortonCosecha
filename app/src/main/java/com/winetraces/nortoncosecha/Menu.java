package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.winetraces.wifimanager.ClientScanResult;
import com.winetraces.wifimanager.FinishScanListener;
import com.winetraces.wifimanager.WifiApManager;

import java.io.InputStream;
import java.util.ArrayList;

public class Menu extends AppCompatActivity implements View.OnClickListener {
    private ImageView mBackground;
    private String sBackground = "";
    private int menu_inx;
    private boolean clickFlag = false;

    private Button[][] btn = new Button[4][8];
    private int [][] btn_id = {
            {R.id.b_vendimia, R.id.b_presentismo, R.id.b_inicializar, R.id.b_reportes, R.id.b_descarga, R.id.b_Configurar, R.id.b_About, -1},
            {R.id.b_t_cosechador, R.id.b_t_vehiculo, R.id.b_t_recipiente, -1},
            {R.id.b_r_cosechador, R.id.b_r_cuadrilla, R.id.b_r_presentismo, R.id.b_r_remito, -1},
            {R.id.b_d_red, R.id.b_d_sd, -1}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int i, j;
        super.onCreate(savedInstanceState);

        if (!Build.BRAND.equals("Unitech") && !Build.MODEL.equals("PA700"))
        {
            int k = 10;
            i = 0;
            j = k/i;
        }
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
        for (j=0; j<4; j++) {
            for (i = 0; (btn_id[j][i] > 0); i++) {
                btn[j][i] = (Button) findViewById(btn_id[j][i]);
                btn[j][i].setOnClickListener(this);
            }
        }
        menu_inx = 0;
        clickFlag = false;
        enableGroup(menu_inx);
    }

    @Override
    public void onClick(View v)
    {
        Intent intent;

        Library.keybeep();
        switch (v.getId()){
            case R.id.b_vendimia:
                onBackPressed();
                break;
            case R.id.b_presentismo:
                enableGroup(-1);
                intent = new Intent(this, Presentismo.class);
                startActivity(intent);
                break;
            case R.id.b_inicializar:
                menu_inx = 1;
                getImage("menu_tarjetas.png");
                enableGroup(menu_inx);
                break;
            case R.id.b_reportes:
                menu_inx = 2;
                getImage("menu_reportes.png");
                enableGroup(menu_inx);
                break;
            case R.id.b_descarga:
                menu_inx = 3;
                getImage("menu_descarga.png");
                enableGroup(menu_inx);
                break;
            case R.id.b_Configurar:
                enableGroup(-1);
                intent = new Intent(this, Config.class);
                startActivity(intent);
                break;
            case R.id.b_About:
                enableGroup(-1);
                intent = new Intent(this, About.class);
                startActivity(intent);
                break;
            case R.id.b_t_cosechador:
                enableGroup(-1);
                Variables.CardType = Defines.T_COSECHADOR;
                intent = new Intent(this, InitCard.class);
                startActivity(intent);
                break;
            case R.id.b_t_vehiculo:
                enableGroup(-1);
                Variables.CardType = Defines.T_CAMION;
                intent = new Intent(this, InitCard.class);
                startActivity(intent);
                break;
            case R.id.b_t_recipiente:
                enableGroup(-1);
                Variables.CardType = Defines.T_BIN;
                intent = new Intent(this, InitCard.class);
                startActivity(intent);
                break;
            case R.id.b_r_cosechador:
                enableGroup(-1);
                Variables.PrintType = Defines.RP_COSECHADOR;
                intent = new Intent(this, Reportes.class);
                startActivity(intent);
                break;
            case R.id.b_r_cuadrilla:
                enableGroup(-1);
                Variables.PrintType = Defines.RP_CUADRILLA;
                intent = new Intent(this, Reportes.class);
                startActivity(intent);
                break;
            case R.id.b_r_presentismo:
                enableGroup(-1);
                Variables.PrintType = Defines.RP_PRESENTISMO;
                intent = new Intent(this, Reportes.class);
                startActivity(intent);
                break;
            case R.id.b_r_remito:
                enableGroup(-1);
                Variables.wProgress = new ProgressDialog(this);
                Variables.wProgress.setCancelable(false);
                Variables.wProgress.setMessage("...Un momento");
                Variables.wProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                Variables.wProgress.setIndeterminate(true);
                Variables.wProgress.show();

                Variables.PrintType = Defines.RP_REMITO;
                intent = new Intent(this, Reportes.class);
                startActivity(intent);
                break;
            case R.id.b_d_red:
                break;
            case R.id.b_d_sd:
                // Save_SD.save(this);
                break;
        }
    }

    @Override
    public boolean dispatchKeyEvent (KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_DOWN)
        {
            int key = event.getKeyCode();
            switch(key)
            {
                case Variables.KEYCODE_UNITECH_SEARCH:
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (menu_inx == 0)
                        onBackPressed();
                    else {
                        menu_inx = 0;
                        enableGroup(menu_inx);
                        getImage("menu_main.png");
                    }
                    return false;
                case Variables.KEYCODE_UNITECH_MENU:
                    return true;
            }
        }
        return false;
    }

    private void enableGroup(int inx)
    {
        int i, j;

        for (j=0; j<4; j++) {
            for (i = 0; (btn_id[j][i] > 0); i++) {
                if (j == inx)
                    btn[j][i].setClickable(true);
                else
                    btn[j][i].setClickable(false);
            }
        }
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    protected void onResume() {
        super.onResume();
        enableGroup(menu_inx);
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
