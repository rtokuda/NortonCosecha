package com.winetraces.nortoncosecha;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by nestor on 05/11/2016.
 */

public class RecordStore {
    private String recordName = null;
    private boolean recordFlag = false;
    private byte[] record = null;

    private RecordEnumeration re = null;

    public static boolean initializeRecordStore(Context contexto, boolean forceCreate) {
        if (forceCreate) {
            try {
                contexto.deleteDatabase("RecordStore");
            }catch(Exception e){}
        }
        Variables.SQLdb = contexto.openOrCreateDatabase("RecordStore", MODE_PRIVATE, null);
        return true;
    }

    public static String[] listRecordStore() {
        Cursor cursor = null;
        try {
            cursor = Variables.SQLdb.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null);
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

    public static RecordStore openRecordStore(String name, boolean createIfNecessary) //ToDo createIfNecessary
    {
        RecordStore rs = new RecordStore();
        rs.open(name, createIfNecessary);
        return (rs);
    }

    //ToDo
    public static RecordStore openRecordStore (String name, boolean createIfNecessary, int authmode, boolean writeable) { return null;}

    //ToDo
    public static RecordStore openRecordStore (String name, String vendorname, String suitname) {return null;}

    public RecordStore open (String name, boolean flag)  //ToDo flag
    {
        if (Variables.SQLdb == null)
            return null;
        recordName = "rd_"+name;
        recordFlag = flag;
        try {
            Variables.SQLdb.execSQL("CREATE TABLE IF NOT EXISTS "+ recordName + " (recordID INTEGER PRIMARY KEY AUTOINCREMENT, recordData TEXT);");
        }catch(Exception e){return null;}
        return this;
    }

    public void closeRecordStore()
    {
        if (Variables.SQLdb == null)
            return;
       // SQLdb.execSQL("COMMIT;");
        recordName = null;
    }

    public static void deleteRecordStore(String name)
    {
        if (Variables.SQLdb == null)
            return;
        try {
            Variables.SQLdb.execSQL("DROP TABLE rd_"+name+";");
        }catch(Exception e){}
    }

    public void deleteRecord(int inx)
    {
        if (Variables.SQLdb == null)
            return;
        try {
            Variables.SQLdb.execSQL("DELETE FROM "+ recordName + " WHERE recordID = "+inx);
        }catch(Exception e){}
    }

    public int getNumRecords()
    {
        if (recordName == null)
            return 0;
        return ((int)(DatabaseUtils.queryNumEntries(Variables.SQLdb, recordName)));
    }

    public byte[] getRecord(int inx)
    {
        String recordS = null;
        Cursor cursor = null;

        if ((recordName == null) || (inx <= 0))
            return null;
        try {
            cursor = Variables.SQLdb.rawQuery("SELECT recordData FROM "+ recordName +" WHERE recordID = "+ inx, null);
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
        Cursor cursor = Variables.SQLdb.rawQuery("SELECT recordData FROM "+ recordName +" WHERE recordID = "+ inx, null);
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
            dt = dt+Library.padHex(dato[offset+i]);
        ContentValues values = new ContentValues();
        values.put("recordData", dt);
        long id = Variables.SQLdb.insert(recordName, null, values);
        return (int)id;
    }

    public int addRecord(String s)
    {
        if (recordName == null)
            return 0;
        ContentValues values = new ContentValues();
        values.put("recordData", s);
        long id = Variables.SQLdb.insert(recordName, null, values);
        return (int)id;
    }

    public void setRecord(int index, byte[] dato, int offset, int numBytes)
    {
        if (recordName == null)
            return;
        String dt = "";
        if (offset+numBytes > dato.length)
            return;
        for (int i=0; i<numBytes; i++)
            dt = dt+Library.padHex(dato[offset+i]);
        ContentValues values = new ContentValues();
        values.put("recordData", dt);
        Variables.SQLdb.update(recordName, values, "recordID = "+index, null);
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
}

class RecordFilter{
//ToDo
}

class RecordComparator {
// ToDo
}

class RecordEnumeration {

    private String rName;
    private int index = 0;
    private int max = 0;
    private int colRecord = 0;
    private int colID = 0;
    Cursor crs = null;

    public RecordEnumeration (String name)
    {
        if (name == null)
            return;
        rName = name;
        index = 0;
        max = 0;
        crs = Variables.SQLdb.rawQuery("SELECT recordData, recordID FROM "+ rName, null);
        if (crs != null) {
            crs.moveToFirst();
            if (crs.isAfterLast()) {
                crs = null;
                return;
            }
            colRecord = crs.getColumnIndex("recordData");
            colID = crs.getColumnIndex("recordID");
            max = crs.getCount();
        }
    }

    public boolean hasNextElement()
    {
        if ((index < max) && (crs != null))
            return true;
        return false;
    }

    public int nextRecordId()
    {
        if ((index >= max) || (crs == null))
            return 0;
        int id = crs.getInt(colID);
        crs.moveToNext();
        index++;
        return id;
    }

    public void reset()
    {
        if (crs != null)
        {
            crs.moveToFirst();
            index = 0;
        }
    }

    public void rebuild()
    {
        index = 0;
        max = 0;
        crs = Variables.SQLdb.rawQuery("SELECT recordData, recordID FROM "+ rName, null);
        if (crs != null) {
            crs.moveToFirst();
            if (crs.isAfterLast()) {
                crs = null;
                return;
            }
            colRecord = crs.getColumnIndex("recordData");
            colID = crs.getColumnIndex("recordID");
            max = crs.getCount();
        }
    }

/*    public boolean hasPreviousElement()  { return false; }

    public byte[] nextRecord()  { return null; }

    public byte[] previousRecord() { return null; }

    public int previousRecordId() { return 0; }

    public void keepUpdated() {}

    public boolean isKeepUpdated() {return false;}

    public void destroy() {}
    */
}

class RecordListener //ToDo
{
    void recordAdded(RecordStore rs, int recordId) {}

    void recordChanged(RecordStore rs, int recordId) {}

    void recordDeleted(RecordStore rs, int recordId) {}
}