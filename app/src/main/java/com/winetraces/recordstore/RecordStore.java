package com.winetraces.recordstore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;

import static android.content.Context.MODE_PRIVATE;
import static com.winetraces.recordstore.RecordStore.SQLdb;

/**
 * Created by nestor on 05/11/2016.
 */

public class RecordStore {
    private String recordName = null;
    private boolean recordFlag = false;
    private byte[] record = null;
    public static SQLiteDatabase SQLdb = null;
    public static int ChannelCount = 0;
    private RecordEnumeration re = null;

    public static boolean initializeRecordStore(Context contexto, boolean forceCreate) {
        if (forceCreate) {
            try {
                contexto.deleteDatabase("RecordStore");
            }catch(Exception e){}
        }
        SQLdb = contexto.openOrCreateDatabase("RecordStore", MODE_PRIVATE, null);
        return true;
    }

    public static String[] listRecordStore() {
        Cursor cursor = null;
        try {
            cursor = SQLdb.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null);
            if (cursor != null) {
                String[] strAux = new String[cursor.getCount()];
                if (cursor.moveToFirst()) {
                    int i = 0, j = 0;
                    String s;
                    while (!cursor.isAfterLast()) {
                        s = cursor.getString(cursor.getColumnIndex("name"));
                        if (s.startsWith("rd_")) {
                            strAux[j] = s.substring(3);
                            j++;
                        }
                        i++;
                        cursor.moveToNext();
                    }
                    if (j > 0) {
                        return Arrays.copyOf(strAux, j);
                    }
                }
            }
        }catch(Exception e){return null;}
        return null;
    }

    public static RecordStore openRecordStore(String name, boolean createIfNecessary, boolean isWrite) //ToDo createIfNecessary
    {
        RecordStore rs = new RecordStore();
        rs.open(name, createIfNecessary, isWrite);
        return (rs);
    }

    //ToDo
    public static RecordStore openRecordStore (String name, boolean createIfNecessary, int authmode, boolean writeable) { return null;}

    //ToDo
    public static RecordStore openRecordStore (String name, String vendorname, String suitname) {return null;}

    public RecordStore open (String name, boolean flag, boolean isWrite)  //ToDo flag
    {
        if (SQLdb == null)
            return null;
        recordName = "rd_"+name;
        recordFlag = flag;
        try {
            SQLdb.execSQL("CREATE TABLE IF NOT EXISTS "+ recordName + " (recordID INTEGER PRIMARY KEY AUTOINCREMENT, recordData TEXT);");
        }catch(Exception e){return null;}
        SQLdb.beginTransaction();
        ChannelCount++;
        return this;
    }

    public void closeRecordStore()
    {
        if (SQLdb == null)
            return;
       // SQLdb.execSQL("COMMIT;");
        SQLdb.setTransactionSuccessful();
        SQLdb.endTransaction();
        ChannelCount--;
        recordName = null;
    }

    public static void deleteRecordStore(String name)
    {
        if (SQLdb == null)
            return;
        try {
            SQLdb.execSQL("DROP TABLE rd_"+name+";");
        }catch(Exception e){}
    }

    public void deleteRecord(int inx)
    {
        if (SQLdb == null)
            return;
        try {
            SQLdb.execSQL("DELETE FROM "+ recordName + " WHERE recordID = "+inx);
        }catch(Exception e){}
    }

    public int getNumRecords()
    {
        if (recordName == null)
            return 0;
        return ((int)(DatabaseUtils.queryNumEntries(SQLdb, recordName)));
    }

    public byte[] getRecord(int inx)
    {
        String recordS = null;
        Cursor cursor = null;

        if ((recordName == null) || (inx <= 0))
            return null;
        try {
            cursor = SQLdb.rawQuery("SELECT recordData FROM "+ recordName +" WHERE recordID = "+ inx, null);
            if (cursor != null)
            {
                cursor.moveToFirst();
                if (cursor.isAfterLast())
                    return null;
                recordS = cursor.getString(cursor.getColumnIndex("recordData"));
                int len = recordS.length();
                if (len < 2)
                    return null;
                byte[] val = new byte[2];
                byte src[] = recordS.getBytes();
                record = new byte[len/2];
                for (int i=0; i<len; i+=2)
                {
                    val[0] = src[i];
                    val[1] = src[i+1];

                    String hex = new String(val);
                    record[i/2]= (byte)(Integer.valueOf(hex, 16).intValue());
                }
            }
        }catch(Exception e){ return null;}
        return record;
    }

    public String getRecordS(int inx)
    {
        String recordS = null;

        if ((recordName == null) || (inx <= 0))
            return null;
        Cursor cursor = SQLdb.rawQuery("SELECT recordData FROM "+ recordName +" WHERE recordID = "+ inx, null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.isAfterLast())
                return null;
            recordS = cursor.getString(cursor.getColumnIndex("recordData"));
        }
        return recordS;
    }

    public int addRecord(byte[] dato, int offset, int numBytes)
    {
        if (recordName == null)
            return 0;
        String dt = "";
        if (offset+numBytes > dato.length)
            return 0;
        for (int i=0; i<numBytes; i++)
            dt = dt+ padHex(dato[offset+i]);
        ContentValues values = new ContentValues();
        values.put("recordData", dt);
        long id = SQLdb.insert(recordName, null, values);
        return (int)id;
    }

    public int addRecord(String s)
    {
        if (recordName == null)
            return 0;
        if ((s == null)|| (s.length()==0))
            return 0;
        return (addRecord(s.getBytes(), 0, s.length()));
    }

    public void setRecord(int index, byte[] dato, int offset, int numBytes)
    {
        if (recordName == null)
            return;
        String dt = "";
        if (offset+numBytes > dato.length)
            return;
        for (int i=0; i<numBytes; i++)
            dt = dt+padHex(dato[offset+i]);
        ContentValues values = new ContentValues();
        values.put("recordData", dt);
        SQLdb.update(recordName, values, "recordID = "+index, null);
    }

    public RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean KeepUpdated)
    {
        //ToDo params
        if (recordName == null)
            return null;
        re = new RecordEnumeration (recordName);
        return re;
    }

  /* ToDo
     public long getLastModifeid() {return 0;}

    public String getName() { return recordName;}

    public int getNextRecordID() {return 0;}

    public int getRecordSize(int recordID) {return 0;}

    public int getSize() { return 0;}

    public int getSizeAvailable() {return 0;}

    public int getVersion() {return 0;}

    public void addRecordListener(RecordListener listener) {}

    public void removeRecordListener(RecordListener listener) {}

    public void setMode(int authmode, boolean writeable) {}
*/
    public static int Ubyte(int b) {
        if (b < 0)
            b += 256;
        return b;
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

}

class RecordFilter{
//ToDo
}

class RecordComparator {
// ToDo
}



class RecordListener //ToDo
{
    void recordAdded(RecordStore rs, int recordId) {}

    void recordChanged(RecordStore rs, int recordId) {}

    void recordDeleted(RecordStore rs, int recordId) {}
}