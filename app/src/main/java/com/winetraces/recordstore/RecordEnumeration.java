package com.winetraces.recordstore;

import android.database.Cursor;

import static com.winetraces.recordstore.RecordStore.SQLdb;

/**
 * Created by nestor on 26/11/2016.
 */

public class RecordEnumeration {

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
        crs = SQLdb.rawQuery("SELECT recordData, recordID FROM "+ rName, null);
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
        crs = SQLdb.rawQuery("SELECT recordData, recordID FROM "+ rName, null);
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