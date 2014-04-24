package cn.yunanalytics.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

/**
 * 异步通讯模拟loopj实现
 * @author Sunxc
 */
public class AsyncHttpRequest implements Runnable {

    private final String TAG="AsyncHttpRequest";

    private final AbstractHttpClient client;

    private final HttpContext context;

    private final HttpUriRequest request;

    private final AsyncSimpleHttpResponseHandler responseHandler;

    private int executionCount;

    private boolean isCancelled=false;

    private boolean cancelIsNotified=false;

    private boolean isFinished=false;

    public AsyncHttpRequest(AbstractHttpClient client, HttpContext context, HttpUriRequest request,
        AsyncSimpleHttpResponseHandler responseHandler) {
        this.client=client;
        this.context=context;
        this.request=request;
        this.responseHandler=responseHandler;
    }

    @Override
    public void run() {
        if(isCancelled()) {
            return;
        }

        if(responseHandler != null) {
            responseHandler.sendStartMessage();
        }

        if(isCancelled()) {
            return;
        }

        try {
            makeRequestWithRetries();
        } catch(IOException e) {
            if(!isCancelled() && responseHandler != null) {
                responseHandler.sendFailureMessage(0, null, null, e);
            } else {
                Log.e(TAG, "makeRequestWithRetries returned error, but handler is null", e);
            }
        }

        if(isCancelled()) {
            return;
        }

        if(responseHandler != null) {
            responseHandler.sendFinishMessage();
        }

        isFinished=true;

    }

    private void makeRequest() throws IOException {
        if(isCancelled()) {
            return;
        }
        if(request.getURI().getScheme() == null) {
            throw new MalformedURLException("No valid URI scheme was provided");
        }

        HttpResponse response=client.execute(request, context);

        // 没取消并且responseHandler不为空，则处理httpResponse
        if(!isCancelled() && responseHandler != null) {
            responseHandler.sendResponseMessage(response);
        }
    }

    private void makeRequestWithRetries() throws IOException {
        boolean retry=true;
        IOException cause=null;
        HttpRequestRetryHandler retryHandler=client.getHttpRequestRetryHandler();
        try {
            while(retry) {
                try {
                    makeRequest();
                    return;
                } catch(UnknownHostException e) {
                    // wifi 切换 移动网络的请求下有可能会出现UnknownHostException异常，这时重试可能比报错会好一些
//                    retry=(executionCount > 0) && retryHandler.retryRequest(cause, ++executionCount, context);
                    cause = new IOException("UnknownHostException exception: " + e.getMessage());
                    retry=retryHandler.retryRequest(e, ++executionCount, context);
                } catch(NullPointerException e) {
                    cause = new IOException("NPE in HttpClient: " + e.getMessage());
                    retry=retryHandler.retryRequest(cause, ++executionCount, context);
                } catch(IOException e) {
                    if(isCancelled()) {
                        return;
                    }
                    cause=e;
                    retry=retryHandler.retryRequest(cause, ++executionCount, context);
                }
                if(retry && (responseHandler != null)) {
                    responseHandler.sendRetryMessage(executionCount);
                }
            }
        } catch(Exception e) {
            Log.e("AsyncHttpRequest", "Unhandled exception origin cause", e);
            cause=new IOException("Unhandled exception: " + e.getMessage());
        }
        throw(cause);
    }

    public boolean isCancelled() {
        if(isCancelled) {
            sendCancelNotification();
        }
        return isCancelled;
    }

    private synchronized void sendCancelNotification() {
        if(!isFinished && isCancelled && !cancelIsNotified) {
            cancelIsNotified=true;
            if(responseHandler != null)
                responseHandler.sendCancelMessage();
        }
    }

    public boolean isDone() {
        return isCancelled() || isFinished;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        isCancelled=true;
        request.abort();
        return isCancelled();
    }

}
