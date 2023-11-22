package com.teamopensmartglasses.sgmlib.events;

import com.teamopensmartglasses.sgmlib.SGMCommand;

import java.io.Serializable;

public class RegisterCommandSuccessEvent implements Serializable {
    public SGMCommand command;
    public static final String eventId = "registerCommandSuccessEvent";

    public RegisterCommandSuccessEvent(SGMCommand command){
        this.command = command;
    }

    public static String getEventId(){
        return("registerCommandSuccessEvent");
    }
}
