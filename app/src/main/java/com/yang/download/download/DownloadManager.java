package com.yang.download.download;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by yang on 2018/6/29.
 * 下载管理器
 */

public class DownloadManager {

    // 下载状态
    public static final int DOWNLOAD_STATE_WAITING = 0x00;//等待
    public static final int DOWNLOAD_STATE_DOWNLOADING = 0x01;//下载中
    public static final int DOWNLOAD_STATE_PAUSE = 0x02;//暂停
    public static final int DOWNLOAD_STATE_CANCLE = 0x03;//取消
    public static final int DOWNLOAD_STATE_FINISH = 0x04;//完成
    public static final int DOWNLOAD_STATE_FAIL = 0x05;//失败
    public static final int DOWNLOAD_STATE_RESTART = 0x06;//重新下载

    private static final AtomicReference<DownloadManager> INSTANCE = new AtomicReference<>();
    private OkHttpClient mClient;//OKHttpClient;
    private Context mContext;
    private int maxRequests = 1;
    private Map<String, DownloadInfo> downloadInfos = new HashMap<>();
    private Map<String, DownloadInfo> completeInfos = new HashMap<>();
    public static final String DOWNLOAD_MAPS = "DOWNLOAD_MAPS";//下载队列的
    public static final String COMPLETE_MAPS = "COMPLETE_MAPS";//已完成的


    //获得一个单例类
    public static DownloadManager getInstance(Context context) {
        for (; ; ) {
            DownloadManager current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new DownloadManager(context);
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    private DownloadManager(Context context) {
        mClient = new OkHttpClient();
        mClient.dispatcher().setMaxRequests(maxRequests);
        mContext = context;
        downloadInfos = SpTool.getMap(mContext, DOWNLOAD_MAPS);
        if (downloadInfos == null)
            downloadInfos = new HashMap<>();
        completeInfos = SpTool.getMap(mContext, COMPLETE_MAPS);
        if (completeInfos == null)
            completeInfos = new HashMap<>();
    }

    /**
     * 设置最大请求数
     *
     * @param maxRequests
     */
    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
        if (mClient != null)
            mClient.dispatcher().setMaxRequests(maxRequests);
    }

    /**
     * 得到下载队列
     *
     * @return
     */
    public Map<String, DownloadInfo> getDownloadInfos() {
        return downloadInfos != null ? downloadInfos : new HashMap<String, DownloadInfo>();
    }

    /**
     * 得到已完成队列
     *
     * @return
     */
    public Map<String, DownloadInfo> getCompleteInfos() {
        return completeInfos != null ? completeInfos : new HashMap<String, DownloadInfo>();
    }

    /**
     * 取消全部下载
     *
     * @param url
     */
    public void cancelAll(String url) {
        if (mClient != null) {
            for (Call call : mClient.dispatcher().queuedCalls()) {
                cancel(call.request().tag().toString());
            }
            for (Call call : mClient.dispatcher().runningCalls()) {
                cancel(call.request().tag().toString());
            }
        }
    }

    /**
     * 取消下载
     *
     * @param url
     */
    public void cancel(String url) {
        if (mClient != null) {
            for (Call call : mClient.dispatcher().queuedCalls()) {
                if (call.request().tag().equals(url))
                    call.cancel();
            }
            for (Call call : mClient.dispatcher().runningCalls()) {
                if (call.request().tag().equals(url))
                    call.cancel();
            }
        }
        if (downloadInfos.get(url) != null) {
            DownloadInfo cancleInfo = downloadInfos.get(url);
            cancleInfo.setDownloadState(DOWNLOAD_STATE_CANCLE);
            downloadInfos.remove(cancleInfo.getUrl());
            SpTool.putMap(mContext, DOWNLOAD_MAPS, downloadInfos);
            File file = new File(cancleInfo.getTargetUrl());
            if (file.exists())
                file.delete();
        }
    }

    /**
     * 暂停全部下载
     */
    public void pauseAll() {
        if (mClient != null) {
            for (Call call : mClient.dispatcher().queuedCalls()) {
                pause(call.request().tag().toString());
            }
            for (Call call : mClient.dispatcher().runningCalls()) {
                pause(call.request().tag().toString());
            }
        }

    }

    /**
     * 暂停下载
     *
     * @param url
     */
    public void pause(String url) {
        if (mClient != null) {
            for (Call call : mClient.dispatcher().queuedCalls()) {
                if (call.request().tag().equals(url))
                    call.cancel();
            }
            for (Call call : mClient.dispatcher().runningCalls()) {
                if (call.request().tag().equals(url))
                    call.cancel();
            }
        }
        if (downloadInfos.get(url) != null) {
            DownloadInfo pauseInfo = downloadInfos.get(url);
            pauseInfo.setDownloadState(DOWNLOAD_STATE_PAUSE);
            downloadInfos.put(pauseInfo.getUrl(), pauseInfo);
            SpTool.putMap(mContext, DOWNLOAD_MAPS, downloadInfos);
        }
    }

    /**
     * 开始下载
     *
     * @param url       下载请求的网址
     * @param targetUrl 下载保存的位置
     */
    public void download(String url, String targetUrl) {
        download(url, targetUrl, new DownloadResponseHandler() {
            @Override
            public void onFinish(File download_file) {

            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {

            }

            @Override
            public void onFailure(String error_msg) {

            }

            @Override
            public void onCancle(DownloadInfo info) {

            }

            @Override
            public void onPause(DownloadInfo info) {

            }
        });
    }

    /**
     * 开始下载
     *
     * @param url       下载请求的网址
     * @param targetUrl 下载保存的位置
     */
    public void download(String url, String targetUrl, final DownloadResponseHandler downloadResponseHandler) {
        DownloadInfo downloadInfo = new DownloadInfo(url);
        downloadInfo.setTargetUrl(targetUrl);
        download(downloadInfo, downloadResponseHandler);
    }

    /**
     * 开始下载
     *
     * @param downloadInfo            下载类
     * @param downloadResponseHandler 用来回调的接口
     */
    public void download(DownloadInfo downloadInfo, final DownloadResponseHandler downloadResponseHandler) {

        if (mClient != null) {//包含下载url,不做处理
            for (Call call : mClient.dispatcher().queuedCalls()) {
                if (call.request().tag().equals(downloadInfo.getUrl()))
                    return;
            }
            for (Call call : mClient.dispatcher().runningCalls()) {
                if (call.request().tag().equals(downloadInfo.getUrl()))
                    return;
            }
        }
        DownloadInfo info;
        Request request;
        if (downloadInfos.get(downloadInfo.getUrl()) != null) {//在下载队列中
            info = downloadInfos.get(downloadInfo.getUrl());
            File file = new File(info.getTargetUrl());
            if (file.exists()) {
                //找到了文件,代表已经下载过,则获取其长度
                info.setProgress(file.length());
                if (info.getProgress() >= info.getTotal()) {
                    file.delete();
                    File newFile = new File(info.getTargetUrl());
                    info.setProgress(newFile.length());
                }
                downloadResponseHandler.onProgress(info.getProgress(), info.getTotal());
            }
            info.setDownloadState(DOWNLOAD_STATE_WAITING);
            downloadInfos.put(info.getUrl(), info);
            SpTool.putMap(mContext, DOWNLOAD_MAPS, downloadInfos);
            EventBus.getDefault().post(info);
            request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + info.getProgress() + "-" + info.getTotal())
                    .url(info.getUrl())
                    .tag(info.getUrl())
                    .build();
        } else {//添加新任务
            info = downloadInfo;
            info.setDownloadState(DOWNLOAD_STATE_WAITING);
            if (TextUtils.isEmpty(downloadInfo.getFileName())) {
                String fileName = downloadInfo.getTargetUrl().substring(downloadInfo.getTargetUrl().lastIndexOf("/"));
                if (fileName.startsWith("/") && fileName.length() > 1)
                    fileName = fileName.substring(1);
                info.setFileName(fileName);
            }
            downloadInfos.put(info.getUrl(), info);
            SpTool.putMap(mContext, DOWNLOAD_MAPS, downloadInfos);
            EventBus.getDefault().post(info);
            request = new Request.Builder()
                    .url(info.getUrl())
                    .tag(info.getUrl())
                    .build();
        }

        final DownloadInfo finalInfo = info;
        Call call = mClient.newBuilder()
                .addNetworkInterceptor(new Interceptor() {      //设置拦截器
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Response originalResponse = chain.proceed(chain.request());
                        return originalResponse.newBuilder()
                                .body(new ResponseProgressBody(mContext, originalResponse.body(), downloadResponseHandler, finalInfo))
                                .build();
                    }
                })
                .build()
                .newCall(request);
        call.enqueue(new MyDownloadCallback(new Handler(), downloadResponseHandler, finalInfo));
    }

    //下载回调
    private class MyDownloadCallback implements Callback {

        private Handler mHandler;
        private DownloadResponseHandler mDownloadResponseHandler;
        private DownloadInfo info;
        private String targetUrl;

        public MyDownloadCallback(Handler handler, DownloadResponseHandler downloadResponseHandler, DownloadInfo info) {
            mHandler = handler;
            mDownloadResponseHandler = downloadResponseHandler;
            this.targetUrl = info.getTargetUrl();
            this.info = info;
        }

        @Override
        public void onFailure(Call call, final IOException e) {
            Log.e("onFailure", e.getMessage());

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDownloadResponseHandler.onFailure(e.toString());
                    info.setDownloadState(DOWNLOAD_STATE_FAIL);
                    downloadInfos.put(info.getUrl(), info);
                    SpTool.putMap(mContext, DOWNLOAD_MAPS, downloadInfos);
                    EventBus.getDefault().post(info);
                }
            });
        }

        @Override
        public void onResponse(Call call, final Response response) throws IOException {
            if (response.isSuccessful()) {
                File file = null;
                try {
                    file = saveFile(response, targetUrl);
                    final File newFile = file;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mDownloadResponseHandler.onFinish(newFile);

                            info.setDownloadState(DOWNLOAD_STATE_FINISH);
                            downloadInfos.remove(info.getUrl());
                            completeInfos.put(info.getUrl(), info);
                            SpTool.putMap(mContext, DOWNLOAD_MAPS, downloadInfos);
                            SpTool.putMap(mContext, COMPLETE_MAPS, completeInfos);
                            EventBus.getDefault().post(info);
                        }
                    });
                } catch (final Exception e) {
                    Log.e("onResponse fail", e.getMessage());

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            switch (info.getDownloadState()) {
                                case DOWNLOAD_STATE_CANCLE:

                                    Log.e("download cancle.", response.code() + "");
                                    mDownloadResponseHandler.onCancle(info);
                                    break;
                                case DOWNLOAD_STATE_PAUSE:
                                    Log.e("download pause.", response.code() + "");
                                    mDownloadResponseHandler.onPause(info);
                                    break;
                                default:
                                    Log.e("onResponse fail status", response.code() + "");
                                    mDownloadResponseHandler.onFailure("onResponse saveFile fail." + e.toString());
                                    info.setDownloadState(DOWNLOAD_STATE_FAIL);
                                    downloadInfos.put(info.getUrl(), info);
                                    SpTool.putMap(mContext, DOWNLOAD_MAPS, downloadInfos);
                                    break;
                            }

                            EventBus.getDefault().post(info);
                        }
                    });
                }

            } else {
                Log.e("onResponse fail status", response.code() + "");

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDownloadResponseHandler.onFailure("fail status=" + response.code());
                        info.setDownloadState(DOWNLOAD_STATE_FAIL);
                        downloadInfos.put(info.getUrl(), info);
                        SpTool.putMap(mContext, DOWNLOAD_MAPS, downloadInfos);
                        EventBus.getDefault().post(info);
                    }
                });
            }
        }
    }

    //保存文件
    private File saveFile(Response response, String targetUrl) throws IOException {
        File file = new File(targetUrl);
        InputStream is = null;
        FileOutputStream fileOutputStream = null;
        try {
            is = response.body().byteStream();
            fileOutputStream = new FileOutputStream(file, true);
            byte[] buffer = new byte[2048];//缓冲数组2kB
            int len;
            while ((len = is.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
            fileOutputStream.flush();
            return file;
        } finally {
            //关闭IO流
            try {
                if (is != null) is.close();
            } catch (IOException e) {
            }
            try {
                if (fileOutputStream != null) fileOutputStream.close();
            } catch (IOException ignored) {
            }
        }
    }
}
