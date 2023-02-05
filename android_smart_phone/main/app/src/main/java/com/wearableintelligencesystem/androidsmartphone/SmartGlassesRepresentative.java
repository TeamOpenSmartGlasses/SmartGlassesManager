//represent the ASG

package com.wearableintelligencesystem.androidsmartphone;

import android.content.Context;


import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramSocket;
import java.util.Random;

import android.os.HandlerThread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.ByteArrayOutputStream;

import android.os.StrictMode;
import android.util.Log;
import android.os.Handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


//custom, our code
import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

import com.wearableintelligencesystem.androidsmartphone.comms.AspWebsocketServer;
import com.wearableintelligencesystem.androidsmartphone.comms.AudioSystem;

import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileRepository;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.SmartGlassesDevice;
import com.wearableintelligencesystem.androidsmartphone.utils.NetworkUtils;

//rxjava
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

class SmartGlassesRepresentative {
    private static final String TAG = "WearableAi_ASGRepresentative";

    private static boolean killme = false;

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
        dataSub = this.dataObservable.subscribe(i -> handleDataStream(i));
    }

    //receive audio and send to vosk
    public void handleDataStream(JSONObject data){
        //first check if it's a type we should handle
        try{
            String type = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (type.equals(MessageTypes.POV_IMAGE)){
                //handleImage(data.getString(MessageTypes.JPG_BYTES_BASE64), data.getLong(MessageTypes.TIMESTAMP));
            } 
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void connectToSmartGlasses(){
        killme = false;

        switch (smartGlassesDevice.getGlassesOs()){
            case ANDROID_OS_GLASSES:
                smartGlassesCommunicator = new AndroidSGC(context, dataObservable);
//            case ACTIVELOOK_OS_GLASSES:
//                smartGlassesCommunicator = new ActiveLookSGC();
        }

        smartGlassesCommunicator.connectToSmartGlasses();
    }

    public void destroy(){
        Log.d(TAG, "ASG rep destroying");
        killme = true;
        Log.d(TAG, "SG rep destroy complete");
    }
    public boolean isConnected(){
//        if (mConnectState != 2){
//            return false;
//        } else {
//            return true;
//        }
        return false;
    }
}
