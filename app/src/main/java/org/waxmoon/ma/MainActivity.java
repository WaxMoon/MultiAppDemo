package org.waxmoon.ma;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.hack.utils.ThreadUtils;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mIndicatorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIndicatorText = findViewById(R.id.tx_indicator);
    }

    @Override
    protected void onResume() {
        super.onResume();
        beginIndicatorAnimate();
        ApkEnv.INSTANCE().installApkAndQuickStartFromTask(this);
    }

    private void beginIndicatorAnimate() {
        ThreadUtils.postOnBackgroundThread(()->{
            long currentTime = System.currentTimeMillis();
            long waitEndTime = currentTime + ApkEnv.MAX_TIME_WAIT;
            int i = 0;
            String[] dots = {".", "..", "..."};
            while (currentTime < waitEndTime) {
                final int index = i%3;
                runOnUiThread(()->mIndicatorText.setText(dots[index]));
                i++;
                if (i >= 3) i = 0;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignore) {
                }
                currentTime = System.currentTimeMillis();
            }
        });
        Toast.makeText(this, "From github.com/WaxMoon/MultiApp", Toast.LENGTH_SHORT).show();
    }
}