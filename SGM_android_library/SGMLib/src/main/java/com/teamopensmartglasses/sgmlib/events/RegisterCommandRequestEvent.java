package com.teamopensmartglasses.sgmlib.events;

import com.teamopensmartglasses.sgmlib.SGMCommand;

import java.io.Serializable;

public class RegisterCommandRequestEvent implements Serializable {
    public SGMCommand command;
    public static final String eventId = "registerCommandRequestEvent";

    public RegisterCommandRequestEvent(SGMCommand command){
        this.command = command;
    }

    public static String getEventId(){
        return("registerCommandRequestEvent");
    }
}
