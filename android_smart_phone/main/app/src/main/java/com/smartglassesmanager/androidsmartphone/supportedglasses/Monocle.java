package com.smartglassesmanager.androidsmartphone.supportedglasses;

public class VuzixUltralite extends SmartGlassesDevice {
    public VuzixUltralite() {
        deviceModelName = "Monocle";
        deviceIconName = "monocle";
        anySupport = true;
        fullSupport = true;
        glassesOs = SmartGlassesOperatingSystem.MONOCLE_OS_GLASSES;
        hasDisplay = true;
        hasSpeakers = false;
        hasCamera = true;
        hasInMic = true;
        hasOutMic = false;
        useScoMic = true;
        weight = 38;
    }
}
