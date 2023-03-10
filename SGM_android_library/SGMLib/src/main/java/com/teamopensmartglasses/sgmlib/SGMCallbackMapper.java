package com.teamopensmartglasses.sgmlib;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class SGMCallbackMapper {
    public HashMap<UUID, SGMCommandWithCallback> registeredCommands;

    public SGMCallbackMapper(){
        registeredCommands = new HashMap<>();
    }

    public void putCommandWithCallback(SGMCommand command, SGMCallback callback){
        registeredCommands.put(command.getId(), new SGMCommandWithCallback(command, callback));
    }

    public SGMCallback getCommandCallback(SGMCommand command){
        SGMCommandWithCallback cwc = registeredCommands.get(command.getId());
        if (cwc != null){
            return cwc.callback;
        } else {
            return null;
        }
    }

    public ArrayList<SGMCommand> getCommandsList(){
        ArrayList<SGMCommand> commandsList = new ArrayList<>();
        for (SGMCommandWithCallback cwc : registeredCommands.values()){
           commandsList.add(cwc.command);
        }
        return commandsList;
    }
}
