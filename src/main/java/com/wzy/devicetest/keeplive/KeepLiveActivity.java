package com.wzy.devicetest.keeplive;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

public class KeepLiveActivity extends Activity {
    public static final String TAG = KeepLiveActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: KeepLiveActivity->onCreate");
        KeepLiveManager.getInstance().initKeepLiveActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: KeepLiveActivity->onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: KeepLiveActivity->onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: KeepLiveActivity->onDestroy");
    }
}
