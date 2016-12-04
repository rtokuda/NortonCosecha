package com.winetraces.nortoncosecha;

import android.os.AsyncTask;

import com.winetraces.recordstore.RecordStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import static com.winetraces.nortoncosecha.WebService.sWebData;
import static com.winetraces.nortoncosecha.WebService.url;

/**
 * Created by nestor on 12/11/2016.
 */

public class WebService {

    public boolean running = false;
    int sz=0;
    int FileSize, ReqPos, ReqInx;
    byte Comando = 0, CmdBk;
    byte[] CardBuf;
    //byte[] CosechaBuf = new byte[40000];
    byte[] ProgramaBuf = new byte[10000];
    byte[] PresenteBuf = new byte[10000];
    int CosechaSize, ProgramaSize, PresenteSize;
    byte[] CardHdr = new byte[17];
    int fecha;
    public byte[] pp = new byte[50];
    public String msg0, msg1, msg2, msg3;
    int cnt = 0;
    int Timeout = 10;
    boolean error;
    boolean sendOK;
    int sel = 0;
    int registros;

    //String urlRaiz = "http://norton.adabyron.org.ar/ota/";
    //String urlRaiz = "https://"+NortonCosecha.sWebServiceURL+"/ota/";
    static String urlRaiz = "http://"+Variables.sWebServiceURL+"/ota/";
    static String url;
    static String sWebData;

    public byte[] GetCardCamion(String Patente, int minlen)
    {
        url = urlRaiz + "VendimiaOtaGetCardCamion.php?Data="+Patente;

        if (!postViaHttpConnection())
            return null;
        if (!XmlGetProp(false))
            return null;
        if (FileSize < minlen)
            return null;
        return(ProgramaBuf);
    }

    String data = "d337303030313130434f4c4f4e494120303038394e696e67756e6f2052697665726f73203030313020202020202020203030303130385445524345524f53416434354e696e67756e6f2044626c616e636120303031304d657a636c6120203030303130375445524345524f5341643435416d6172696c6c6f4775616a6172646f303031304d657a636c612020EA4FF400";

    public boolean GetProgram()
    {
        int i;
        RecordStore record;

        url = urlRaiz+"VendimiaOtaGetProCos.php?Fecha=";
        Calendar Fecha = Library.Fecha(Misc.GetClock()*1000L);
//		if (defines.CommTest)
        //		url += "2009-03-03";
//		else
        {
            url += Integer.toString(Fecha.get(Calendar.YEAR));
            url += "-"+Library.padNum(Fecha.get(Calendar.MONTH)+1, 2);
            url += "-"+Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2);
        }


        //if (!postViaHttpConnection())
        //    return false;

        //sWebData = "<Data>c33a303031303337504552445249454c30303138414d5f3030315f42566964656c61202030303130303031303338414752454c4f202030303037414d5f3030315f42566964656c612020303031303030313033394d454452414e4f2030303133414d5f3030315f4252697665726f7320303031303030313034304d454452414e4f2030303133414d5f3030315f4244626c616e63612030303130303031303431544552454e522020303030364e494e47554e412052697665726f732030303130303031303432544552454e522020303030364e494e47554e412044626c616e63612030303130303031303433544552454e522020303030354e494e47554e412052697665726f732030303130303031303434544552454e522020303030354e494e47554e412044626c616e63612030303130FEC1507C</Data>";" +
        sWebData = "<Data>d337303030313130434f4c4f4e494120303038394e696e67756e6f2052697665726f73203030313020202020202020203030303130385445524345524f53416434354e696e67756e6f2044626c616e636120303031304d657a636c6120203030303130375445524345524f5341643435416d6172696c6c6f4775616a6172646f303031304d657a636c612020EA4FF400</Data>";

        Misc.InitProgramFile();
        Variables.FechaProg = Misc.GetClock() / 86400;
        Variables.Presentes = 0;
        Variables.ProgSel = false;
        Variables.Programa = "NO DEFINIDO";
        Misc.SaveConfig();

        if (sWebData.substring(0,2).equals("NO"))
        {
            msg3 = "No hay programa para hoy";
            error = true;
            return false;
        }
        XmlGetProp(true);

	/*	int FechaProg = Library.fromIntelDataWord(ProgramaBuf, 0);
		int horaAct = NortonCosecha.GetClock();
		horaAct = horaAct / 86400;
		msg1 = msg2 = null;

		if (defines.CommTest)
			FechaProg = horaAct;

		if (FechaProg != horaAct)
		{
            msg1 = "Fecha invalida";
            msg2 = " "+FechaProg+" "+horaAct;
            error = true;
            return false;
		}*/
        int PrgCnt = (FileSize - 2 - 4)/Defines.PRG_LEN;
/*		if ((PrgCnt*38 + 6) != FileSize)
		{
			msg3 = "Datos inconsistentes "+PrgCnt+" "+FileSize;
			error = true;
			return false;
		}*/
        try {
            Misc.InitProgramFile();
            record = RecordStore.openRecordStore("Programas", true, Defines.OPEN_WRITE);
            for (i=0; i<PrgCnt; i++)
            {
                record.addRecord(ProgramaBuf, 2+i*Defines.PRG_LEN, Defines.PRG_LEN);
                byte[] b = new byte[15];
                Library.byteArrayCopy(ProgramaBuf, 2+i*Defines.PRG_LEN, b, 0);
            }
            record.closeRecordStore();
        }catch (Exception e){return false;}
        msg3= " "+PrgCnt+" Programas OK";
        return true;
    }


private boolean postViaHttpConnection()
    {
        HttpURLConnection conn = null;
        InputStream is = null;
        boolean flag = false;
        boolean error = false;

        try {
            URL wURL = new URL(url);
            conn = (HttpURLConnection)wURL.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Language", "es");
            conn.setRequestProperty("Content-type", "text/xml");
            conn.setDoInput(true);
            int rc = conn.getResponseCode();
            if ((rc != HttpURLConnection.HTTP_OK) && (rc != 100)) {//HttpConnection.Continue
                error = true;
                throw new IOException("HTTP response code: " + rc);
            } else{
                is = conn.getInputStream();;
                int ch;
                char cc;
                sWebData = "";
                while ((ch = is.read()) != -1) {
                    if (!flag && (ch <= 32))
                        continue;
                    flag = true;
                    cc = (char)ch;
                    sWebData = sWebData+cc;
                }
            }
        }
        catch(IOException err)
        {
            error = true;
            System.out.println("Caught IOException: " + err.toString());
        }
        finally {
            if (is != null) {
                try{
                    is.close();
                } catch (Exception err){err.printStackTrace();error = true;};
            }
            if (conn != null)
            {
                try {
                    conn.disconnect();
                }catch (Exception err){err.printStackTrace();error = true;};
            }
        }
        return !error;
    }

    private boolean XmlGetProp(boolean flag)
    {
        byte[] src = sWebData.getBytes();
        int len = sWebData.length();
        byte[] dst = new byte[len];
        int i, j, k;

        if (flag)
        {
            for (i=0; i<len; i++)
            {
                if (src[i] == '>')
                    break;
            }
            if (i>=len)
                return false;
            k = 0;
            for (j=i+1; j<len; j++)
            {
                if (src[j] == '<')
                    break;
                dst[k++] = src[j];
            }
            FileSize = k;
        }
        else
        {
            for (i=0; i<len; i++)
            {
                dst[i] = src[i];
                if (dst[i]==' ')
                    dst[i] = '0';
            }
            FileSize = len;
            k = len;
        }
        byte[] val = new byte[2];
        for (i=0; i<k; i+=2)
        {
            try {
                val[0] = dst[i];
                val[1] = dst[i+1];
                String hex = new String(val);
                ProgramaBuf[i/2]= (byte)(Integer.valueOf(hex, 16).intValue());
            }catch (Exception e){
                FileSize = i/2;
                return true;
            }
        }
        FileSize /= 2;
        return true;
    }
}

/*
class postViaHttpConnection extends AsyncTask <Void, Void, Void>
{
    HttpURLConnection conn = null;
    InputStream is = null;
    boolean flag = false;
    boolean error = false;

    @Override
    protected Void doInBackground (Void... arg0) {
        try {
            URL wURL = new URL(url);
            conn = (HttpURLConnection) wURL.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Language", "es");
            conn.setRequestProperty("Content-type", "text/xml");
            conn.setDoInput(true);
            int rc = conn.getResponseCode();
            if ((rc != HttpURLConnection.HTTP_OK) && (rc != 100)) {//HttpConnection.Continue
                error = true;
                throw new IOException("HTTP response code: " + rc);
            } else {
                is = conn.getInputStream();
                ;
                int ch;
                char cc;
                sWebData = "";
                while ((ch = is.read()) != -1) {
                    if (!flag && (ch <= 32))
                        continue;
                    flag = true;
                    cc = (char) ch;
                    sWebData = sWebData + cc;
                }
            }
        } catch (IOException err) {
            error = true;
            System.out.println("Caught IOException: " + err.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception err) {
                    err.printStackTrace();
                    error = true;
                }
                ;
            }
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception err) {
                    err.printStackTrace();
                    error = true;
                }
                ;
            }
        }
        return null;
    }
}
*/