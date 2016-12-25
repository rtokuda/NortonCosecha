package com.winetraces.nortoncosecha;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.winetraces.recordstore.RecordStore;
import java.io.InputStream;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class InitCard extends AppCompatActivity {
    private ImageView mBackground;
    private RelativeLayout mainFrame;
    private EditText edit;
    private TextView text1, text2, text3;
    private String sBackground = "";
    private Button editClick, grabar;
    private InputMethodManager imm;
    private final Handler mHideHandler = new Handler();
    private String editData;
    private ProgressDialog progress;
    private boolean grabarReady;
    private WebView waitTagView;
    private Context context;
    byte[] bNombre, bLegajo, bPatente, bCamion;
    WebService ws;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_card);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mBackground = (ImageView) findViewById(R.id.initCardBackground);
        mainFrame = (RelativeLayout) findViewById(R.id.mainFrame);
        mainFrame.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        text1 = (TextView)findViewById(R.id.text1);
        text2 = (TextView)findViewById(R.id.text2);
        text3 = (TextView)findViewById(R.id.text3);
        Variables.currView = mainFrame;
        context = this;
        ws = new WebService();

        bNombre = new byte[16];
        bPatente = new byte[16];
        bCamion = new byte[16];
        bLegajo = new byte[16];

        progress = new ProgressDialog(this);
        progress.setCancelable(true);
        progress.setMessage("...Recuperando tarjeta");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);

        editClick = (Button)findViewById(R.id.editClick);
        editClick.setVisibility(Button.INVISIBLE);
        grabar = (Button)findViewById(R.id.grabar);
        grabar.setEnabled(false);
        grabar.setAlpha(.3f);

        edit = (EditText) findViewById(R.id.edit);
        edit.setVisibility(View.VISIBLE);
        edit.setSelection(0);
        edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit.setImeActionLabel("Aceptar", EditorInfo.IME_ACTION_DONE);
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Library.keybeep();
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    //edit.setEnabled(false);
                    editClick.setVisibility(Button.VISIBLE);
                    editData = v.getText().toString();
                    editClick.setText(editData);
                    edit.setVisibility(View.INVISIBLE);
                    if (editData.length()>0) {
                        switch (Variables.CardType) {
                            case Defines.T_BIN:
                                grabar.setEnabled(true);
                                grabar.setAlpha(1f);
                                break;
                            case Defines.T_CAMION:
                            case Defines.T_COSECHADOR:
                                progress.show();
                                progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                    public void onCancel(DialogInterface dialog) {
                                        Library.keybeep();
                                    }
                                });
                                recuperaTarjeta(editData);
                                break;
                        }
                    }
                    else
                    {
                        grabar.setEnabled(false);
                        grabar.setAlpha(.3f);
                    }
                }
                return false;
            }
        });
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        switch (Variables.CardType) {
            case Defines.T_COSECHADOR:
                getImage("t_cosecha_ini.png");
                edit.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                break;
            case Defines.T_BIN:
                getImage("t_bin_ini.png");
                edit.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                break;
            case Defines.T_CAMION:
                getImage("t_truck_ini.png");
                edit.setInputType(EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                edit.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
                break;
        }
        grabarReady = false;
        waitTagView = (WebView) findViewById(R.id.waitCard);
        waitTagView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mHideHandler.removeCallbacks(mHideRunnable);
            imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
        }
    };

    public void editorClick(View target)
    {
        editClick.setVisibility(Button.INVISIBLE);
        edit.setVisibility(View.VISIBLE);
        imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
    }

    public void cancelarClick(View target)
    {
        Library.keybeep();
        onBackPressed();
    }

    public void grabarClick(View target)
    {
        Library.keybeep();
        grabarReady = true;
        mainFrame.setAlpha(0.2F);
        waitTagView.loadUrl("file:///android_asset/wait_for_tag.html");
        waitTagView.setVisibility(View.VISIBLE);
        waitTagView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                waitTagView.setVisibility(View.INVISIBLE);
                grabarReady = false;
                return false;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (grabarReady && MifareIO.connect(this, intent)) {
            if (writeCard(intent))
            {
                MifareIO.disconnect();
                Library.goodBeep();
                waitTagView.setVisibility(View.INVISIBLE);
                mainFrame.setAlpha(1F);
                Library.alert(context, "Información", "La tarjeta se grabó EXITOSAMENTE", android.R.drawable.ic_dialog_info);
            }
            else
                MifareIO.disconnect();
        }
    }

    private void recuperaTarjeta(final String data)
    {

        if (Variables.CardType == Defines.T_CAMION) {
            progress.setMessage("...Recuperando tarjeta");
            progress.show();
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buff = ws.GetCardCamion(data, 16);
                    if (buff == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.cancel();
                                Library.alert(context, "Atención", "DATO INVÁLIDO", android.R.drawable.ic_dialog_alert);
                                text1.setText("");
                                text2.setText("");
                                text3.setText("");
                                grabar.setEnabled(false);
                                grabar.setAlpha(0.3f);
                            }
                        });
                        return;
                    }
                    final byte[] bLast = new byte[16];
                    final byte[] bFirst = new byte[16];
                    boolean flg = false;
                    for (int i = 0; i < 16; i++)
                    {
                        bNombre[i] = bFirst[i]= bLast[i] = 32;
                    }
                    int j = 0;
                    for (int i = 0; i < 16; i++) {
                        if (!flg)
                            bLast[i] = buff[i];
                        else {
                            bFirst[j] = buff[i];
                            j++;
                        }
                        if (buff[i] == 32)
                            flg = true;
                    }
                    for (int i = 0; i < 16; i++) {
                        bPatente[i] = buff[i + 16];
                        bCamion[i] = buff[i + 32];
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            text1.setText(new String(bFirst));
                            text2.setText(new String(bLast));
                            text3.setText(new String(bCamion));
                            progress.cancel();
                            grabar.setEnabled(true);
                            grabar.setAlpha(1f);
                        }
                    });
                }
            });
            th.start();
        }
        else if (Variables.CardType == Defines.T_COSECHADOR) {
            progress.setMessage("...Recuperando tarjeta");
            progress.show();
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buff = ws.GetCardCosechador(data, 16);
                    if (buff == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.cancel();
                                Library.alert(context, "Atención", "DATO INVÁLIDO", android.R.drawable.ic_dialog_alert);
                                text1.setText("");
                                text2.setText("");
                                text3.setText("");
                                grabar.setEnabled(false);
                                grabar.setAlpha(0.3f);
                            }
                        });
                        return;
                    }
                    final byte[] bLast = new byte[16];
                    final byte[] bFirst = new byte[16];
                    boolean flg = false;
                    for (int i = 0; i < 16; i++)
                    {
                        bNombre[i] = bFirst[i]= bLast[i] = 32;
                    }
                    int j = 0;
                    for (int i = 0; i < 16; i++) {
                        if (!flg)
                            bLast[i] = buff[i];
                        else
                        {
                            bFirst[j] = buff[i];
                            j++;
                        }
                        if (buff[i] == 32)
                            flg = true;
                        bNombre[i] = buff[i];
                        bLegajo[i] = buff[i + 16];
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            text1.setText(Library.String(bFirst));
                            text2.setText(Library.String(bLast));
                            progress.cancel();
                            grabar.setEnabled(true);
                            grabar.setAlpha(1f);
                            int iLegajo = Integer.parseInt(data);
                            iLegajo -= 230000;
                            iLegajo &= 0xffff;
                            bLegajo[6] = (byte)(iLegajo & 0xff);
                            bLegajo[7] = (byte)((iLegajo>>8) & 0xff);
                        }
                    });
                }
            });
            th.start();
        }
    }

    private boolean writeCard(Intent intent)
    {
        byte[] blockBin = {
                (byte)0x01, (byte)0x4E, (byte)0x52, (byte)0x54,
                (byte)0x31, (byte)0x06, (byte)0x42, (byte)0x49,
                (byte)0x4e, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};

        byte[] blockCamion = {
                (byte)0x01, (byte)0x4E, (byte)0x52, (byte)0x54,
                (byte)0x31, (byte)0x03, (byte)0x43, (byte)0x41,
                (byte)0x4d, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};

        byte[] blockCosechador = {
                (byte)0x01, (byte)0x4E, (byte)0x52, (byte)0x54,
                (byte)0x31, (byte)0x01, (byte)0x43, (byte)0x43,
                (byte)0x48, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};


        byte[] data = new byte[16];
        byte[] blank = new byte[16];
        byte[] block;
        int i, j;

        for (i=0; i<16; i++)
        {
            data[i] = 0;
            blank[i] = 0;
        }

        byte[] hid = MifareIO.Serial;
        String h = Library.padHex(hid[0])+Library.padHex(hid[1])+Library.padHex(hid[2])+Library.padHex(hid[3]);
        int horaAct;
        byte[] num = null;

        String hh = Library.padHex(data[11])+Library.padHex(data[10])+Library.padHex(data[9])+Library.padHex(data[8]);
        String send="";

        switch(Variables.CardType) {
            case Defines.T_COSECHADOR:
                break;
            case Defines.T_BIN:
                horaAct = Misc.GetClock();
                num = editData.getBytes();
                for (i=0; i<num.length; i++)
                    data[i] = num[i];
                Library.toIntelDataInt(horaAct, data, 8);
                send = "00000000" + editData;
                send = send.substring(send.length() - 8, send.length()) + hh;
                break;
            case Defines.T_CAMION:
                send = h+editData;
                break;
        }

        int paso = 0;
        int errcnt  = 0;

        for (i=0; i<30; i++)
        {
            try
            {
                Thread.sleep(200);
            }catch (Exception e){}
            switch (paso)
            {
                case 0:
                    MifareIO.write(this, intent, Defines.trailer, Defines.KEY_B, 3);
                    paso++;
                    break;
                case 1:
                    if (MifareIO.read(this, intent, Defines.KEY_A, 1, 1)) {
                        paso++;
                        errcnt=0;
                    }
                    else {
                        paso=0;
                        errcnt++;
                    }
                    break;
                case 2:
                    MifareIO.write(this, intent, Defines.trailer, Defines.KEY_B, 7);
                    paso++;
                    break;
                case 3:
                    if (MifareIO.read(this, intent, Defines.KEY_A, 4, 1))  {
                        paso++;
                        errcnt=0;
                    }
                    else {
                        paso=2;
                        errcnt++;
                    }
                    break;
                case 4:
                    switch (Variables.CardType) {
                        case Defines.T_COSECHADOR:
                            block = blockCosechador;
                            break;
                        case Defines.T_BIN:
                            block = blockBin;
                            break;
                        case Defines.T_CAMION:
                            block = blockCamion;
                            break;
                        default:
                            block = blank;
                            break;
                    }
                    if (MifareIO.write(this, intent, block, Defines.KEY_A, 1)) {
                        errcnt = 0;
                        paso++;
                    } else errcnt++;
                    break;
                case 5:
                    switch (Variables.CardType) {
                        case Defines.T_COSECHADOR:
                            block = bNombre;
                            break;
                        case Defines.T_BIN:
                            block = blank;
                            break;
                        case Defines.T_CAMION:
                            block = bNombre;
                            break;
                        default:
                            block = blank;
                            break;
                    }
                    if (MifareIO.write(this, intent, block, Defines.KEY_A, 2))
                        paso++;
                    else
                        errcnt++;
                    break;
                case 6:
                    switch (Variables.CardType) {
                        case Defines.T_COSECHADOR:
                            block = blank;
                            break;
                        case Defines.T_BIN:
                            block = blank;
                            break;
                        case Defines.T_CAMION:
                            block = bPatente;
                            break;
                        default:
                            block = blank;
                            break;
                    }
                    if (MifareIO.write(this, intent, block, Defines.KEY_A, 4))
                        paso++;
                    else
                        errcnt++;
                    break;
                case 7:
                    switch (Variables.CardType) {
                        case Defines.T_COSECHADOR:
                            block = bLegajo;
                            break;
                        case Defines.T_BIN:
                            block = data;
                            break;
                        case Defines.T_CAMION:
                            block = bCamion;
                            break;
                        default:
                            block = blank;
                            break;
                    }
                    if (MifareIO.write(this, intent, block, Defines.KEY_A, 5))
                        paso++;
                    else
                        errcnt++;
                    break;
                case 8:
                    if (MifareIO.write(this, intent, blank, Defines.KEY_A, 6))
                        paso++;
                    else
                        errcnt++;
                    break;
                case 9:
                    i = 100;
                    break;
            }
            if (errcnt > 3)
                break;
        }
        if (i>=100)
        {
            switch (Variables.CardType) {
                case Defines.T_COSECHADOR:
                    SendData("TXCCH", send);
                    break;
                case Defines.T_BIN:
                    SendData("TXBIN", send);
                    break;
                case Defines.T_CAMION:
                    SendData("TXCAM", send);
                    break;
                default:
                    block = blank;
                    break;
            }
            return true;
        }
        return false;
    }

    public void SendData(String type, String Data)
    {
        try {
            RecordStore record = RecordStore.openRecordStore(type+Misc.GetClock(), true, Defines.OPEN_WRITE);
            record.addRecord(Data.getBytes(), 0, Data.length());
            record.closeRecordStore();
        }catch (Exception e){};

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
        Intent intent = new Intent(this, InitCard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
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
}
