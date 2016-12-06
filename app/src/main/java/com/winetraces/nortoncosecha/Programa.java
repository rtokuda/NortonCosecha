package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.winetraces.recordstore.RecordStore;

import java.io.InputStream;
import java.util.Calendar;

public class Programa extends AppCompatActivity {
    private ImageView mBackground;
    public String txt[][] = new String[50][10];
    public String prg[] = new String[50];
    private byte ProgInx, MaxProg, ProgAct;
    int cnt=0;
    byte i;
    RecordStore record = null;
    int horaAct;
    int fechaHoy;
    private TextView programaAct, fincaView, cuartelView, areaView, programaView;
    private TextView cuadrillaView, cantidadView, variedadView;
    private String sBackground = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        byte[] datos;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programa);
        mBackground = (ImageView) findViewById(R.id.background);
        getImage("sel_programa.png");
        programaAct = (TextView) findViewById(R.id.progamaAct);
        fincaView = (TextView) findViewById(R.id.fincaView);
        cuartelView = (TextView) findViewById(R.id.cuartelView);
        areaView = (TextView) findViewById(R.id.areaView);
        programaView = (TextView) findViewById(R.id.programaView);
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
        try {
            record = RecordStore.openRecordStore("Programas", true, Defines.OPEN_READ);
            cnt = record.getNumRecords();
            MaxProg = 0;
            for (i=0; i<cnt; i++)
            {
                String tx[] = new String [10];
                datos = record.getRecord(i+1);
                Misc.splitPrograma(datos, tx);
                prg[i]= tx[0];
                for (byte j=0; j<9; j++)
                    txt[i][j]=tx[j+1];
                MaxProg++;
            }
            record.closeRecordStore();
        }catch(Exception e){}
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
        byte datos[]=new byte[10];

        if ((ProgAct != ProgInx) || !Variables.ProgSel)
        {
            Variables.ProgInx = ProgInx;

            Variables.ProgID = prg[ProgInx];
            Variables.Programa= txt[ProgInx][0];
            Variables.Finca = txt[ProgInx][1];
            Variables.Cuartel = txt[ProgInx][2];
            Variables.Area = txt[ProgInx][3];
            Variables.CuadrillaPrg = txt[ProgInx][4];
            Variables.ModoCosecha = Integer.parseInt(txt[ProgInx][7]);
            Variables.VariedadUva = txt[ProgInx][8];
            //NortonCosecha.TachoCajaCnt = 0;
            try {
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
            }catch (Exception e){}
            try {
                RecordStore record = RecordStore.openRecordStore("CamionReg", true, Defines.OPEN_WRITE);
                byte[] buff = new byte[512];
                int inx = Library.setField(buff, Defines.R_CAMIONHDR, 0, Defines.TIPO_LOG);
                inx = Library.setField(buff, Variables.Finca, inx, Defines.R_FINCA);
                inx = Library.setField(buff, Variables.Cuartel, inx, Defines.R_CUARTEL);
                inx = Library.setField(buff, Variables.Area, inx, Defines.R_AREA);
                inx = Library.setField(buff, Variables.VariedadUva, inx, Defines.R_VARIEDAD);
                record.addRecord(buff, 0, inx);
                record.closeRecordStore();
            }catch(Exception e){e.printStackTrace();}
            Calendar Fecha = Library.Fecha(horaAct*1000L);
            try {
                String fname = "NLOG"+Integer.toString(Fecha.get(Calendar.YEAR))+
                        Library.padNum(Fecha.get(Calendar.MONTH)+1, 2)+
                        Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2);
                RecordStore record = RecordStore.openRecordStore(fname, true, Defines.OPEN_WRITE);
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
            }catch(Exception e){e.printStackTrace();}
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
        programaAct.setText(txt[ProgInx][0]);
        fincaView.setText(txt[ProgInx][1]);
        cuartelView.setText(txt[ProgInx][2]);
        areaView.setText(txt[ProgInx][3]);
        programaView.setText(txt[ProgInx][4]);
        cuadrillaView.setText(Variables.Cuadrilla);
        cantidadView.setText(txt[ProgInx][5]);
        variedadView.setText(txt[ProgInx][8]);
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
        byte datos[]=new byte[10];

        Library.keybeep();
        if ((Variables.TachoCajaCnt != 0) && Variables.ProgSel)
        {
            Library.alert (this, "Atención", "Debe cerrar el bin para cambiar el programa", android.R.drawable.ic_dialog_alert);
            return;
        }
        try {
            if ((ProgAct != ProgInx) || !Variables.ProgSel)
            {
                Variables.ProgInx = ProgInx;

                Variables.ProgID = prg[ProgInx];
                Variables.Programa= txt[ProgInx][0];
                Variables.Finca = txt[ProgInx][1];
                Variables.Cuartel = txt[ProgInx][2];
                Variables.Area = txt[ProgInx][3];
                Variables.CuadrillaPrg = txt[ProgInx][4];
                Variables.ModoCosecha = Integer.parseInt(txt[ProgInx][7]);
                Variables.VariedadUva = txt[ProgInx][8];
                Variables.TachoCajaCnt = 0;
                try {
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
                }catch (Exception e){}
                try {
                    RecordStore record = RecordStore.openRecordStore("CamionReg", true, Defines.OPEN_WRITE);
                    byte[] buff = new byte[512];
                    int inx = Library.setField(buff, Defines.R_CAMIONHDR, 0, Defines.TIPO_LOG);
                    inx = Library.setField(buff, Variables.Finca, inx, Defines.R_FINCA);
                    inx = Library.setField(buff, Variables.Cuartel, inx, Defines.R_CUARTEL);
                    inx = Library.setField(buff, Variables.Area, inx, Defines.R_AREA);
                    inx = Library.setField(buff, Variables.VariedadUva, inx, Defines.R_VARIEDAD);
                    record.addRecord(buff, 0, inx);
                    record.closeRecordStore();
                }catch(Exception e){e.printStackTrace();}
                Calendar Fecha = Library.Fecha(horaAct*1000L);
                try {
                    String fname = "NLOG"+Integer.toString(Fecha.get(Calendar.YEAR))+
                            Library.padNum(Fecha.get(Calendar.MONTH)+1, 2)+
                            Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2);
                    RecordStore record = RecordStore.openRecordStore(fname, true, Defines.OPEN_WRITE);
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
                }catch(Exception e){e.printStackTrace();}
                Misc.SaveConfig();
                Variables.bFlagCosecha = true;
            }
            onBackPressed();
        } catch (Exception e) { }
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
