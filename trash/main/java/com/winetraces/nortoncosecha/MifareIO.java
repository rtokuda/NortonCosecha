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
            {(byte)0xC0, (byte)0xC1, (byte)0xC2,
                    (byte)0xC3, (byte)0xC4, (byte)0xC5},
            {(byte)0xD0, (byte)0xD1, (byte)0xD2,
                    (byte)0xD3, (byte)0xD4, (byte)0xD5}
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
        // 1) Parse the intent and get the action that triggered this intent
        String action = intent.getAction();
        // 2) Check if it was triggered by a tag discovered interruption.
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            //  3) Get an instance of the TAG from the NfcAdapter
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Serial = tagFromIntent.getId();
            // 4) Get an instance of the Mifare classic card from this TAG intent
            mfc = MifareClassic.get(tagFromIntent);
            try {       //  5.1) Connect to card
                mfc.connect();
            } catch (IOException e) {
                //Log.e(TAG, e.getLocalizedMessage());
                //showAlert(3);
                return false;
            }
            flag = true;
        }
        return flag;
    }

    public static void disconnect()
    {
        try {       //  5.1) Connect to card
            if (mfc != null)
                mfc.close();
        } catch (IOException e) {
            //Log.e(TAG, e.getLocalizedMessage());
            //showAlert(3);
        }
    }

    public static boolean read(Context context, Intent intent, int key, int blqIni, int blqCnt)
    {
        try {       //  5.1) Connect to card
            boolean auth = false;
            String cardData = null;
            // 6.1) authenticate the sector
            int sector = blqIni/4;
            auth = mfc.authenticateSectorWithKeyA(sector, authent_buffer[key]);
            if (auth) {
               // 6.2) In each sector - get the block count
               for (int i = 0; i < blqCnt; i++) {
                   // 6.3) Read the block
                    ReadBuff[i] = mfc.readBlock(sector*4+i+(blqIni&3));
                    //ReadBuff[j*4+i] = mfc.readBlock(bIndex);
                }
            } else { // Authentication failed - Handle it
                return false;
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


