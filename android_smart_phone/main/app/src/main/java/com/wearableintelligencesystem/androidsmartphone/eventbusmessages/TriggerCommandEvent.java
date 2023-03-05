package com.wearableintelligencesystem.androidsmartphone.eventbusmessages;
import java.util.UUID;

public class TriggerCommandEvent {
    public UUID commandId;

    public TriggerCommandEvent(UUID commandId){
        this.commandId = commandId;
    }
}
