package com.winetraces.nortoncosecha;

/**
 *
 */
import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;



public class MifareIO {

    private static NfcAdapter nfcAdapter = null;
    private static String errorMessage = null;
    private static MifareClassic mfc = null;
    static byte[][] authent_buffer = {
            {(byte)0xC0, (byte)0xC1, (byte)0xC2, (byte)0xC3, (byte)0xC4, (byte)0xC5},
            {(byte)0xD0, (byte)0xD1, (byte)0xD2, (byte)0xD3, (byte)0xD4, (byte)0xD5},
            {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF},
            {(byte)0xA0, (byte)0xA1, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5}
    };
    public static byte[][] ReadBuff = null;
    public static byte[] Serial = null;


    public MifareIO(Context context) {
    }

    public static void enable(Activity activity, PendingIntent pendingIntent) {
        ReadBuff = new byte[3][16];
        Serial = new byte[4];
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    public static void disable(Activity activity) {
        nfcAdapter.disableForegroundDispatch(activity);
    }

    public static boolean connect(Context context, Intent intent) {
        boolean flag = false;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Serial = tagFromIntent.getId();
            mfc = MifareClassic.get(tagFromIntent);
            try {
                mfc.connect();
            } catch (IOException e) {
                return false;
            }
            flag = true;
        }
        return flag;
    }

    public static void disconnect()
    {
        try {
            if (mfc != null)
                mfc.close();
        } catch (IOException e) {
        }
    }

    public static boolean read(Context context, Intent intent, int key, int blqIni, int blqCnt)
    {
        if ((blqIni&3)+blqCnt > 3)
            return false;
        try {
            boolean auth = false;
            String cardData = null;
            int sector = blqIni/4;
            if (key == Defines.KEY_B)
                auth = mfc.authenticateSectorWithKeyB(sector, authent_buffer[key]);
            else
                auth = mfc.authenticateSectorWithKeyA(sector, authent_buffer[key]);
            if (auth) {
                for (int i = 0; i < blqCnt; i++) {
                    ReadBuff[i] = mfc.readBlock(sector*4+i+(blqIni&3));
                }
                auth = false;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean write(Context context, Intent intent, byte[]wdata, int key, int bloque)
    {
        try {
            boolean auth = false;
            String cardData = null;
            int sector = bloque/4;
            byte[] rdata;
            if (key == Defines.KEY_A)
                auth = mfc.authenticateSectorWithKeyA(sector, authent_buffer[key]);
            else
                auth = mfc.authenticateSectorWithKeyB(sector, authent_buffer[key]);
            if (auth)
            {
                mfc.writeBlock(bloque, wdata);
                if (bloque != 3) {
                    rdata = mfc.readBlock(bloque);
                    for (int i = 0; i < 15; i++) {
                        if (rdata[i] != wdata[i])
                            return false;
                    }
                }
            }
        } catch (IOException e) {
            //Log.e(TAG, e.getLocalizedMessage());
            //showAlert(3);
            return false;
        }
        return true;
    }

    public static boolean write(Context context, Intent intent, byte[][] wdata, int key, int blqIni, int blqCount) {

        try {       //  5.1) Connect to card
            boolean auth = false;
            String cardData = null;
            int secIni = blqIni/4;
            byte[] rdata;
            auth = mfc.authenticateSectorWithKeyA(secIni, authent_buffer[key]);
            if (auth)
            {
                for (int j= 0; j<blqCount; j++)
                {
                    mfc.writeBlock(blqIni+j, wdata[j]);
                    rdata = mfc.readBlock(blqIni+j);
                    for (int i=0; i<15; i++)
                    {
                        if (rdata[i]!=wdata[j][i])
                            return false;
                    }
                }
            }
        } catch (IOException e) {
            //Log.e(TAG, e.getLocalizedMessage());
            //showAlert(3);
            return false;
        }
        return true;
    }



    public String getErrorMessage() {
        return this.errorMessage;
    }

}


