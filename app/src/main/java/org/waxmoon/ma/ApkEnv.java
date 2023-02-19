package org.waxmoon.ma;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ConditionVariable;

import com.hack.Slog;
import com.hack.opensdk.Cmd;
import com.hack.opensdk.CmdConstants;
import com.hack.opensdk.HackApi;
import com.hack.utils.FileUtils;
import com.hack.utils.ProcessUtils;
import com.hack.utils.Singleton;
import com.hack.utils.ThreadUtils;

import java.io.File;
import java.io.IOException;

public class ApkEnv {
    private static final String TAG = "ApkEnv";

    /***************BEGIN TARGET APP CONSTANTS.You can modify!**************/
    public static final boolean PREINSTALL_GOOGLE_SUITE = true;
    public static final String APP_PACKAGE_NAME = "com.example.android.codelab.animation";
    public static final int APP_INSTALL_USER = 0;
    public static final String APP_BASE_NAME = "target.apk";
    public static final long MAX_TIME_WAIT = 10000;//10s
    /******************END TARGET APP CONSTANTS*************************/

    public static final String SP_NAME = "moon";
    public static final String APP_DEST_DIR = ".plugin/direct/";
    private ConditionVariable mExtractTaskComplete = new ConditionVariable(true);

    private Context mContext;

    private static final Singleton<ApkEnv> singleton = new Singleton<ApkEnv>() {
        @Override
        protected ApkEnv create() {
            return new ApkEnv();
        }
    };

    public static ApkEnv INSTANCE() {
        return singleton.get();
    }

    public void waitExtractComplete() {
        mExtractTaskComplete.block();
    }

    private ApkEnv() {}

    public void prepare(Context context) {
        mContext = context;
        ProcessUtils.tryGetProcessType(context);
        extractTargetApkIfNeeded();
    }

    public void installApkAndQuickStartFromTask(final Activity from) {
        final SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        boolean hasInstalled = sp.getBoolean(APP_PACKAGE_NAME + "_installed", false);
        if (hasInstalled) {
            startTargetApp(from);
        } else {
            ThreadUtils.postOnBackgroundThread(()->{
                if (PREINSTALL_GOOGLE_SUITE) {
                    installGoogleTools();
                }
                waitExtractComplete();
                File apkFile = new File(mContext.getFilesDir(), APP_DEST_DIR + APP_BASE_NAME);
                int installResult = HackApi.installApkFiles(apkFile.getAbsolutePath(), APP_INSTALL_USER, false);
                if (installResult != 1 && installResult != -1/*INSTALL_FAILED_ALREADY_EXISTS*/) {
                    throw new RuntimeException("installApkIfNeeded failed");
                }
                Slog.d(TAG, "installApkIfNeeded pkg@%s success", APP_PACKAGE_NAME);
                sp.edit().putBoolean(APP_PACKAGE_NAME + "_installed", true).apply();
                startTargetApp(from);
            });
        }
    }

    private void installGoogleTools() {
        Slog.d(TAG, "installGoogleTools begin");
        String[] googlePkgs = {
                "com.google.android.gms",
                "com.google.android.gsf",
                "com.android.vending"
        };
        for (String pkg : googlePkgs) {
            HackApi.installPackageFromHost(pkg, APP_INSTALL_USER, false);
        }
        Slog.d(TAG, "installGoogleTools end");
    }

    private void extractTargetApkIfNeeded() {
        if (ProcessUtils.isClient()) {
            final SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            boolean hasExtracted = sp.getBoolean(APP_PACKAGE_NAME + "_extract", false);
            if (!hasExtracted) {
                mExtractTaskComplete.close();
                extractTargetApkAsync();
            }
        }
    }

    private void extractTargetApkAsync() {
        ThreadUtils.postOnBackgroundThread(() -> {

            File destDir = new File(mContext.getFilesDir(), APP_DEST_DIR);

            if (!destDir.exists()) destDir.mkdirs();

            File destApkFile = new File(destDir, APP_BASE_NAME);

            try {
                Slog.d(TAG, "extractTargetApkAsync to %s begin", destApkFile.getAbsolutePath());
                FileUtils.extractAsset(mContext, APP_BASE_NAME, destApkFile);
                Slog.d(TAG, "extractTargetApkAsync to %s success", destApkFile.getAbsolutePath());
                final SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                sp.edit().putBoolean(APP_PACKAGE_NAME + "_extract", true).apply();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            mExtractTaskComplete.open();
        });
    }

    private void startTargetApp(Activity activity) {
        try {
            Slog.d(TAG, "startTargetApp begin");
            Intent intent = HackApi.getLaunchIntentForPackage(APP_PACKAGE_NAME, APP_INSTALL_USER);
            //HackApi.startActivity(intent, MoonApp.APP_INSTALL_USER);
            Cmd.INSTANCE().exec(CmdConstants.CMD_QUICK_START_ACTIVITY, activity, intent, 0);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Slog.d(TAG, "startTargetApp end");
        activity.finish();
    }
}
