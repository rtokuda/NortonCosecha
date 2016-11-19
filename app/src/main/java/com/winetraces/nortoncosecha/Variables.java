package com.winetraces.nortoncosecha;

import android.app.ActivityManager;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by nestor on 30/10/2016.
 */

public class Variables {
    /** Key code constant: Unitech Menu Key */
    public static final int KEYCODE_UNITECH_MENU = 247;
    /** Key code constant: Unitech Search key   */
    public static final int KEYCODE_UNITECH_SEARCH = 248;

    public static int DiffTime; //Tiempo de espera entre tarjetas
    public static long CosechadorLastTime = 0;
    public static boolean bFlagCosecha = false;
    public static boolean mifareEnable = false;
    public static boolean mainEnable = false;
    public static boolean keyEnable = false;
    public static byte ProgInx;
    public static String ProgID;
    public static String Programa;
    public static String Area;
    public static String Finca;
    public static String Cuartel;
    public static String Cuadrilla;
    public static String CuadrillaPrg;
    public static String VariedadUva;
    public static String Cosechador_name;
    public static String Cosechador_count;
    public static String Cosechador_legajo;

    public static int FechaProg;
    public static int DiffClock; //Diferencia de reloj tiempo real
    public static int Presentes;
    public static boolean ProgSel;
    public static int DeviceID;
    public static int ErrNum;
    public static int ModoCosecha;
    public static int TachoCajaCnt;
    public static int TotalTachos;
    public static int Tachos4Bin;
    public static int Bin4Camion;
    public static int BinCnt;
    public static int CamionCnt;
    public static int Cajas4Pallet;
    public static int Pallet4Camion;
    public static int AlarmTimeOut;
    public static int logInx;
    public static int Viaje;
    public static int RemitoInx;
    public static String BinNumber;

    public static String sWiFiURL;
    public static String sWebServiceURL;
    public static String sMailAddr;

    private boolean error;
    public static int csCount;

    public static String errmsg = "1 ";
    public static SQLiteDatabase SQLdb = null;

}
