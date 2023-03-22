package com.smartglassesmanager.androidsmartphone.eventbusmessages;

public class SmartGlassesConnectionEvent {
    public final int connectionStatus;

    public SmartGlassesConnectionEvent(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
}