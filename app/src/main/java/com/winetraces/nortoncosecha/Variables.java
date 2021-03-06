package com.winetraces.nortoncosecha;


import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.winetraces.wifimanager.WifiApManager;

/**
 * Created by nestor on 30/10/2016.
 */

public class Variables {
    /** Key code constant: Unitech Menu Key */
    public static final int KEYCODE_UNITECH_MENU = 247;
    /** Key code constant: Unitech Search key   */
    public static final int KEYCODE_UNITECH_SEARCH = 248;

    public static int WaitCardTime; //Tiempo de espera entre tarjetas
    public static long CosechadorLastTime = 0;
    public static boolean bFlagCosecha = false;
    public static boolean mifareEnable = false;
    public static boolean mainEnable = false;
    public static boolean keyEnable = false;
    public static boolean Cosechador_waiting = false;
    public static byte ProgInx;
    public static String ProgID;
    public static String Programa;
    public static String Area;
    public static String Finca;
    public static String Cuartel;
    public static String Cuadrilla;
    public static String Unidad;
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

    public static String sPrinterURL;
    public static String sWebServiceURL;
    public static String sMailAddr;

    public static int csCount;

    public static int CardType = 0;
    public static int PrintType = 0;
    public static String errmsg = "1 ";
    public static String msg;
//    public static SQLiteDatabase SQLdb = null;
  //  public static ProgressDialog wProgress;
    public static Context pContext;
    public static String sProgressMsg="";

    public static String url;

    //public static WebService ws;
    //public static WifiApManager wfAP;

    static public View currView = null;
    static public String msgTxt = "";
    static public int writeTiming = 0;
    static public String toastMsg = "";
    static public int ProgressCosechador=0;
}
