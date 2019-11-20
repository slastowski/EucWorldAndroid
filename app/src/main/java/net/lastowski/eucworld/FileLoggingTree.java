package net.lastowski.eucworld;

import timber.log.Timber;
import android.content.Context;
import java.io.File;
import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import net.lastowski.eucworld.utils.Constants;

public class FileLoggingTree extends Timber.DebugTree {

    Context context;
    private String fileName;

    FileLoggingTree(Context context) {
        this.context = context;
        fileName = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss", Locale.getDefault()).format(new Date()) + ".html";
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        try {
            String color = "lightgray";
            switch (priority) {
                case Log.VERBOSE:
                    color = "#b3b3b3";
                    break;
                case Log.DEBUG:
                    color = "#b366ff";
                    break;
                case Log.INFO:
                    color = "#cccccc";
                    break;
                case Log.WARN:
                    color = "#ffd966";
                    break;
                case Log.ERROR:
                    color = "#ff8c66";
                    break;
                case Log.ASSERT:
                    color = "#ff6666";
                    break;
            }
            String ts = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()).format(new Date());
            File file = generateFile();
            if (file != null) {
                FileWriter writer = new FileWriter(file, true);
                String line = "<p style=\"padding: 0; margin: 0; font-family: sans-serif; background-color: "+color+";\"><strong style=\"background-color: rgba(240, 240, 240, 0.5);\">" + ts + "</strong>&nbsp&nbsp" + message + "</p>";
                writer.append(line);
                writer.flush();
                writer.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File generateFile() {
        File file = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dir = new File(Environment.getExternalStorageDirectory(), Constants.SUPPORT_FOLDER_NAME);
            boolean dirExists = true;
            if (!dir.exists())
                dirExists = dir.mkdirs();
            if (dirExists) {
                file = new File(dir, fileName);
            }
        }
        return file;
    }

}