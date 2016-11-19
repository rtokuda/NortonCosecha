package com.winetraces.nortoncosecha;

import android.content.Context;
import android.net.ConnectivityManager;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by nestor on 18/11/2016.
 */

class Print implements Runnable
{
    HttpURLConnection connection = null;
    OutputStreamWriter outs;
    //String url = "socket://192.168.0.96:80;interface=wifi";
    //String url = "http://192.168.0." + (NortonCosecha.DeviceID & 255)+":80;interface=wifi";

    String url; // = NortonCosecha.sWiFiURL+":80;interface=wifi";

    String[] buff = new String[512];
    byte[] attrib;
    int len;
    public boolean running = false;
    boolean error = false;

    public Print ()
    {
        boolean first = true;
        error = false;

        //url = "http://"+Variables.sWiFiURL;
        try {
            RecordStore record = RecordStore.openRecordStore("printBuffer", true);
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
        catch (Exception e){error = true;}
        running = false;
    }

    public void run() {
        if (error)
            return;
        running = true;
        if (SocketOpen())
        {
            boolean doble = false;
            for (int i=0; i<len; i++)
            {
                if (running = false)
                    break;
                if ((attrib[i]==1) && (doble == false))
                {
                    try {
                        outs.write(14);
                        outs.write(28);
                    }
                    catch(IOException error){}
                    doble = true;
                }
                if ((attrib[i]==0) && (doble = true))
                {
                    try {
                        outs.write(15);
                        outs.write(29);
                    }
                    catch(IOException error){}
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
        SocketClose();
        running = false;
    }

    private boolean SocketOpen()
    {
        if (outs != null)
        {
            try {
                outs.close();
            }catch (IOException error){}
            outs = null;
        }

        try {
            //connection = (StreamConnection)Connector.open(url);
            URL url = new URL("http://192.168.0.32/"); //+Variables.sWiFiURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);
        }
        catch(IOException error)
        {
            //System.out.println("Open Socket: " + error.toString());
            return false;
        }
        try {
            outs = 	new OutputStreamWriter(connection.getOutputStream());
        }
        catch(IOException error)
        {
            //System.out.println("Stream: " + error.toString());
            return false;
        }
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
        catch(IOException error)
        {
            //System.out.println("Out: " + error.toString());
        }
        try {
            if (connection != null)
                connection.disconnect();
            connection = null;
        }
        catch(Exception error)
        {
            //System.out.println("Close Socket: " + error.toString());
        }
    }

    private void print(String line)
    {
        line += "\r\n";
        try {
            outs.write(line, 0, line.length());
            try
            {
                Thread.sleep(100);
            }catch (Exception e){}
        }
        catch(IOException error)
        {
            //System.out.println("Out: " + error.toString());
        }
    }
}