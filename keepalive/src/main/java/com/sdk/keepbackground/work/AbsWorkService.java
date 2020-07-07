package com.sdk.keepbackground.work;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;


import com.sdk.keepbackground.utils.FileUtils;
import com.sdk.keepbackground.utils.SpManager;
import com.sdk.keepbackground.watch.AbsServiceConnection;
import com.sdk.keepbackground.utils.ForegroundNotificationUtils;
import com.sdk.keepbackground.singlepixel.ScreenReceiverUtil;
import com.sdk.keepbackground.watch.WatchDogService;
import com.sdk.keepbackground.watch.WatchProcessPrefHelper;

import static com.sdk.keepbackground.utils.SpManager.Keys.WORK_SERVICE;


/**
 * 主要Service 用户继承该类用来处理自己业务逻辑
 * <p>
 * 该类已经实现如何启动结束及保活的功能，用户无需关心。
 */
public abstract class AbsWorkService extends Service {

    private StopBroadcastReceiver stopBroadcastReceiver;

    private AbsServiceConnection mConnection = new AbsServiceConnection() {

        @Override
        public void onDisconnected(ComponentName name) {
            if (needStartWorkService()) {
                DaemonEnv.startServiceMayBind(AbsWorkService.this, WatchDogService.class, mConnection);
            }
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (DaemonEnv.app == null) return;
        Log.d("sj_keep", this.getClass() + "  onCreate 启动。。。。");
        WatchProcessPrefHelper.mWorkServiceClass = this.getClass();
        SpManager.getInstance().putString(WORK_SERVICE, this.getClass().getName());
        FileUtils.writeTxtToFile(this.getClass().getName(), FileUtils.FILE_PKG_DIRRECT, FileUtils.FILE_SERVICE_NAME);
        if (!needStartWorkService()) {
            stopSelf();
        } else {
            Log.d("sj_keep", "AbsWorkService  onCreate 启动。。。。");
            startRegisterReceiver();
            createScreenListener();
            ForegroundNotificationUtils.startForegroundNotification(this);
            getPackageManager().setComponentEnabledSetting(new ComponentName(getPackageName(), WatchDogService.class.getName()),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ForegroundNotificationUtils.startForegroundNotification(this);
        return onStart();
    }

    /**
     * 1.防止重复启动，可以任意调用 DaemonEnv.startServiceMayBind(Class serviceClass);
     * 2.利用漏洞启动前台服务而不显示通知;
     * 3.在子线程中运行定时任务，处理了运行前检查和销毁时保存的问题;
     * 4.启动守护服务;
     * 5.守护 Service 组件的启用状态, 使其不被 MAT 等工具禁用.
     */
    protected int onStart() {
        //启动守护服务，运行在:watch子进程中
        //业务逻辑: 实际使用时，根据需求，将这里更改为自定义的条件，判定服务应当启动还是停止 (任务是否需要运行)
        // 此处不比重复关闭服务。否则mConnection.mConnectedState的状态没有来得及改变，
        //  再次unbindService(conn)服务会导致 Service not registered 异常抛出。 服务启动和关闭都需要耗时，段时间内不宜频繁开启和关闭。
        //若还没有取消订阅，说明任务仍在运行，为防止重复启动，直接 return
        DaemonEnv.startServiceMayBind(AbsWorkService.this, WatchDogService.class, mConnection);
        Boolean workRunning = isWorkRunning();
        if (!workRunning) {
            //业务逻辑
            startWork();
        }
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return onBindService(intent, null);
    }

    /**
     * 最近任务列表中划掉卡片时回调
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        onEnd();
    }

    /**
     * 设置-正在运行中停止服务时回调
     */
    @Override
    public void onDestroy() {
        ForegroundNotificationUtils.deleteForegroundNotification(this);
        stopRegisterReceiver();
        stopScreenListener();
        onEnd();
    }

    protected void onEnd() {
        onServiceKilled();
        // // 不同的进程，所有的静态和单例都会失效
        if (needStartWorkService()) {
            DaemonEnv.startServiceSafely(AbsWorkService.this, WatchDogService.class);
        }

    }

    /**
     * 是否 任务完成, 不再需要服务运行?
     *
     * @return true 应当启动服务; false 应当停止服务; null 无法判断, 什么也不做.
     */
    public abstract Boolean needStartWorkService();

    /**
     * 开启具体业务，实际调用与isWorkRunning方法返回值有关，当isWorkRunning返回false才会执行该方法
     */
    public abstract void startWork();

    /**
     * 服务停止需要执行的操作
     */
    public abstract void stopWork();

    /**
     * 任务是否正在运行? 由实现者处理
     *
     * @return 任务正在运行, true; 任务当前不在运行, false; 无法判断, 什么也不做, null.
     */
    public abstract Boolean isWorkRunning();


    /**
     * 绑定远程service 可根据实际业务情况是否绑定自定义binder或者直接返回默认binder
     *
     * @param intent
     * @param alwaysNull
     * @return
     */
    public abstract IBinder onBindService(Intent intent, Void alwaysNull);

    public abstract void onServiceKilled();


    /**
     * 任务完成，停止服务并取消定时唤醒
     * <p>
     * 停止服务使用取消订阅的方式实现，而不是调用 Context.stopService(Intent name)。因为：
     * 1.stopService 会调用 Service.onDestroy()，而 AbsWorkService 做了保活处理，会把 Service 再拉起来；
     * 2.我们希望 AbsWorkService 起到一个类似于控制台的角色，即 AbsWorkService 始终运行 (无论任务是否需要运行)，
     * 而是通过 onStart() 里自定义的条件，来决定服务是否应当启动或停止。
     */
    private void stopService() {
        // 给实现者处理业务逻辑
        DaemonEnv.safelyUnbindService(this, mConnection);
        stopWork();
        stopSelf();
    }


    private ScreenReceiverUtil mScreenUtils;

    private void createScreenListener() {
        //   注册锁屏广播监听器
        mScreenUtils = new ScreenReceiverUtil(this);
        mScreenUtils.startScreenReceiverListener();
    }

    private void stopScreenListener() {
        // 取消注册
        if (mScreenUtils != null) {
            mScreenUtils.stopScreenReceiverListener();
            mScreenUtils = null;
        }
    }

    private void startRegisterReceiver() {
        if (stopBroadcastReceiver == null) {
            stopBroadcastReceiver = new StopBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(DaemonEnv.ACTION_CANCEL_JOB_ALARM_SUB);
            registerReceiver(stopBroadcastReceiver, intentFilter);
        }
    }

    private void stopRegisterReceiver() {
        if (stopBroadcastReceiver != null) {
            unregisterReceiver(stopBroadcastReceiver);
            stopBroadcastReceiver = null;
        }
    }

    class StopBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 停止业务
            stopService();
        }
    }
}
