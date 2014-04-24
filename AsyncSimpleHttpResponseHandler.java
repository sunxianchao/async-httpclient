package cn.yunanalytics.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EncodingUtils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public abstract class AsyncSimpleHttpResponseHandler {

    private static final String LOG_TAG="AsyncHttpResponseHandler";

    protected static final int SUCCESS_MESSAGE=0;

    protected static final int FAILURE_MESSAGE=1;

    protected static final int START_MESSAGE=2;

    protected static final int FINISH_MESSAGE=3;

    protected static final int PROGRESS_MESSAGE=4;

    protected static final int RETRY_MESSAGE=5;

    protected static final int CANCEL_MESSAGE=6;

    protected static final int BUFFER_SIZE=4096;

    public static final String DEFAULT_CHARSET="UTF-8";

    private String responseCharset=DEFAULT_CHARSET;

    private Handler handler;

    private boolean useSynchronousMode;

    private URI requestURI=null;

    private Header[] requestHeaders=null;

    public URI getRequestURI() {
        return this.requestURI;
    }

    public Header[] getRequestHeaders() {
        return this.requestHeaders;
    }

    public void setRequestURI(URI requestURI) {
        this.requestURI=requestURI;
    }

    public void setRequestHeaders(Header[] requestHeaders) {
        this.requestHeaders=requestHeaders;
    }

    /**
     * Avoid leaks by using a non-anonymous handler class.
     */
    private static class ResponderHandler extends Handler {

        private final AsyncSimpleHttpResponseHandler mResponder;

        ResponderHandler(AsyncSimpleHttpResponseHandler mResponder) {
            this.mResponder=mResponder;
        }

        @Override
        public void handleMessage(Message msg) {
            mResponder.handleMessage(msg);
        }
    }

    public boolean getUseSynchronousMode() {
        return useSynchronousMode;
    }

    public void setUseSynchronousMode(boolean value) {
        // A looper must be prepared before setting asynchronous mode.
        if(!value && Looper.myLooper() == null) {
            value=true;
            Log.w(LOG_TAG, "Current thread has not called Looper.prepare(). Forcing synchronous mode.");
        }

        // If using synchronous mode.
        if(!value && handler == null) {
            // Create a handler on current thread to submit tasks
            handler=new ResponderHandler(this);
        } else if(value && handler != null) {
            // TODO: Consider adding a flag to remove all queued messages.
            handler=null;
        }

        useSynchronousMode=value;
    }

    public void setCharset(final String charset) {
        this.responseCharset=charset;
    }

    /**
     * 默认utf-8
     * @return
     */
    public String getCharset() {
        return this.responseCharset == null ? DEFAULT_CHARSET : this.responseCharset;
    }

    /**
     * 创建一个AsyncHttpResponseHandler对象默认是异步请求
     */
    public AsyncSimpleHttpResponseHandler() {
        setUseSynchronousMode(false);
    }

    /**
     * 显示进度条，目前未实现
     */
    public void onProgress(int bytesWritten, int totalSize) {
        Log.d(LOG_TAG, String.format("Progress %d from %d (%2.0f%%)", bytesWritten, totalSize, (totalSize > 0)
            ? (bytesWritten * 1.0 / totalSize) * 100 : -1));
    }

    /**
     * 开始请求的时候触发，覆盖实现自己的方法
     */
    public void onStart() {
    }

    /**
     * 请求完成
     */
    public void onFinish() {
    }

    /**
     * 实现此方法，实现请求成功后的回掉接口
     */
    public abstract void onSuccess(int statusCode, String responseBody, Header[] headers);

    /**
     * 实现此方法，实现请求后后的回掉接口
     */
    public abstract void onFailure(int statusCode, String responseBody, Header[] headers, Throwable error);

    public void onRetry(int retryNo) {
        Log.d(LOG_TAG, String.format("Request retry no. %d", retryNo));
    }

    public void onCancel() {
        Log.d(LOG_TAG, "Request got cancelled");
    }

    final public void sendProgressMessage(int bytesWritten, int bytesTotal) {
        sendMessage(obtainMessage(PROGRESS_MESSAGE, new Object[]{bytesWritten, bytesTotal}));
    }

    final public void sendSuccessMessage(int statusCode, String responseBody, Header[] headers) {
        sendMessage(obtainMessage(SUCCESS_MESSAGE, new Object[]{statusCode, responseBody, headers}));
    }

    final public void sendFailureMessage(int statusCode, String responseBody, Header[] headers, Throwable throwable) {
        sendMessage(obtainMessage(FAILURE_MESSAGE, new Object[]{statusCode, headers, responseBody, throwable}));
    }

    final public void sendStartMessage() {
        sendMessage(obtainMessage(START_MESSAGE, null));
    }

    final public void sendFinishMessage() {
        sendMessage(obtainMessage(FINISH_MESSAGE, null));
    }

    final public void sendRetryMessage(int retryNo) {
        sendMessage(obtainMessage(RETRY_MESSAGE, new Object[]{retryNo}));
    }

    final public void sendCancelMessage() {
        sendMessage(obtainMessage(CANCEL_MESSAGE, null));
    }

    // Methods which emulate android's Handler and Message methods
    protected void handleMessage(Message message) {
        Object[] response;

        switch(message.what) {
            case SUCCESS_MESSAGE:
                response=(Object[])message.obj;
                if(response != null && response.length >= 3) {
                    onSuccess((Integer)response[0], (String)response[1], (Header[])response[2]);
                } else {
                    Log.e(LOG_TAG, "SUCCESS_MESSAGE didn't got enough params");
                }
                break;
            case FAILURE_MESSAGE:
                response=(Object[])message.obj;
                if(response != null && response.length >= 4) {
                    onFailure((Integer)response[0], (String)response[1], (Header[])response[2], (Throwable)response[3]);
                } else {
                    Log.e(LOG_TAG, "FAILURE_MESSAGE didn't got enough params");
                }
                break;
            case START_MESSAGE:
                onStart();
                break;
            case FINISH_MESSAGE:
                onFinish();
                break;
            case PROGRESS_MESSAGE:
                response=(Object[])message.obj;
                if(response != null && response.length >= 2) {
                    try {
                        onProgress((Integer)response[0], (Integer)response[1]);
                    } catch(Throwable t) {
                        Log.e(LOG_TAG, "custom onProgress contains an error", t);
                    }
                } else {
                    Log.e(LOG_TAG, "PROGRESS_MESSAGE didn't got enough params");
                }
                break;
            case RETRY_MESSAGE:
                response=(Object[])message.obj;
                if(response != null && response.length == 1)
                    onRetry((Integer)response[0]);
                else
                    Log.e(LOG_TAG, "RETRY_MESSAGE didn't get enough params");
                break;
            case CANCEL_MESSAGE:
                onCancel();
                break;
        }
    }

    public static String getResponseString(byte[] stringBytes, String charset) {
        try {
            return stringBytes == null ? null : new String(stringBytes, charset);
        } catch(UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Encoding response into string failed", e);
            return null;
        }
    }

    protected void sendMessage(Message msg) {
        if(getUseSynchronousMode() || handler == null) {
            handleMessage(msg);
        } else if(!Thread.currentThread().isInterrupted()) { // do not send messages if request has been cancelled
            handler.sendMessage(msg);
        }
    }

    /**
     * Helper method to send runnable into local handler loop
     * @param runnable runnable instance, can be null
     */
    protected void postRunnable(Runnable runnable) {
        if(runnable != null) {
            if(getUseSynchronousMode() || handler == null) {
                // This response handler is synchronous, run on current thread
                runnable.run();
            } else {
                // Otherwise, run on provided handler
                handler.post(runnable);
            }
        }
    }

    /**
     * Helper method to create Message instance from handler
     * @param responseMessageId constant to identify Handler message
     * @param responseMessageData object to be passed to message receiver
     * @return Message instance, should not be null
     */
    protected Message obtainMessage(int responseMessageId, Object responseMessageData) {
        Message msg;
        if(handler == null) {
            msg=Message.obtain();
            if(msg != null) {
                msg.what=responseMessageId;
                msg.obj=responseMessageData;
            }
        } else {
            msg=Message.obtain(handler, responseMessageId, responseMessageData);
        }
        return msg;
    }

    public void sendResponseMessage(HttpResponse response) throws IOException {
        // do not process if request has been cancelled
        if(!Thread.currentThread().isInterrupted()) {
            StatusLine status=response.getStatusLine();
            String responseBody=getResponseData(response.getEntity());
            // additional cancellation check as getResponseData() can take non-zero time to process
            if(!Thread.currentThread().isInterrupted()) {
                if(status.getStatusCode() >= 300) {
                    sendFailureMessage(status.getStatusCode(), responseBody, response.getAllHeaders(), new HttpResponseException(
                        status.getStatusCode(), status.getReasonPhrase()));
                } else {
                    sendSuccessMessage(status.getStatusCode(), responseBody, response.getAllHeaders());
                }
                System.out.println(response.getAllHeaders());
            }
        }
    }

    String getResponseData(HttpEntity entity) throws IOException {
        byte[] responseBody=null;
        if(entity != null) {
            InputStream instream=entity.getContent();
            if(instream != null) {
                long contentLength=entity.getContentLength();
                if(contentLength > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
                }
                int buffersize=(contentLength <= 0) ? BUFFER_SIZE : (int)contentLength;
                try {
                    ByteArrayBuffer buffer=new ByteArrayBuffer(buffersize);
                    try {
                        byte[] tmp=new byte[BUFFER_SIZE];
                        int l, count=0;
                        // do not send messages if request has been cancelled
                        while((l=instream.read(tmp)) != -1 && !Thread.currentThread().isInterrupted()) {
                            count+=l;
                            buffer.append(tmp, 0, l);
                            sendProgressMessage(count, (int)(contentLength <= 0 ? 1 : contentLength));
                        }
                    } finally {
                        AsyncHttpClient.silentCloseInputStream(instream);
                    }
                    responseBody=buffer.toByteArray();
                } catch(OutOfMemoryError e) {
                    System.gc();
                    throw new IOException("File too large to fit into available memory");
                }finally{
                }
            }
        }
        return EncodingUtils.getString(responseBody, "utf-8");  
    }
}
