package com.yang.download.download;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

public class DownLoadManager {
    private static final AtomicReference<DownLoadManager> INSTANCE = new AtomicReference<>();
    private OkHttpClient mClient;//OKHttpClient;
    private Context mContext;
    private int maxRequests = 1;


    //获得一个单例类
    public static DownLoadManager getInstance(Context context) {
        for (; ; ) {
            DownLoadManager current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new DownLoadManager(context);
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    private DownLoadManager(Context context) {
//        downCalls = new HashMap<>();
        mClient = new OkHttpClient();
        mClient.dispatcher().setMaxRequests(maxRequests);
        mContext = context;
    }

    /**
     * 设置最大请求数
     * @param maxRequests
     */
    public void setMaxRequests(int maxRequests){
        this.maxRequests = maxRequests;
        if(mClient!=null)
            mClient.dispatcher().setMaxRequests(maxRequests);
    }

    /**
     * 暂停/取消下载
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
    }

    public DownloadResponseHandler getDownloadResponseHandler(String url){
        DownloadResponseHandler downloadResponseHandler  = new DownloadResponseHandler() {
            @Override
            public void onFinish(File download_file) {

            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {

            }

            @Override
            public void onFailure(String error_msg) {

            }
        };
        return downloadResponseHandler;
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
        });
    }

    /**
     * 开始下载
     *
     * @param url                     下载请求的网址
     * @param targetUrl               下载保存的位置
     * @param downloadResponseHandler 用来回调的接口
     */
    public void download(final String url, final String targetUrl, final DownloadResponseHandler downloadResponseHandler) {

        if (mClient != null) {//包含下载url,不做处理
            for (Call call : mClient.dispatcher().queuedCalls()) {
                if (call.request().tag().equals(url))
                    return;
            }
            for (Call call : mClient.dispatcher().runningCalls()) {
                if (call.request().tag().equals(url))
                   return;
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                DownloadInfo info = createDownInfo(url,targetUrl);

                Request request;
                if (info!=null){
                    final DownloadInfo finalInfo1 = info;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            downloadResponseHandler.onProgress(finalInfo1.getProgress(), finalInfo1.getTotal());
                        }
                    });
                    request = new Request.Builder()
                            .addHeader("RANGE", "bytes=" + info.getProgress() + "-" + info.getTotal())
                            .url(url)
                            .tag(url)
                            .build();
                }else {
                    info = new DownloadInfo(url);
                    info.setTargetUrl(targetUrl);
                    request = new Request.Builder()
                            .url(url)
                            .tag(url)
                            .build();
                }

                final DownloadInfo finalInfo = info;
                Call call = mClient.newBuilder()
                        .addNetworkInterceptor(new Interceptor() {      //设置拦截器
                            @Override
                            public Response intercept(Chain chain) throws IOException {
                                Response originalResponse = chain.proceed(chain.request());
                                return originalResponse.newBuilder()
                                        .body(new ResponseProgressBody(originalResponse.body(), downloadResponseHandler, finalInfo))
                                        .build();
                            }
                        })
                        .build()
                        .newCall(request);
                call.enqueue(new MyDownloadCallback(new Handler(Looper.getMainLooper()), downloadResponseHandler, targetUrl));
            }
        }).start();

    }

    //下载回调
    private class MyDownloadCallback implements Callback {

        private Handler mHandler;
        private DownloadResponseHandler mDownloadResponseHandler;
        private String targetUrl;

        public MyDownloadCallback(Handler handler, DownloadResponseHandler downloadResponseHandler,
                                  String targetUrl) {
            mHandler = handler;
            mDownloadResponseHandler = downloadResponseHandler;
            this.targetUrl = targetUrl;
        }

        @Override
        public void onFailure(Call call, final IOException e) {
            Log.e("onFailure", e.getMessage());

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDownloadResponseHandler.onFailure(e.toString());
                }
            });
        }

        @Override
        public void onResponse(Call call, final Response response) throws IOException {
            if (response.isSuccessful()) {
                File file = null;
                try {
                    file = saveFile(response, targetUrl);
                } catch (final IOException e) {
                    Log.e("onResponse fail", e.getMessage());

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mDownloadResponseHandler.onFailure("onResponse saveFile fail." + e.toString());
                        }
                    });
                }

                final File newFile = file;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDownloadResponseHandler.onFinish(newFile);
                    }
                });
            } else {
                Log.e("onResponse fail status", response.code() + "");

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDownloadResponseHandler.onFailure("fail status=" + response.code());
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

    /**
     * 创建DownInfo
     *
     * @param url 请求网址
     * @return DownInfo
     */
    private DownloadInfo createDownInfo(String url,String targetUrl) {
        DownloadInfo downloadInfo = new DownloadInfo(url);
        long contentLength = getContentLength(url);//获得文件大小
        downloadInfo.setTotal(contentLength);
        String fileName = targetUrl.substring(targetUrl.lastIndexOf("/"));
        if (fileName.startsWith("/") && fileName.length() > 1)
            fileName = fileName.substring(1);
        downloadInfo.setFileName(fileName);
        downloadInfo.setTargetUrl(targetUrl);
        long downloadLength = 0;
        File file = new File(targetUrl);
        if (file.exists()) {
            //找到了文件,代表已经下载过,则获取其长度
            downloadLength = file.length();
        }
        //之前下载过,需要重新来一个文件
        int i = 1;
        while (downloadLength >= contentLength) {//删除重新下载
//            int dotIndex = fileName.lastIndexOf(".");
//            String fileNameOther;
//            if (dotIndex == -1) {
//                fileNameOther = fileName + "(" + i + ")";
//            } else {
//                fileNameOther = fileName.substring(0, dotIndex)
//                        + "(" + i + ")" + fileName.substring(dotIndex);
//            }
//            File newFile = new File(new File(targetUrl).getParent(), fileNameOther);
//            file = newFile;
//            downloadLength = newFile.length();
//            i++;
            file.delete();
            File newFile = new File(targetUrl);
            file = newFile;
            downloadLength = newFile.length();
        }
        //设置改变过的文件名/大小
        downloadInfo.setProgress(downloadLength);
        downloadInfo.setFileName(file.getName());
        return downloadInfo;
    }

    /**
     * 获取下载长度
     *
     * @param downloadUrl
     * @return
     */
    private long getContentLength(String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength == 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }
}
