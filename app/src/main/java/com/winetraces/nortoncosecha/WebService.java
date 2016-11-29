package com.winetraces.nortoncosecha;

import com.winetraces.recordstore.RecordStore;

import java.util.Calendar;

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
    String urlRaiz = "http://"+Variables.sWebServiceURL+"/ota/";
    String url;
    String sWebData;

    String data = "d337303030313130434f4c4f4e494120303038394e696e67756e6f2052697665726f73203030313020202020202020203030303130385445524345524f53416434354e696e67756e6f2044626c616e636120303031304d657a636c6120203030303130375445524345524f5341643435416d6172696c6c6f4775616a6172646f303031304d657a636c612020EA4FF400";

    private boolean GetProgram()
    {
        int i;
        RecordStore record;

        url = urlRaiz+"VendimiaOtaGetProCos.php?Fecha=";
        Calendar Fecha = Library.Fecha(NortonCosecha.GetClock()*1000L);
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

        NortonCosecha.InitProgramFile();
        Variables.FechaProg = NortonCosecha.GetClock() / 86400;
        Variables.Presentes = 0;
        Variables.ProgSel = false;
        Variables.Programa = "NO DEFINIDO";
        NortonCosecha.SaveConfig();

        if (sWebData.substring(0,2).equals("NO"))
        {
            msg3 = "No hay programa para hoy";
            error = true;
            return false;
        }

        XmlGetProp();

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
            NortonCosecha.InitProgramFile();
            record = RecordStore.openRecordStore("Programas", true);
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

    private boolean XmlGetProp()
    {
        byte[] src = sWebData.getBytes();
        int len = sWebData.length();
        byte[] dst = new byte[len];
        int i, j, k;

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
        byte[] val = new byte[2];
        for (i=0; i<k; i+=2)
        {
            val[0] = dst[i];
            val[1] = dst[i+1];

            String hex = new String(val);
            try {
                ProgramaBuf[i/2]= (byte)(Integer.valueOf(hex, 16).intValue());
            }catch (Exception e){}
        }
        FileSize /= 2;
        return true;
    }

}
