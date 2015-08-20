package com.dblue.downloadapp.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dblue.downloadapp.db.Thread_DAO;
import com.dblue.downloadapp.entities.FileInfo;
import com.dblue.downloadapp.entities.ThreadInfo;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2015/8/19.
 * 进行下载任务来将文件进行下载
 */
public class DownLoadTask {

    private Context context;
    private FileInfo fileInfo;
    private Thread_DAO dao = null;
    private int mFinished;
    public boolean isPause;
    private  int ThreadCount =1;
    private List<DownLoadThread> threadList = null;
    public static ExecutorService executorService = Executors.newCachedThreadPool();//创建线程池对文件启动线程

    public DownLoadTask(Context context, FileInfo fileInfo,int count) {
        this.context = context;
        this.fileInfo = fileInfo;
        this.ThreadCount = count;
        dao = new Thread_DAO(context);
    }
    public void downLoad(){
        List<ThreadInfo> list = dao.query(fileInfo.getUrl());
        if(0 == list.size()){
            //计算每个线程下载的长度
            int len = fileInfo.getLength()/ThreadCount;
            for(int i=0;i<ThreadCount;i++){
                ThreadInfo  threadInfo = new ThreadInfo(i,fileInfo.getUrl(),len * i,(i + 1) * len - 1,0);
                if(ThreadCount-1==0){
                    threadInfo.setEnd(fileInfo.getLength());
                }
                list.add(threadInfo);
                dao.insertThread(threadInfo);
            }
        }
        threadList = new ArrayList<DownLoadThread>();//使用线程集合用来判断所有线程是否下载完毕
        //使用线程池启动多个线程进行下载
        for(ThreadInfo threads:list){
            DownLoadThread thread = new DownLoadThread(threads);
//            thread.start();
            DownLoadTask.executorService.execute(thread);
            //将下载任务添加进线程集合
            threadList.add(thread);
        }
    }

    /**
     * 进行下载任务
     */
    class DownLoadThread extends Thread{
        public boolean isFinished = false;
        ThreadInfo threadInfo;
        public DownLoadThread(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }
        @Override
        public void run() {
            HttpURLConnection connection = null;
            RandomAccessFile raf = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(threadInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(6000);
                connection.setRequestMethod("GET");
                //设置下载位置
                int start = threadInfo.getStart()+threadInfo.getFinished();
                //从网络上取得上次下载位置
                connection.setRequestProperty("Range",
                        "bytes=" + start + "-" + threadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DownLoadService.DOWNLOAD_PATH+fileInfo.getFileName());
                raf = new RandomAccessFile(file,"rwd");
                raf.seek(start);
                Intent intent = new Intent();
                intent.setAction(DownLoadService.ACTION_UPDATE);
                //整个文件的长度
                mFinished += threadInfo.getFinished();
                //累加每个线程的下载程度

                //开始下载
                if(connection.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT){
                    Log.i("TAG","开始下载");
                    inputStream = connection.getInputStream();
                    byte[] buffer = new byte[1024];
                    int len = -1;
                    long time = System.currentTimeMillis();
                    while((len = inputStream.read(buffer))!=-1){
                        raf.write(buffer,0,len);
                        //累积整个文件的长度
                        mFinished += len;
                        //设置每个线程的完成程度
                        threadInfo.setFinished(threadInfo.getFinished() + len);
                        //当时间超出1000毫秒时发送广播
                        if(System.currentTimeMillis()-time>1000){
                            time = System.currentTimeMillis();
                            //得到当前下载的进度是%分数
                            int f = mFinished*100/fileInfo.getLength();
                            //当目前下载进度大于上次的下载进度时发送广播
                            if(f>fileInfo.getFinished()){
                                intent.putExtra("finished",f);
                                intent.putExtra("id",fileInfo.getId());
                                context.sendBroadcast(intent);
                            }
                        }
                        //当暂停时
                        if(isPause){
                            Log.i("TAG","停止下载");
                            dao.updateThread(threadInfo.getUrl(),threadInfo.getId(),threadInfo.getFinished());
                            return;
                        }
                    }
                    isFinished = true;//标志下载完毕
                    checkAllThreadFinished();
                    Log.i("TAG","下载完毕");

                }

            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                connection.disconnect();
                try {
                    raf.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }

    /**
     * .标志着所有线程执行完毕
     */
    private synchronized void checkAllThreadFinished(){
        boolean allFinished = true;
        for(DownLoadThread thread:threadList){
            if(!thread.isFinished){
                allFinished = false;
                break;
            }
        }
        if (allFinished){
            //下载完毕删除下载信息
            dao.delete(fileInfo.getUrl());
            //下载完毕通知Activity
            Intent intent = new Intent();
            intent.setAction(DownLoadService.ACTION_FINISHED);
            intent.putExtra("fileInfo",fileInfo);
            context.sendBroadcast(intent);
        }
    }
}
