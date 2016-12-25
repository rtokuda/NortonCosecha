package com.winetraces.recordstore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.winetraces.nortoncosecha.Defines;

import java.util.Arrays;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by nestor on 05/11/2016.
 */

public class RecordStore {
    private String recordName = null;
    private boolean recordFlag = false;
    private boolean writeFlag = false;
    private byte[] record = null;
    public static SQLiteDatabase SQLdb = null;
    public static int ChannelCount = 0;
    private RecordEnumeration re = null;
    private int[] indexBuffRd;
    private byte[][] dataBuffRd;
    private String[] dataBuffSRd;

    private int countBuffRd;
    private int currInxRd;
    private String[] dataBuffWr;
    private int currInxWr;

    public static boolean initializeRecordStore(Context contexto, boolean forceCreate) {
        if (forceCreate) {
            try {
                contexto.deleteDatabase("RecordStore");
            }catch(Exception e){}
        }
        SQLdb = contexto.openOrCreateDatabase("RecordStore", MODE_PRIVATE, null);
        return true;
    }

    public static String[] listRecordStores() {
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
        return (rs.open(name, createIfNecessary, isWrite));
    }
    public static RecordStore openRecordStoreBuffered (String name, boolean Mode) {
        RecordStore rs = new RecordStore();
        return(rs.openBuffered(name, Mode));
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
        writeFlag = isWrite;
        try {
            SQLdb.execSQL("CREATE TABLE IF NOT EXISTS " + recordName + " (recordID INTEGER PRIMARY KEY AUTOINCREMENT, recordData TEXT);");
        } catch (Exception e) {
            recordName = null;
            return null;
        }
        ChannelCount++;
        SQLdb.beginTransaction();
        return this;
    }

    public RecordStore openBuffered (String name, boolean Mode)
    {
        if (SQLdb == null)
            return null;
        recordName = "rd_"+name;
        try {
            SQLdb.execSQL("CREATE TABLE IF NOT EXISTS " + recordName + " (recordID INTEGER PRIMARY KEY AUTOINCREMENT, recordData TEXT);");
        } catch (Exception e) {
            recordName = null;
            return null;
        }
        countBuffRd = 0;
        currInxWr = -1;
        currInxRd = 0;

        Cursor cursor = null;
        String recordS;
        try {
            cursor = SQLdb.rawQuery("SELECT recordID, recordData FROM " + recordName, null);
            if (cursor != null) {
                int count = ((int)(DatabaseUtils.queryNumEntries(SQLdb, recordName)));
                indexBuffRd = new int[count];
                if (Mode == Defines.OPEN_BINARY)
                    dataBuffRd = new byte[count][];
                else
                    dataBuffSRd = new String[count];
                cursor.moveToFirst();
                if (cursor.isAfterLast())
                    return this;
                for (int j = 0; j < count; j++) {
                    recordS = cursor.getString(cursor.getColumnIndex("recordData"));
                    int len = recordS.length();
                    if (len < 2)
                        continue;
                    if (Mode == Defines.OPEN_BINARY) {
                        byte[] val = new byte[2];
                        byte src[] = recordS.getBytes();
                        record = new byte[len / 2];
                        for (int i = 0; i < len; i += 2) {
                            val[0] = src[i];
                            val[1] = src[i + 1];

                            String hex = new String(val);
                            record[i / 2] = (byte) (Integer.valueOf(hex, 16).intValue());
                        }
                        dataBuffRd[countBuffRd] = record;
                    }
                    else
                        dataBuffSRd[countBuffRd] = recordS;
                    indexBuffRd[countBuffRd++] = new Integer(cursor.getString(cursor.getColumnIndex("recordID")));
                    cursor.moveToNext();
                    if (cursor.isAfterLast())
                        break;
                }
            }
        }catch (Exception e){
            recordName = null;
            return null;
        }
        ChannelCount++;
        SQLdb.beginTransaction();
        return this;
    }

    public void closeRecordStore()
    {
        if (SQLdb == null)
            return;
        if (currInxWr > 0)
            saveRecords();
        if (SQLdb.inTransaction()) {
            SQLdb.setTransactionSuccessful();
            SQLdb.endTransaction();
        }
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
        if ((SQLdb == null) || (recordName == null))
            return;
        try {
            SQLdb.execSQL("DELETE FROM "+ recordName + " WHERE recordID = "+inx);
        }catch(Exception e){}
    }

    public int getNumRecords()
    {
        if ((SQLdb == null) || (recordName == null))
            return 0;
        return ((int)(DatabaseUtils.queryNumEntries(SQLdb, recordName)));
    }

    public void reset()
    {
        currInxRd = 0;
    }

    public int setIndex(int inx)
    {
        if (inx == -1)
        {
            currInxRd++;
            if (currInxRd > countBuffRd) {
                currInxRd--;
                return -1;
            }
            return currInxRd;
        }
        if ((inx < 0) || (inx >= countBuffRd))
            return -1;
        else
            currInxRd = inx;
        return inx;
    }

    public byte[] getBuffRecord()
    {
        if ((countBuffRd == 0)|| (currInxRd == -1))
            return null;
        return (dataBuffRd[currInxRd]);
    }

    public byte[] getBuffRecord(int inx)
    {
        if (inx >= countBuffRd)
            return null;
        return (dataBuffRd[inx]);
    }

    public String getBuffSRecord(int inx)
    {
        if (inx >= countBuffRd)
            return null;
        return (dataBuffSRd[inx]);
    }

    public int getBuffIndex()
    {
        if (countBuffRd == 0)
            return -1;
        return (indexBuffRd[currInxRd]);
    }

    public byte[] getRecord(int inx)
    {
        String recordS = null;
        Cursor cursor = null;

        if ((SQLdb == null) || (recordName == null) || (inx <= 0))
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

        if ((SQLdb == null) || (recordName == null) || (inx <= 0))
            return null;
        Cursor cursor = SQLdb.rawQuery("SELECT recordData FROM "+ recordName +" WHERE recordID = "+ inx, null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.isAfterLast())
                return null;
            recordS = cursor.getString(cursor.getColumnIndex("recordData"));
            cursor.close();
        }
        return recordS;
    }

    public int addRecord(byte[] dato, int offset, int numBytes)
    {
        if ((SQLdb == null) || (recordName == null))
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

    public void addRecordBuff(byte[] dato, int offset, int numBytes)
    {
        if ((SQLdb == null) || (recordName == null) || (currInxWr == -1))
            return;
        String dt = "";
        if (offset+numBytes > dato.length)
            return;
        for (int i=0; i<numBytes; i++)
            dt = dt+ padHex(dato[offset+i]);
        dataBuffWr[currInxWr] = dt;
        currInxWr++;
        if (currInxWr >= Defines.MAX_BUFF_WRITE)
            saveRecords();
    }

    private void saveRecords()
    {
        SQLdb.beginTransaction();
        for (int i=0; i<currInxWr; i++)
        {
            ContentValues values = new ContentValues();
            values.put("recordData", dataBuffWr[i]);
            SQLdb.insert(recordName, null, values);
        }
        SQLdb.setTransactionSuccessful();
        SQLdb.endTransaction();

    }

    public void setRecord(int index, byte[] dato, int offset, int numBytes)
    {
        if ((SQLdb == null) || (recordName == null))
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
        if ((SQLdb == null) || (recordName == null))
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