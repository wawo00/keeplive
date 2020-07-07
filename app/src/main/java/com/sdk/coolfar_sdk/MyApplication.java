package com.sdk.coolfar_sdk;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.sdk.keepbackground.work.DaemonEnv;

/**
 * @ProjectName: KeepAlive
 * @Package: com.sdk.coolfar_sdk
 * @ClassName: MyApplication
 * @Description: MyApplication
 * @Author: Roy
 * @CreateDate: 2020/6/1 17:02
 */

public class MyApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        DaemonEnv.init(this);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        super.onCreate();
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            MultiDex.install(this);
        }
    }


}
