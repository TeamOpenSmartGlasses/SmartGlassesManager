package com.wearableintelligencesystem.androidsmartphone.supportedglasses;

public class InmoAirOne extends SmartGlassesDevice {
    public InmoAirOne() {
        deviceModelName = "Inmo Air 1";
        deviceIconName = "inmo_air";
        anySupport = true;
        fullSupport = true;
        glassesOs = SmartGlassesOperatingSystem.ANDROID_OS_GLASSES;
        hasDisplay = true;
        hasSpeakers = true;
        hasCamera = true;
        hasInMic = true;
        hasOutMic = false; //unknown
        weight = 76;
    }
}
