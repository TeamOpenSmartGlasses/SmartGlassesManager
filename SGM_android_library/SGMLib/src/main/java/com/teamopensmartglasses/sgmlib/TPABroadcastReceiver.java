package com.teamopensmartglasses.sgmlib;

import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;
import java.util.UUID;

public class TPABroadcastReceiver extends BroadcastReceiver {
    private String filterPkg;
    private Context context;
    public String TAG = "SGMLib_TPABroadcastReceiver";

    public TPABroadcastReceiver(Context myContext) {
        this.context = myContext;
        this.filterPkg = "com.teamopensmartglasses.from3pa";
        IntentFilter intentFilter = new IntentFilter(this.filterPkg);
        this.context.registerReceiver(this, intentFilter);
    }

    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received broadcast");
    }
}
