package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.winetraces.recordstore.RecordStore;

import java.io.InputStream;
import java.util.Calendar;

public class Programa extends AppCompatActivity {
    private ImageView mBackground;
    public String txt[][] = new String[50][20];
    public String prg[] = new String[50];
    private byte ProgInx, MaxProg, ProgAct;
    int cnt=0;
    byte i;
    int horaAct;
    int fechaHoy;
    private TextView programaAct, fincaView, cuartelView, areaView, unidadView;
    private TextView cuadrillaView, cantidadView, variedadView;
    private String sBackground = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        byte[] datos;
        RecordStore record;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programa);
        mBackground = (ImageView) findViewById(R.id.background);
        getImage("sel_programa.png");
        programaAct = (TextView) findViewById(R.id.progamaAct);
        fincaView = (TextView) findViewById(R.id.fincaView);
        cuartelView = (TextView) findViewById(R.id.cuartelView);
        areaView = (TextView) findViewById(R.id.areaView);
        unidadView = (TextView) findViewById(R.id.unidadView);
        cuadrillaView = (TextView) findViewById(R.id.cuadrillaView);
        cantidadView = (TextView) findViewById(R.id.cantidadView);
        variedadView = (TextView) findViewById(R.id.variedadView);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        Variables.currView = mBackground;

        horaAct = Misc.GetClock();

        record = RecordStore.openRecordStore("Programas", true, Defines.OPEN_READ);
        cnt = record.getNumRecords();
        MaxProg = 0;
        for (i=0; i<cnt; i++)
        {
            String tx[] = new String [20];
            datos = record.getRecord(i+1);
            Misc.splitProgramas(datos, tx);
            prg[i]= tx[0];
            for (byte j=0; j<19; j++)
                txt[i][j]=tx[j];
            MaxProg++;
        }
        record.closeRecordStore();
        ProgInx = Variables.ProgInx;
        ProgAct = ProgInx;
        pantalla();
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
            mBackground.setVisibility(ImageView.VISIBLE);
        }
        sBackground = file;
    }

    private void ProgSel()
    {
        RecordStore record;
        byte datos[]=new byte[10];

        if ((ProgAct != ProgInx) || !Variables.ProgSel)
        {
            Variables.ProgInx = ProgInx;

            Variables.ProgID = prg[ProgInx];
            Variables.Programa= txt[ProgInx][Defines.PRG_NAME_COMPLETE];
            Variables.Finca = txt[ProgInx][Defines.PRG_FINCA];
            Variables.Cuartel = txt[ProgInx][Defines.PRG_CUARTEL];
            Variables.Area = txt[ProgInx][Defines.PRG_AREA];
            Variables.CuadrillaPrg = txt[ProgInx][Defines.PRG_CUADRILLA];
            Variables.ModoCosecha = Integer.parseInt(txt[ProgInx][Defines.PRG_MODO_COSECHA]);
            Variables.VariedadUva = txt[ProgInx][Defines.PRG_VARIEDAD];
            Variables.Unidad = txt[ProgInx][Defines.PRG_UNIDAD];
            //NortonCosecha.TachoCajaCnt = 0;

            record = RecordStore.openRecordStore("ProgSel",true, Defines.OPEN_WRITE);
            byte dd[] = prg[ProgInx].getBytes();
            Library.byteArrayCopy(dd, datos);
            int horaAct = Misc.GetClock();
            Library.toIntelDataInt(horaAct, datos, 6);
            record.addRecord(datos,0,10);
            record.closeRecordStore();

            Variables.FechaProg = horaAct / 86400;
            Variables.ProgSel = true;
            Misc.SaveConfig();

            record = RecordStore.openRecordStore("CamionReg", true, Defines.OPEN_WRITE);
            byte[] buff = new byte[512];
            int inx = Library.setField(buff, Defines.R_CAMIONHDR, 0, Defines.TIPO_LOG);
            inx = Library.setField(buff, Variables.Finca, inx, Defines.R_FINCA);
            inx = Library.setField(buff, Variables.Cuartel, inx, Defines.R_CUARTEL);
            inx = Library.setField(buff, Variables.Area, inx, Defines.R_AREA);
            inx = Library.setField(buff, Variables.VariedadUva, inx, Defines.R_VARIEDAD);
            record.addRecord(buff, 0, inx);
            record.closeRecordStore();

            Calendar Fecha = Library.Fecha(horaAct*1000L);

            String fname = "NLOG"+Integer.toString(Fecha.get(Calendar.YEAR))+
                    Library.padNum(Fecha.get(Calendar.MONTH)+1, 2)+
                    Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2);
            record = RecordStore.openRecordStore(fname, true, Defines.OPEN_WRITE);

            datos = new byte [15];
            datos[0] = 3;

            byte[] aux = Variables.ProgID.getBytes();
            Library.byteArrayCopy(aux, 0, datos, 1);

            aux = new byte[4];
            Library.toIntelDataInt(Variables.DeviceID, aux, 0);
            aux = Library.arrayInvert(aux);
            Library.byteArrayCopy(aux, 0, datos, 7);

            aux = new byte[4];
            Library.toIntelDataInt(horaAct, aux, 0);
            aux = Library.arrayInvert(aux);
            Library.byteArrayCopy(aux, 0, datos, 11);

            record.addRecord(datos, 0, 15);
            record.closeRecordStore();
            Misc.SaveConfig();
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
                    Library.keybeep();
                    onBackPressed();
                    return false;
                case Variables.KEYCODE_UNITECH_MENU:
                    return true;
            }
        }
        return false;
    }

    public void pantalla()
    {
        programaAct.setText(txt[ProgInx][Defines.PRG_NAME_COMPLETE]);
        fincaView.setText(txt[ProgInx][Defines.PRG_FINCA]);
        cuartelView.setText(txt[ProgInx][Defines.PRG_CUARTEL]);
        areaView.setText(txt[ProgInx][Defines.PRG_AREA]);
        unidadView.setText(txt[ProgInx][Defines.PRG_UNIDAD]);
        cuadrillaView.setText(txt[ProgInx][Defines.PRG_CUADRILLA]);
        cantidadView.setText(txt[ProgInx][Defines.PRG_CANTIDAD]);
        variedadView.setText(txt[ProgInx][Defines.PRG_VARIEDAD]);
    }

    public void buttonLeftClick(View target)
    {
        if (ProgInx == 0)
            ProgInx = (byte)(MaxProg - 1);
        else
            ProgInx--;
        pantalla();
        Library.keybeep();
    }

    public void buttonRightClick(View target)
    {
        if (ProgInx < (MaxProg-1))
            ProgInx++;
        else
            ProgInx = 0;
        pantalla();
        Library.keybeep();
    }

    public void cancelarClick(View target)
    {
        Library.keybeep();
        onBackPressed();
    }

    public void seleccionarClick(View target)
    {
        RecordStore record;
        byte datos[]=new byte[10];

        Library.keybeep();
        if ((Variables.TachoCajaCnt != 0) && Variables.ProgSel)
        {
            Library.alert (this, "Atención", "Debe cerrar el bin para cambiar el programa", android.R.drawable.ic_dialog_alert);
            return;
        }
        if ((ProgAct != ProgInx) || !Variables.ProgSel)
        {
            Variables.ProgInx = ProgInx;
            Variables.TachoCajaCnt = 0;
            Variables.ProgID = prg[ProgInx];
            Variables.Programa= txt[ProgInx][Defines.PRG_NAME_COMPLETE];
            Variables.Finca = txt[ProgInx][Defines.PRG_FINCA];
            Variables.Cuartel = txt[ProgInx][Defines.PRG_CUARTEL];
            Variables.Area = txt[ProgInx][Defines.PRG_AREA];
            Variables.CuadrillaPrg = txt[ProgInx][Defines.PRG_CUADRILLA];
            Variables.ModoCosecha = Integer.parseInt(txt[ProgInx][Defines.PRG_MODO_COSECHA]);
            Variables.VariedadUva = txt[ProgInx][Defines.PRG_VARIEDAD];
            Variables.Unidad = txt[ProgInx][Defines.PRG_UNIDAD];

            record = RecordStore.openRecordStore("ProgSel",true, Defines.OPEN_WRITE);
            byte dd[] = prg[ProgInx].getBytes();
            Library.byteArrayCopy(dd, datos);
            int horaAct = Misc.GetClock();
            Library.toIntelDataInt(horaAct, datos, 6);
            record.addRecord(datos,0,10);
            record.closeRecordStore();
            Variables.FechaProg = horaAct / 86400;
            Variables.ProgSel = true;
            Misc.SaveConfig();

            record = RecordStore.openRecordStore("CamionReg", true, Defines.OPEN_WRITE);
            byte[] buff = new byte[512];
            int inx = Library.setField(buff, Defines.R_CAMIONHDR, 0, Defines.TIPO_LOG);
            inx = Library.setField(buff, Variables.Finca, inx, Defines.R_FINCA);
            inx = Library.setField(buff, Variables.Cuartel, inx, Defines.R_CUARTEL);
            inx = Library.setField(buff, Variables.Area, inx, Defines.R_AREA);
            inx = Library.setField(buff, Variables.VariedadUva, inx, Defines.R_VARIEDAD);
            record.addRecord(buff, 0, inx);
            record.closeRecordStore();

            Calendar Fecha = Library.Fecha(horaAct*1000L);
            String fname = "NLOG"+Integer.toString(Fecha.get(Calendar.YEAR))+
                    Library.padNum(Fecha.get(Calendar.MONTH)+1, 2)+
                    Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2);

            record = RecordStore.openRecordStore(fname, true, Defines.OPEN_WRITE);
            datos = new byte [15];
            datos[0] = 3;

            byte[] aux = Variables.ProgID.getBytes();
            Library.byteArrayCopy(aux, 0, datos, 1);

            aux = new byte[4];
            Library.toIntelDataInt(Variables.DeviceID, aux, 0);
            aux = Library.arrayInvert(aux);
            Library.byteArrayCopy(aux, 0, datos, 7);

            aux = new byte[4];
            Library.toIntelDataInt(horaAct, aux, 0);
            aux = Library.arrayInvert(aux);
            Library.byteArrayCopy(aux, 0, datos, 11);

            record.addRecord(datos, 0, 15);
            record.closeRecordStore();
            Misc.SaveConfig();
            Variables.bFlagCosecha = true;
        }
        onBackPressed();
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
        Intent intent = new Intent(this, Programa.class);
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
