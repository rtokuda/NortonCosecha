package com.winetraces.nortoncosecha;


import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.winetraces.recordstore.RecordEnumeration;
import com.winetraces.recordstore.RecordStore;

import java.io.InputStream;
import java.util.Calendar;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Reportes extends AppCompatActivity {
    private ImageView mBackground;
    private RelativeLayout mainFrame;
    private TextView view;
    private String sBackground = "";
    int _remitoInx = 0;
    int _remitoCnt = 0;
    public String txt[] = new String[512];
    public byte attrib[] = new byte[512];
    public String txtBuff[][] = new String[Defines.MAX_REPORTES][512];
    public byte attribBuff[][] = new byte[Defines.MAX_REPORTES][512];
    private WebView waitTagView;

    int prtInx = 0;
    private SpannableStringBuilder viewText = null;
    public static ProgressDialog wProgress;
    private Handler handler = new Handler();
    private final Handler mHideHandler = new Handler();
    String _cosechador;
    String _legajo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int i, j;
        super.onCreate(savedInstanceState);
        Thread th;

        setContentView(R.layout.activity_reportes);
        mBackground = (ImageView) findViewById(R.id.reporteMain);
        mainFrame = (RelativeLayout) findViewById(R.id.mainFrame);
        view = (TextView) findViewById(R.id.textView);
        Variables.pContext = this;
        viewText = new SpannableStringBuilder();
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        waitTagView = (WebView) findViewById(R.id.waitCard);
        waitTagView.setVisibility(View.INVISIBLE);
        wProgress = new ProgressDialog(this);
        wProgress.setCancelable(false);
        wProgress.setMessage("...Un momento");
        wProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        wProgress.setIndeterminate(true);

        switch (Variables.PrintType) {
            case Defines.RP_COSECHADOR:
                getImage("print_cosechador.png");
                mainFrame.setAlpha(0.2F);
                waitTagView.loadUrl("file:///android_asset/wait_for_tag.html");
                waitTagView.setVisibility(View.VISIBLE);
                waitTagView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        waitTagView.setVisibility(View.INVISIBLE);
                        mainFrame.setAlpha(1F);
                        return false;
                    }
                });
                break;
            case Defines.RP_CUADRILLA:
                getImage("print_cuadrilla.png");
                wProgress.show();
                th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        PrintCuadrilla();
                                    }
                                });
                            }
                        });
                    }
                });
                th.start();
                break;
            case Defines.RP_PRESENTISMO:
                getImage("print_presentismo.png");
                Presentismo();
                break;
            case Defines.RP_REMITO:
                wProgress.show();
                getImage("print_remito.png");
                th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        readRemitos();
                                        if (_remitoCnt > 0) {
                                            _remitoInx = Variables.RemitoInx;
                                            _remitoInx--;
                                            if (_remitoInx < 0) {
                                                _remitoInx = Defines.MAX_REPORTES - 1;
                                            }
                                            Reimpresion();
                                            wProgress.cancel();
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
                th.start();
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //mHideHandler.postDelayed(mHideRunnable, 500);
    }

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mHideHandler.removeCallbacks(mHideRunnable);
            if (Variables.PrintType == Defines.RP_REMITO) {
                readRemitos();
                if (_remitoCnt > 0) {
                    _remitoInx = Variables.RemitoInx;
                    _remitoInx--;
                    if (_remitoInx < 0) {
                        _remitoInx = Defines.MAX_REPORTES - 1;
                    }
                    Reimpresion();
                }
            }
            wProgress.cancel();
        }
    };

    private void readRemitos() {
        int i;
        String s;
        RecordStore rc;

        _remitoCnt = 0;
        for (i = 0; i < Defines.MAX_REPORTES; i++)
            txtBuff[i][0] = null;
        for (i = 0; i < Defines.MAX_REPORTES; i++) {
            s = "RemitoBK" + i;
            rc = RecordStore.openRecordStore(s, false, Defines.OPEN_READ);
            if (rc != null) {
                RecordEnumeration rd = rc.enumerateRecords(null, null, false);
                int j = 0;
                while (rd.hasNextElement()) {
                    int ID = rd.nextRecordId();
                    byte[] rec = rc.getRecord(ID);
                    txtBuff[i][j++] = new String(rec);
                }
                txtBuff[i][j] = null;
                _remitoCnt++;
                rc.closeRecordStore();
            }
        }
    }

    public void buttonLeftClick(View target) {
        if (Variables.PrintType != Defines.RP_REMITO)
            return;
        Library.keybeep();

        int _remitobk = _remitoInx;
        for (int i = 0; i < Defines.MAX_REPORTES; i++) {
            _remitoInx--;
            if (_remitoInx < 0)
                _remitoInx = Defines.MAX_REPORTES - 1;
            if (txtBuff[_remitoInx][0] != null)
                break;
            if (_remitoInx == _remitobk)
                break;
        }
        Reimpresion();
    }

    public void buttonRightClick(View target) {
        if (Variables.PrintType != Defines.RP_REMITO)
            return;
        Library.keybeep();
        int _remitobk = _remitoInx;
        for (int i = 0; i < Defines.MAX_REPORTES; i++) {
            _remitoInx++;
            if (_remitoInx >= Defines.MAX_REPORTES)
                _remitoInx = 0;
            if (txtBuff[_remitoInx][0] != null)
                break;
            if (_remitoInx == _remitobk)
                break;
        }
        Reimpresion();
    }

    public void salirClick(View target) {
        Library.keybeep();
        onBackPressed();
    }

    public void imprimirClick(View target) {
        final Context contexto = this;

        Library.keybeep();
        wProgress = new ProgressDialog(this);
        wProgress.setCancelable(false);
        wProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        wProgress.setIndeterminate(true);
        wProgress.setMessage("Imprimiendo...");
        wProgress.show();
        new AsyncPrint().execute();
    }

    private void getImage(String file) {
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

    private void view_list() {
        int off = 0;
        viewText.clear();
        for (int i = 0; i < prtInx; i++) {
            if (attrib[i] == 1)
                off = viewText.length();
            viewText.append(txt[i]);
            if (attrib[i] == 1) {
                viewText.setSpan(new RelativeSizeSpan(2f), off, viewText.length(), 0);
            }
            viewText.append("\r\n");
        }
        view.setMovementMethod(new ScrollingMovementMethod());
        view.setText(viewText);
    }

    void Reimpresion() {
        int i;

        for (i = 0; i < 512; i++)
            attrib[i] = 0;
        i = 0;
        prtInx = 0;
        for (; ; ) {
            if (txtBuff[_remitoInx][i] == null)
                break;
            txt[prtInx++] = txtBuff[_remitoInx][i++];
        }
        prt_footer();
        //save_prtbuff();
        view_list();
    }

    void Presentismo() {
       // Misc.getPresentes();
        prt_hdr(Variables.Cuadrilla);
        txt[prtInx++] = "Resumen de presentismo";
        txt[prtInx++] = "Total de cosechadores registrados";
        attrib[prtInx] = 1;
        txt[prtInx++] = "       " + Integer.toString(Variables.Presentes);
        prt_footer();
        save_prtbuff();
        view_list();
    }

    void save_prtbuff() {
        try {
            RecordStore.deleteRecordStore("printBuffer");
        } catch (Exception e) {
        }
        try {
            RecordStore record = RecordStore.openRecordStore("printBuffer", true, Defines.OPEN_WRITE);
            record.addRecord(attrib, 0, 512);
            for (int i = 0; i < prtInx; i++) {
                record.addRecord(txt[i].getBytes(), 0, txt[i].length());
            }
            record.closeRecordStore();
        } catch (Exception e) {
        }

    }

    void prt_hdr(String Cuadrilla) {
        Calendar Hoy = Library.Fecha(Misc.GetClock() * 1000L);
        txt[prtInx++] = " ";
        txt[prtInx++] = "Bodega Norton S.A.  Cosecha " + Hoy.get(Calendar.YEAR);
        txt[prtInx++] = " ";
        txt[prtInx++] = "       Cuadrilla - " + Cuadrilla;
        txt[prtInx++] = " ";
        txt[prtInx++] = " Fecha: " + Library.padNum(Hoy.get(Calendar.DAY_OF_MONTH), 2) + "/" +
                Defines.meses[Hoy.get(Calendar.MONTH)] + "/" + Hoy.get(Calendar.YEAR);
        txt[prtInx++] = Defines.prtLine;
    }

    void prt_footer() {
        Calendar Hoy = Library.Fecha(Misc.GetClock() * 1000L);
        txt[prtInx++] = " ";
        txt[prtInx++] = "Impreso: " + Library.padNum(Hoy.get(Calendar.DAY_OF_MONTH), 2) + "/" +
                (Defines.meses[Hoy.get(Calendar.MONTH)]) + "/" + Hoy.get(Calendar.YEAR) + "  " +
                Library.padNum(Hoy.get(Calendar.HOUR_OF_DAY), 2) + ":" +
                Library.padNum(Hoy.get(Calendar.MINUTE), 2) + ":" +
                Library.padNum(Hoy.get(Calendar.SECOND), 2);
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
        Intent intent = new Intent(this, Reportes.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        byte[] CardId;
        byte[] authent_card = {(byte)0x01, (byte)0x4E, (byte)0x52, (byte)0x54, (byte)0x31, (byte)0x01};
        boolean err = false;
        super.onNewIntent(intent);
        if ((Variables.PrintType == Defines.RP_COSECHADOR) && MifareIO.connect(this, intent)) {

            if (MifareIO.read(this, intent, Defines.KEY_A, 1, 2)) {
                CardId = MifareIO.ReadBuff[0];
                for (int i = 0; i < 6; i++) {
                    if (CardId[i] != authent_card[i]) {
                        err = true;
                        break;
                    }
                }
                if (!err) {
                    byte[] Cosechador = new byte[16];
                    Library.byteArrayCopy(MifareIO.ReadBuff[1], Cosechador);
                    if (MifareIO.read(this, intent, Defines.KEY_A, 4, 2)) {
                        byte[] Legajo = new byte[16];
                        Library.byteArrayCopy(MifareIO.ReadBuff[1], Legajo);
                        byte[] L = new byte[6];
                        Library.byteArrayCopy(Legajo, 0, L, 0);
                        _legajo = new String(L).toString();
                        _cosechador = Library.String(Cosechador);
                        waitTagView.setVisibility(View.INVISIBLE);
                        mainFrame.setAlpha(1F);
                        wProgress.show();
                        Thread th = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                PrintCosechador();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        th.start();
                    }
                }
            }
        }
        MifareIO.disconnect();
    }

    void PrintCosechador() {
        int i, j;
        int total;

        String[] Fincas = new String[50];
        String[] Cuarteles = new String[50];
        String[] Areas = new String[50];
        String[] Programas = new String[50];
        String[] Variedades = new String[50];
        int[] Fichas = new int[50];
        int prgInx = 0;
        total = 0;
        prtInx = 0;
        for (i = 0; i < 50; i++) {
            Programas[i] = null;
            Fichas[i] = 0;
        }
        for (i = 0; i < 512; i++)
            attrib[i] = 0;
        String fname = "RECORD" + Integer.toString(Variables.logInx);
        RecordStore record = null;
        try {
            record = RecordStore.openRecordStore(fname, true, Defines.OPEN_READ);
            RecordEnumeration rd = record.enumerateRecords(null, null, false);
            while (rd.hasNextElement()) {
                int ID = rd.nextRecordId();
                byte[] rec = record.getRecord(ID);
                String sLegajo = new String(Library.getField(rec, Defines.LEGAJO, true));
                if (!_legajo.equals(sLegajo))
                    continue;
                String Programa = new String(Library.getField(rec, Defines.PROGRAMA, true));
                String Variedad = new String(Library.getField(rec, Defines.VARIEDAD, true));
                String Finca = new String(Library.getField(rec, Defines.FINCA, true));
                String Cuartel = new String(Library.getField(rec, Defines.CUARTEL, true));
                String Area = new String(Library.getField(rec, Defines.AREA, true));
                for (i = 0; i < prgInx; i++) {
                    if (Programas[i] == null)
                        break;
                    if (Programas[i].equals(Programa))
                        break;
                }
                if (i >= prgInx) {
                    Programas[prgInx] = Programa;
                    Variedades[prgInx] = Variedad;
                    Fincas[prgInx] = (Finca + "        ").substring(0, 8);
                    Cuarteles[prgInx] = (Cuartel + "    ").substring(0, 4);
                    Areas[prgInx] = (Area + "        ").substring(0, 8);
                    Fichas[prgInx] = 1;
                    prgInx++;
                } else
                    Fichas[i]++;
                total++;
            }
        } catch (Exception e) {
        }
        if (record != null) {
            try {
                record.closeRecordStore();
            } catch (Exception e) {
            }
        }
        prt_hdr(Variables.Cuadrilla);
        if (_legajo != null) {
            txt[prtInx++] = _cosechador + " " + _legajo;
            txt[prtInx++] = Defines.prtLine;
        }
        txt[prtInx++] = "Programa  Variedad";
        txt[prtInx++] = "Finca  Cuartel  Area   Fichas";
        txt[prtInx++] = Defines.prtLine;
        for (j = 0; j < prgInx; j++) {
            txt[prtInx++] = Programas[j] + "  " + Variedades[j];
            txt[prtInx++] = Fincas[j] + " " + Cuarteles[j] + " " + Areas[j] + "   " + Library.padNum(Fichas[j], 4);
        }
        txt[prtInx++] = " ";
        txt[prtInx++] = Defines.prtLine;
        txt[prtInx++] = "Total                    " + Library.padNum(total, 4);
        txt[prtInx++] = Defines.prtLine;
        prt_footer();
        save_prtbuff();
        view_list();
        wProgress.cancel();
    }


    void PrintCuadrilla()
    {
        int i, j;
        int total;

        String[] Legajo = new String[100];
        String[] Nombre = new String[100];
        int[] Fichas = new int[100];
        int inx = 0;
        total = 0;
        prtInx = 0;
        for (i=0; i<100; i++)
        {
            Legajo[i]=null;
            Fichas[i] = 0;
        }
        for (i=0;i<512; i++)
            attrib[i] = 0;
        String fname = "RECORD"+Integer.toString(Variables.logInx);
        RecordStore record = null;
        try {
            record = RecordStore.openRecordStore(fname, true, Defines.OPEN_READ);
            RecordEnumeration rd = record.enumerateRecords(null, null, false);
            while( rd.hasNextElement() )
            {
                int ID = rd.nextRecordId();
                byte[] rec = record.getRecord(ID);
                String sLegajo = new String(Library.getField(rec, Defines.LEGAJO, true));
                String sNombre = new String(Library.getField(rec, Defines.NOMBRE, true));
                for (i=0; i<inx; i++)
                {
                    if (Legajo[i]==null)
                        break;
                    if (Legajo[i].equals(sLegajo))
                        break;
                }
                if (i >= inx)
                {
                    Legajo[inx]=sLegajo;
                    Nombre[inx]=sNombre;
                    Fichas[i]=1;
                    inx++;
                }
                else
                    Fichas[i]++;
                total++;
            }
        } catch (Exception e){}
        if (record != null)
        {
            try {
                record.closeRecordStore();
            }catch (Exception e){}
        }
        prt_hdr(Variables.Cuadrilla);
        txt[prtInx++]="Legajo      Nombre         Fichas";
        txt[prtInx++]=Defines.prtLine;
        for (j=0; j<inx; j++)
        {
            String s =(Legajo[j]+"        ").substring(0,8);
            s = s+(Nombre[j]+"                  ").substring(0,18);
            txt[prtInx++] = s+"  "+Library.padNum(Fichas[j], 4);
        }
        txt[prtInx++]=" ";
        txt[prtInx++]=Defines.prtLine;
        txt[prtInx++]="Total                "+Library.padNum(total, 4);
        int prm = total * 100;
        if (inx != 0)
            prm = prm / inx;

        txt[prtInx++]="Promedio             "+Library.NumFormat(Integer.toString(prm));
        txt[prtInx++]=Defines.prtLine;
        prt_footer();
        save_prtbuff();
        view_list();
        wProgress.cancel();
    }

}

class AsyncPrint extends AsyncTask<Void, String, Integer>
{
    @Override
    protected Integer doInBackground(Void...arg0) {
        publishProgress("MSG","Imprimiendo...");
        Print pr = new Print();
        int err = pr.imprimir();
        if ((err == 1) || (err == 2))
            publishProgress("ERR","WiFi Impresora");
        if (err == 3)
            publishProgress("ERR","Conexion Impresora");
        return 0;
    }
    @Override
    protected void onProgressUpdate(String...msg){
        if (msg[0].equals("MSG")) {
            if (!Reportes.wProgress.isShowing())
                Reportes.wProgress.show();
            Reportes.wProgress.setMessage(msg[1]);
        }
        if (msg[0].equals("ERR"))
            Library.alert(Variables.pContext, "ERROR", msg[1], android.R.drawable.ic_dialog_alert);
    }

    @Override
    protected void onPostExecute(Integer arg0) {
        super.onPostExecute(arg0);
        Reportes.wProgress.cancel();
    }
}
