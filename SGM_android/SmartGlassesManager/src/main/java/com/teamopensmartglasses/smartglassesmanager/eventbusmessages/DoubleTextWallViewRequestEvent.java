package com.teamopensmartglasses.smartglassesmanager.eventbusmessages;

public class DoubleTextWallViewRequestEvent {
    public String textTop;
    public String textBottom;

    public DoubleTextWallViewRequestEvent(String textTop, String textBottom) {
        this.textTop = textTop;
        this.textBottom = textBottom;
    }
}
