package org.waxmoon.ma;

import android.content.Context;
import com.hack.opensdk.HackApplication;

public class MoonApp extends HackApplication {

    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        /********BEGIN early init********/
        ApkEnv.INSTANCE().prepare(this.getBaseContext());
        /********END early init********/
        //Your code must be after earlyInit
        //todo...
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
