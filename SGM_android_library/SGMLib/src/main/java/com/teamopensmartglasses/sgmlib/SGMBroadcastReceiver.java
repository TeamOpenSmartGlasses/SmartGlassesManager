package com.teamopensmartglasses.sgmlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import org.greenrobot.eventbus.EventBus;

public class SGMBroadcastReceiver extends BroadcastReceiver {
    private String filterPkg;
    private Context context;
    public String TAG = "HELLO123";

    public SGMBroadcastReceiver(Context myContext) {
        this.context = myContext;
        Log.d(TAG, "log: ctxpkgn: " + this.context.getPackageName() + " sgmdat: " + SGMData.SGMPkgName);
        this.filterPkg = this.context.getPackageName().contains(SGMData.SGMPkgName) ? "com.teamopensmartglasses.from3pa" : "com.teamopensmartglasses.to3pa";
        Log.d(TAG, "PKGFILTER: " + this.filterPkg);
        IntentFilter intentFilter = new IntentFilter(this.filterPkg);
        this.context.registerReceiver(this, intentFilter);
        Log.d(TAG, "PKGFILTER: " + this.filterPkg);
    }

    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String data = bundle.getString("data");

        Log.i("Broadcastreceiver", "BroadcastReceiver Received");

        EventBus.getDefault().post(new ReceivedIntentEvent(data));
    }
}
