package com.winetraces.nortoncosecha;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.winetraces.recordstore.RecordEnumeration;
import com.winetraces.recordstore.RecordStore;

import java.util.Calendar;


/**
 * Created by nestor on 05/11/2016.
 */

public class Cosecha {
    private static int horaAct, mm;
    private static int _cantidad, _total;
    private static String _legajo;
    private static int _legajoAbrev;
    private static String _cosechador;
    private static byte[] _legajoAbreviado = new byte[2];
    int counter;
    static String _chofer;
    static String _patente;
    static String _camion;

    public static boolean CardProcess(Context context, Intent intent) {
        byte i;
        byte[] CardId;

        byte[] authent_card = {(byte) 0x01, (byte) 0x4E, (byte) 0x52,
                (byte) 0x54, (byte) 0x31};

        for (i = 0; i < 16; i++) {
            MifareIO.ReadBuff[0][i] = 0;
            MifareIO.ReadBuff[1][i] = 0;
        }

        if (!MifareIO.read(context, intent, Defines.KEY_A, 1, 2)) {
            Variables.errmsg = "Error Tarjeta";
            return false;
        }
        CardId = MifareIO.ReadBuff[0];
        for (i = 0; i < 5; i++) {
            if (CardId[i] != authent_card[i]) {
                Variables.errmsg = context.getString(R.string.err_CardInvalid);
                return false;
            }
        }
        byte aux[] = new byte[3];
        Library.byteArrayCopy(CardId, 6, aux, 0);
        String s = new String(aux);

        if (s.equals(Defines.THDR_PROGRAMA) && (CardId[5] == 2)) {
            Variables.CardType = Defines.T_PROGRAMA;
            //return(CardPrograma());
            return true;
        }
        if (s.equals(Defines.THDR_CHANGE) && (CardId[5] == 4)) {
            Variables.CardType = Defines.T_CHANGE;
            return (CambiaBin(context, intent, false));
        }
        if (s.equals(Defines.THDR_BIN) && (CardId[5] == 6)) {
            Variables.CardType = Defines.T_BIN;
            return (CambiaBin(context, intent, true));
        }
        if (s.equals(Defines.THDR_CAMION) && (CardId[5] == 3)) {
            Variables.CardType = Defines.T_CAMION;
            return(SendCamion(context, intent));
        }
        if (CardId[5] == 1) {
            Variables.CardType = Defines.T_COSECHADOR;
            if (!Variables.ProgSel) {
                return false;
            }
            return (CardCosecha(context, intent));

        }
        Variables.errmsg = context.getString(R.string.err_CardInvalid);
        return false;
    }

    private static boolean CardCosecha(Context contexto, Intent intent) {
        final byte[] Cosechador = new byte[16];
        final byte[] Legajo = new byte[16];
        final byte[] Contador = new byte[16];
        byte[] WriteData = new byte[16];
        byte[][] wd = new byte[3][16];
        int i;
        boolean flg;

        String hora = "";
        horaAct = NortonCosecha.GetClock();
        final Calendar Hoy = Library.Fecha(horaAct * 1000L);

        Library.byteArrayCopy(MifareIO.ReadBuff[1], Cosechador);
        for (i = 0; i < 16; i++) {
            MifareIO.ReadBuff[0][i] = 0;
            MifareIO.ReadBuff[1][i] = 0;
        }
        if (!MifareIO.read(contexto, intent, Defines.KEY_A, 4, 2)) {
            //NortonCosecha.setError(20);
            return false;
        }
        flg = false;
        for (i = 0; i < 16; i++) {
            if ((MifareIO.ReadBuff[0][i] != 0) || (MifareIO.ReadBuff[1][i] != 0)) {
                flg = true;
                break;
            }
        }
        if (flg) {
            flg = false;
            for (i = 0; i < 16; i++) {
                if (MifareIO.ReadBuff[1][i] != Cosechador[i]) {
                    flg = true;
                    break;
                }
            }
        }
        if (!flg) {
            // NortonCosecha.setError(21);
            return false;
        }
        Library.byteArrayCopy(MifareIO.ReadBuff[0], Contador);
        Library.byteArrayCopy(MifareIO.ReadBuff[1], Legajo);

        long TimeAnt = Library.fromIntelDataIntLE(Contador, 8);
        _cantidad = (Library.fromIntelDataIntLE(Contador, 0) & 0xffff);
        _cantidad++;
        _total = (Library.fromIntelDataIntLE(Contador, 4) & 0xffff);
        _total++;

        byte[] L = new byte[6];
        Library.byteArrayCopy(Legajo, 0, L, 0);
        _legajo = new String(L);
        _cosechador = Library.String(Cosechador);
        _legajoAbreviado[0] = Legajo[6];
        _legajoAbreviado[1] = Legajo[7];
        _legajoAbrev = Library.Ubyte(Legajo[6]) + Library.Ubyte(Legajo[7]) * 256;

        int diff = (horaAct % 86400) - ((int) (TimeAnt % 86400L));
        int DiffTime = Variables.DiffTime;
        if (diff < 0) {
            diff = DiffTime;
            if (-diff > DiffTime)
                diff = DiffTime;
        }
        Variables.CosechadorLastTime = TimeAnt;
        if (diff < DiffTime) {
            Variables.Cosechador_name = _cosechador;
            Variables.Cosechador_legajo = _legajo;
            Variables.Cosechador_count = Library.padNum(_cantidad - 1, 3);
            return true;
        }
        Variables.CosechadorLastTime = horaAct;
        final int diaAct = horaAct / 86400;
        if (diaAct != (Library.fromIntelDataIntLE(Contador, 8) / 86400))
            _cantidad = 1;

        Library.toIntelDataInt(_cantidad, WriteData, 0);
        Library.toIntelDataInt(_total, WriteData, 4);
        Library.toIntelDataInt(horaAct, WriteData, 8);
        Library.byteArrayCopy(WriteData, wd[0]);
        if (!MifareIO.write(contexto, intent, wd, Defines.KEY_A, 4, 1))
            return false;
        Calendar Fecha = Library.Fecha(TimeAnt * 1000L);
        int hh = Fecha.get(Calendar.HOUR_OF_DAY);
        int mm = Fecha.get(Calendar.MINUTE);
        if (_cantidad == 1)
            hora = "00:00";
        else
            hora = Library.pad(hh, 2, '0', false) + ":" + Library.pad(mm, 2, '0', false);
        //MainScr.text("  Ultima carga: "+hora, 4);

        Variables.TachoCajaCnt++;
        Variables.TotalTachos++;
        Variables.Cosechador_name = _cosechador;
        Variables.Cosechador_legajo = _legajo;
        Variables.Cosechador_count = Library.padNum(_cantidad, 3);

        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                NortonCosecha.SaveConfig();
                saveRecordCosecha(diaAct, Contador, Legajo, Cosechador, Hoy);
            }
        });
        th.start();
        return true;
    }

    private static void saveRecordCosecha(int diaAct, byte[]Contador, byte[] Legajo, byte[]Cosechador, Calendar Hoy)
    {
        byte[] WriteData = new byte[16];

        if (diaAct != (Library.fromIntelDataIntLE(Contador, 8)/86400))
        {
            Variables.Presentes++;
            NortonCosecha.SaveConfig();
            RecordStore record = null;
            try {
                byte W[] = new byte[22];
                try {
                    record = RecordStore.openRecordStore("Presente", true);
                    W[0]=Legajo[6];
                    W[1]=Legajo[7];
                    Library.toIntelDataInt(horaAct, W, 2);
                    Library.byteArrayCopy(Cosechador, 0, W, 6);
                    record.addRecord(W, 0, 22);
                    record.closeRecordStore();
                }catch (Exception e)
                {
                    if (record != null)
                        record.closeRecordStore();
                }
                record = null;
                String fname = "NLOG"+Integer.toString(Hoy.get(Calendar.YEAR))+
                        Library.padNum(Hoy.get(Calendar.MONTH)+1, 2)+
                        Library.padNum(Hoy.get(Calendar.DAY_OF_MONTH), 2);
                record = RecordStore.openRecordStore(fname, true);

                byte[] datos = new byte [11];
                datos[0] = 1;

                byte[] aux = new byte[4];
                Library.toIntelDataInt(Variables.DeviceID, aux, 0);
                aux = Library.arrayInvert(aux);
                Library.byteArrayCopy(aux, 0, datos, 1);

                datos[5]=_legajoAbreviado[0];
                datos[6]=_legajoAbreviado[1];

                Library.toIntelDataInt(horaAct, aux, 0);
                aux = Library.arrayInvert(aux);
                Library.byteArrayCopy(aux, 0, datos, 7);

                record.addRecord(datos, 0, 11);
                record.closeRecordStore();
                record = null;

            }catch(Exception e){}
            if (record != null)
            {
                try{
                    record.closeRecordStore();
                }catch(Exception e){}
            }
        }
        Library.toIntelDataInt(horaAct, WriteData, 0);
        int LegajoAbrev = Library.fromIntelDataWord(_legajoAbreviado, 0);
        RecordStore record = null;
        try {
            record = RecordStore.openRecordStore("0"+LegajoAbrev,true);
            record.addRecord(WriteData,0,4);
            record.closeRecordStore();
            record = null;
        } catch (Exception e){}
        if (record != null)
        {
            try {
                record.closeRecordStore();
            }catch(Exception e){}
        }
        record = null;
        try {
            String fname = "NLOG"+Integer.toString(Hoy.get(Calendar.YEAR))+
                    Library.padNum(Hoy.get(Calendar.MONTH)+1, 2)+
                    Library.padNum(Hoy.get(Calendar.DAY_OF_MONTH), 2);
            record = RecordStore.openRecordStore(fname, true);

            byte[] datos = new byte [17];
            datos[0] = 2;

            byte[] aux = Variables.ProgID.getBytes();
            Library.byteArrayCopy(aux, 0, datos, 1);

            aux = new byte[4];
            Library.toIntelDataInt(Variables.DeviceID, aux, 0);
            aux = Library.arrayInvert(aux);
            Library.byteArrayCopy(aux, 0, datos, 7);

            datos[11]=_legajoAbreviado[0];
            datos[12]=_legajoAbreviado[1];

            aux = new byte[4];
            Library.toIntelDataInt(horaAct, aux, 0);
            aux = Library.arrayInvert(aux);
            Library.byteArrayCopy(aux, 0, datos, 13);

            record.addRecord(datos, 0, 17);
            record.closeRecordStore();
            record = null;

        }catch(Exception e){e.printStackTrace();}
        if (record != null)
        {
            try {
                record.closeRecordStore();
            }catch(Exception e){}
        }
        record = null;
        try {
            String fname = "RECORD"+Integer.toString(Variables.logInx);
            record = RecordStore.openRecordStore(fname, true);
            byte[] buff = new byte[512];
            horaAct = NortonCosecha.GetClock();
            int inx = Library.setField(buff, Integer.toString(horaAct), 0, Defines.FECHA);
            inx = Library.setField(buff, Defines.R_COSECHADOR, inx, Defines.TIPO_LOG);
            inx = Library.setField(buff, Variables.Programa, inx, Defines.PROGRAMA);
            inx = Library.setField(buff, Variables.Finca, inx, Defines.FINCA);
            inx = Library.setField(buff, Variables.Cuadrilla, inx, Defines.CUADRILLA);
            inx = Library.setField(buff, Variables.CuadrillaPrg, inx, Defines.CUADRILLAPRG);
            inx = Library.setField(buff, Variables.VariedadUva, inx, Defines.VARIEDAD);
            inx = Library.setField(buff, Variables.Cuartel, inx, Defines.CUARTEL);
            inx = Library.setField(buff, Variables.Area, inx, Defines.AREA);
            inx = Library.setField(buff, _legajo, inx, Defines.LEGAJO);
            inx = Library.setField(buff, _cosechador, inx, Defines.NOMBRE);
            inx = Library.setField(buff, Integer.toString(_cantidad), inx, Defines.CANTIDAD);
            inx = Library.setField(buff, Integer.toString(_total), inx, Defines.TOTAL);
            inx = Library.setField(buff, Integer.toString(_legajoAbrev), inx, Defines.LEGAJOABREV);
            record.addRecord(buff, 0, inx);
            record.closeRecordStore();
            record = null;
        }catch(Exception e){e.printStackTrace();}
        if (record != null)
        {
            try {
                record.closeRecordStore();
            }catch(Exception e){}
        }
        record = null;
    }

    private static boolean CambiaBin(Context context, Intent intent, boolean flag) {
        String msg;
        int Historico;
        byte[] Contador = new byte[16];
        byte[] BinNum = new byte[8];

        MifareIO.read(context, intent, Defines.KEY_A, 4, 2);
        Library.byteArrayCopy(MifareIO.ReadBuff[0], Contador);
        if (flag)
            Library.byteArrayCopy(MifareIO.ReadBuff[1], BinNum);
        Historico = Library.fromIntelDataIntLE(Contador, 0);
        Variables.BinNumber = new String(BinNum);

        msg = "Se cierra el ";
        if (Variables.ModoCosecha == Defines.MODO_CAJA) {
            if (flag)
                msg += "Pallet No. " + Variables.BinNumber;
            else
                msg += "Pallet No. " + Variables.BinCnt;
            msg += "\r\n Con " + Variables.TachoCajaCnt;
            if (Variables.TachoCajaCnt == 1)
                msg += " caja";
            else
                msg += " cajas";
        } else {
            if (flag)
                msg += "Bin No. " + Variables.BinNumber;
            else
                msg += "Bin No. " + Variables.BinCnt;
            msg += "\r\n Con " + Variables.TachoCajaCnt;
            if (Variables.TachoCajaCnt == 1)
                msg += " tacho";
            else
                msg += " tachos";
        }
        Variables.msg = msg;
        Historico++;
        byte[] wd = new byte[16];
        Library.toIntelDataInt(Historico, wd, 0);
        Library.toIntelDataInt(Variables.BinCnt, wd, 4);
        Library.toIntelDataInt(horaAct, wd, 8);
        if (!MifareIO.write(context, intent, wd, Defines.KEY_A, 4))
            return false;

        Variables.BinCnt++;
        Variables.CamionCnt++;
        RecordStore record = null;
        try {
            record = RecordStore.openRecordStore("CamionReg", true);
            byte[] buff = new byte[512];
            horaAct = NortonCosecha.GetClock();
            int inx = Library.setField(buff, Integer.toString(horaAct), 0, Defines.FECHA);
            inx = Library.setField(buff, Defines.R_CAMIONREG, inx, Defines.TIPO_LOG);
            inx = Library.setField(buff, Integer.toString(Variables.TachoCajaCnt), inx, Defines.CONTENIDO);
            if (flag)
            {
                String s = Variables.BinNumber+"              ";
                s = s.substring(1, 8);
                inx = Library.setField(buff, s, inx, Defines.BINCODE);
            }
            else
                inx = Library.setField(buff, "99"+Library.padNum(Historico,6), inx, Defines.BINCODE);
            inx = Library.setField(buff, Integer.toString(Variables.CamionCnt), inx, Defines.CAMIONCNT);
            inx = Library.setField(buff, Integer.toString(Variables.ModoCosecha), inx, Defines.MODO_COSECHA);
            record.addRecord(buff, 0, inx);
            record.closeRecordStore();
        }catch(Exception e){e.printStackTrace();}
        NortonCosecha.SaveConfig();
        Variables.TachoCajaCnt = 0;
        return true;
    }

    private static boolean SendCamion(Context context, Intent intent)
    {
        byte[] Nombre = new byte[16];
        byte[] Info = new byte[16];
        byte[] Camion = new byte[16];

        Library.byteArrayCopy(MifareIO.ReadBuff[1], Nombre);
        if (!MifareIO.read(context, intent, Defines.KEY_A,4,3))
            return false;
        Library.byteArrayCopy(MifareIO.ReadBuff[0], Info);
        Library.byteArrayCopy(MifareIO.ReadBuff[1], Camion);

        _chofer = new String(Nombre);
        byte[] p = new byte[6];
        Library.byteArrayCopy(Info, 0, p, 0);
        _patente = new String(p);
        _camion = new String(Camion);

        String msg = "Se ha despachado el camion ";
        msg += "\r\n"+_camion;
        msg +="\r\npatente "+_patente;
        msg += "\r\nde "+_chofer;
        msg += "\r\ncon "+Variables.CamionCnt;
        if (Variables.ModoCosecha == Defines.MODO_CAJA)
        {
            if (Variables.CamionCnt == 1)
                msg += " pallet";
            else
                msg += " pallets";
        }
        else
        {
            if (Variables.CamionCnt == 1)
                msg += " bin";
            else
                msg += " bines";
        }
        PrintRemito();
        Variables.CamionCnt = 0;
        NortonCosecha.SaveConfig();
        Variables.msg = msg;
        /*
        NortonCosecha.backLightON(30);
        Alert alert = new Alert("Informacion", msg, null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        alert.setCommandListener(this);
        NortonCosecha.display.setCurrent(alert);
        enable = false;
        */
        return true;
    }

    static void PrintRemito()
    {
        int tipo = 0;
        String sRemito="";
        RecordStore recordW = null;
        RecordStore recordR = null;
        RecordStore recordBK = null;

        Calendar Fecha = Library.Fecha(NortonCosecha.GetClock()*1000L);
        try {
            RecordStore.deleteRecordStore("Remito");
        }catch (Exception e){}
        try {
            RecordStore.deleteRecordStore("CamionBK"+Variables.RemitoInx);
        }catch (Exception e){}
        try {
            recordW = RecordStore.openRecordStore("Remito", true);
            recordW.addRecord(" ");
            recordW.addRecord("Bodega Norton S.A.  Cosecha "+Fecha.get(Calendar.YEAR));
            recordW.addRecord(" ");
            recordW.addRecord("       Cuadrilla - "+Variables.Cuadrilla);
            recordW.addRecord(" ");
            recordW.addRecord("Remito de transporte");
            recordW.addRecord("Camion: "+_camion);
            recordW.addRecord("Patente: "+_patente);
            recordW.addRecord("Chofer: "+_chofer);
            recordW.addRecord(Defines.prtLine);
            Variables.Viaje++;
            String s=" Viaje: "+Library.padNum(Variables.Viaje,3);
            recordW.addRecord(s);
            recordW.addRecord(Defines.prtLine);
            recordW.addRecord("   Bin    Tachos      Hora");
            recordW.addRecord(Defines.prtLine);
            recordR = RecordStore.openRecordStore("CamionReg", true);
            recordBK = RecordStore.openRecordStore("CamionBK"+Variables.RemitoInx, true);
            RecordEnumeration rd = recordR.enumerateRecords(null, null, false);
            int bines = 0;
            int tachosTot =0;
            while( rd.hasNextElement() )
            {
                int ID = rd.nextRecordId();
                byte[] rec = recordR.getRecord(ID);
                recordBK.addRecord(rec, 0, rec.length);
                String tipoLog = new String(Library.getField(rec, Defines.TIPO_LOG, true));
                if (tipoLog.equals(Defines.R_CAMIONHDR))
                {
                    s="Finca: "+new String(Library.getField(rec, Defines.R_FINCA, true))+"                         ";
                    s = s.substring(0,17);
                    s +=" Cuartel: "+new String(Library.getField(rec, Defines.R_CUARTEL, true));
                    recordW.addRecord(s);
                    s="Area: "+new String(Library.getField(rec, Defines.R_AREA, true))+"                            ";
                    s = s.substring(0,17);
                    s+="Variedad: "+new String(Library.getField(rec, Defines.R_VARIEDAD, true));
                    recordW.addRecord(s);
                }
                if (tipoLog.equals(Defines.R_CAMIONREG))
                {
                    try {
                        int hora = Integer.parseInt(new String(Library.getField(rec, Defines.FECHA, true)));
                        Fecha = Library.Fecha(hora*1000L);
                        int cnt = Integer.parseInt(new String(Library.getField(rec, Defines.CAMIONCNT, true)));
                        s = Library.padNum(cnt, 3);
                        s += " "+new String(Library.getField(rec, Defines.BINCODE, true));
                        int tachos = Integer.parseInt(new String(Library.getField(rec, Defines.CONTENIDO, true)));
                        tachosTot+=tachos;
                        s += "    "+Library.padNum(tachos, 2);
                        s += "    "+Library.padNum(Fecha.get(Calendar.HOUR_OF_DAY),2)+":"+
                                Library.padNum(Fecha.get(Calendar.MINUTE),2)+":"+
                                Library.padNum(Fecha.get(Calendar.SECOND),2);
                    }catch (Exception e){s = "ERROR BIN";}

                    recordW.addRecord(s);
                    tipo = Integer.parseInt(new String(Library.getField(rec, Defines.MODO_COSECHA, true)));
                    bines++;
                }
            }
            recordW.addRecord(Defines.prtLine);
            s = "Total "+bines;
            if (tipo == Defines.MODO_TACHO)
            {
                if (bines == 1)
                    s += " bin ("+tachosTot+") tachos";
                else
                    s += " bines ("+tachosTot+") tachos";
            }
            else
            {
                if (bines == 1)
                    s += " pallet ("+tachosTot+") cajas";
                else
                    s += " pallets ("+tachosTot+") cajas";
            }
            recordW.addRecord(s);
            Fecha = Library.Fecha(NortonCosecha.GetClock()*1000L);
            s=" Fecha: "+Fecha.get(Calendar.DAY_OF_MONTH)+"/"+
                    Defines.meses[Fecha.get(Calendar.MONTH)]+"/"+Fecha.get(Calendar.YEAR)+"  "+
                    Library.padNum(Fecha.get(Calendar.HOUR_OF_DAY),2)+":"+
                    Library.padNum(Fecha.get(Calendar.MINUTE),2)+":"+
                    Library.padNum(Fecha.get(Calendar.SECOND),2);
            recordW.addRecord(s);
            recordW.addRecord(Defines.prtLine);
        }catch(Exception e){e.printStackTrace();}
        try {
            recordW.closeRecordStore();
            recordR.closeRecordStore();
            recordBK.closeRecordStore();
        }catch(Exception e){};

        try {
            RecordStore.deleteRecordStore("CamionReg");
        }catch (Exception e){}
        try {
            RecordStore record = RecordStore.openRecordStore("CamionReg", true);
            byte[] buff = new byte[512];
            int inx = Library.setField(buff, Defines.R_CAMIONHDR, 0, Defines.TIPO_LOG);
            inx = Library.setField(buff, Variables.Finca, inx, Defines.R_FINCA);
            inx = Library.setField(buff, Variables.Cuartel, inx, Defines.R_CUARTEL);
            inx = Library.setField(buff, Variables.Area, inx, Defines.R_AREA);
            inx = Library.setField(buff, Variables.VariedadUva, inx, Defines.R_VARIEDAD);
            record.addRecord(buff, 0, inx);
            record.closeRecordStore();
        }catch(Exception e){e.printStackTrace();}
        try {
            RecordStore.deleteRecordStore("printBuffer");
        }catch (Exception e){}
        try {
            RecordStore.deleteRecordStore("RemitoBK"+Variables.RemitoInx);
        }catch (Exception e){}
        try {
            recordW = RecordStore.openRecordStore("printBuffer", true);
            recordBK = RecordStore.openRecordStore("RemitoBK"+Variables.RemitoInx, true);
            byte[] attrib = new byte[512];
            for (int i=0; i<512; i++)
                attrib[i] = 0;
            recordW.addRecord(attrib, 0, 512);
            recordR = RecordStore.openRecordStore("Remito", true);
            RecordEnumeration rd = recordR.enumerateRecords(null, null, false);
            sRemito = "";
            while( rd.hasNextElement() )
            {
                int ID = rd.nextRecordId();
                byte[] rec = recordR.getRecord(ID);
                for (int i=0; i<rec.length; i++)
                {
                    if ((rec[i]<32)||(rec[i]>128))
                        rec[i]=32;
                }
                sRemito += new String(rec);
                sRemito += "\r\n";
                recordW.addRecord(rec, 0, rec.length);
                recordBK.addRecord(rec, 0, rec.length);
            }
        }catch (Exception e){}
        try {
            recordR.closeRecordStore();
            recordW.closeRecordStore();
            recordBK.closeRecordStore();
        }catch (Exception e){};

        Print pr = new Print();
        (new Thread(pr)).start();
     /*   if (NortonCosecha.sMailAddr.length()>0)
        {
            NortonCosecha.BasicMail(sRemito);
        }*/
        Variables.RemitoInx++;
        if (Variables.RemitoInx>=20)
            Variables.RemitoInx = 0;
        NortonCosecha.SaveConfig();
    }
}
