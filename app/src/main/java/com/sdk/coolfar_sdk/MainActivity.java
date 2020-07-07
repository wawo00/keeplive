package com.sdk.coolfar_sdk;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import com.google.android.gms.ads.MobileAds;
import com.roy.ShortCutHelper;
import com.sdk.coolfar_sdk.shortcut.ShortcutActivity;
import com.sdk.keepbackground.work.DaemonEnv;
import com.sdk.keepbackground.work.IntentWrapper;


public class MainActivity extends Activity {
    private static String TAG = "Roy_KeepLive";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DaemonEnv.setActivity(this);
        initLocaService();
    }

    private void initLocaService() {
        //初始化 ,移动到application中进行初始化
        //請求用戶忽略电池优化
//        String reason="轨迹跟踪服务的持续运行";
//        DaemonEnv.whiteListMatters(this, reason);
        //启动work服务
        DaemonEnv.startServiceSafelyWithData(MainActivity.this,MyService.class);

    }

    //防止华为机型未加入白名单时按返回键回到桌面再锁屏后几秒钟进程被杀
    @Override
    public void onBackPressed() {
        IntentWrapper.onBackPressed(this);
    }

    public void hideIcon(View view) {
        ShortCutHelper.hideAppIcon(this);

    }

    public void addShortCut(View view) {
        ShortCutHelper.addShortcut(this,MainActivity.this, ShortcutActivity.class,"学习任务",true,R.drawable.ic_shortcut);

    }
}
