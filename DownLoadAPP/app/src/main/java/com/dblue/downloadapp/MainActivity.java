package com.dblue.downloadapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import com.dblue.downloadapp.entities.FileInfo;
import com.dblue.downloadapp.service.DownLoadService;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private MyBaseAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //配置ListView下载项
        listView = (ListView) findViewById(R.id.listview);
        FileInfo fileInfo = new FileInfo(0,"http://www.imooc.com/mobile/imooc.apk","imooc.apk",0,0);
        FileInfo fileInfo1 = new FileInfo(1,"http://s1.music.126.net/download/android/CloudMusic_2.8.1_official_4.apk","CloudMusic_2.8.1_official_4.apk",0,0);
        FileInfo fileInfo2 = new FileInfo(2,"http://download.alicdn.com/wireless/taobao4android/latest/702757.apk","702757.apk",0,0);
        FileInfo fileInfo3 = new FileInfo(3,"http://www.51job.com/client/51job_51JOB_1_AND2.9.3.apk","51job_51JOB_1_AND2.9.3.apk",0,0);
        List<FileInfo> list = new ArrayList<>();
        list.add(fileInfo);
        list.add(fileInfo1);
        list.add(fileInfo2);
        list.add(fileInfo3);
        adapter = new MyBaseAdapter(list,MainActivity.this);
        listView.setAdapter(adapter);
        //注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownLoadService.ACTION_UPDATE);
        intentFilter.addAction(DownLoadService.ACTION_FINISHED);
        registerReceiver(reciver, intentFilter);
    }

    BroadcastReceiver reciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DownLoadService.ACTION_UPDATE.equals(intent.getAction())){
                int finished = intent.getIntExtra("finished",0);
                int id = intent.getIntExtra("id",0);
                adapter.updateProgress(id,finished);
            }else if (DownLoadService.ACTION_FINISHED.equals(intent.getAction())){
                FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
                adapter.updateProgress(fileInfo.getId(),0);
                Toast.makeText(MainActivity.this,"下载完成",Toast.LENGTH_LONG).show();
            }
        }
    };


}
