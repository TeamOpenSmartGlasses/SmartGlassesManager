package com.teamopensmartglasses.sgmlib.events;

import com.teamopensmartglasses.sgmlib.SGMCommand;

import java.io.Serializable;
import java.util.UUID;

public class KillTpaEvent implements Serializable {
    public UUID uuid;
    public static final String eventId = "killTpaEvent";

    public KillTpaEvent(SGMCommand command){
        this.uuid = command.getId();
    }

    public KillTpaEvent(UUID uuid) {
        this.uuid = uuid;
    }

}
