package com.yang.download.download;

import java.io.File;

/**
 * 下载回调
 * Created by yang on 18/6/29.
 */
public abstract class DownloadResponseHandler {

    public abstract void onProgress(long currentBytes, long totalBytes);
    public abstract void onFinish(File download_file);
    public abstract void onPause(DownloadInfo downloadInfo);
    public abstract void onCancle(DownloadInfo downloadInfo);
    public abstract void onFailure(String error_msg);
}
