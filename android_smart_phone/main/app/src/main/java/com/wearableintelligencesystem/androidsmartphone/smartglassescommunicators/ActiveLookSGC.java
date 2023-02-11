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
import java.util.Hashtable;

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

    //map font size to actual size in pixels - specific to Activelook because we have to handle text wrapping manually
    private Hashtable<Integer, Integer> fontToSize;
    private Hashtable<Integer, Float> fontToRatio;

    public ActiveLookSGC(Context context, PublishSubject< JSONObject > dataObservable) {
        super();
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
    protected void setFontSizes(){
        int EXTRA_LARGE_FONT = 3;
        LARGE_FONT = 2;
        MEDIUM_FONT = 1;
        SMALL_FONT = 0;

        fontToSize = new Hashtable<>();
        fontToSize.put(LARGE_FONT, 35);
        fontToSize.put(MEDIUM_FONT, 24);
        fontToSize.put(SMALL_FONT, 24);
        fontToSize.put(EXTRA_LARGE_FONT, 49);

        fontToRatio = new Hashtable<>();
        fontToRatio.put(LARGE_FONT, 2.0f);
        fontToRatio.put(MEDIUM_FONT, 2.1f);
        fontToRatio.put(SMALL_FONT, 2.1f);
        fontToRatio.put(EXTRA_LARGE_FONT, 3.0f);
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

    public void displayReferenceCardSimple(String title, String body){
        ArrayList<Object> stuffToDisplay = new ArrayList<>();
        stuffToDisplay.add(new TextLineSG(title, LARGE_FONT));
        stuffToDisplay.add(new TextLineSG(body, SMALL_FONT));
        displayLinearStuff(stuffToDisplay);
    }

    //takes a list of strings and images and displays them in a line from top of the screen to the bottom
    private void displayLinearStuff(ArrayList<Object> stuffToDisplay){
        if (!isConnected()){
            return;
        }

        //make sure our glasses are powered on and cleared
        connectedGlasses.power(true);
        connectedGlasses.clear();

        //loop through the stuff to display, and display it, moving the next item below the previous item in each loop
        Point currPercentPoint = new Point(0,0);
        int yMargin = 5;
        for (Object thing : stuffToDisplay){
            Point endPercentPoint;
            if (thing instanceof TextLineSG) { //could this be done cleaner with polymorphism? https://stackoverflow.com/questions/5579309/is-it-possible-to-use-the-instanceof-operator-in-a-switch-statement
                endPercentPoint = displayText((TextLineSG) thing, currPercentPoint);
//            } else if (thing instanceof ImageSG) {
                //endPoint = displaySomething((ImageSG) thing, currPoint);
            } else { //unsupported type
                continue;
            }
            currPercentPoint = new Point(0, endPercentPoint.y + yMargin); //move the next thing to display down below the previous thing
        }
    }

    private Point percentScreenToPixels(int xLocVw, int yLocVh){
        Log.d(TAG, "percentScreenToPixels x: " + xLocVw);
        Log.d(TAG, "percentScreenToPixels y: " + yLocVh);
        int xCoord = displayWidthPixels - Math.round((((float) xLocVw) / 100) * displayWidthPixels);
        int yCoord = displayHeightPixels - Math.round((((float) yLocVh) / 100) * displayHeightPixels);
        return new Point(xCoord, yCoord);
    }

    private Point pixelsToPercentScreen(Point point){
        Log.d(TAG, "pixels to percent screen point INPUT: " + point);
        int xPercent = (int)(((float)point.x / displayWidthPixels) * 100);
        int yPercent = (int)(((float)point.y / displayHeightPixels) * 100);
        Point percentPoint = new Point(xPercent, yPercent);
        Log.d(TAG, "pixels to percent screen point RETURN : " + percentPoint);
        return percentPoint;
    }

    //handles text wrapping, returns how many lines were wrapped
    private Point displayText(TextLineSG textLine, Point percentLoc){
        if (!isConnected()){
            return null;
        }

        //compute information about how to wrap this line
        int fontHeight = fontToSize.get(textLine.getFontSize());
        float fontAspectRatio = fontToRatio.get(textLine.getFontSize()); //this many times as taller than, found by trial and error
        int fontWidth = (int) (fontHeight / fontAspectRatio);
        int textWidth = (int) (textLine.getText().length() * fontWidth); //width in pixels
        int numWraps = ((int) Math.floor(((float) textWidth) / displayWidthPixels));
        int wrapLen = ((int) Math.floor((displayWidthPixels / (float) fontWidth)));

        //loop through the text, writing out individual lines to the glasses
        ArrayList<String> chunkedText = new ArrayList<>();
        Point textPoint = percentLoc;
        int textMarginY = pixelsToPercentScreen(new Point((int)(fontHeight * 1.3), 0)).x;
        for (int i = 0; i <= numWraps; i++){
            int startIdx = wrapLen * i;
            int endIdx = Math.min(startIdx + wrapLen, textLine.getText().length());
            String subText = textLine.getText().substring(startIdx, endIdx).trim();
            chunkedText.add(subText);
            Log.d(TAG, "display text as percent pos: " + textPoint);
//            connectedGlasses.txt(textPoint, Rotation.TOP_LR, (byte) textLine.getFontSize(), (byte) 0x0F, subText);
            sendTextToGlasses(new TextLineSG(subText, textLine.getFontSize()), textPoint);
            textPoint = new Point(textPoint.x, textPoint.y + textMarginY); //lower our text for the next loop
        }

        return textPoint;
    }

    private void sendTextToGlasses(TextLineSG textLine, Point percentLoc){
        Log.d(TAG, "Sending text to glasses: " + textLine.getText());
        Log.d(TAG, "At pos: " + percentLoc);
        Point pixelLoc = percentScreenToPixels(percentLoc.x, percentLoc.y);
        connectedGlasses.txt(pixelLoc, Rotation.TOP_LR, (byte) textLine.getFontSize(), (byte) 0x0F, textLine.getText());
    }
}
