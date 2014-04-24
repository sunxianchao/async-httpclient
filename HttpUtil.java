package cn.yunanalytics.util;

import cn.yunanalytics.http.AsyncHttpClient;
import cn.yunanalytics.http.AsyncHttpRequest;
import cn.yunanalytics.http.AsyncSimpleHttpResponseHandler;
import cn.yunanalytics.http.RequestParams;

/**
 * Http请求工具类
 * @author Liu Fei 2014-2-28
 */
public class HttpUtil {

    private static AsyncHttpClient client=new AsyncHttpClient();

    static {
        client.setTimeout(20000);
    }

    /**
     * 获取AsyncHttpClient对象
     * @return
     */
    public static AsyncHttpClient getClient() {
        return client;
    }

    /**
     * get方式请求, 可以请求一个文件，并通过返回的request进行取消操作
     */
    public static AsyncHttpRequest get(String url, AsyncSimpleHttpResponseHandler responseHandler) {
        return client.get(url, responseHandler);
    }

    public static AsyncHttpRequest get(String url, RequestParams params, AsyncSimpleHttpResponseHandler responseHandler) {
        return client.get(url, params, responseHandler);
    }

    /**
     * post方式请求
     */
    public static void post(String url, AsyncSimpleHttpResponseHandler responseHandler) {
        client.post(url, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncSimpleHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }

}
