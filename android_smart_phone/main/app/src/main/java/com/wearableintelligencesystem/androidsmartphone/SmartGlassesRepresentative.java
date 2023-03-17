package com.wearableintelligencesystem.androidsmartphone;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

//custom, our code
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.AudioChunkNewEvent;
import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextEvent;
import com.teamopensmartglasses.sgmlib.events.IntermediateScrollingTextEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.PromptViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStopEvent;
import com.wearableintelligencesystem.androidsmartphone.sensors.AudioChunkCallback;
import com.wearableintelligencesystem.androidsmartphone.sensors.MicrophoneLocalAndBluetooth;
import com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators.ActiveLookSGC;
import com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators.AndroidSGC;
import com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators.SmartGlassesCommunicator;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.SmartGlassesDevice;

//rxjava
import java.nio.ByteBuffer;

import io.reactivex.rxjava3.subjects.PublishSubject;

class SmartGlassesRepresentative {
    private static final String TAG = "WearableAi_ASGRepresentative";

    //receive/send data stream
    PublishSubject<JSONObject> dataObservable;

    Context context;

    SmartGlassesDevice smartGlassesDevice;
    SmartGlassesCommunicator smartGlassesCommunicator;
    MicrophoneLocalAndBluetooth bluetoothAudio;

    //timing settings
    long referenceCardDelayTime = 10000;

    //handler to handle delayed UI events
    Handler uiHandler;

    SmartGlassesRepresentative(Context context, SmartGlassesDevice smartGlassesDevice, PublishSubject<JSONObject> dataObservable){
        this.context = context;
        this.smartGlassesDevice = smartGlassesDevice;

        //receive/send data
        this.dataObservable = dataObservable;

        uiHandler = new Handler();

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

        //if the glasses don't support a microphone, this Representative handles local microphone
        if (!smartGlassesDevice.getHasInMic() && !smartGlassesDevice.getHasOutMic()) {
            connectAndStreamLocalMicrophone();
        }
    }

    private void connectAndStreamLocalMicrophone(){
        //follow this order for speed
        //start audio from bluetooth headset
        bluetoothAudio = new MicrophoneLocalAndBluetooth(context, new AudioChunkCallback(){
            @Override
            public void onSuccess(ByteBuffer chunk){
                receiveChunk(chunk);
            }
        });
    }

    private void receiveChunk(ByteBuffer chunk){
        byte[] audio_bytes = chunk.array();
        //throw off new audio chunk event
        EventBus.getDefault().post(new AudioChunkNewEvent(audio_bytes));
    }

    public void destroy(){
        Log.d(TAG, "SG rep destroying");

        EventBus.getDefault().unregister(this);

        if (bluetoothAudio != null) {
            bluetoothAudio.destroy();
        }

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

    public void startScrollingTextViewModeTest(){
        //pass for now
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.startScrollingTextViewMode("ScrollingTextView");
            smartGlassesCommunicator.scrollingTextViewFinalText("test line 1");
            smartGlassesCommunicator.scrollingTextViewFinalText("line 2 testy boi");
            smartGlassesCommunicator.scrollingTextViewFinalText("how's this?");
            smartGlassesCommunicator.scrollingTextViewFinalText("this is a line of text that is going to be long enough to wrap around, it would be good to see if it doesn so, that would be super cool");
            smartGlassesCommunicator.scrollingTextViewFinalText("test line n");
            smartGlassesCommunicator.scrollingTextViewFinalText("line n + 1 testy boi");
            smartGlassesCommunicator.scrollingTextViewFinalText("seconnndd how's this?");
        }
    }

    private void homeUiAfterDelay(long delayTime){
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                homeScreen();
            }
        }, delayTime);
    }

    public void homeScreen(){
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.showHomeScreen();
        }
    }

    @Subscribe
    public void onReferenceCardSimpleViewEvent(ReferenceCardSimpleViewRequestEvent receivedEvent){
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.displayReferenceCardSimple(receivedEvent.title, receivedEvent.body);
//            homeUiAfterDelay(referenceCardDelayTime);
        }
    }

    @Subscribe
    public void onStartScrollingTextViewEvent(ScrollingTextViewStartEvent receivedEvent){
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.startScrollingTextViewMode(receivedEvent.title);
        }
    }

    @Subscribe
    public void onStopScrollingTextViewEvent(ScrollingTextViewStopEvent receivedEvent){
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.stopScrollingTextViewMode();
        }
    }

    @Subscribe
    public void onFinalScrollingTextEvent(FinalScrollingTextEvent receivedEvent) {
        Log.d(TAG, "onFinalScrollingTextEvent");
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.scrollingTextViewFinalText(receivedEvent.text);
        }
    }

    @Subscribe
    public void onIntermediateScrollingTextEvent(IntermediateScrollingTextEvent receivedEvent) {
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.scrollingTextViewIntermediateText(receivedEvent.text);
        }
    }

    @Subscribe
    public void onPromptViewRequestEvent(PromptViewRequestEvent receivedEvent) {
        Log.d(TAG, "onPromptViewRequestEvent called");
        if (smartGlassesCommunicator != null) {
            smartGlassesCommunicator.displayPromptView(receivedEvent.prompt, receivedEvent.options);
        }
    }
}
