/*
 * Copyright (c) 2014 Twitter Inc.
 */
package net.callmeike.android.buildid;

import android.app.Application;


/**
 * Sample use of AppSignatureTask
 * Launch the task ASAP after launch
 */
public class MainApplication extends Application implements AppSignatureTask.Callback {
    private String appSignature = "unknown";

    @Override
    public void onCreate() {
        super.onCreate();

        new AppSignatureTask(this, this).execute(MainActivity.class);
    }

    @Override
    public void onSignature(String digest) { appSignature = digest; }

    public String getAppSignature() { return appSignature; }
}
