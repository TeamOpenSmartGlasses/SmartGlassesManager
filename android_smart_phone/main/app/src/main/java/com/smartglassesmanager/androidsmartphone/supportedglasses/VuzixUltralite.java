package com.smartglassesmanager.androidsmartphone.supportedglasses;

public class VuzixUltralite extends SmartGlassesDevice {
    public VuzixUltralite() {
        deviceModelName = "Vuzix Ultralite";
        deviceIconName = "vuzix_ultralite";
        anySupport = false;
        fullSupport = false;
        glassesOs = SmartGlassesOperatingSystem.ULTRALITE_MCU_OS_GLASSES;
        hasDisplay = true;
        hasSpeakers = false;
        hasCamera = false;
        hasInMic = false;
        hasOutMic = false;
        weight = 38;
    }
}
