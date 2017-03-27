package com.wzy.devicetest.keeplive;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class KeepLiveService extends Service {
    private static final String TAG = KeepLiveService.class.getSimpleName();
    static KeepLiveService sKeepLiveService;
    public KeepLiveService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sKeepLiveService = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 模拟耗时操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Log.d(TAG, "run: do somethind waste time !!!");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        startService(new Intent(this, InnerService.class));
        return Service.START_STICKY;
    }

    public static class InnerService extends Service {

        @Override
        public int onStartCommand(Intent intent,int flags, int startId) {
            KeepLiveManager.getInstance().setForegroundService(sKeepLiveService, this);
            return super.onStartCommand(intent, flags, startId);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
