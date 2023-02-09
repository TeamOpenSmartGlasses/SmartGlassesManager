package com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.activelook.activelooksdk.DiscoveredGlasses;
import com.activelook.activelooksdk.Glasses;
import com.activelook.activelooksdk.Sdk;
import com.activelook.activelooksdk.types.GlassesUpdate;
import com.wearableintelligencesystem.androidsmartphone.R;
import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.SmartGlassesConnectionEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.ArrayList;

import io.reactivex.rxjava3.subjects.PublishSubject;


//communicate with ActiveLook smart glasses
public class ActiveLookSGC extends SmartGlassesCommunicator {
    private static final String TAG = "WearableAi_ActivelookSGC";

    private Glasses connectedGlasses;
    private Sdk alsdk;
    private ArrayAdapter<DiscoveredGlasses> scanResults;
    private ArrayList<DiscoveredGlasses> discoveredGlasses;

    public ActiveLookSGC(Context context, PublishSubject< JSONObject > dataObservable) {
        //hold all glasses we find
        discoveredGlasses = new ArrayList<>();

        //state information
        mConnectState = 0;

        this.alsdk = Sdk.init(context,
                "",
                this::onUpdateStart,
                this::onUpdateAvailableCallback,
                this::onUpdateProgress,
                this::onUpdateSuccess,
                this::onUpdateError
        );
    }

    @Override
    public void connectToSmartGlasses(){
        this.alsdk.startScan(activeLookDiscoveredGlassesI -> {
            discoveredGlasses.add(activeLookDiscoveredGlassesI);
            tryConnectNewGlasses(activeLookDiscoveredGlassesI);
        });
    }

    private void tryConnectNewGlasses(DiscoveredGlasses device){
        connectionEvent(MessageTypes.TRYNA_CONNECT_GLASSES);
        device.connect(glasses -> {
                    Log.d(TAG, "Activelook connected.");
                    mConnectState = 2;
                    connectionEvent(MessageTypes.CONNECTED_GLASSES);
                },
                discoveredGlasses -> {
                    Log.d(TAG, "Activelook connection failed.");
                    connectionEvent(MessageTypes.TRYNA_CONNECT_GLASSES);
                },
                glasses -> {
                    Log.d(TAG, "Activelook disconnected.");
                    connectionEvent(MessageTypes.DISCONNECTED_GLASSES);
                });
    }

    @Override
    public void destroy(){
    }

    private void onUpdateStart(final GlassesUpdate glassesUpdate) {
        Log.d(TAG, "onUpdateStart");
    }

    private void onUpdateAvailableCallback(final android.util.Pair<GlassesUpdate, Runnable> glassesUpdateAndRunnable) {
        Log.d(TAG, "onUpdateAvailableCallback");
        Log.d("GLASSES_UPDATE", String.format("onUpdateAvailableCallback   : %s", glassesUpdateAndRunnable.first));
        glassesUpdateAndRunnable.second.run();
    }
    private void onUpdateProgress(final GlassesUpdate glassesUpdate) {
        Log.d(TAG, "onUpdateProgress");
    }
    private void onUpdateSuccess(final GlassesUpdate glassesUpdate) {
        Log.d(TAG, "onUpdateSuccess");
    }
    private void onUpdateError(final GlassesUpdate glassesUpdate) {
        Log.d(TAG, "onUpdateError");
    }
}
