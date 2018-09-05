package info.tinyapps.huges.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import info.tinyapps.huges.R;

/**
 * simple splash screen just waits 350 ms before redirect user to
 * login screen
 */
public class SplashScreen extends BaseActivity {

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            startActivity(new Intent(getThis(),LoginActivity.class));
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mHandler.sendEmptyMessageDelayed(1,350);
    }
}

