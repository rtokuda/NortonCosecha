package com.winetraces.nortoncosecha;


import android.content.Context;
import android.os.AsyncTask;

import com.winetraces.recordstore.RecordEnumeration;
import com.winetraces.recordstore.RecordStore;
import com.winetraces.wifimanager.WifiApManager;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by nestor on 18/11/2016.
 */



class Print
{
    PrintWriter outs;
    private Socket socket;

    String[] buff = new String[512];
    byte[] attrib;
    int len;
    int error = 0;
    Context cxt;
    WifiApManager wfAP;


    public int imprimir() {
        int i;

        error = 0;
        if (Defines.CommTest) {
            Variables.sPrinterURL = "192.168.43.23";
        }
        wfAP = new WifiApManager(Variables.pContext);
        if (!wfAP.isWifiApEnabled())
        {
            if (!wfAP.setWifiApEnabled(null, true))
                error = 1;
            else {
                for (i = 0; i < 10; i++) {
                    if (wfAP.isWifiApEnabled())
                        break;
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
                if (i >= 10)
                    error = 2;
            }
            if (error > 0){
                return error;
            }
        }
        boolean first = true;
        try {
            RecordStore record = RecordStore.openRecordStore("printBuffer", true, Defines.OPEN_READ);
            RecordEnumeration rd = record.enumerateRecords(null, null, false);
            len = 0;
            while( rd.hasNextElement() )
            {
                int ID = rd.nextRecordId();
                if (first)
                {
                    attrib = record.getRecord(ID);
                    first = false;
                    continue;
                }
                byte[] rec = record.getRecord(ID);
                buff[len++]= new String(rec);
            }
            record.closeRecordStore();
        }
        catch (Exception e){}
        if (SocketOpen())
        {
            boolean doble = false;
            for (i=0; i<len; i++)
            {
                if ((attrib[i]==1) && (doble == false))
                {
                    try {
                        outs.write(14);
                        outs.write(28);
                    }
                    catch(Exception error){}
                    doble = true;
                }
                if ((attrib[i]==0) && (doble == true))
                {
                    try {
                        outs.write(15);
                        outs.write(29);
                    }
                    catch(Exception error){}
                    doble = false;
                }
                print(buff[i]);
            }
            print(" ");
            print(" ");
            print(" ");
            print(" ");
            print(" ");
            print(" ");
        }
        else {
            error = 3;
        }
        SocketClose();
        return error;
    }


    private boolean SocketOpen()
    {
        socket = null;
        outs = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(Variables.sPrinterURL, 80), 5000);
            outs = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        } catch (Exception e){ return false;}
        return true;
    }

    private void SocketClose()
    {
        try {
            if (outs != null)
            {
                outs.flush();
                outs.close();
            }
            outs = null;
        }
        catch(Exception error)
        {
            //System.out.println("Out: " + error.toString());
        }
        try {
            if (socket != null)
            {
                socket.close();
            }
            socket = null;
        }
        catch(Exception error)
        {
            //System.out.println("Out: " + error.toString());
        }
    }

    private void print(String line)
    {
        line += "\r\n";
        try {
            outs.write(line, 0, line.length());
            try
            {
                Thread.sleep(50);
            }catch (Exception e){}
        }
        catch(Exception error)
        {
            //System.out.println("Out: " + error.toString());
        }
    }
}
