# DownloadManager
##Android下载管理器

1、可实现下载、暂停、取消、完成、失败、队列等待等状态的监听

2、可断点续传

3、基于okhttp的下载管理器，可实现单个页面的进度回调，也可实现所有下载进度的监听.

4、可获取下载队列里和已完成下载的所有任务

5、可设置最大同时下载线程数

下载方式有三种：

    /**
     * 添加下载任务
     *
     * @param url       下载请求的网址
     * @param targetUrl 下载保存的位置
     */
    DownLoadManager.getInstance(this).download(String url, String targetUrl);
    
    /**
     * 添加下载任务
     *
     * @param url                     下载请求的网址
     * @param targetUrl               下载保存的位置
     * @param downloadResponseHandler 用来回调的接口
     */
    DownLoadManager.getInstance(this).download(String url, String targetUrl, final DownloadResponseHandler downloadResponseHandler);
    
    /**
     * 添加下载任务
     *
     * @param downloadInfo            下载类
     * @param downloadResponseHandler 用来回调的接口
     */
    DownLoadManager.getInstance(this).download(DownloadInfo downloadInfo, final DownloadResponseHandler downloadResponseHandler)
        
##监听下载进度

单个任务监听
    DownloadResponseHandler回调

全局监听(eventbus实现，也可用广播等)

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DownloadInfo info){
        //返回info对象
    }
    
取消下载

    DownLoadManager.getInstance(this).cancel(url);
    
取消全部下载

    DownLoadManager.getInstance(this).cancelAll();
    
    
暂停下载

    DownLoadManager.getInstance(this).pause(url);
    
暂停全部下载

    DownLoadManager.getInstance(this).pauseAll();
    
设置同时下载请求数（默认为1）

    DownLoadManager.getInstance(this).setMaxRequests(1);

得到下载队列(等待中，下载中和暂停的任务)

    DownloadManager.getInstance(this).getDownloadInfos();

得到已完成队列(不校验文件是否存在或完整，如用户手动删除了源文件)

    DownloadManager.getInstance(this).getCompleteInfos();

DownloadInfo类

    private String url;//下载路径
    private String targetUrl;//存储路径
    private long total;//总大小
    private long progress;//当前进度
    private String fileName;//名称
    private int downloadState;//下载状态
   
各个下载状态

    public static final int DOWNLOAD_STATE_WAITING = 0x00;//等待
    public static final int DOWNLOAD_STATE_DOWNLOADING = 0x01;//下载中
    public static final int DOWNLOAD_STATE_PAUSE = 0x02;//暂停
    public static final int DOWNLOAD_STATE_CANCLE = 0x03;//取消
    public static final int DOWNLOAD_STATE_FINISH = 0x04;//完成
    public static final int DOWNLOAD_STATE_FAIL = 0x05;//失败
    public static final int DOWNLOAD_STATE_RESTART = 0x06;//重新下载
