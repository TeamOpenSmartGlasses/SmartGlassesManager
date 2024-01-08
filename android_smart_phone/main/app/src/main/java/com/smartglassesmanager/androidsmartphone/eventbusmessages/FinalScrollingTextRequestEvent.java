package com.smartglassesmanager.androidsmartphone.eventbusmessages;

import java.io.Serializable;

public class FinalScrollingTextRequestEvent implements Serializable {
    public String text;
    public static final String eventId = "finalScrollingTextEvent";

    public FinalScrollingTextRequestEvent(String text){
        this.text = text;
    }
}
