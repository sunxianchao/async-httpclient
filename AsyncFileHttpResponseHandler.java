package cn.yunanalytics.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import android.content.Context;
import android.util.Log;


public abstract class AsyncFileHttpResponseHandler extends AsyncSimpleHttpResponseHandler {

    private static final String LOG_TAG = "FileAsyncHttpResponseHandler";

    protected final File mFile;
    
    public AsyncFileHttpResponseHandler(Context context) {
        super();
        this.mFile = getTemporaryFile(context);
    }
    public abstract void onSuccess(int statusCode, String responseBody, Header[] headers);

    public abstract void onFailure(int statusCode, String responseBody, Header[] headers, Throwable error);

    protected File getTargetFile() {
        assert (mFile != null);
        return mFile;
    }
    
    public boolean deleteTargetFile() {
        return getTargetFile() != null && getTargetFile().delete();
    }
    
    protected File getTemporaryFile(Context context) {
        assert (context != null);
        try {
            return File.createTempFile("temp_", "_handled", context.getCacheDir());
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Cannot create temporary file", t);
        }
        return null;
    }
    
    protected String getResponseData(HttpEntity entity) throws IOException {
        if (entity != null) {
            InputStream instream = entity.getContent();
            long contentLength = entity.getContentLength();
            FileOutputStream buffer = new FileOutputStream(getTargetFile());
            if (instream != null) {
                try {
                    byte[] tmp = new byte[BUFFER_SIZE];
                    int l, count = 0;
                    // do not send messages if request has been cancelled
                    while ((l = instream.read(tmp)) != -1 && !Thread.currentThread().isInterrupted()) {
                        count += l;
                        buffer.write(tmp, 0, l);
                        sendProgressMessage(count, (int) contentLength);
                    }
                } finally {
                    AsyncHttpClient.silentCloseInputStream(instream);
                    buffer.flush();
                    AsyncHttpClient.silentCloseOutputStream(buffer);
                }
            }
        }
        return null;
    }
}
