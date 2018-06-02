package com.example.loong.fmanager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileManage {
    private static Context mContext;
    private static File currentDirectory;
    private static File cutFile;
    private static File copyFile;
    private static File[] files;

    public static void setCutFile(File cutFile) {
        FileManage.cutFile = cutFile;
    }

    public static void setCopyFile(File copyFile) {
        FileManage.copyFile = copyFile;
    }

    public FileManage(Context mContext) {
        this.mContext = mContext;
    }

    public static File[] getFiles() {
        return files;
    }

    public File getCurrentDirectory(){
        return currentDirectory;
    }

    public List<Map<String, Object>> populate_list(File file) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;
        if (file == null) {
            file = new File("/");
        }
        if (file.isDirectory()) {
            if (file.canRead()) {
                currentDirectory = file;

                map = new HashMap<String, Object>();

                File[] listFiles;
                listFiles = file.listFiles();
                Arrays.sort(listFiles);
                files = new File[listFiles.length];

                int i=0;
                for (File f : listFiles) {
                    if (f.isDirectory()){
                        files[i] = f;
                        i++;
                        map = new HashMap<String, Object>();
                        map.put("text", f.getName());
                        map.put("img", R.mipmap.folder);
                        map.put("ext", "目录 | "+getFileLastTime(f));
                        list.add(map);
                    }
                }
                for (File f : listFiles) {
                    if (!f.isDirectory()){
                        files[i] = f;
                        i++;
                        map = new HashMap<String, Object>();
                        map.put("text", f.getName());
                        if (f.isFile()) {
                            map.put("img", R.mipmap.file);
                            map.put("ext", getFileSize(f)+" | "+getFileLastTime(f));
                        } else {
                            map.put("img", R.mipmap.file);
                            map.put("ext", getFileSize(f)+" | "+getFileLastTime(f));
                        }
                        list.add(map);
                    }
                }
            } else {

            }
        }
        return list;
    }

    public static void openFile(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        mimeType = (mimeType == null) ? "*/*" : mimeType;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), mimeType);
        mContext.startActivity(intent);
    }

    public static void createFile(String name, int type) {
        if (currentDirectory.canWrite()) {
            File newFile = new File(currentDirectory, name);
            switch (type) {
                case 0:
                    try {
                        if (newFile.createNewFile()) {
                            alert("创建文件成功~");
                            refreshNotify();
                        } else {
                            alert("文件已存在~");
                        }
                    } catch (IOException e) {
                        alert("创建文件失败~");
                    }
                    break;
                case 1:
                    if (newFile.mkdirs()) {
                        alert("创建文件夹成功~");
                        refreshNotify();
                    } else {
                        alert("创建文件夹失败~");
                    }
                    break;
            }
        } else {
            alert("没有权限创建~");
        }
    }

    public static void deleteFile(File file) {
        if (file == null || !file.exists() ){
            alert("删除失败！");
            return;
        }
        if(file.isDirectory()){
            for (File f : file.listFiles()) {
                if (f.isFile())
                    f.delete();
                else if (f.isDirectory())
                    deleteFile(f);
            }
            file.delete();
            alert("文件夹 "+file.getName()+" 删除成功！");
            refreshNotify();
        }else{
            file.delete();
            alert("文件 "+file.getName()+" 删除成功！");
            refreshNotify();
        }
    }

    public void pasteFile(){
        File targetFile = null;
        if (!currentDirectory.canWrite()) {
            alert("此文件夹不可写~");
        } else if (copyFile != null) {
            copyFile(copyFile,currentDirectory);
        } else if (cutFile != null) {
            copyFile(cutFile,currentDirectory);
            cutFile.delete();
        } else {
            alert("然而并没有什么东西可以用来粘贴~");
        }
    }

    public boolean copyFile(File sourceFile, File targetDirectory) {
        if (!sourceFile.exists()){
            alert("删除失败！");
            return false;
        }
        if(sourceFile.isDirectory()){
            if(!new File(targetDirectory, sourceFile.getName()).exists()){
                new File(targetDirectory, sourceFile.getName()).mkdir();
            }
            for (File f : sourceFile.listFiles()) {
                if (f.isFile()){
                    File targetFile = new File(new File(targetDirectory, sourceFile.getName()), f.getName());
                    if (targetFile.exists())
                        targetFile = new File(currentDirectory, "[副本]" + sourceFile.getName());
                    nioBufferCopy(f, targetFile);
                }
                else if (f.isDirectory())
                    copyFile(f,new File(new File(targetDirectory, sourceFile.getName()), f.getName()));
            }
        }else{
            File targetFile = new File(targetDirectory, sourceFile.getName());
            if (targetFile.exists())
                targetFile = new File(currentDirectory, "[副本]" + sourceFile.getName());
            nioBufferCopy(sourceFile, targetFile);
        }
        return true;
    }

    public void nioBufferCopy(File source, File target) {

        try {
            final FileInputStream inStream = new FileInputStream(source);
            final FileOutputStream outStream = new FileOutputStream(target);
            final FileChannel in = inStream.getChannel();
            final FileChannel out = outStream.getChannel();
            final ByteBuffer buffer = ByteBuffer.allocate(4096);
            final ProgressDialog progressDialog = new ProgressDialog(mContext);
            progressDialog.setTitle("粘贴中...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    Looper.prepare();
                    try {
                        progressDialog.setMax((int) in.size());
                        while (in.read(buffer) != -1) {
                            buffer.flip();
                            out.write(buffer);
                            buffer.clear();
                            progressDialog.setProgress((int) out.size());
                        }
                        progressDialog.cancel();
                        alert("粘贴完成");
                        refreshNotify();
                    } catch (IOException e) {
                        progressDialog.cancel();
                        alert("粘贴出错");
                        e.printStackTrace();
                    } finally {
                        try {
                            inStream.close();
                            in.close();
                            outStream.close();
                            out.close();
                        } catch (IOException e) {
                            alert("关闭流出错");
                            e.printStackTrace();
                        }
                    }
                    Looper.loop();
                }
            });
            thread.start();

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

    }

    private static String getFileLastTime(File file){
        String dateTime;
        if (file.exists()) {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateTime=df.format(new Date(file.lastModified()));
        } else {
            return null;
        }
        return dateTime;
    }

    private static String getFileSize(File file){
        long size = 0;
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                size = fis.available();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return null;
        }
        return FormetFileSize(size);
    }

    private static String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    private static void refreshNotify(){
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        Intent intent = new Intent("com.example.loong.fmanager.FLASH_FILES_LIST");
        localBroadcastManager.sendBroadcast(intent);
    }

    private static void alert(Object message) {
        final String text = message.toString();
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }

}
