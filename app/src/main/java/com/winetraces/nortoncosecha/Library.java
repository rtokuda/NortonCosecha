package com.winetraces.nortoncosecha;

/**
 * Created by nestor on 05/11/2016.
 */

import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

public class Library {

    /**
     * Pads a string left|right with [length] characters
     *
     * @param str
     * @param length
     * @param fill
     * @param rightPadding
     * @return
     */
    public static String pad(String str, int length, char fill, boolean rightPadding) {
        StringBuffer paddedStr = new StringBuffer();

        for (int i = 0; i < length - str.length(); i++) {
            paddedStr.append(fill);
        }
        return (rightPadding ? str + paddedStr : paddedStr + str);
    }

    /**
     * Pads a number left|right with [length] characters
     *
     * @param number
     * @param length
     * @param fill
     * @param rightPadding
     * @return
     */
    public static String pad(int number, int length, char fill, boolean rightPadding) {
        StringBuffer paddedStr = new StringBuffer();

        for (int i = 0; i < length - String.valueOf(number).length(); i++) {
            paddedStr.append(fill);
        }
        return (rightPadding ? String.valueOf(number) + paddedStr : paddedStr + String.valueOf(number));
    }

    public static void byteArrayCopy(byte[] source, int srcOff, byte[] dest, int dstOff) {
        for (int i = 0; i < (source.length - srcOff) && i < (dest.length - dstOff); i++) {
            if (((i + dstOff) >= dest.length) || ((i + srcOff) >= source.length))
                break;
            dest[i + dstOff] = source[i + srcOff];
        }
    }

    public static void byteArrayCopy(byte[] source, int srcOff, byte[] dest, int dstOff, int len) {
        for (int i = 0; i < (source.length - srcOff) && i < (dest.length - dstOff); i++) {
            if (i >= len)
                break;
            if (((i + dstOff) >= dest.length) || ((i + srcOff) >= source.length))
                break;
            dest[i + dstOff] = source[i + srcOff];
        }
    }

    public static void byteArrayCopy(byte[] source, byte[] dest) {
        for (int i = 0; i < source.length && i < dest.length; i++) {
            dest[i] = source[i];
        }
    }

    public static int toUnsigned(byte u) {
        return (u < 0) ? (128 + (u & 0x7f)) : (u);
    }

    public static void toIntelDataInt(int value, byte array[], int index) {
        array[index] = (byte) (value & 0x000000FF);
        array[index + 1] = (byte) ((value & 0x0000FF00) >> 8);
        array[index + 2] = (byte) ((value & 0x00FF0000) >> 16);
        array[index + 3] = (byte) ((value & 0xFF000000) >> 24);
    }

    public static int fromIntelDataIntLE(byte array[], int index) {
        return (toUnsigned(array[index + 3]) << 24) +
                (toUnsigned(array[index + 2]) << 16) +
                (toUnsigned(array[index + 1]) << 8) +
                (toUnsigned(array[index]));
    }

    public static void toJavaDataInt(int value, byte array[], int index) {
        array[index + 3] = (byte) (value & 0x000000FF);
        array[index + 2] = (byte) ((value & 0x0000FF00) >> 8);
        array[index + 1] = (byte) ((value & 0x00FF0000) >> 16);
        array[index] = (byte) ((value & 0xFF000000) >> 24);
    }

    public static int fromJavaDataIntLE(byte array[], int index) {
        return (toUnsigned(array[index]) << 24) +
                (toUnsigned(array[index + 1]) << 16) +
                (toUnsigned(array[index + 2]) << 8) +
                (toUnsigned(array[index + 3]));
    }

    public static int fromIntelDataWord(byte array[], int index) {
        return ((toUnsigned(array[index + 1]) << 8) +
                (toUnsigned(array[index])));
    }

    public static int fromJavaDataWord(byte array[], int index) {
        return ((toUnsigned(array[index]) << 8) +
                (toUnsigned(array[index + 1])));
    }

    public static String Int2Hex(int i, int j) {
        StringBuffer s = new StringBuffer(Integer.toHexString(i));
        while (s.length() < j)
            s.insert(0, "0");
        return s.toString().toUpperCase();
    }

    public static int Ubyte(int b) {
        if (b < 0)
            b += 256;
        return b;
    }

    public static String padNum(long value, int len) {
        String s = "";
        for (int i = 0; i < len; i++)
            s = s + "0";
        s += Long.toString((value < 0) ? -value : value);
        if (value < 0)
            return ("-" + s.substring(s.length() - len - 1));
        else
            return s.substring(s.length() - len);
    }

    public static String padNum(int value, int len) {
        return (padNum((long) value, len));
    }

    public static String padNum(byte value) {
        int k = Ubyte(value);
        String s = "000" + Integer.toString(k).toUpperCase();
        if (k > 99)
            return s.substring(s.length() - 3);
        return s.substring(s.length() - 2);
    }

    public static String padHex(byte value) {
        String s = "00" + Integer.toHexString(Ubyte(value)).toUpperCase();
        return s.substring(s.length() - 2);
    }

    public static String padHex(int value, int len) {
        String s = "";
        for (int i = 0; i < len; i++)
            s = s + "0";
        s += Integer.toHexString(value).toUpperCase();
        return s.substring(s.length() - len);
    }

    public static byte[] arrayInvert(byte[] src) {
        byte[] data = new byte[src.length];

        for (int i = 0; i < src.length / 2; i++) {
            data[i] = src[src.length - i - 1];
            data[src.length - i - 1] = src[i];

        }
        return data;
    }

    public static byte[] getField(byte[] src, int field, boolean safe) {
        return (getField(src, field, 0, safe));
    }

    public static byte[] getField(byte[] src, int field, int fieldSeparator, boolean safe) {
        int ix;
        int i, len = 0;

        for (i = 0; i < src.length - 1; i++) {
            if (src[i] == (byte) fieldSeparator) {
                if (src[i + 1] == field)
                    break;
            }
        }
        i += 2;
        if (i >= src.length) {
            if (safe) {
                byte[] dst = {'0'};
                return dst;
            }
            //System.out.println("Eror Field "+field);
            return null;
        }
        ix = i;
        for (; i < src.length; i++) {
            if (src[i] == (byte) fieldSeparator)
                break;
            len++;
        }
        byte[] dst = new byte[len];
        for (i = 0; i < len; i++)
            dst[i] = src[ix + i];
        return dst;
    }

    public static int setField(byte[] dst, byte[] src, int inx, int field, int fieldSeparator) {
        dst[inx++] = (byte) fieldSeparator;
        dst[inx++] = (byte) field;
        for (int i = 0; i < src.length; i++) {
            dst[inx++] = src[i];
        }
        dst[inx] = (byte) fieldSeparator;
        return inx;
    }

    public static int setField(byte[] dst, int inx, int fieldSeparator) {
        dst[inx++] = (byte) fieldSeparator;
        return inx;
    }

    public static int setField(byte[] dst, byte[] src, int inx, int field) {
        return (setField(dst, src, inx, field, 0));
    }

    public static int setField(byte[] dst, String src, int inx, int field) {
        byte[] sc = src.getBytes();
        return setField(dst, sc, inx, field);
    }

    public static int setField(byte[] dst, String src, int inx, int field, int fieldSeparator) {
        byte[] sc = src.getBytes();
        return setField(dst, sc, inx, field, fieldSeparator);
    }

    public static int toInt(String s) {
        int resul = 0;
        try {
            resul = Integer.parseInt(s);
        } catch (Exception e) {
        }  //ToDo: Log del error?.
        return resul;
    }

    public static long toLong(String s) {
        long resul = 0;
        try {
            resul = Long.parseLong(s);
        } catch (Exception e) {
        }  //ToDo: Log del error?.
        return resul;
    }

    static Date Dia = new Date();
    static Calendar Fecha = Calendar.getInstance();

    public static Calendar Fecha(long tt) {
        Dia.setTime(tt);
        Fecha.setTime(Dia);
        return Fecha;
    }

    public static String NumFormat(String src) {
        String s = "";
        String neg = "";

        if ((src.length() > 1) && src.substring(0, 1).equals("-")) {
            src = src.substring(1);
            neg = "-";
        }
        if (src.length() == 0)
            s = "0,00";
        if (src.length() == 1)
            s = "0,0" + src;
        if (src.length() == 2)
            s = "0," + src;
        if (src.length() > 2)
            s = src.substring(0, src.length() - 2) + "," + src.substring(src.length() - 2, src.length());
        return (neg + s);
    }

    public static String String(byte[]dt)
    {
        Charset latin1Charset = Charset.forName("ISO-8859-1");
        CharBuffer buf = latin1Charset.decode(ByteBuffer.wrap(dt));
        return  buf.toString();
    }

    public static void LogMem(String Where)
    {
        Runtime info = Runtime.getRuntime();
        try {
            Log.d("Memory ", Where+" Free "+info.freeMemory());
            Log.d("Memory ", Where+" Total "+info.totalMemory());
            Log.d("Memory ", Where+" Max "+info.maxMemory());
        }catch (Exception e){}
    }
/*
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        else
            return Html.fromHtml(source);
    }
    */
}