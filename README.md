# DownLoadManager
Android下载管理器

基于okhttp的下载管理器，可实现单个页面的进度回调，也可实现所有下载进度的监听.

下载方法：
    
    //DownloadResponseHandler为单个进度的回调
    DownLoadManager.getInstance(this).download(url1, getDiskFileDir(this) + "/app.apk", new DownloadResponseHandler() {
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
        });
        
监听各个下载进度（eventbus实现，也可用广播等）

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadInfo info){
        txt.setText(txt.getText().toString()+"\n"+info.getUrl()+"---progress:"+info.getProgress()+"---total:"+info.getTotal());
    }
    
暂停、取消下载

    DownLoadManager.getInstance(this).cancel(url);
    
设置同时下载请求数（默认为1）

    DownLoadManager.getInstance(this).setMaxRequests(1);

    

   
