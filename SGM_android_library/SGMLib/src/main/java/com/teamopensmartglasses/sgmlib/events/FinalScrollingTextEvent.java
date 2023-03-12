package com.teamopensmartglasses.sgmlib.events;

import java.io.Serializable;

public class FinalScrollingTextEvent implements Serializable {
    public String text;
    public static final String eventId = "finalScrollingTextEvent";

    public FinalScrollingTextEvent(String text){
        this.text = text;
    }
}
