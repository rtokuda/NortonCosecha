package com.winetraces.nortoncosecha;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;


/**
 * Created by nestor on 09/11/2016.
 */

public class Save_SD {
    public static void save(Context contexto)
    {
        File sd = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File currentDB = contexto.getDatabasePath("RecordStore"); //databaseName=your current application database name, for example "my_data.db"
        try {
            if (sd.canWrite()) {
                File backupDB = new File(sd, "Norton_RecordStore.sqlite"); // for example "my_data_backup.db"
                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    MediaScannerConnection.scanFile(contexto,
                            new String[] { backupDB.toString() }, null,null );
                }
            }
        } catch (Exception e) {
        }
    }

}
