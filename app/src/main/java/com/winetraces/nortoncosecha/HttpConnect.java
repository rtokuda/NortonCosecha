package com.winetraces.nortoncosecha;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by nestor on 06/12/2016.
 */

public class HttpConnect implements Runnable
{
    String sWebData;
    int error;
    boolean  running;

    public HttpConnect()
    {
        running = true;
        error = 0;
        sWebData = "";
    }

    public String getResult()
    {
        return sWebData;
    }

    public boolean isRunning()
    {
        return running;
    }

    public int getError()
    {
        return error;
    }

    public void run()
    {
        HttpURLConnection conn = null;
        InputStream is = null;
        boolean flag = false;
        try {
            URL wURL = new URL(Variables.url);
            conn = (HttpURLConnection) wURL.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Language", "es");
            conn.setRequestProperty("Content-type", "text/xml");
            conn.setReadTimeout(10000);
            conn.setDoInput(true);
            int rc = conn.getResponseCode();
            if ((rc != HttpURLConnection.HTTP_OK) && (rc != 100)) {//HttpConnection.Continue
                error = 1;
                //throw new IOException("HTTP response code: " + rc);
            } else {
                is = conn.getInputStream();
                int ch;
                char cc;
                sWebData = "";
                error = 2;
                while ((ch = is.read()) != -1) {
                    if (!flag && (ch <= 32))
                        continue;
                    flag = true;
                    cc = (char) ch;
                    sWebData = sWebData + cc;
                }
                error = 0;
                running = false;
            }
        } catch (IOException err) {
            error = 3;
            //System.out.println("Caught IOException: " + err.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception err) {
                    //err.printStackTrace();
                    error = 4;
                }
            }
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception err) {
                    //err.printStackTrace();
                    error = 5;
                }
            }
        }
        running = false;
    }
}
