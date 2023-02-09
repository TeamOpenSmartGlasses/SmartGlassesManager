package com.wearableintelligencesystem.androidsmartphone;

import android.content.Context;
import org.json.JSONObject;
import android.util.Log;

//custom, our code
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
    Disposable dataSub;

    Context context;

    SmartGlassesDevice smartGlassesDevice;
    SmartGlassesCommunicator smartGlassesCommunicator;

    SmartGlassesRepresentative(Context context, SmartGlassesDevice smartGlassesDevice, PublishSubject<JSONObject> dataObservable){
        this.context = context;
        this.smartGlassesDevice = smartGlassesDevice;

        //receive/send data
        this.dataObservable = dataObservable;
    }

    public void connectToSmartGlasses(){
        switch (smartGlassesDevice.getGlassesOs()){
            case ANDROID_OS_GLASSES:
                smartGlassesCommunicator = new AndroidSGC(context, dataObservable);
            case ACTIVELOOK_OS_GLASSES:
                smartGlassesCommunicator = new ActiveLookSGC(context, dataObservable);
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
}
