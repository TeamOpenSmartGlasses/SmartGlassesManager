package com.smartglassesmanager.androidsmartphone.eventbusmessages;

import java.io.Serializable;

public class ScrollingTextViewStartRequestEvent implements Serializable {
    public String title;
    public static final String eventId = "scrollingTextViewStartEvent";
    public ScrollingTextViewStartRequestEvent(String title){
        this.title = title;
    }
}
