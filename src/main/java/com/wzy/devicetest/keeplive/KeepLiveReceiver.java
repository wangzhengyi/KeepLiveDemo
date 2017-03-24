package com.wzy.devicetest.keeplive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wzy.devicetest.keeplive.KeepLiveManager;

public class KeepLiveReceiver extends BroadcastReceiver {
    public static final String TAG = KeepLiveReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "onReceive: KeepLiveReceiver receive action =" + action);

        if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_USER_BACKGROUND.equals(action)) {
            KeepLiveManager.getInstance().startKeepLiveActivity(context);
        } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_BACKGROUND.equals(action)) {
            KeepLiveManager.getInstance().finishKeepLiveActivity();
        }
    }
}
