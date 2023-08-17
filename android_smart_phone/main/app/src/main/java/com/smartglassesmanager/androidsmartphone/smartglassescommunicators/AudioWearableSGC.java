package com.smartglassesmanager.androidsmartphone.smartglassescommunicators;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.util.Log;

import com.smartglassesmanager.androidsmartphone.comms.AspWebsocketServer;
import com.smartglassesmanager.androidsmartphone.comms.AudioSystem;
import com.smartglassesmanager.androidsmartphone.comms.MessageTypes;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.AudioChunkNewEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.TextToSpeechEvent;
import com.smartglassesmanager.androidsmartphone.utils.NetworkUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class AudioWearableSGC extends SmartGlassesCommunicator {
    private static final String TAG = "WearableAi_AndroidWearableSGC";

    private static boolean killme;

    Context context;

    public AudioWearableSGC(Context context){
        super();

        //state information
        killme = false;
        mConnectState = 0;
    }

    public void setFontSizes(){
    }

    public void connectToSmartGlasses(){
        connectionEvent(2);
    }

    public void blankScreen(){
    }

    public void destroy(){
        killme = true;
    }

    public void displayReferenceCardSimple(String title, String body){
        Log.d(TAG, "TTS reference card");
        EventBus.getDefault().post(new TextToSpeechEvent(title + ", " + body));
    }

    public void displayReferenceCardImage(String title, String body, String imgUrl){
        Log.d(TAG, "TTS reference card");
        EventBus.getDefault().post(new TextToSpeechEvent(title + ", " + body));
    }

    public void displayBulletList(String title, String [] bullets){
        displayBulletList(title, bullets, 0);
    }

    public void displayBulletList(String title, String [] bullets, int lingerTime){

    }


    public void stopScrollingTextViewMode() {
    }

    public void startScrollingTextViewMode(String title){
    }

    public void scrollingTextViewIntermediateText(String text){
    }

    public void scrollingTextViewFinalText(String text){
    }

    public void showHomeScreen(){
    }

    public void displayPromptView(String prompt, String [] options){
    }

    public void displayTextLine(String text){
        Log.d(TAG, "displayTextLine: " + text);
        EventBus.getDefault().post(new TextToSpeechEvent(text));
    }

    public void displayCenteredText(String text){
        //TODO: Find a way to add (optional) pauses between lines?
        displayTextLine(text);
    }


    public void showNaturalLanguageCommandScreen(String prompt, String naturalLanguageArgs){
    }

    public void updateNaturalLanguageCommandScreen(String naturalLanguageArgs){
    }
}