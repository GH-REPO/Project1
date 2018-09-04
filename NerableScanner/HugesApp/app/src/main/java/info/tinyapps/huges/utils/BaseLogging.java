package info.tinyapps.huges.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BaseLogging {
    public static void deleteFile(File file){
        if(!file.isDirectory()){
            file.delete();
            return;
        }

        File [] files = file.listFiles();

        for(int i = 0; i < files.length;i++){
            if(files[i].isDirectory())
                deleteFile(files[i]);
            else
                files[i].delete();
        }
    }

    static File getLogsDir(String folder) {
        String path = Environment.getExternalStorageDirectory().toString();

        if (path == null)
            return null;

        File dir = new File(path, folder);

        if (!dir.exists()) {
            dir.mkdir();
        }
        else {
            if (!dir.isDirectory()) {
                dir.delete();
                dir.mkdir();
            }
        }

        return dir;
    }

    public static void clearLogs(){
        File zip_file = getLogsDir();
        deleteFile(zip_file);
    }

    public static File getLogsDir() {
        return getLogsDir("hlogs");
    }

    static File getZipsDir() {
        File zip_dir = getLogsDir("hzips");
        //delete all old zip files
        deleteFile(zip_dir);

        return getLogsDir("hzips");
    }

    public static String getNameByDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMMdd");
        return sdf.format(new Date()) + ".log";
    }

    public static void addLog(String txt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

            String file_name = getNameByDate();
            File file = new File(getLogsDir(), file_name);

            if (!file.exists())
                file.createNewFile();

            FileWriter fw = new FileWriter(file, true);
            fw.write(sdf.format(new Date()));
            fw.write("\t");
            fw.write(txt);
            fw.write("\n");
            fw.close();

            Log.d("HLOG", txt);
        }
        catch (Exception e) {
        }
    }

    public static void addLog(String txt, Exception e) {
        StringBuffer buf = new StringBuffer();
        buf.append("\n");
        buf.append(txt);
        buf.append("\n");
        buf.append(getErrorInfo(e));
        addLog(buf.toString());
    }

    public static void addLog(String txt, Throwable e) {
        StringBuffer buf = new StringBuffer();
        buf.append("\n");
        buf.append(txt);
        buf.append("\n");
        buf.append(getErrorInfo(e));
        addLog(buf.toString());
    }

    public static String getErrorInfo(Throwable e) {
        if(e == null)
            return "";

        StringBuffer buf = new StringBuffer();
        buf.append("\n");
        buf.append(e.getMessage());
        buf.append("\n");
        buf.append(e.getClass().getName());

        StackTraceElement[] traces = e.getStackTrace();

        if (traces != null) {
            for (int i = 0; i < traces.length; i++) {
                buf.append("\n");
                buf.append(traces[i].getClassName());
                buf.append(traces[i].getMethodName());
                buf.append(traces[i].getFileName());
                buf.append(traces[i].getLineNumber());
            }
        }

        return buf.toString();
    }

    public static File zipFolder() {
        int count;
        byte data[] = new byte[4096];

        try {
            File folder = getLogsDir();

            String zip_name = getNameByDate() + ".zip";
            File zips_folder = getZipsDir();
            File zip_file = new File(zips_folder,zip_name);

            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zip_file);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            String [] _files = folder.list();

            for (int i = 0; i < _files.length; i++) {
                FileInputStream fi = new FileInputStream(new File(folder,_files[i]));
                origin = new BufferedInputStream(fi, data.length);
                ZipEntry entry = new ZipEntry(_files[i].substring(_files[i].lastIndexOf("/") + 1));
                out.putNextEntry(entry);

                while ((count = origin.read(data, 0, data.length)) != -1)
                    out.write(data, 0, count);

                origin.close();
            }

            out.close();

            return zip_file;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getLogsText(){
        int count;
        byte data[] = new byte[4096];

        StringBuilder res = new StringBuilder();
        try {
            File folder = getLogsDir();

            File zips_folder = getZipsDir();

            BufferedInputStream origin = null;

            String [] _files = folder.list();

            for (int i = 0; i < _files.length; i++) {
                FileInputStream fi = new FileInputStream(new File(folder,_files[i]));
                origin = new BufferedInputStream(fi, data.length);

                while ((count = origin.read(data, 0, data.length)) != -1)
                    res.append(new String(data, 0, count));

                origin.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return res.toString();
    }

    public static void sendLogs2(Activity ctx, String subject, String text, String mail) throws Exception{
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        if(text != null){
            File file = zipFolder();
            intent.setType("application/zip");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        }
        else{
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, getLogsText());
        }

        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);

        if(mail != null)
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mail});

        ctx.startActivity(intent);
    }
}

