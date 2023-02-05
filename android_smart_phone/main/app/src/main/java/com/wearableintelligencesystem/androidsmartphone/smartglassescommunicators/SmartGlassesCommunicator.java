package com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators;

import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.SmartGlassesConnectionEvent;

import org.greenrobot.eventbus.EventBus;

public abstract class SmartGlassesCommunicator {
    public abstract void connectToSmartGlasses();
    public abstract void destroy();
    public abstract int getConnectionState();

    public void sendConnectionEvent(int connectionState){
        EventBus.getDefault().post(new SmartGlassesConnectionEvent(connectionState));
    }
}
