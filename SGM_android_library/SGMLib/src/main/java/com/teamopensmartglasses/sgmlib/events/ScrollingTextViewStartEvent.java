package com.teamopensmartglasses.sgmlib.events;

import java.io.Serializable;

public class ScrollingTextViewStartEvent implements Serializable {
    public String title;
    public static final String eventId = "scrollingTextViewStartEvent";
    public ScrollingTextViewStartEvent(String title){
        this.title = title;
    }
}
