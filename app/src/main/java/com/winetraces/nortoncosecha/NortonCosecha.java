package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.InputStream;


public class NortonCosecha extends AppCompatActivity {

    private View mMainView;
    private ImageView mBackground;
    private TextView mNombre, mCount, mLegajo;
    private TextView count1, count2, count3, count4, count5, count6;
    private Button setPrograma;
    private ProgressBar mCosechadorTimer;
    private Handler handler = new Handler();
    private String sBackground;
    private int prgInx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Library.LogMem("On create0 ");

        setContentView(R.layout.activity_nortoncosecha);
        mMainView = findViewById(R.id.mainScreen);
        mBackground = (ImageView) findViewById(R.id.background);
        getImage("wait_programa.png");

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
        Variables.DeviceID = Build.SERIAL.hashCode(); //706F36D6
        String s = Build.SERIAL;
        SetPrograma();
       // Save_SD.save(this);
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
                                 horaAct = NortonCosecha.GetClock();
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
                refreshScreenCosecha();
                goodBeep();
            }
            else { badBeep(); }
        }
        else {
            badBeep();
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
                if (!sBackground.equals("cosechando_tacho.png"))
                    getImage("cosechando_tacho.png");
                mBackground.setVisibility(View.VISIBLE);
            }
            else {
                if (!sBackground.equals("cosechando_caja.png"))
                    getImage("cosechando_caja.png");
                mBackground.setVisibility(View.VISIBLE);
            }
            setPrograma.setText(Variables.Programa);
        }
        else {
            mBackground.setVisibility(View.VISIBLE);
            setPrograma.setText(R.string.program_name);
        }
    }

    public void refreshScreenCosecha()
    {
        if (!Variables.bFlagCosecha)
            return;
        mNombre.setText(Variables.Cosechador_name);
        mCount.setText(Variables.Cosechador_count);
        mLegajo.setText(Variables.Cosechador_legajo);
        if (Variables.ModoCosecha == Defines.MODO_TACHO)
        {
            count1.setText(Library.padNum(Variables.TachoCajaCnt,3));
            count2.setText(Library.padNum(Variables.Tachos4Bin,3));
            count3.setText(Library.padNum(Variables.TotalTachos,3));
            count4.setText(Library.padNum(Variables.CamionCnt,3));
            count5.setText(Library.padNum(Variables.Bin4Camion,3));
            count6.setText(Library.padNum(Variables.BinCnt,3));
        }
        else
        {
            count1.setText(Library.padNum(Variables.TachoCajaCnt,3));
            count2.setText(Library.padNum(Variables.Cajas4Pallet,3));
            count3.setText(Library.padNum(Variables.TotalTachos,3));
            count4.setText(Library.padNum(Variables.CamionCnt,3));
            count5.setText(Library.padNum(Variables.Pallet4Camion,3));
            count6.setText(Library.padNum(Variables.BinCnt,3));
        }
        setPrograma.setText(Variables.Programa);
        if  (Variables.CosechadorLastTime > 0) {
            if (mCosechadorTimer.getVisibility() == View.INVISIBLE)
                mCosechadorTimer.setVisibility(View.VISIBLE);
            int horaAct = NortonCosecha.GetClock();
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
                        /*Print pr = new Print();
                        (new Thread(pr)).start();*/
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

    public static int GetClock()
    {
        return (int)(System.currentTimeMillis()/1000)+Variables.DiffClock;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void goodBeep()
    {
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
        //tone.startTone(ToneGenerator.TONE_PROP_ACK);
        tone.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT);
        try {
            Thread.sleep(400);
        }catch (InterruptedException e)
        {
            return;
        }
        tone.release();
    }

    public void badBeep()
    {
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
        //tone.startTone(ToneGenerator.TONE_PROP_NACK);
        tone.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE);
        try {
            Thread.sleep(300);
        }catch (InterruptedException e)
        {
            return;
        }
        tone.release();
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

    public void SetPrograma()
    {
        int x,i;
        RecordStore record;

        try {
            record = RecordStore.openRecordStore("Config", true);
            x = record.getNumRecords();
            byte[] datos = new byte[300];
            if (x!=0)
                datos = record.getRecord(1);
            record.closeRecordStore();
            byte c[]=new byte[10];
            Library.byteArrayCopy(datos, 31, c, 0);
            Variables.Cuadrilla = new String(c);
            if ((x == 0) || (datos[11] != Defines.CONFIG_VERSION))
            {
                RecordStore.initializeRecordStore(this, true);
                Variables.ProgInx = 0;
                Variables.DiffTime = Defines.COSECHADOR_TIMEOUT;
                Variables.FechaProg = 0;
                Variables.DiffClock = 0;
                Variables.Presentes = 0;
                Variables.ModoCosecha = 0;
                Variables.TachoCajaCnt = 0;
                Variables.TotalTachos = 0;
                Variables.Tachos4Bin = 16;
                Variables.Bin4Camion = 24;
                Variables.Cajas4Pallet = 32;
                Variables.Pallet4Camion = 10;
                Variables.AlarmTimeOut = 5000;
                Variables.BinCnt = 1;
                Variables.CamionCnt = 0;
                Variables.logInx = 0;
                Variables.Viaje = 0;
                Variables.RemitoInx = 0;
                Variables.Cuadrilla = "NINGUNO   ";
                Variables.ProgSel = false;
                //Variables.sWiFiURL = "192.168.0." + (Variables.DeviceID & 255);
                Variables.sWiFiURL = "192.168.0.32";
                //sWebServiceURL = "vendimia.norton.com.ar";
                //Variables.sWebServiceURL = "192.168.50.9";
                Variables.sWebServiceURL = "192.185.17.39";
                Variables.sMailAddr = "";
                SaveConfig();
            }
            else
            {
                Variables.ProgInx = datos[0];
                Variables.DiffTime = Library.fromIntelDataWord(datos, 1);
                Variables.FechaProg = Library.fromIntelDataWord(datos, 3);
                Variables.DiffClock = Library.fromIntelDataIntLE(datos, 5);
                if (Variables.DiffClock > 86400)
                    Variables.DiffClock = 0;
                Variables.Presentes = Library.fromIntelDataWord(datos, 9);
                Variables.ProgSel = (datos[12]==0)?false:true;
                Variables.Tachos4Bin = Library.fromIntelDataWord(datos, 13);
                Variables.Bin4Camion = Library.fromIntelDataWord(datos, 15);
                Variables.TachoCajaCnt = Library.fromIntelDataWord(datos, 17);
                Variables.BinCnt = Library.fromIntelDataWord(datos, 19);
                Variables.CamionCnt = Library.fromIntelDataWord(datos, 21);
                Variables.TotalTachos = Library.fromIntelDataWord(datos, 23);
                Variables.Cajas4Pallet = Library.fromIntelDataWord(datos, 25);
                Variables.Pallet4Camion = Library.fromIntelDataWord(datos, 27);
                Variables.AlarmTimeOut = Library.fromIntelDataWord(datos, 29);
                Variables.logInx = datos[41];
                Variables.Viaje = datos[42];
                Variables.RemitoInx = datos[43];
                x = datos[100];
                byte[]aux = new byte[x];
                for(i=0; i<x; i++)
                    aux[i]=datos[101+i];
                Variables.sWiFiURL = new String(aux);
                x = datos[150];
                aux = new byte[x];
                for(i=0; i<x; i++)
                    aux[i]=datos[151+i];
                Variables.sWebServiceURL = new String(aux);
                x = datos[200];
                if (x == 0)
                    Variables.sMailAddr = "";
                else
                {
                    aux = new byte[x];
                    for(i=0; i<x; i++)
                        aux[i]=datos[201+i];
                    Variables.sMailAddr = new String(aux);
                }
            }
            int horaAct = GetClock() / 86400;
            if (Variables.FechaProg != horaAct)
            {
                ResetConfig();
            }
            if (Variables.ProgSel)
            {
                try {
                    record = RecordStore.openRecordStore("Programas", true);
                    int cnt = record.getNumRecords();
                    if (Variables.ProgInx >= cnt)
                        Variables.ProgInx = 0;
                    datos = record.getRecord(Variables.ProgInx+1);
                    String tx[] = new String [10];
                    NortonCosecha.splitPrograma(datos, tx);
                    Variables.ProgID = tx[0];
                    Variables.Finca = tx[2];
                    Variables.Cuartel = tx[3];
                    Variables.Area = tx[4];
                    Variables.CuadrillaPrg = tx[5];
                    Variables.Programa = tx[7];
                    Variables.ModoCosecha = Integer.parseInt(tx[8]);
                    Variables.VariedadUva = tx[9];
                    if (Variables.ModoCosecha == 1)
                        Variables.Programa+="-CAJAS";
                    else
                        Variables.Programa+="-BINES";

                }catch (Exception e){};
                try {
                    record.closeRecordStore();
                }catch (Exception e){};
            }
            else
            {
                Variables.Finca = "Ninguno";
                Variables.Cuartel = "0001";
                Variables.Area = "Ninguno";
                Variables.CuadrillaPrg = "Ninguno";
                Variables.Programa = "NO DEFINIDO";
                Variables.ModoCosecha = 0;
                Variables.VariedadUva = "Generico";
            }
        }catch(Exception e){}
        Variables.Cosechador_name = " ";
        Variables.Cosechador_count= " ";
        Variables.Cosechador_legajo = " ";
        Variables.CosechadorLastTime = -1;
    }

    public static void ResetConfig()
    {
        int horaAct = GetClock() / 86400;
        InitProgramFile();
        Variables.FechaProg = horaAct;
        Variables.Presentes = 0;
        Variables.TachoCajaCnt = 0;
        Variables.TotalTachos = 0;
        Variables.BinCnt = 1;
        Variables.CamionCnt = 0;
        if (Variables.logInx < 99)
            Variables.logInx++;
        else
            Variables.logInx = 0;
        String fname = "RECORD"+Integer.toString(Variables.logInx);
        try {
            RecordStore.deleteRecordStore(fname);
        }catch (Exception e){}
        Variables.Viaje = 0;
        SaveConfig();
    }

    public static void SaveConfig()
    {
        byte[] datos = new byte[400];
        int i;

        datos[0] = Variables.ProgInx;
        datos[1] = (byte)(Variables.DiffTime & 255);
        datos[2] = (byte)(Variables.DiffTime / 256);
        datos[3] = (byte)(Variables.FechaProg & 255);
        datos[4] = (byte)(Variables.FechaProg / 256);
        Library.toIntelDataInt(Variables.DiffClock, datos, 5);
        datos[9] = (byte)(Variables.Presentes & 255);
        datos[10] = (byte)(Variables.Presentes / 256);
        datos[11] = Defines.CONFIG_VERSION;
        datos[12] = (Variables.ProgSel) ? (byte)1 : 0;
        datos[13] = (byte)(Variables.Tachos4Bin & 255);
        datos[14] = (byte)(Variables.Tachos4Bin / 256);
        datos[15] = (byte)(Variables.Bin4Camion & 255);
        datos[16] = (byte)(Variables.Bin4Camion / 256);
        datos[17] = (byte)(Variables.TachoCajaCnt & 255);
        datos[18] = (byte)(Variables.TachoCajaCnt / 256);
        datos[19] = (byte)(Variables.BinCnt & 255);
        datos[20] = (byte)(Variables.BinCnt / 256);
        datos[21] = (byte)(Variables.CamionCnt & 255);
        datos[22] = (byte)(Variables.CamionCnt / 256);
        datos[23] = (byte)(Variables.TotalTachos & 255);
        datos[24] = (byte)(Variables.TotalTachos / 256);
        datos[25] = (byte)(Variables.Cajas4Pallet & 255);
        datos[26] = (byte)(Variables.Cajas4Pallet / 256);
        datos[27] = (byte)(Variables.Pallet4Camion & 255);
        datos[28] = (byte)(Variables.Pallet4Camion / 256);
        datos[29] = (byte)(Variables.AlarmTimeOut & 255);
        datos[30] = (byte)(Variables.AlarmTimeOut / 256);

        byte[] c = Variables.Cuadrilla.getBytes();
        Library.byteArrayCopy(c, 0, datos, 31, 10);

        datos[41] = (byte)Variables.logInx;
        datos[42] = (byte)Variables.Viaje;
        datos[43] = (byte)Variables.RemitoInx;

        byte[]aux = Variables.sWiFiURL.getBytes();
        datos[100] = (byte)aux.length;
        for (i=0; i<aux.length; i++)
        {
            datos[101+i]=aux[i];
            if (i >= 49)
                break;
        }
        aux = Variables.sWebServiceURL.getBytes();
        datos[150]=(byte)aux.length;
        for (i=0; i<aux.length;i++)
        {
            datos[151+i]=aux[i];
            if (i>=49)
                break;
        }
        if (Variables.sMailAddr.length()>0)
        {
            aux = Variables.sMailAddr.getBytes();
            datos[200]=(byte)aux.length;
            for (i=0; i<aux.length;i++)
            {
                datos[201+i]=aux[i];
                if (i>=49)
                    break;
            }
        }
        else
            datos[200]=0;
        try {
            RecordStore record = RecordStore.openRecordStore("Config", true);
            if (record.getNumRecords() == 0)
                record.addRecord(datos, 0, 300);
            else
                record.setRecord(1, datos, 0, 300);
            record.closeRecordStore();
        }catch(Exception e){}
    }


    public static void InitProgramFile()
    {
        int i, j;
        RecordStore record;

        String progDef[][]={
                {"0000AA", "NORTON  ", "0001", "NINGUNO ", "Generica", "  1", "0", "Generica"},
                {"0000BB", "NORTON  ", "0002", "NINGUNO ", "Generica", "  1", "0", "Generica"},
                {"0000CC", "NORTON  ", "0003", "NINGUNO ", "Generica", "  1", "0", "Generica"},
                {"0000DD", "NORTON  ", "0004", "NINGUNO ", "Generica", "  1", "1", "Generica"},
                {"0000EE", "NORTON  ", "0005", "NINGUNO ", "Generica", "  1", "1", "Generica"},
                {"0000FF", "NORTON  ", "0006", "NINGUNO ", "Generica", "  1", "1", "Generica"},
        };

        try {
            RecordStore.deleteRecordStore("Programas");
            RecordStore.deleteRecordStore("CamionReg");
        }catch(Exception e){}
        try {
            record = RecordStore.openRecordStore("Programas", true);
            for (i=0; i<6; i++)
            {
                int x = 0;
                byte[] datos = new byte[Defines.PRG_LEN];
                for (j=0; j<8; j++)
                {
                    byte aux[] = progDef[i][j].getBytes();
                    Library.byteArrayCopy(aux, 0, datos, x);
                    x += progDef[i][j].length();
                }
                record.addRecord(datos, 0, Defines.PRG_LEN);
            }
            record.closeRecordStore();
        }catch (Exception e){}
        Variables.Finca = "Ninguno";
        Variables.Cuartel = "0001";
        Variables.ModoCosecha = 0;
        Variables.CuadrillaPrg = "Ninguno";
        Variables.Programa = "NO DEFINIDO";
        Variables.Area = "Ninguno";
        Variables.VariedadUva = "Generica";
        Variables.ProgInx = 0;
        Variables.ProgSel = false;
        Variables.Cosechador_name = " ";
        Variables.Cosechador_count= " ";
        Variables.Cosechador_legajo = " ";
        Variables.CosechadorLastTime = -1;
    }

    public static void splitPrograma(byte[] datos, String txt[])
    {
        byte prog[]= new byte[6];
        boolean flag = false;

        Library.byteArrayCopy(datos, prog);
        String s = new String(prog);
        txt[0] = s;
        switch (s) {
            case "0000AA":
                s = "1er CUARTEL";
                break;
            case "0000BB":
                s = "2do CUARTEL";
                break;
            case "0000CC":
                s = "3er CUARTEL";
                break;
            case "0000DD":
                s = "4to CUARTEL";
                break;
            case "0000EE":
                s = "5to CUARTEL";
                break;
            case "0000FF":
                s = "6to CUARTEL";
                break;
            default:
                flag = true;
                s = "PRG"+s;
                break;
        }
        txt[1] = s;

        byte Finca[] = new byte[8];
        Library.byteArrayCopy(datos, 6, Finca, 0);
        txt[2] = new String(Finca);
        byte cuartel[] = new byte[4];
        Library.byteArrayCopy(datos, 14, cuartel, 0);
        txt[3] = new String(cuartel);
        if (flag)
            txt[7]=txt[2]+"-"+txt[3];
        else
            txt[7]=s;
        byte area[] = new byte[8];
        Library.byteArrayCopy(datos, 18, area, 0);
        txt[4] = new String(area);
        byte CuadrillaPrg[]=new byte[8];
        Library.byteArrayCopy(datos, 26, CuadrillaPrg, 0);
        txt[5] = new String(CuadrillaPrg);
        byte cantidad[]=new byte[3];
        Library.byteArrayCopy(datos, 34, cantidad, 0);
        txt[6] = new String(cantidad);
        byte modoCosecha[]=new byte[1];
        Library.byteArrayCopy(datos, 37, modoCosecha, 0);
        txt[8] = new String(modoCosecha);
        if (modoCosecha[0]== '1')
            txt[1]+="-CAJAS";
        else
            txt[1]+="-BINES";
        byte variedadUva[]=new byte[8];
        Library.byteArrayCopy(datos, 38, variedadUva, 0);
        txt[9] = new String(variedadUva);
    }
}