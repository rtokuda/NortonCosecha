package com.winetraces.nortoncosecha;

import android.content.Context;

import com.winetraces.recordstore.RecordStore;

/**
 * Created by nestor on 02/12/2016.
 */

public class Misc {
    public static void SetPrograma(Context context)
    {
        int x,i;
        RecordStore record;

        try {
            record = RecordStore.openRecordStore("Config", true, Defines.OPEN_READ);
            x = record.getNumRecords();
            byte[] datos = new byte[300];
            if (x!=0)
                datos = record.getRecord(1);
            record.closeRecordStore();
            byte c[]=new byte[10];
            Library.byteArrayCopy(datos, 31, c, 0);
            Variables.Cuadrilla = new String(c);
            if ((x == 0) || (datos[11] != Defines.CONFIG_VERSION))
            {
                RecordStore.initializeRecordStore(context, true);
                Variables.ProgInx = 0;
                Variables.DiffTime = Defines.COSECHADOR_TIMEOUT;
                Variables.FechaProg = 0;
                Variables.DiffClock = 0;
                Variables.Presentes = 0;
                Variables.ModoCosecha = 0;
                Variables.TachoCajaCnt = 0;
                Variables.TotalTachos = 0;
                Variables.Tachos4Bin = 16;
                Variables.Bin4Camion = 24;
                Variables.Cajas4Pallet = 32;
                Variables.Pallet4Camion = 10;
                Variables.AlarmTimeOut = 5000;
                Variables.BinCnt = 1;
                Variables.CamionCnt = 0;
                Variables.logInx = 0;
                Variables.Viaje = 0;
                Variables.RemitoInx = 0;
                Variables.Cuadrilla = "NINGUNO   ";
                Variables.ProgSel = false;
                //Variables.sWiFiURL = "192.168.0." + (Variables.DeviceID & 255);
                Variables.sWiFiURL = "192.168.0.32";
                Variables.sWebServiceURL = "norton.fundacionadabyron.org";
                //Variables.sWebServiceURL = "192.168.50.9";
                //Variables.sWebServiceURL = "192.185.17.39";
                Variables.sMailAddr = "";
                SaveConfig();
            }
            else
            {
                Variables.ProgInx = datos[0];
                Variables.DiffTime = Library.fromIntelDataWord(datos, 1);
                Variables.FechaProg = Library.fromIntelDataWord(datos, 3);
                Variables.DiffClock = Library.fromIntelDataIntLE(datos, 5);
                if (Variables.DiffClock > 86400)
                    Variables.DiffClock = 0;
                Variables.Presentes = Library.fromIntelDataWord(datos, 9);
                Variables.ProgSel = (datos[12]==0)?false:true;
                Variables.Tachos4Bin = Library.fromIntelDataWord(datos, 13);
                Variables.Bin4Camion = Library.fromIntelDataWord(datos, 15);
                Variables.TachoCajaCnt = Library.fromIntelDataWord(datos, 17);
                Variables.BinCnt = Library.fromIntelDataWord(datos, 19);
                Variables.CamionCnt = Library.fromIntelDataWord(datos, 21);
                Variables.TotalTachos = Library.fromIntelDataWord(datos, 23);
                Variables.Cajas4Pallet = Library.fromIntelDataWord(datos, 25);
                Variables.Pallet4Camion = Library.fromIntelDataWord(datos, 27);
                Variables.AlarmTimeOut = Library.fromIntelDataWord(datos, 29);
                Variables.logInx = datos[41];
                Variables.Viaje = datos[42];
                Variables.RemitoInx = datos[43];
                x = datos[100];
                byte[]aux = new byte[x];
                for(i=0; i<x; i++)
                    aux[i]=datos[101+i];
                Variables.sWiFiURL = new String(aux);
                x = datos[150];
                aux = new byte[x];
                for(i=0; i<x; i++)
                    aux[i]=datos[151+i];
                Variables.sWebServiceURL = new String(aux);
                x = datos[200];
                if (x == 0)
                    Variables.sMailAddr = "";
                else
                {
                    aux = new byte[x];
                    for(i=0; i<x; i++)
                        aux[i]=datos[201+i];
                    Variables.sMailAddr = new String(aux);
                }
            }
            int horaAct = GetClock() / 86400;
            if (Variables.FechaProg != horaAct)
            {
                ResetConfig();
            }
            if (Variables.ProgSel)
            {
                try {
                    record = RecordStore.openRecordStore("Programas", true, Defines.OPEN_READ);
                    int cnt = record.getNumRecords();
                    if (Variables.ProgInx >= cnt)
                        Variables.ProgInx = 0;
                    datos = record.getRecord(Variables.ProgInx+1);
                    String tx[] = new String [10];
                    splitPrograma(datos, tx);
                    Variables.ProgID = tx[0];
                    Variables.Finca = tx[2];
                    Variables.Cuartel = tx[3];
                    Variables.Area = tx[4];
                    Variables.CuadrillaPrg = tx[5];
                    Variables.Programa = tx[7];
                    Variables.ModoCosecha = Integer.parseInt(tx[8]);
                    Variables.VariedadUva = tx[9];
                    if (Variables.ModoCosecha == 1)
                        Variables.Programa+="-CAJAS";
                    else
                        Variables.Programa+="-BINES";

                }catch (Exception e){};
                try {
                    record.closeRecordStore();
                }catch (Exception e){};
            }
            else
            {
                Variables.Finca = "Ninguno";
                Variables.Cuartel = "0001";
                Variables.Area = "Ninguno";
                Variables.CuadrillaPrg = "Ninguno";
                Variables.Programa = "NO DEFINIDO";
                Variables.ModoCosecha = 0;
                Variables.VariedadUva = "Generico";
            }
        }catch(Exception e){}
        Variables.Cosechador_name = " ";
        Variables.Cosechador_count= " ";
        Variables.Cosechador_legajo = " ";
        Variables.CosechadorLastTime = -1;
    }

    public static void ResetConfig()
    {
        int horaAct = GetClock() / 86400;
        InitProgramFile();
        Variables.FechaProg = horaAct;
        Variables.Presentes = 0;
        Variables.TachoCajaCnt = 0;
        Variables.TotalTachos = 0;
        Variables.BinCnt = 1;
        Variables.CamionCnt = 0;
        if (Variables.logInx < 99)
            Variables.logInx++;
        else
            Variables.logInx = 0;
        String fname = "RECORD"+Integer.toString(Variables.logInx);
        try {
            RecordStore.deleteRecordStore(fname);
        }catch (Exception e){}
        Variables.Viaje = 0;
        SaveConfig();
    }

    public static void SaveConfig()
    {
        byte[] datos = new byte[400];
        int i;

        datos[0] = Variables.ProgInx;
        datos[1] = (byte)(Variables.DiffTime & 255);
        datos[2] = (byte)(Variables.DiffTime / 256);
        datos[3] = (byte)(Variables.FechaProg & 255);
        datos[4] = (byte)(Variables.FechaProg / 256);
        Library.toIntelDataInt(Variables.DiffClock, datos, 5);
        datos[9] = (byte)(Variables.Presentes & 255);
        datos[10] = (byte)(Variables.Presentes / 256);
        datos[11] = Defines.CONFIG_VERSION;
        datos[12] = (Variables.ProgSel) ? (byte)1 : 0;
        datos[13] = (byte)(Variables.Tachos4Bin & 255);
        datos[14] = (byte)(Variables.Tachos4Bin / 256);
        datos[15] = (byte)(Variables.Bin4Camion & 255);
        datos[16] = (byte)(Variables.Bin4Camion / 256);
        datos[17] = (byte)(Variables.TachoCajaCnt & 255);
        datos[18] = (byte)(Variables.TachoCajaCnt / 256);
        datos[19] = (byte)(Variables.BinCnt & 255);
        datos[20] = (byte)(Variables.BinCnt / 256);
        datos[21] = (byte)(Variables.CamionCnt & 255);
        datos[22] = (byte)(Variables.CamionCnt / 256);
        datos[23] = (byte)(Variables.TotalTachos & 255);
        datos[24] = (byte)(Variables.TotalTachos / 256);
        datos[25] = (byte)(Variables.Cajas4Pallet & 255);
        datos[26] = (byte)(Variables.Cajas4Pallet / 256);
        datos[27] = (byte)(Variables.Pallet4Camion & 255);
        datos[28] = (byte)(Variables.Pallet4Camion / 256);
        datos[29] = (byte)(Variables.AlarmTimeOut & 255);
        datos[30] = (byte)(Variables.AlarmTimeOut / 256);

        byte[] c = Variables.Cuadrilla.getBytes();
        Library.byteArrayCopy(c, 0, datos, 31, 10);

        datos[41] = (byte)Variables.logInx;
        datos[42] = (byte)Variables.Viaje;
        datos[43] = (byte)Variables.RemitoInx;

        byte[]aux = Variables.sWiFiURL.getBytes();
        datos[100] = (byte)aux.length;
        for (i=0; i<aux.length; i++)
        {
            datos[101+i]=aux[i];
            if (i >= 49)
                break;
        }
        aux = Variables.sWebServiceURL.getBytes();
        datos[150]=(byte)aux.length;
        for (i=0; i<aux.length;i++)
        {
            datos[151+i]=aux[i];
            if (i>=49)
                break;
        }
        if (Variables.sMailAddr.length()>0)
        {
            aux = Variables.sMailAddr.getBytes();
            datos[200]=(byte)aux.length;
            for (i=0; i<aux.length;i++)
            {
                datos[201+i]=aux[i];
                if (i>=49)
                    break;
            }
        }
        else
            datos[200]=0;
        try {
            RecordStore record = RecordStore.openRecordStore("Config", true, Defines.OPEN_WRITE);
            if (record.getNumRecords() == 0)
                record.addRecord(datos, 0, 300);
            else
                record.setRecord(1, datos, 0, 300);
            record.closeRecordStore();
        }catch(Exception e){}
    }


    public static void InitProgramFile()
    {
        int i, j;
        RecordStore record;

        String progDef[][]={
                {"0000AA", "NORTON  ", "0001", "NINGUNO ", "Generica", "  1", "0", "Generica"},
                {"0000BB", "NORTON  ", "0002", "NINGUNO ", "Generica", "  1", "0", "Generica"},
                {"0000CC", "NORTON  ", "0003", "NINGUNO ", "Generica", "  1", "0", "Generica"},
                {"0000DD", "NORTON  ", "0004", "NINGUNO ", "Generica", "  1", "1", "Generica"},
                {"0000EE", "NORTON  ", "0005", "NINGUNO ", "Generica", "  1", "1", "Generica"},
                {"0000FF", "NORTON  ", "0006", "NINGUNO ", "Generica", "  1", "1", "Generica"},
        };

        try {
            RecordStore.deleteRecordStore("Programas");
            RecordStore.deleteRecordStore("CamionReg");
        }catch(Exception e){}
        try {
            record = RecordStore.openRecordStore("Programas", true, Defines.OPEN_WRITE);
            for (i=0; i<6; i++)
            {
                int x = 0;
                byte[] datos = new byte[Defines.PRG_LEN];
                for (j=0; j<8; j++)
                {
                    byte aux[] = progDef[i][j].getBytes();
                    Library.byteArrayCopy(aux, 0, datos, x);
                    x += progDef[i][j].length();
                }
                record.addRecord(datos, 0, Defines.PRG_LEN);
            }
            record.closeRecordStore();
        }catch (Exception e){}
        Variables.Finca = "Ninguno";
        Variables.Cuartel = "0001";
        Variables.ModoCosecha = 0;
        Variables.CuadrillaPrg = "Ninguno";
        Variables.Programa = "NO DEFINIDO";
        Variables.Area = "Ninguno";
        Variables.VariedadUva = "Generica";
        Variables.ProgInx = 0;
        Variables.ProgSel = false;
        Variables.Cosechador_name = " ";
        Variables.Cosechador_count= " ";
        Variables.Cosechador_legajo = " ";
        Variables.CosechadorLastTime = -1;
    }

    public static void splitPrograma(byte[] datos, String txt[])
    {
        byte prog[]= new byte[6];
        boolean flag = false;

        Library.byteArrayCopy(datos, prog);
        String s = new String(prog);
        txt[0] = s;
        switch (s) {
            case "0000AA":
                s = "1er CUARTEL";
                break;
            case "0000BB":
                s = "2do CUARTEL";
                break;
            case "0000CC":
                s = "3er CUARTEL";
                break;
            case "0000DD":
                s = "4to CUARTEL";
                break;
            case "0000EE":
                s = "5to CUARTEL";
                break;
            case "0000FF":
                s = "6to CUARTEL";
                break;
            default:
                flag = true;
                s = "PRG"+s;
                break;
        }
        txt[1] = s;

        byte Finca[] = new byte[8];
        Library.byteArrayCopy(datos, 6, Finca, 0);
        txt[2] = new String(Finca);
        byte cuartel[] = new byte[4];
        Library.byteArrayCopy(datos, 14, cuartel, 0);
        txt[3] = new String(cuartel);
        if (flag)
            txt[7]=txt[2]+"-"+txt[3];
        else
            txt[7]=s;
        byte area[] = new byte[8];
        Library.byteArrayCopy(datos, 18, area, 0);
        txt[4] = new String(area);
        byte CuadrillaPrg[]=new byte[8];
        Library.byteArrayCopy(datos, 26, CuadrillaPrg, 0);
        txt[5] = new String(CuadrillaPrg);
        byte cantidad[]=new byte[3];
        Library.byteArrayCopy(datos, 34, cantidad, 0);
        txt[6] = new String(cantidad);
        byte modoCosecha[]=new byte[1];
        Library.byteArrayCopy(datos, 37, modoCosecha, 0);
        txt[8] = new String(modoCosecha);
        if (modoCosecha[0]== '1')
            txt[1]+="-CAJAS";
        else
            txt[1]+="-BINES";
        byte variedadUva[]=new byte[8];
        Library.byteArrayCopy(datos, 38, variedadUva, 0);
        txt[9] = new String(variedadUva);
    }

    public static int GetClock()
    {
        return (int)(System.currentTimeMillis()/1000)+Variables.DiffClock;
    }

}
