package com.teamopensmartglasses.smartglassesmanager.eventbusmessages;

import java.io.Serializable;

public class RowsCardViewRequestEvent implements Serializable {
    public String[] rowStrings;
    public static final String eventId = "rowStringsViewRequestEvent";

    public RowsCardViewRequestEvent(String[] rowStrings) {
        this.rowStrings = rowStrings;
    }
}
