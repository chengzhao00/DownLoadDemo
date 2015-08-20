package com.dblue.downloadapp;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dblue.downloadapp.entities.FileInfo;
import com.dblue.downloadapp.service.DownLoadService;

import java.util.List;

/**
 * Created by Administrator on 2015/8/19.
 * listView的Adapter
 */
public class MyBaseAdapter extends BaseAdapter {
    private List<FileInfo> list;
    private Context context;

    public MyBaseAdapter(List<FileInfo> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final FileInfo fileInfo = list.get(position);
        ViewHolder viewHolder;
        if(convertView == null){
            convertView = View.inflate(context,R.layout.listview_item,null);
            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) convertView.findViewById(R.id.textView);
            viewHolder.start = (Button) convertView.findViewById(R.id.start);
            viewHolder.stop = (Button) convertView.findViewById(R.id.stop);
            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            viewHolder.progressBar.setMax(100);
            viewHolder.textView.setText(fileInfo.getFileName());
            //启动Service
            viewHolder.start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, DownLoadService.class);
                    intent.setAction(DownLoadService.ACTIOV_START);
                    intent.putExtra("fileInfo", fileInfo);
                    context.startService(intent);
                }
            });

            viewHolder.stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, DownLoadService.class);
                    intent.setAction(DownLoadService.ACTION_STOP);
                    intent.putExtra("fileInfo", fileInfo);
                    context.startService(intent);
                }
            });
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder. progressBar.setProgress(fileInfo.getFinished());
        return convertView;
    }

    /**
     * 更新进度条
     * @param id
     * @param progress
     */
    public void updateProgress(int id,int progress){
        FileInfo mFileInfo = list.get(id);
        mFileInfo.setFinished(progress);
        notifyDataSetChanged();
    }

    /**
     * listView的优化
     */
   static class ViewHolder{
        private Button start,stop;
        private ProgressBar progressBar;
        TextView textView;
    }
}
