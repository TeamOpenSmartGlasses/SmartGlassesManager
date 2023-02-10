package com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
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
import com.activelook.activelooksdk.types.Rotation;
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
    private int displayWidthPixels = 304;
    private int displayHeightPixels = 256;
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
                    connectedGlasses = glasses;
                    connectionEvent(MessageTypes.CONNECTED_GLASSES);
                    // Power on the glasses
                    connectedGlasses.power(true);
                    // Clear the glasses display
                    connectedGlasses.clear();
                    //clear any fonts to use just default fonts
                    connectedGlasses.fontDeleteAll();
                    // Set the display luminance level
                    glasses.luma((byte) 7);
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
       if (connectedGlasses != null){
           connectedGlasses.disconnect();
       }
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

    private boolean isConnected(){
        return (mConnectState == 2);
    }
    //HCI
    public void displayText(String text, int fontSize, int xLocVw, int yLocVh){
        /**
         * Write text string at coordinates (x, y) with rotation, font size, and color.
         *
         * @param x The x coordinate for the text.
         * @param y The y coordinate for the text.
         * @param r The rotation of the text.
         * @param f The font size of the text.
         * @param c The color of the text.
         * @param s The text.
         */
        if (isConnected()) {
            int xCoord = displayWidthPixels - Math.round((((float) xLocVw) / 100) * displayWidthPixels);
            int yCoord = displayHeightPixels - Math.round((((float) yLocVh) / 100) * displayHeightPixels);
            connectedGlasses.power(true);
            // we flip coords because Activelook (0,0) is bottom right, whereas computer graphics is always top left
            textWrapping(xCoord, yCoord, fontSize, text);
        }
    }

    //handles text wrapping, returns how many lines were wrapped
    public int textWrapping(int xCoord, int yCoord, int fontSize, String text){
        int fontHeight = 24;
        double fontAspectRatio = 2.5; //this many times as taller than, found by trial and error
        int fontWidth = (int) (fontHeight / fontAspectRatio);
        int textWidth = (int) (text.length() * fontWidth); //width in pixels
        ArrayList<String> chunkedText = new ArrayList<>();

        Log.d(TAG, "Text width is: " + textWidth);
        int numWraps = ((int) Math.floor(((float) textWidth) / displayWidthPixels));
        int wrapLen = ((int) Math.floor((displayWidthPixels / (float) fontWidth)));
        Log.d(TAG, "Num wraps is: " + numWraps);
        Log.d(TAG, "Wrap len is: " + wrapLen);
        for (int i = 0; i <= numWraps; i++){
            int startIdx = wrapLen * i;
            int endIdx = Math.min(startIdx + wrapLen, text.length());
            String subText = text.substring(startIdx, endIdx);
            chunkedText.add(subText);
            Log.d(TAG, "Check it out: " + text.substring(startIdx, endIdx));
            connectedGlasses.txt(new Point(xCoord, yCoord - (int)((i * fontHeight) * 1.1)), Rotation.TOP_LR, (byte) 0, (byte) 0x0F, subText);
        }

        return numWraps;
    }
}
