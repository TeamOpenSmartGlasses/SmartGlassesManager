package com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators;

import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.SmartGlassesConnectionEvent;

import org.greenrobot.eventbus.EventBus;

public abstract class SmartGlassesCommunicator {
    public int mConnectState = 0;

    public abstract void connectToSmartGlasses();
    public abstract void destroy();
    public abstract void displayReferenceCardSimple(String title, String body);

    public int LARGE_FONT;
    public int MEDIUM_FONT;
    public int SMALL_FONT;

    public SmartGlassesCommunicator(){
        setFontSizes();
    }

    //must be run and set font sizes
    protected abstract void setFontSizes();

    public int getConnectionState(){
        return mConnectState;
    }

    public void connectionEvent(int connectState){
        mConnectState = connectState;
        EventBus.getDefault().post(new SmartGlassesConnectionEvent(mConnectState));
    }
}
