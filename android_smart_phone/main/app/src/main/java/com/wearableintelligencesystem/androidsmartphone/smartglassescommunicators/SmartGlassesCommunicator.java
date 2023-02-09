package com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators;

import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.SmartGlassesConnectionEvent;

import org.greenrobot.eventbus.EventBus;

public abstract class SmartGlassesCommunicator {
    public int mConnectState = 0;

    public abstract void connectToSmartGlasses();
    public abstract void destroy();

    public int getConnectionState(){
        return mConnectState;
    }

    public void connectionEvent(int connectState){
        mConnectState = connectState;
        EventBus.getDefault().post(new SmartGlassesConnectionEvent(mConnectState));
    }
}
