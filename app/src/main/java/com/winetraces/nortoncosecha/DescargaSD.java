package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.winetraces.recordstore.RecordStore;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Enumeration;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DescargaSD extends AppCompatActivity {
    private ImageView mBackground;
    private String sBackground = null;
    private TextView vLog;
    public Context contexto;
    boolean cancelFlag, runFlag, endFlag;
    static int kk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int i, j;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_descarga_sd);
        mBackground = (ImageView) findViewById(R.id.DescargaSDMain);
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
        getImage("uploadSD.png");
        contexto = this;
        runFlag = true;
        cancelFlag = false;
        endFlag = false;

        Variables.msgTxt = "Guardando...\r\n";
        Thread th = new Thread(new Runnable() {
            String lastMsg = "";
            @Override
            public void run() {
                while (true) {
                    if (!runFlag)
                        break;
                    if (endFlag)
                        runFlag = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!Variables.msgTxt.equals(lastMsg)) {
                                vLog.setText(Variables.msgTxt);
                                lastMsg = Variables.msgTxt;
                                while(vLog.canScrollVertically(1))
                                    vLog.scrollBy(0,10);
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
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                saveSD();
            }
        });
        th.start();
    }

    public void saveSD()
    {
        int i, j, k;
        FileChannel fSrc, fDst;
        FileWriter fOut;
        String path;
        byte[] data;
        RecordStore record;
        ByteBuffer dd;

        Calendar Fecha = Library.Fecha(Misc.GetClock()*1000L);
        String dirBase = Environment.DIRECTORY_DOWNLOADS+"/Norton/";
        boolean error = false;
        File sBase = Environment.getExternalStoragePublicDirectory(dirBase);

        dirBase += Integer.toString(Fecha.get(Calendar.YEAR))+
                "-"+Library.padNum(Fecha.get(Calendar.MONTH)+1, 2)+
                "-"+Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2);
        File sd = Environment.getExternalStoragePublicDirectory(dirBase);
        File sdDesc = Environment.getExternalStoragePublicDirectory(dirBase+"/Descarga/");
        File sdComp = Environment.getExternalStoragePublicDirectory(dirBase+"/Completo/");
        if ((sBase != null) && (sd != null) && (sdDesc != null ) && (sdComp != null)) {
            if (!sBase.exists()) {
                if (!sBase.mkdir()) error = true;
            } else if (!sBase.isDirectory()) error = true;
            if (!sd.exists()) {
                if (!sd.mkdir()) error = true;
            } else if (!sd.isDirectory()) error = true;
            if (!sdDesc.exists()) {
                if (!sdDesc.mkdir()) error = true;
            } else if (!sdDesc.isDirectory()) error = true;
            if (!sdComp.exists()) {
                if (!sdComp.mkdir()) error = true;
            } else if (!sdComp.isDirectory()) error = true;
            if (!sBase.canWrite() || !sd.canWrite() || !sdDesc.canWrite() || !sdComp.canWrite()) error = true;
        } else error = true;
        if (error) {
            Variables.msgTxt += "Error acceso a carpetas...\r\n";
            return;
        }

        File currentDB = contexto.getDatabasePath("RecordStore"); //databaseName=your current application database name, for example "my_data.db"
        try {
            File backupDB = new File(sd, "Norton_RecordStore.sqlite"); // for example "my_data_backup.db"
            if (currentDB.exists()) {
                fSrc = new FileInputStream(currentDB).getChannel();
                fDst = new FileOutputStream(backupDB).getChannel();
                fDst.transferFrom(fSrc, 0, fSrc.size());
                fSrc.close();
                fDst.close();
                scanFile(backupDB.getAbsolutePath());
            }
            String ss[] = RecordStore.listRecordStores();
            if (ss == null) {
                Variables.msgTxt += "Error acceso a base de datos...\r\n";
                return;
            }
            for (i = 0; i < ss.length; i++) {
                Variables.msgTxt += "Guardando " + (i+1) + " de "+ss.length+" ("+ss[i]+")...\r\n";
                if ((ss[i].length()<12) || (!ss[i].substring(0, 4).equals("NLOG")))
                    path = sdComp.getAbsolutePath()+"/"+ss[i];
                else
                    path = sdDesc.getAbsolutePath()+"/"+ss[i];
                fOut = new FileWriter(path);
                BufferedWriter op = new BufferedWriter(fOut);
                //record = RecordStore.openRecordStore(ss[i], true, Defines.OPEN_READ);
                record = RecordStore.openRecordStoreBuffered(ss[i], Defines.OPEN_TEXT);
                int tot = record.getNumRecords();
                for (j=1; j<=tot; j++)
                {
                    /*data = record.getRecord(j);
                    String d = "";
                    for (k=0; k<data.length; k++)
                    {
                        d = d+Library.padHex(data[k]);

                    }
                    op.write(d);*/
                    op.write(record.getBuffSRecord(0));
                    op.newLine();
                }
                op.flush();
                op.close();
                record.closeRecordStore();
                scanFile(path);
                if (cancelFlag) {
                    break;
                }
                record = null;
            }
            Variables.msgTxt += "FIN...\r\n";
        } catch (Exception e) {
            Variables.msgTxt += "Error guardando datos...\r\n";
        }
        endFlag = true;
    }

    private void scanFile(String path)
    {
        MediaScannerConnection.scanFile(contexto, new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener(){
                    public void onScanCompleted (String path, Uri uri){
                        Log.d("TAG", "Finished scanning"+path);
                    }
                }
        );
    }

    public void salirClick(View target)
    {
        endActivity();
    }

    public void endActivity()
    {
        cancelFlag = true;
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
        Intent intent = new Intent(this, DescargaSD.class);
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
