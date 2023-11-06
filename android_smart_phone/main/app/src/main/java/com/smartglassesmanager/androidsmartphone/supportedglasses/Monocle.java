package com.smartglassesmanager.androidsmartphone.supportedglasses;

// these glasses are pretty bad. There's no home button/gesture... the text/font size is all off android specs. Arms are big. The company has zero customer support. We might drop support for these - cayden
public class Monocle extends SmartGlassesDevice {
    public Monocle() {
        deviceModelName = "Monocle;
        deviceIconName = "Monocle";
        anySupport = true;
        fullSupport = false;
        glassesOs = SmartGlassesOperatingSystem.ANDROID_OS_GLASSES;
        hasDisplay = true;
        hasSpeakers = false;
        hasCamera = true;
        hasInMic = true;
        hasOutMic = false; //unknown
        weight = 76;
    }
}
