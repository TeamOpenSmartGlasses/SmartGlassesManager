package com.smartglassesmanager.androidsmartphone.smartglassescommunicators;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.activelook.activelooksdk.DiscoveredGlasses;
import com.activelook.activelooksdk.Glasses;
import com.smartglassesmanager.androidsmartphone.comms.MessageTypes;

import java.util.ArrayList;
import java.util.Hashtable;

public class MonocleSGC extends SmartGlassesCommunicator {
    private static final String TAG = "WearableAi_ActivelookSGC";

    private Glasses connectedGlasses;
    private Sdk alsdk;
    private ArrayAdapter<DiscoveredGlasses> scanResults;
    private ArrayList<DiscoveredGlasses> discoveredGlasses;

    private Hashtable<Integer, Integer> fontToSize;
    private Hashtable<Integer, Float> fontToRatio;

    public MonocleSGC(Context context) {
        super();
        // Hold all glasses we find
        discoveredGlasses = new ArrayList<>();

        alsdk = Sdk.init(context,
                "",
                this::onUpdateStart,
                this::onUpdateAvailableCallback,
                this::onUpdateProgress,
                this::onUpdateSuccess,
                this::onUpdateError
        );
    }

    // ... Other methods ...

    public void displayReferenceCardSimple(String title, String body){
        Log.d(TAG, "Checking if connected... Displaying simple reference card");
        if (isConnected()) {
            Log.d(TAG, "Displaying simple reference card");
            connectedGlasses.clear();
            ArrayList<Object> stuffToDisplay = new ArrayList<>();
            stuffToDisplay.add(new TextLineSG(title, LARGE_FONT));
            stuffToDisplay.add(new TextLineSG(body, SMALL_FONT));
            displayLinearStuff(stuffToDisplay);
        }
    }

    // ... Other methods ...
}
