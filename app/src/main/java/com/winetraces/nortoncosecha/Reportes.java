package com.winetraces.nortoncosecha;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
public class Reportes extends AppCompatActivity {
    private ImageView mBackground;
    private TextView view;
    private String sBackground = "";
    int _remitoInx = 0;
    public String txt[] = new String[512];
    public byte attrib[] = new byte[512];
    public String txtBuff[][] = new String[20][512];
    public byte attribBuff [][] = new byte[20][512];

    int prtInx = 0;
    private SpannableStringBuilder viewText = null;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int i, j;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reportes);
        mBackground = (ImageView) findViewById(R.id.reporteMain);
        view = (TextView)findViewById(R.id.textView);

        viewText = new SpannableStringBuilder();
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        switch(Variables.PrintType)
        {
            case Defines.RP_COSECHADOR:
                getImage("print_cosechador.png");
                break;
            case Defines.RP_CUADRILLA:
                getImage("print_cuadrilla.png");
                break;
            case Defines.RP_PRESENTISMO:
                getImage("print_presentismo.png");
                break;
            case Defines.RP_REMITO:
                getImage("print_remito.png");
                _remitoInx = Variables.RemitoInx;
                _remitoInx--;
                if (_remitoInx < 0)
                {
                    _remitoInx = 19;
                }
                String s = "RemitoBK"+_remitoInx;
                try {
                    RecordStore record = RecordStore.openRecordStore(s, true, Defines.OPEN_READ);
                    if (record.getNumRecords()> 0)
                        Reimpresion(s);
                    record.closeRecordStore();
                }catch (Exception e){}
                Variables.wProgress.cancel();
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
    }



    public void buttonLeftClick(View target)
    {
        if (Variables.PrintType != Defines.RP_REMITO)
            return;
        Library.keybeep();

        int _remitobk = _remitoInx;
        for (int i=0; i<20; i++) {
            _remitoInx--;
            if (_remitoInx < 0)
                _remitoInx = 19;
            if (_remitoInx == _remitobk)
                break;
            String s = "RemitoBK" + _remitoInx;
            RecordStore record = null;
            try {
                record = RecordStore.openRecordStore(s, true, Defines.OPEN_READ);
                if (record.getNumRecords() > 0) {
                    Reimpresion(s);
                    break;
                }
                else
                    _remitoInx = _remitobk;
            } catch (Exception e) {
            }
            try {
                record.closeRecordStore();
            } catch (Exception e) {
            }
        }
    }

    public void buttonRightClick(View target)
    {
        if (Variables.PrintType != Defines.RP_REMITO)
            return;
        Library.keybeep();
        int _remitobk = _remitoInx;
        for (int i=0; i<20; i++) {
            _remitoInx++;
            if (_remitoInx >= 20)
                _remitoInx = 0;
            if (_remitoInx == _remitobk)
                break;
            String s = "RemitoBK" + _remitoInx;
            RecordStore record = null;
            try {
                record = RecordStore.openRecordStore(s, true, Defines.OPEN_READ);
                if (record.getNumRecords() > 0) {
                    Reimpresion(s);
                    break;
                }
                else
                    _remitoInx = _remitobk;
            } catch (Exception e) {
            }
            try {
                record.closeRecordStore();
            } catch (Exception e) {
            }
        }
    }

    public void salirClick(View target)
    {
        Library.keybeep();
        onBackPressed();
    }

    public void imprimirClick(View target)
    {
        Library.keybeep();
        Print pr = new Print();
        (new Thread(pr)).start();
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

    private void view_list()
    {
        int off = 0;
        viewText.clear();
        for (int i=0; i<prtInx; i++)
        {
            if (attrib[i]==1)
                off = viewText.length();
            viewText.append(txt[i]);
            if (attrib[i]==1)
            {
                viewText.setSpan(new RelativeSizeSpan(2f), off, viewText.length(), 0);
            }
            viewText.append("\r\n");
        }
        view.setMovementMethod(new ScrollingMovementMethod());
        view.setText(viewText);
    }

    void Reimpresion(String Remito)
    {
        for (int i=0;i<512; i++)
            attrib[i] = 0;
        try {
            RecordStore record = RecordStore.openRecordStore(Remito, true, Defines.OPEN_READ);
            RecordEnumeration rd = record.enumerateRecords(null, null, false);
            prtInx = 0;
            while( rd.hasNextElement() )
            {
                int ID = rd.nextRecordId();
                byte[] rec = record.getRecord(ID);
                txt[prtInx++]= new String(rec);
            }
            record.closeRecordStore();
        }
        catch (Exception e){}
        prt_footer();
        save_prtbuff();
        view_list();
    }

    void save_prtbuff()
    {
        try {
            RecordStore.deleteRecordStore("printBuffer");
        }catch (Exception e){}
        try {
            RecordStore record = RecordStore.openRecordStore("printBuffer", true, Defines.OPEN_WRITE);
            record.addRecord(attrib, 0, 512);
            for (int i=0; i<prtInx; i++)
            {
                record.addRecord(txt[i].getBytes(), 0, txt[i].length());
            }
            record.closeRecordStore();
        }catch (Exception e){}

    }

    void prt_hdr(String Cuadrilla)
    {
        Calendar Hoy = Library.Fecha(Misc.GetClock()*1000L);
        txt[prtInx++]=" ";
        txt[prtInx++]="Bodega Norton S.A.  Cosecha "+Hoy.get(Calendar.YEAR);
        txt[prtInx++]=" ";
        txt[prtInx++]="       Cuadrilla - "+Cuadrilla;
        txt[prtInx++]=" ";
        txt[prtInx++]=" Fecha: "+Library.padNum(Hoy.get(Calendar.DAY_OF_MONTH),2)+"/"+
                Defines.meses[Hoy.get(Calendar.MONTH)]+"/"+Hoy.get(Calendar.YEAR);
        txt[prtInx++]=Defines.prtLine;
    }

    void prt_footer()
    {
        Calendar Hoy = Library.Fecha(Misc.GetClock()*1000L);
        txt[prtInx++]=" ";
        txt[prtInx++]="Impreso: "+Library.padNum(Hoy.get(Calendar.DAY_OF_MONTH),2)+"/"+
                (Defines.meses[Hoy.get(Calendar.MONTH)])+"/"+Hoy.get(Calendar.YEAR)+"  "+
                Library.padNum(Hoy.get(Calendar.HOUR_OF_DAY),2)+":"+
                Library.padNum(Hoy.get(Calendar.MINUTE),2)+":"+
                Library.padNum(Hoy.get(Calendar.SECOND),2);
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
        super.onNewIntent(intent);
        MifareIO.disconnect();
    }
}
