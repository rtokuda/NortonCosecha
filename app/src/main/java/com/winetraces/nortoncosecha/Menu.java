package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.InputStream;

public class Menu extends AppCompatActivity implements View.OnClickListener {
    private ImageView mBackground;
    private String sBackground;
    private int menu_inx;

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
                break;
            case R.id.b_About:
                break;
            case R.id.b_t_cosechador:
                Variables.CardType = Defines.T_COSECHADOR;
                intent = new Intent(this, InitCard.class);
                startActivity(intent);
                break;
            case R.id.b_t_vehiculo:
                Variables.CardType = Defines.T_CAMION;
                intent = new Intent(this, InitCard.class);
                startActivity(intent);
                break;
            case R.id.b_t_recipiente:
                Variables.CardType = Defines.T_BIN;
                intent = new Intent(this, InitCard.class);
                startActivity(intent);
                break;
            case R.id.b_r_cosechador:
                break;
            case R.id.b_r_cuadrilla:
                break;
            case R.id.b_r_presentismo:
                break;
            case R.id.b_r_remito:
                break;
            case R.id.b_d_red:
                break;
            case R.id.b_d_sd:
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
