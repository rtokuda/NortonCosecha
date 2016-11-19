package com.winetraces.nortoncosecha;

/**
 * Created by nestor on 05/11/2016.
 */

public class Defines {
    public final static int CONFIG_VERSION = 11;

    public final static int COSECHADOR_TIMEOUT = 30;

    public final static int MODO_TACHO = 0;
    public final static int MODO_CAJA = 1;

    public final static boolean CardTest = false;
    public final static boolean TimeTest = false;

    public final static int NO_ERROR = 0;
    public final static int ERROR_CARD_BADFORMAT = 1;
    public final static int ERROR_CARD_EXCEPTION = 2;
    public final static int ERROR_CARD_WRITE = 3;

    public final static int KEY_A = 0;
    public final static int KEY_B = 0;

    public final static String COSECHADOR = "CCH";
    public final static String CAMIONREG = "CMR";
    public final static String CAMIONHDR = "CMH";

    public final static int T_COSECHADOR = 1;
    public final static int T_CAMION = 2;
    public final static int T_BIN = 3;

    static public final int DEF_FIELD = 0;

    static public final int TIPO_LOG = 32;
    static public final int FECHA = 33;
    static public final int FINCA = 34;
    static public final int CUADRILLA = 35;
    static public final int CUARTEL = 36;
    static public final int AREA = 37;
    static public final int FICHAS = 38;
    static public final int LEGAJO = 39;
    static public final int NOMBRE = 40;
    static public final int CANTIDAD = 41;
    static public final int TOTAL = 42;
    static public final int LEGAJOABREV = 43;
    static public final int PROGRAMA = 44;
    static public final int CUADRILLAPRG = 45;
    static public final int VARIEDAD = 46;

    static public final int CONTENIDO = 50;
    static public final int BINCODE = 51;
    static public final int CAMIONCNT = 52;
    static public final int MODO_COSECHA = 53;

    static public final int R_FINCA = 70;
    static public final int R_CUARTEL = 71;
    static public final int R_AREA = 72;
    static public final int R_VARIEDAD = 73;
    static public final int PRG_LEN = 46;

    static public final String prtLine = "--------------------------------------";
    static public final String[] meses = {"Ene","Feb","Mar","Abr","May","Jun",
            "Jul","Ago","Sep","Oct","Nov","Dic"
    };
    static byte[] trailer = {
            (byte)0xC0,(byte)0xC1,(byte)0xC2,(byte)0xC3,(byte)0xC4,(byte)0xC5,
            (byte)0x7F,(byte)0x07,(byte)0x88,(byte)0xFF,
            (byte)0xD0,(byte)0xD1,(byte)0xD2,(byte)0xD3,(byte)0xD4,(byte)0xD5};

}
