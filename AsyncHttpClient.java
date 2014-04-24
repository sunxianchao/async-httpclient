package cn.yunanalytics.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

import android.content.Context;
import android.util.Log;

public class AsyncHttpClient {

    public static final int DEFAULT_MAX_CONNECTIONS=10;

    public static final int DEFAULT_SOCKET_TIMEOUT=10 * 1000;

    public static final int DEFAULT_MAX_RETRIES=2;

    public static final int DEFAULT_SOCKET_BUFFER_SIZE=8192;

    public static final String HEADER_ACCEPT_ENCODING="Accept-Encoding";

    public static final String ENCODING_GZIP="gzip";

    public static final String LOG_TAG="AsyncHttpClient";

    private int maxConnections=DEFAULT_MAX_CONNECTIONS;

    private int timeout=DEFAULT_SOCKET_TIMEOUT;

    private final DefaultHttpClient httpClient;

    private final HttpContext httpContext;

    private ExecutorService threadPool;

    private boolean isUrlEncodingEnabled=true;

    public AsyncHttpClient() {
        this(80, 443);
    }

    public AsyncHttpClient(int httpPort) {
        this(httpPort, 443);
    }

    public AsyncHttpClient(int httpPort, int httpsPort) {
        this(getDefaultSchemeRegistry(httpPort, httpsPort));
    }

    private AsyncHttpClient(SchemeRegistry schemeRegistry) {

        BasicHttpParams httpParams=new BasicHttpParams();

        ConnManagerParams.setTimeout(httpParams, timeout);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(maxConnections));
        ConnManagerParams.setMaxTotalConnections(httpParams, DEFAULT_MAX_CONNECTIONS);

        HttpConnectionParams.setSoTimeout(httpParams, timeout);
        HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpConnectionParams.setSocketBufferSize(httpParams, DEFAULT_SOCKET_BUFFER_SIZE);

        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);

        ThreadSafeClientConnManager cm=new ThreadSafeClientConnManager(httpParams, schemeRegistry);

        threadPool=getDefaultThreadPool();

        httpContext=new SyncBasicHttpContext(new BasicHttpContext());
        httpClient=new DefaultHttpClient(cm, httpParams);
        httpClient.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
            
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                boolean retry = true;
                // 设置恢复策略，在发生异常时候将自动重试3次    
                if (executionCount > DEFAULT_MAX_RETRIES) {      
                    // 超过最大次数则不需要重试      
                    retry=false;      
                }      
                if (exception instanceof NoHttpResponseException) {      
                    // 服务停掉则重新尝试连接      
                    retry = true;      
                }
                if(exception instanceof UnknownHostException){
                    retry= true;
                }
                if (exception instanceof SSLHandshakeException) {      
                    // SSL异常不需要重试      
                    retry= false;      
                }
                if(retry){
                    HttpRequest currentReq = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);    
                    if (currentReq == null) {
                        retry=false;
                    }
                }
                return retry;    
            }    
        });
    }

    /**
     * 设置http http是访问协议
     * @param fixNoHttpResponseException
     * @param httpPort
     * @param httpsPort
     * @return
     */
    private static SchemeRegistry getDefaultSchemeRegistry(int httpPort, int httpsPort) {

        if(httpPort < 1) {
            httpPort=80;
            Log.d(LOG_TAG, "Invalid HTTP port number specified, defaulting to 80");
        }

        if(httpsPort < 1) {
            httpsPort=443;
            Log.d(LOG_TAG, "Invalid HTTPS port number specified, defaulting to 443");
        }

        SchemeRegistry schemeRegistry=new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), httpPort));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), httpsPort));
        return schemeRegistry;
    }

    public AsyncHttpRequest get(String url, AsyncSimpleHttpResponseHandler responseHandler) {
        return get(null, url, null, responseHandler);
    }

    public AsyncHttpRequest get(String url, RequestParams params, AsyncSimpleHttpResponseHandler responseHandler) {
        return get(null, url, params, responseHandler);
    }

    public AsyncHttpRequest get(Context context, String url, AsyncSimpleHttpResponseHandler responseHandler) {
        return get(context, url, null, responseHandler);
    }

    public AsyncHttpRequest get(Context context, String url, RequestParams params, AsyncSimpleHttpResponseHandler responseHandler) {
        return sendRequest(httpClient, httpContext, new HttpGet(getUrlWithQueryString(isUrlEncodingEnabled, url, params)), null,
            responseHandler, context);
    }

    public AsyncHttpRequest get(Context context, String url, Header[] headers, RequestParams params, AsyncSimpleHttpResponseHandler responseHandler) {
        HttpUriRequest request=new HttpGet(getUrlWithQueryString(isUrlEncodingEnabled, url, params));
        if(headers != null)
            request.setHeaders(headers);
        return sendRequest(httpClient, httpContext, request, null, responseHandler, context);
    }

    public AsyncHttpRequest post(String url, AsyncSimpleHttpResponseHandler responseHandler) {
        return post(null, url, null, responseHandler);
    }

    public AsyncHttpRequest post(String url, RequestParams params, AsyncSimpleHttpResponseHandler responseHandler) {
        return post(null, url, params, responseHandler);
    }

    public AsyncHttpRequest post(Context context, String url, RequestParams params, AsyncSimpleHttpResponseHandler responseHandler) {
        return post(context, url, null, params, null, responseHandler);
    }

    public AsyncHttpRequest post(Context context, String url, Header[] headers, RequestParams params, String contentType,
        AsyncSimpleHttpResponseHandler responseHandler) {
        HttpEntityEnclosingRequestBase request=new HttpPost(URI.create(url).normalize());
        if(params != null)
            request.setEntity(paramsToEntity(params, responseHandler));
        if(headers != null)
            request.setHeaders(headers);
        return sendRequest(httpClient, httpContext, request, contentType, responseHandler, context);
    }

    protected AsyncHttpRequest sendRequest(DefaultHttpClient client, HttpContext httpContext, HttpUriRequest uriRequest, String contentType,
        AsyncSimpleHttpResponseHandler responseHandler, Context context) {
        if(uriRequest == null) {
            throw new IllegalArgumentException("HttpUriRequest must not be null");
        }

        if(responseHandler == null) {
            throw new IllegalArgumentException("ResponseHandler must not be null");
        }

        if(responseHandler.getUseSynchronousMode()) {
            throw new IllegalArgumentException(
                "Synchronous ResponseHandler used in AsyncHttpClient. You should create your response handler in a looper thread or use SyncHttpClient instead.");
        }

        if(contentType != null) {
            uriRequest.setHeader("Content-Type", contentType);
        }

        responseHandler.setRequestHeaders(uriRequest.getAllHeaders());
        responseHandler.setRequestURI(uriRequest.getURI());

        AsyncHttpRequest request=new AsyncHttpRequest(client, httpContext, uriRequest, responseHandler);
        httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, uriRequest);
        threadPool.submit(request);
        return request;
    }

    private HttpEntity paramsToEntity(RequestParams params, AsyncSimpleHttpResponseHandler responseHandler) {
        HttpEntity entity=null;

        try {
            if(params != null) {
                entity=params.getEntity(responseHandler);
            }
        } catch(Throwable t) {
            if(responseHandler != null)
                responseHandler.sendFailureMessage(0, null, null, t);
            else
                t.printStackTrace();
        }

        return entity;
    }

    public boolean isUrlEncodingEnabled() {
        return isUrlEncodingEnabled;
    }

    public static String getUrlWithQueryString(boolean shouldEncodeUrl, String url, RequestParams params) {
        if(shouldEncodeUrl)
            url=url.replace(" ", "%20");

        if(params != null) {
            // Construct the query string and trim it, in case it
            // includes any excessive white spaces.
            String paramString=params.getParamString().trim();

            // Only add the query string if it isn't empty and it
            // isn't equal to '?'.
            if(!paramString.equals("") && !paramString.equals("?")) {
                url+=url.contains("?") ? "&" : "?";
                url+=paramString;
            }
        }

        return url;
    }

    public static void silentCloseInputStream(InputStream is) {
        try {
            if(is != null) {
                is.close();
            }
        } catch(IOException e) {
            Log.w(LOG_TAG, "Cannot close input stream", e);
        }
    }

    public static void silentCloseOutputStream(OutputStream os) {
        try {
            if(os != null) {
                os.close();
            }
        } catch(IOException e) {
            Log.w(LOG_TAG, "Cannot close output stream", e);
        }
    }

    /**
     * 获取缓存连接池连接池
     * @return
     */
    protected ExecutorService getDefaultThreadPool() {
        return Executors.newCachedThreadPool();
    }

    public HttpClient getHttpClient() {
        return this.httpClient;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        if(timeout < 1000)
            timeout=DEFAULT_SOCKET_TIMEOUT;
        this.timeout=timeout;
        final HttpParams httpParams=this.httpClient.getParams();
        ConnManagerParams.setTimeout(httpParams, this.timeout);
        HttpConnectionParams.setSoTimeout(httpParams, this.timeout);
        HttpConnectionParams.setConnectionTimeout(httpParams, this.timeout);
    }

}
