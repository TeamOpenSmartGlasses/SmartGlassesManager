package com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators;

import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.SmartGlassesConnectionEvent;

import org.greenrobot.eventbus.EventBus;

public abstract class SmartGlassesCommunicator {
    //basic glasses utils/settings
    public int mConnectState = 0;
    protected SmartGlassesModes currentMode;
    public abstract void connectToSmartGlasses();
    public abstract void blankScreen();
    public abstract void destroy();

    //reference card
    public abstract void displayReferenceCardSimple(String title, String body);

    //scrolling text view
    public void startScrollingTextViewMode(String title){
        setMode(SmartGlassesModes.SCROLLING_TEXT_VIEW);
    }
    public abstract void scrollingTextViewIntermediateText(String text);
    public abstract void scrollingTextViewFinalText(String text);
    public abstract void stopScrollingTextViewMode();
    public abstract void showHomeScreen();

    //fonts
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

    protected boolean isConnected(){
        return (mConnectState == 2);
    }

    public void connectionEvent(int connectState){
        mConnectState = connectState;
        EventBus.getDefault().post(new SmartGlassesConnectionEvent(mConnectState));
        showHomeScreen();
    }


    public void setMode(SmartGlassesModes mode){
        currentMode = mode;
    }
}
