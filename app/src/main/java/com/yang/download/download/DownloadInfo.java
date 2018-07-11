package com.yang.download.download;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yang on 2018/6/21.
 * 下载信息
 */

public class DownloadInfo implements Serializable{
    private String url;//下载路径
    private String targetUrl;//存储路径
    private long total;//总大小
    private long progress;//当前进度
    private String fileName;//名称
    private int downloadState;//下载状态

    public DownloadInfo(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public void setDownloadState(int downloadState) {
        this.downloadState = downloadState;
    }

    public int getDownloadState() {
        return downloadState;
    }
}
