package com.teamopensmartglasses.sgmlib.events;

import com.teamopensmartglasses.sgmlib.SGMCommand;

import java.io.Serializable;

public class CommandTriggeredEvent implements Serializable {
    public SGMCommand command;
    public static final String eventId = "commandTriggeredEvent";

    public CommandTriggeredEvent(SGMCommand command){
        this.command = command;
    }
}
