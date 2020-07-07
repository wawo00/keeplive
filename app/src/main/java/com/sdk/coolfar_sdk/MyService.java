package com.sdk.coolfar_sdk;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.sdk.keepbackground.work.AbsWorkService;
import com.sdk.keepbackground.work.DaemonEnv;

import static java.lang.Thread.sleep;

public class MyService extends AbsWorkService {
    private static String TAG = "Roy_KeepLive";
    private InterstitialAd mInterstitialAd;
    private RewardedAd rewardedAd;

    public MyService() {
        MobileAds.initialize(this);
        // Use an activity context to get the rewarded video instance.
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
//        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("BBE2D353B5B28E1E4E0DC5B5FB6B2493")).build());
    }

    private boolean mIsRunning;

    @Override
    public Boolean needStartWorkService() {
        return true;
    }

    /**
     * 开启具体业务，实际执行与isWorkRunning方法返回值有关，
     * 当isWorkRunning返回false才会执行该方法
     */
    @Override
    public void startWork() {
        Log.i(TAG, "startWork -- ");
        doWork();
    }

    private void doWork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // do something
//                SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");
//                String retStrFormatNowDate = sdFormatter.format(System.currentTimeMillis());
                for (int i = 0; i < 2000; i++) {
                    try {
                        Log.i(TAG, "run ----: " + i);
//                        loadRewardedVideoAd();
//                        showVideoAd();
                        loadInterAds();
                        showInter();
                        sleep(1000 * 15);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "run has error" + e.getMessage());
                    }
                }
                mIsRunning = true;
            }
        }).start();
    }

    /**
     * 业务执行完成需要进行的操作
     * 手动停止服务sendStopWorkBroadcast时回调
     */
    @Override
    public void stopWork() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        rewardedAd = new RewardedAd(this,
                "ca-app-pub-3940256099942544/5224354917");
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 任务是否正在运行? 由实现者处理
     *
     * @return 任务正在运行, true; 任务当前不在运行, false; 无法判断, 什么也不做, null.
     */
    @Override
    public Boolean isWorkRunning() {
        return mIsRunning;
    }

    /**
     * 绑定远程service 可根据实际业务情况是否绑定自定义binder或者直接返回默认binder
     */
    @Override
    public IBinder onBindService(Intent intent, Void aVoid) {
        return new Messenger(new Handler()).getBinder();
    }

    /**
     * 服务被kill
     */
    @Override
    public void onServiceKilled() {

    }

    private void loadRewardedVideoAd() {
        DaemonEnv.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rewardedAd.loadAd(new AdRequest.Builder().build(), new MyRWLoadCallBack());
            }
        });

    }

    private void showVideoAd() {
        DaemonEnv.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showBlankActivity();
                if (rewardedAd.isLoaded()) {
                    RewardedAdCallback adCallback = new RewardedAdCallback() {
                        @Override
                        public void onRewardedAdOpened() {
                            // Ad opened.
                        }

                        @Override
                        public void onRewardedAdClosed() {
                            // Ad closed.
                            loadRewardedVideoAd();
                        }

                        @Override
                        public void onUserEarnedReward(@NonNull com.google.android.gms.ads.rewarded.RewardItem rewardItem) {

                        }

                        @Override
                        public void onRewardedAdFailedToShow(int errorCode) {
                            // Ad failed to display.
                            Log.i(TAG, "onRewardedAdFailedToShow: " + errorCode);
                        }
                    };
                    rewardedAd.show(DaemonEnv.getActivity(), adCallback);
                } else {
                    Log.i(TAG, "not ready");
                }
            }
        });
    }

    private void loadInterAds() {
        DaemonEnv.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        // Load the next interstitial.
                        mInterstitialAd.loadAd(new AdRequest.Builder().build());

                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        Log.i(TAG, "onAdLoaded: ");
                    }

                    @Override
                    public void onAdFailedToLoad(int i) {
                        super.onAdFailedToLoad(i);
                        Log.i(TAG, "onAdFailedToLoad: " + i);
                    }
                });
            }
        });

    }

    public void showInter() {
        DaemonEnv.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                DaemonEnv.getActivity().startActivity(new Intent(DaemonEnv.getActivity(), BlankActivity.class));
                Log.i(TAG, " will showInter: ");
                //android 10上需要修改
                showBlankActivity();
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
            }
        });
    }

    private void showBlankActivity() {
        Intent intent = new Intent(DaemonEnv.getActivity(), BlankActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        DaemonEnv.getActivity().startActivity(intent);
    }

    class MyRWLoadCallBack extends RewardedAdLoadCallback {
        @Override
        public void onRewardedAdLoaded() {
            Log.i(TAG, "onRewardedAdLoaded: ");
        }

        @Override
        public void onRewardedAdFailedToLoad(int errorCode) {
            Log.i(TAG, "onRewardedAdFailedToLoad: " + errorCode);
            rewardedAd.loadAd(new AdRequest.Builder().build(),new MyRWLoadCallBack());
        }
    }
}
