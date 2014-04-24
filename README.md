async-httpclient
================
简易httpclient 实现，post、get请求服务端接口，文件下载取消等操作,网络不好的清空下自动重发请求

## 只是将封装的类放到github上了，拷贝到工程中修改包名即可

参数可以通过RequestParam 封装，主要参考的是loopj的实现

    HttpUtil.get("http://www.baidu.com", new 
    AsyncSimpleHttpResponseHandler() {
    @Override
     public void onSuccess(int statusCode, String responseBody, Header[] headers) {
        System.out.println(statusCode+responseBody+headers);                            
    }                        
    @Override
    public void onFailure(int statusCode, String responseBody, Header[] headers, Throwable error) {
        System.out.println(statusCode+responseBody+headers+error);                        
    }
});

文件下载示例：

    AsyncHttpRequest request=HttpUtil.get("http://yyygwtj.yunyoyo.cn/da/2/1/?http://yyyzy.yunyoyo.cn/game/yyygw/mxm/MXM_V1.0.apk#mp.weixin.qq.com", new AsyncFileHttpResponseHandler(MainActivity.this) {
                    
        @Override
        public void onSuccess(int statusCode, String responseBody, Header[] headers) {
               // TODO Auto-generated method stub
                        
        }
                    
        @Override
        public void onFailure(int statusCode, String responseBody, Header[] headers, Throwable error) {
              // TODO Auto-generated method stub
                        
        } 
        @Override
        public void onProgress(int bytesWritten, int totalSize) {
            super.onProgress(bytesWritten, totalSize);
         }             
    });

通过request对象可以取消下载，request.cancel();

未实现全部取消的功能
