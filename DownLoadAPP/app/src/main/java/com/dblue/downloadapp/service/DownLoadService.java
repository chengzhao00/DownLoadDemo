package com.dblue.downloadapp.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.dblue.downloadapp.entities.FileInfo;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class DownLoadService extends Service {
    //启动service的Action
    public static final String ACTIOV_START = "ServiceStart";
    //停止service的Action
    public static final String ACTION_STOP = "ServiceStop";
    //更新进图条发送广播的Action
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    //完成下载任务的Action
    public static final String ACTION_FINISHED = "ACTION_FINISHED";
    //下载路径
    public static final  String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/downLoads/";
    private Map<Integer,DownLoadTask> mTasks = new LinkedHashMap<Integer,DownLoadTask>();
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    FileInfo fileInfo = (FileInfo) msg.obj;
                    Log.i("TAG", "INit :" + fileInfo.toString());
                    //启动下载任务每个任务分3个线程进行下载
                    DownLoadTask task = new DownLoadTask(DownLoadService.this,fileInfo,3);
                    task.downLoad();
                    //将下载任务存入到集合中
                    mTasks.put(fileInfo.getId(),task);
                    break;

            }
        }
    };
    public DownLoadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获得Activity传来的参数
        if(ACTIOV_START.equals(intent.getAction())){
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i("TAG", "start:" + fileInfo.toString());
//            new InitThread(fileInfo).start();
            DownLoadTask.executorService.execute(new InitThread(fileInfo));
        }else if(ACTION_STOP.equals(intent.getAction())){
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i("TAG","停止下载"+fileInfo.toString());
            //停止
            DownLoadTask task = mTasks.get(fileInfo.getId());
            if(task!=null){
                task.isPause = true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 初始化线程的到创建文件文件的长度
     */
    class InitThread extends Thread{
        private FileInfo fileInfo;
        public InitThread(FileInfo fileInfo) {
            this.fileInfo = fileInfo;
        }

        @Override
        public void run() {
            HttpURLConnection connection = null;
            RandomAccessFile randomAccessFile = null;
            try {
                URL url = new URL(fileInfo.getUrl());
                connection = (HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(6000);
                connection.setRequestMethod("GET");
                int length = -1;
                if(connection.getResponseCode() == HttpStatus.SC_OK){
                    length = connection.getContentLength();
                }
                if(length<=0){
                    return;
                }
                File dir = new File(DOWNLOAD_PATH);
                if(!dir.exists()){
                    dir.mkdir();
                }
                //创建文件
                File file = new File(DOWNLOAD_PATH+fileInfo.getFileName());
                if(!file.exists()){
                    file.createNewFile();
                }
                 randomAccessFile = new RandomAccessFile(file,"rwd");
                randomAccessFile.setLength(length);
                fileInfo.setLength(length);
                handler.sendMessage(handler.obtainMessage(1,fileInfo));
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (connection!=null){
                    connection.disconnect();
                }
                if(randomAccessFile!=null){
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }
}
