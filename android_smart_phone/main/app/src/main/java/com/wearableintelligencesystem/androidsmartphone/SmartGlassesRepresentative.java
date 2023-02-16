package com.wearableintelligencesystem.androidsmartphone;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

//custom, our code
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.ReferenceCardSimpleViewRequestEvent;
import com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators.ActiveLookSGC;
import com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators.AndroidSGC;
import com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators.SmartGlassesCommunicator;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.SmartGlassesDevice;

//rxjava
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

class SmartGlassesRepresentative {
    private static final String TAG = "WearableAi_ASGRepresentative";

    //receive/send data stream
    PublishSubject<JSONObject> dataObservable;

    Context context;

    SmartGlassesDevice smartGlassesDevice;
    SmartGlassesCommunicator smartGlassesCommunicator;

    SmartGlassesRepresentative(Context context, SmartGlassesDevice smartGlassesDevice, PublishSubject<JSONObject> dataObservable){
        this.context = context;
        this.smartGlassesDevice = smartGlassesDevice;

        //receive/send data
        this.dataObservable = dataObservable;

        //register event bus subscribers
        EventBus.getDefault().register(this);
    }

    public void connectToSmartGlasses(){
        switch (smartGlassesDevice.getGlassesOs()){
            case ANDROID_OS_GLASSES:
                Log.d(TAG, "MAKING ANDROID SGC");
                smartGlassesCommunicator = new AndroidSGC(context, dataObservable);
                break;
            case ACTIVELOOK_OS_GLASSES:
                smartGlassesCommunicator = new ActiveLookSGC(context);
                break;
        }

        smartGlassesCommunicator.connectToSmartGlasses();
    }

    public void destroy(){
        Log.d(TAG, "SG rep destroying");

        if (smartGlassesCommunicator != null){
            smartGlassesCommunicator.destroy();
            smartGlassesCommunicator = null;
        }

        Log.d(TAG, "SG rep destroy complete");
    }

    //are our smart glasses currently connected?
    public int getConnectionState(){
        if (smartGlassesCommunicator == null){
            return 0;
        } else {
            return smartGlassesCommunicator.getConnectionState();
        }
    }

    public void showReferenceCard(String title, String body){
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.displayReferenceCardSimple(title, body);
        }
    }

    public void startLiveCaptions(){
        //pass for now
        if (smartGlassesCommunicator != null) {
//            smartGlassesCommunicator.startLiveCaptions();
        }
    }

    @Subscribe
    public void onReferenceCardSimpleViewEvent(ReferenceCardSimpleViewRequestEvent receivedEvent){
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.displayReferenceCardSimple(receivedEvent.title, receivedEvent.body);
        }
    }
}
