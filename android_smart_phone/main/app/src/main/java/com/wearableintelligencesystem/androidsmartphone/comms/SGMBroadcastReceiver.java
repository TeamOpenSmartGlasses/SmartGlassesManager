package com.wearableintelligencesystem.androidsmartphone.comms;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import com.wearableintelligencesystem.androidsmartphone.WearableAiAspService;

public class SGMBroadcastReceiver extends BroadcastReceiver {
    private Context context;
    private WearableAiAspService mService;

    public SGMBroadcastReceiver(Context myContext, WearableAiAspService myService){
        this.mService = myService;
        this.context = myContext;
        IntentFilter intentFilter = new IntentFilter("com.teamsmartglasses.from3pa");
        this.context.registerReceiver(this, intentFilter);
    }

    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String data = bundle.getString("data");

        Log.i("Broadcastreceiver", "BroadcastReceiver Received");
        Toast.makeText(context, "Broadcast message is received", Toast.LENGTH_LONG).show();

        try {
            //For now, send generic card
            String testCardImg = "https://ichef.bbci.co.uk/news/976/cpsprodpb/7727/production/_103330503_musk3.jpg";
            String testCardTitle = "3pa title";
            String testCardContent = "Woah dude this is from a 3pa";

            this.mService.sendTestCard(testCardTitle, testCardContent, testCardImg);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}

