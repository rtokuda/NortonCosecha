package com.winetraces.nortoncosecha;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.winetraces.recordstore.RecordEnumeration;
import com.winetraces.recordstore.RecordStore;

import java.io.InputStream;
import java.util.Calendar;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Presentismo extends AppCompatActivity {
    private ImageView mBackground;
    private TextView vPresentes, vCosechador, vDetalle;
    private String sBackground = null;
    int horaAct;
    int _cantidad, _total;
    String _legajo;
    String _cosechador;
    byte[] _legajoAbreviado = new byte[2];
    int WaitTime;
    String _fecha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int i, j;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_presentismo);
        mBackground = (ImageView) findViewById(R.id.presentismoMain);
        vPresentes = (TextView)findViewById(R.id.presentes);
        vCosechador = (TextView)findViewById(R.id.cosechador);
        vDetalle = (TextView)findViewById(R.id.detalle);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        getImage("presentismo.png");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        vPresentes.setText(""+Variables.Presentes);
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

    private boolean CardProcess(Context context, Intent intent)
    {
        byte[] authent_card = {(byte)0x01, (byte)0x4E, (byte)0x52,
                (byte)0x54, (byte)0x31, (byte)0x01};
        byte[] Cosechador = new byte[16];
        byte[] CardId = new byte[16];
        byte[] Legajo = new byte[16];
        byte[] Contador = new byte[16];
        byte[][] wd = new byte[3][16];
        byte i;

        horaAct = Misc.GetClock();
        if (!MifareIO.read(context, intent, Defines.KEY_A, 1, 2))
            return false;
        CardId = MifareIO.ReadBuff[0];
        for (i=0; i<6; i++)
        {
            if (CardId[i] != authent_card[i])
                return false;
        }
        Library.byteArrayCopy(MifareIO.ReadBuff[1], Cosechador);
        if (!MifareIO.read(context, intent, Defines.KEY_A,4,3))
        {
            return false;
        }
        Library.byteArrayCopy(MifareIO.ReadBuff[0], Contador);
        Library.byteArrayCopy(MifareIO.ReadBuff[1], Legajo);
        int horaAnt = (Library.fromIntelDataIntLE(Contador, 8));
        int diaAnt = horaAnt / 86400;
        int diaAct = horaAct / 86400;
        _cantidad = Library.fromIntelDataIntLE(Contador,0);
        _total = Library.fromIntelDataIntLE(Contador,4);

        Calendar Fecha = Library.Fecha(horaAnt*1000L);
        if (horaAnt == 0)
            _fecha = "00/00";
        else
            _fecha = Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2)+"/"+
                    Library.padNum(Fecha.get(Calendar.MONTH)+1, 2);
        byte[] L = new byte[6];
        Library.byteArrayCopy(Legajo, 0, L, 0);
        _legajo = new String(L).toString();
        _cosechador = Library.String(Cosechador);
        _legajoAbreviado[0] = Legajo[6];
        _legajoAbreviado[1] = Legajo[7];

        if (diaAct != diaAnt)
        {
            Fecha = Library.Fecha(horaAct*1000L);
            horaAct -= 600;
            _cantidad = 0;
            Library.toIntelDataInt(_cantidad, Contador, 0);
            Library.toIntelDataInt(horaAct, Contador, 8);
            Library.byteArrayCopy(Contador, wd[0]);
            if (!MifareIO.write(context, intent, wd, Defines.KEY_A, 4, 1))
                return false;
            Variables.Presentes++;
            Misc.SaveConfig();
            RecordStore record = null;
            try {
                byte[] datos = new byte [22];
                record = RecordStore.openRecordStore("Presente", true, Defines.OPEN_WRITE);
                datos[0]=_legajoAbreviado[0];
                datos[1]=_legajoAbreviado[1];
                Library.toIntelDataInt(horaAct, datos, 2);
                Library.byteArrayCopy(Cosechador, 0, datos, 6);
                record.addRecord(datos, 0, 22);
                record.closeRecordStore();

                String fname = "NLOG"+Integer.toString(Fecha.get(Calendar.YEAR))+
                        Library.padNum(Fecha.get(Calendar.MONTH)+1, 2)+
                        Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2);
                record = RecordStore.openRecordStore(fname, true, Defines.OPEN_WRITE);

                datos = new byte [11];
                datos[0] = 1;

                byte[] aux = new byte[4];
                Library.toIntelDataInt(Variables.DeviceID, aux, 0);
                aux = Library.arrayInvert(aux);
                Library.byteArrayCopy(aux, 0, datos, 1);

                datos[5]=_legajoAbreviado[0];
                datos[6]=_legajoAbreviado[1];

                Library.toIntelDataInt(Misc.GetClock(), aux, 0);
                aux = Library.arrayInvert(aux);
                Library.byteArrayCopy(aux, 0, datos, 7);
                record.addRecord(datos, 0, 11);
                record.closeRecordStore();

            }catch(Exception e){
                try {
                    if (record != null)
                        record.closeRecordStore();
                }catch (Exception ee){}
            }
        }
        WaitTime = 30;
        return true;
    }


    void refreshScreen()
    {
        String s = "Legajo: "+_legajo+"\n\r"+"Tachos/Cajas actual: "+_cantidad+"\n\r";
        s = s+"Total temporada: "+_total+"\n\r"+"Ultimo registro: "+_fecha+"\n\r";
        vPresentes.setText(""+Variables.Presentes);
        vCosechador.setText(_cosechador);
        vDetalle.setText(s);
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
        if (MifareIO.connect(this, intent)) {
            if (CardProcess(this, intent))
                refreshScreen();
        }
        MifareIO.disconnect();
    }
}
