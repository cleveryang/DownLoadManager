package com.yang.download.download;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * 重写responsebody 设置下载进度监听
 * Created by yang on 18/6/29.
 */
public class ResponseProgressBody extends ResponseBody {

    private ResponseBody mResponseBody;
    private DownloadResponseHandler mDownloadResponseHandler;
    private BufferedSource bufferedSource;
    private DownloadInfo info;
    private long progress;//开始前已下载进度

    public ResponseProgressBody(ResponseBody responseBody, DownloadResponseHandler downloadResponseHandler, DownloadInfo info) {
        this.mResponseBody = responseBody;
        this.mDownloadResponseHandler = downloadResponseHandler;
        this.info = info;
        progress = info.getProgress();
    }

    @Override
    public MediaType contentType() {
        return mResponseBody.contentType();
    }

    @Override
    public long contentLength() {
        if(info.getTotal()<=0){
            info.setTotal(mResponseBody.contentLength());
        }
        return mResponseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(mResponseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {

        return new ForwardingSource(source) {

            long totalBytesRead;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                totalBytesRead += ((bytesRead != -1) ? bytesRead : 0);
                if (mDownloadResponseHandler != null) {
                    info.setProgress(totalBytesRead+progress);
//                    mDownloadResponseHandler.onProgress(totalBytesRead+(info!=null?info.getProgress():0), info!=null?info.getProgress()+mResponseBody.contentLength():mResponseBody.contentLength());
                    mDownloadResponseHandler.onProgress(info.getProgress(), info.getTotal());
                    info.setDownloadState(DownLoadManager.DOWNLOAD_STATE_DOWNLOADING);
                    EventBus.getDefault().post(info);
                }
                return bytesRead;
            }
        };
    }
}
