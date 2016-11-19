package com.winetraces.nortoncosecha;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.widget.TextView;

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
        if (s.equals("PRG") && (CardId[5] == 2)) {
            //return(CardPrograma());
            return true;
        }
        if (s.equals("CHG") && (CardId[5] == 4)) {
            //return (CambiaBin(false));
            return true;
        }
        if (s.equals("BIN") && (CardId[5] == 6)) {
            //return (CambiaBin(true));
            return true;
        }
        if (s.equals("CAM") && (CardId[5] == 3)) {
            // SendCamion();
            return true;
        }
        if (CardId[5] == 1) {
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
                saveRecord(diaAct, Contador, Legajo, Cosechador, Hoy);
            }
        });
        th.start();
        return true;
    }


    private static void saveRecord(int diaAct, byte[]Contador, byte[] Legajo, byte[]Cosechador, Calendar Hoy)
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
            inx = Library.setField(buff, Defines.COSECHADOR, inx, Defines.TIPO_LOG);
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
}
