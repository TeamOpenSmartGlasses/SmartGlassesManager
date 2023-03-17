package com.teamopensmartglasses.sgmlib.events;

import com.teamopensmartglasses.sgmlib.SGMCommand;

import java.io.Serializable;

public class CommandTriggeredEvent implements Serializable {
    public SGMCommand command;
    public String args;
    public long commandTriggeredTime;
    public static final String eventId = "commandTriggeredEvent";

    public CommandTriggeredEvent(SGMCommand command, String args, long commandTriggeredTime){
        this.command = command;
        this.args = args;
        this.commandTriggeredTime = commandTriggeredTime;
    }
}
