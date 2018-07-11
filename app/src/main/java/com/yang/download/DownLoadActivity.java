package com.yang.download;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yang.download.download.DownloadManager;
import com.yang.download.download.DownloadInfo;
import com.yang.download.download.DownloadResponseHandler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by yang on 2018/3/27.
 */

public class DownLoadActivity extends AppCompatActivity {

    @BindView(R.id.main_progress1)
    ProgressBar progressBar1;

    @BindView(R.id.main_progress2)
    ProgressBar progressBar2;

    @BindView(R.id.main_progress3)
    ProgressBar progressBar3;

    @BindView(R.id.txt)
    TextView txt;
    
    protected Unbinder mUnbinder;

    private String url1 = "https://raw.githubusercontent.com/scwang90/SmartRefreshLayout/master/art/app-debug.apk";
    private String url2 = "https://download.superlearn.com/app/toefl.apk";
    private String url3 = "https://leguimg.qcloud.com/files/LEGUMAC.zip";


    public static void startActivity(Context ctx) {
        Intent intent = new Intent(ctx, DownLoadActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        mUnbinder = ButterKnife.bind(this);
        DownloadManager.getInstance(this).setMaxRequests(2);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadInfo info){
        txt.setText(txt.getText().toString()+"\n"+info.getUrl()+"---progress:"+info.getProgress()+"---total:"+info.getTotal());
    }

    @OnClick(R.id.main_btn_down1)
    void onDown1() {
        DownloadManager.getInstance(this).download(url1, getDiskFileDir(this) + "/app.apk", new DownloadResponseHandler() {
            @Override
            public void onFinish(File download_file) {

            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
                Log.e("DownLoadManager", currentBytes + "----" + totalBytes);
                progressBar1.setMax((int) totalBytes);
                progressBar1.setProgress((int) currentBytes);
            }

            @Override
            public void onFailure(String error_msg) {

            }

            @Override
            public void onPause(DownloadInfo info) {

            }

            @Override
            public void onCancle(DownloadInfo info) {

            }
        });
    }

    @OnClick(R.id.main_btn_down2)
    void onDown2() {
        DownloadManager.getInstance(this).download(url2, getDiskFileDir(this) + "/toefl.apk", new DownloadResponseHandler() {
            @Override
            public void onFinish(File download_file) {

            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
                Log.e("DownLoadManager", currentBytes + "----" + totalBytes);
                progressBar2.setMax((int) totalBytes);
                progressBar2.setProgress((int) currentBytes);
            }

            @Override
            public void onFailure(String error_msg) {

            }

            @Override
            public void onPause(DownloadInfo info) {

            }

            @Override
            public void onCancle(DownloadInfo info) {

            }
        });
    }

    @OnClick(R.id.main_btn_down3)
    void onDown3() {
        DownloadManager.getInstance(this).download(url3, getDiskFileDir(this) + "/le.zip", new DownloadResponseHandler() {
            @Override
            public void onFinish(File download_file) {

            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
                Log.e("DownLoadManager", currentBytes + "----" + totalBytes);
                progressBar3.setMax((int) totalBytes);
                progressBar3.setProgress((int) currentBytes);
            }

            @Override
            public void onFailure(String error_msg) {

            }

            @Override
            public void onPause(DownloadInfo info) {

            }

            @Override
            public void onCancle(DownloadInfo info) {

            }
        });
    }

    @OnClick(R.id.main_btn_cancel1)
    void onCancle1() {
        DownloadManager.getInstance(this).pause(url1);
    }

    @OnClick(R.id.main_btn_cancel2)
    void onCancle2() {
        DownloadManager.getInstance(this).pause(url2);
    }

    @OnClick(R.id.main_btn_cancel3)
    void onCancle3() {
        DownloadManager.getInstance(this).pause(url3);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mUnbinder) mUnbinder.unbind();
    }

    /**
     * 获取缓存文件目录
     *
     * @param context
     * @return
     */
    public static String getDiskFileDir(Context context) {
        String cachePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalFilesDir(null).getPath();
        } else {
            cachePath = context.getFilesDir().getPath();
        }
        return cachePath;
    }
}
