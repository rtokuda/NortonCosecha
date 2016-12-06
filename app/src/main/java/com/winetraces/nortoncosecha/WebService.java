package com.winetraces.nortoncosecha;

import com.winetraces.recordstore.RecordStore;
import java.util.Calendar;


/**
 * Created by nestor on 12/11/2016.
 */

public class WebService {

    public boolean running = false;
    int sz=0;
    static int FileSize, ReqPos, ReqInx;
    byte Comando = 0, CmdBk;
    byte[] CardBuf;
    //byte[] CosechaBuf = new byte[40000];
    static byte[] ProgramaBuf = new byte[10000];
    byte[] PresenteBuf = new byte[10000];
    int CosechaSize, ProgramaSize, PresenteSize;
    byte[] CardHdr = new byte[17];
    int fecha;
    public byte[] pp = new byte[50];
    int cnt = 0;
    int Timeout = 10;
    static boolean error;
    boolean sendOK;
    int sel = 0;
    int registros;
    static String sWebData;

    public byte[] GetCardCamion(String Patente, int minlen)
    {
        String s = "VendimiaOtaGetCardCamion.php?Data="+Patente;

        if (!postViaHttpConnection(s))
            return null;
        if (!XmlGetProp(false))
            return null;
        if (FileSize < minlen)
            return null;
        return(ProgramaBuf);
    }


    public byte[] GetCardCosechador(String Legajo, int minlen)
    {
        String s = "VendimiaOtaGetCardCosecha.php?Data="+Legajo;

        if (!postViaHttpConnection(s))
            return null;
        if (!XmlGetProp(false))
            return null;
        if (FileSize < minlen)
            return null;
        return(ProgramaBuf);
    }


    String data = "d337303030313130434f4c4f4e494120303038394e696e67756e6f2052697665726f73203030313020202020202020203030303130385445524345524f53416434354e696e67756e6f2044626c616e636120303031304d657a636c6120203030303130375445524345524f5341643435416d6172696c6c6f4775616a6172646f303031304d657a636c612020EA4FF400";

    public boolean GetConfig()
    {
        return true;
    }

    public boolean SendData()
    {
        int i, j, k, tot;
        String url;
        byte[] data;
        String[] dt = new String[3];
        String dd;
        RecordStore record, backup;

        String ss[] = RecordStore.listRecordStore();
        try {
            if (ss == null)
                return false;
            for (i = 0; i < ss.length; i++)
            {
                if (!ss[i].substring(0, 2).equals("TX"))
                    continue;
                if (ss[i].substring(0, 5).equals("TXBIN"))
                    url = "VendimiaOtaPutCardBin.php?Data=";
                else if (ss[i].substring(0, 5).equals("TXCCH"))
                    url = "VendimiaOtaPutCardCosecha.php?Data=";
                else if (ss[i].substring(0, 5).equals("TXCAM"))
                    url = "VendimiaOtaPutCardCamion.php?Data=";
                else
                    continue;
                record = RecordStore.openRecordStore(ss[i], true, Defines.OPEN_READ);
                data = record.getRecord(1);
                dd = new String(data);
                url = url + dd;
                record.closeRecordStore();
                if (postViaHttpConnection(url))
                {
                    try {
                        record = RecordStore.openRecordStore("Log", true, Defines.OPEN_WRITE);
                        record.addRecord(data, 0, data.length);
                        record.closeRecordStore();
                        RecordStore.deleteRecordStore(ss[i]);
                    }catch (Exception e){};
                }
            }
        }catch (Exception e){};

        dt[0]=dt[1]=dt[2]="";
        tot = 0;
        record = backup = null;
        try {
            for (i = 0; i < ss.length; i++)
            {
                if ((ss[i].length()<12) || (!ss[i].substring(0, 4).equals("NLOG")))
                    continue;
                record = RecordStore.openRecordStore(ss[i], true, Defines.OPEN_READ);
                backup = RecordStore.openRecordStore("BK_"+ss[i], true, Defines.OPEN_WRITE);
                for (j=1; j<=record.getNumRecords(); j++)
                {
                    if ((j & 63) == 1)
                    {
                        //MainScr.text("Enviando "+j+" de "+record.getNumRecords()+"   ", 5);
                        //flushGraphics();
                        //NortonCosecha.backLightON(3);
                    }
                    registros++;
                    data = record.getRecord(j);
                    backup.addRecord(data, 0, data.length);
                    String d = "";
                    for (k=0; k<data.length; k++)
                        d = d+Library.padHex(data[k]);
                    url = null;
                    switch(data[0])
                    {
                        case 1:
                            dt[0]+=d;
                            if (dt[0].length()>512)
                            {
                                url = "VendimiaOtaPutTAEvent.php?Data="+dt[0];
                                dt[0] = "";
                            }
                            break;
                        case 2:
                            dt[1]+=d;
                            tot++;
                            if (dt[1].length()>512)
                            {
                                url = "VendimiaOtaPutEvent.php?Data="+dt[1];
                                dt[1] = "";
                            }
                            break;
                        case 3:
                            dt[2]+=d;
                            if (dt[2].length()>512)
                            {
                                url = "VendimiaOtaPutUsedProCos.php?Data="+dt[2];
                                dt[2]="";
                            }
                            break;
                    }
                    if (url != null)
                    {
                        if (!postViaHttpConnection(url))
                        {
                            record.closeRecordStore();
                            backup.closeRecordStore();
                            return false;
                        }
                    }
                }
                for (k=0; k<3; k++)
                {
                    url = null;
                    switch(k)
                    {
                        case 0:
                            if (dt[0].length()>2)
                                url = "VendimiaOtaPutTAEvent.php?Data="+dt[0];
                            break;
                        case 1:
                            if (dt[1].length()>2)
                                url = "VendimiaOtaPutEvent.php?Data="+dt[1];
                            break;
                        case 2:
                            if (dt[2].length()>2)
                                url = "VendimiaOtaPutUsedProCos.php?Data="+dt[2];
                            break;
                    }
                    if (url != null)
                    {
                        if (!postViaHttpConnection(url))
                        {
                            record.closeRecordStore();
                            backup.closeRecordStore();
                            return false;
                        }
                    }
                }
                record.closeRecordStore();
                backup.closeRecordStore();
                RecordStore.deleteRecordStore(ss[i]);
            }
        }
        catch (Exception e){
            if (record != null)
            {
                try{
                    record.closeRecordStore();
                }catch(Exception ee){}
            }
            if (backup != null)
            {
                try{
                    backup.closeRecordStore();
                }catch(Exception ee){}
            }
        }
      //  msg1 = tot+" fichas enviadas";
        return true;
    }

    public boolean GetPrograma()
    {
        int i;
        RecordStore record;

        String s = "VendimiaOtaGetProCos.php?Fecha=";
        Calendar Fecha = Library.Fecha(Misc.GetClock()*1000L);
		if (Defines.CommTest)
        		s += "2012-01-15";
		else
        {
            s += Integer.toString(Fecha.get(Calendar.YEAR));
            s += "-"+Library.padNum(Fecha.get(Calendar.MONTH)+1, 2);
            s += "-"+Library.padNum(Fecha.get(Calendar.DAY_OF_MONTH), 2);
        }

        s += "&Terminal="+Library.padHex(Variables.DeviceID, 8); //5DBE1EDE
        if (!postViaHttpConnection(s))
            return false;
        //sWebData = "<Data>c33a303031303337504552445249454c30303138414d5f3030315f42566964656c61202030303130303031303338414752454c4f202030303037414d5f3030315f42566964656c612020303031303030313033394d454452414e4f2030303133414d5f3030315f4252697665726f7320303031303030313034304d454452414e4f2030303133414d5f3030315f4244626c616e63612030303130303031303431544552454e522020303030364e494e47554e412052697665726f732030303130303031303432544552454e522020303030364e494e47554e412044626c616e63612030303130303031303433544552454e522020303030354e494e47554e412052697665726f732030303130303031303434544552454e522020303030354e494e47554e412044626c616e63612030303130FEC1507C</Data>";" +
        //sWebData = "<Data>d337303030313130434f4c4f4e494120303038394e696e67756e6f2052697665726f73203030313020202020202020203030303130385445524345524f53416434354e696e67756e6f2044626c616e636120303031304d657a636c6120203030303130375445524345524f5341643435416d6172696c6c6f4775616a6172646f303031304d657a636c612020EA4FF400</Data>";

        // ToDo si no hay novedades de programa descargado, no inicializar.
        Misc.InitProgramFile();
        Variables.FechaProg = Misc.GetClock() / 86400;
        Variables.Presentes = 0;
        Variables.ProgSel = false;
        Variables.Programa = "NO DEFINIDO";
        Misc.SaveConfig();

        if (sWebData.substring(0,2).equals("NO"))
        {
            Variables.msgTxt += "No hay programas para hoy\n\r";
            error = true;
            return false;
        }
        XmlGetProp(true);
        int PrgCnt = (FileSize - 2 - 4)/Defines.PRG_LEN;
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
        Variables.msgTxt += " "+PrgCnt+" Programas OK\r\n";
        return true;
    }

private boolean postViaHttpConnection(String param)
    {
        Variables.url = "http://"+Variables.sWebServiceURL+"/ota/" + param;
        HttpConnect cn = new HttpConnect();
        (new Thread(cn)).start();
        int timeout = 0;
        while (cn.isRunning())
        {
            try {
                Thread.sleep(100);
            }catch (InterruptedException e){};
            if (timeout>100)
                return false;
        }
        if (cn.getError()>0)
            return false;
        sWebData = cn.getResult();
        return true;
    }

    private static boolean XmlGetProp(boolean flag)
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