package cn.yunanalytics.demo;

import org.apache.http.Header;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import cn.yunanalytics.http.AsyncFileHttpResponseHandler;
import cn.yunanalytics.http.AsyncHttpRequest;
import cn.yunanalytics.http.AsyncSimpleHttpResponseHandler;
import cn.yunanalytics.util.HttpUtil;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button btn=new Button(this);
        btn.setId(12);
        btn.setText("云游游登录");
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                HttpUtil.get("http://www.baidu.com", new AsyncSimpleHttpResponseHandler() {
                    
                    @Override
                    public void onSuccess(int statusCode, String responseBody, Header[] headers) {
                        System.out.println(statusCode+responseBody+headers);
                        
                    }
                    
                    @Override
                    public void onFailure(int statusCode, String responseBody, Header[] headers, Throwable error) {
                        System.out.println(statusCode+responseBody+headers+error);                        
                    }
                });

            }

        });
        
        Button btn2=new Button(this);
        btn2.setId(12);
        btn2.setText("下载测试");
        btn2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
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
               
               try {
                Thread.sleep(10000);
            } catch(InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
               request.cancel(true);

            }

        });
        
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(btn);
        layout.addView(btn2);
        setContentView(layout);

    }

}
