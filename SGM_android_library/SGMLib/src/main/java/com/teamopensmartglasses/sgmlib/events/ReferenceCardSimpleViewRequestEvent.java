package com.teamopensmartglasses.sgmlib.events;

import java.io.Serializable;

public class ReferenceCardSimpleViewRequestEvent implements Serializable {
    public String title;
    public String body;

    public ReferenceCardSimpleViewRequestEvent(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public static String getEventId(){
        return("referenceCardSimpleViewRequestEvent");
    }
}
