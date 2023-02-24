package com.wearableintelligencesystem.androidsmartphone.commands;

import com.teamopensmartglasses.sgmlib.SGMCommand;

import java.util.ArrayList;
import java.util.UUID;

public class CommandSystem {
    ArrayList<SGMCommand> allCommands;

    public CommandSystem(){
        allCommands = new ArrayList<>();
        loadDefaultCommands();
    }

    private void loadDefaultCommands(){
        //live life captions
        SGMCommand liveCaptionsCommand = new SGMCommand("Live Captions", UUID.fromString("933b8950-412e-429e-8fb6-430f973cc9dc"), new String[] { "life captions", "live life captions", "captions", "transcription" }, "Starts streaming captions live to the glasses display.");
        allCommands.add(liveCaptionsCommand);

        //test card
        SGMCommand testCardCommand = new SGMCommand("Test Card", UUID.fromString("f4290426-18d5-431a-aea4-21844b832735"), new String[] { "show me a test card", "test card", "test", "testing", "show test card" }, "Shows a test Reference Card on the glasses display.");
        allCommands.add(testCardCommand);

        //blank screen
        SGMCommand blankScreenCommand = new SGMCommand("Blank Screen", UUID.fromString("93401154-b4ab-4166-9aa3-58b79db41ff0"), new String[] { "turn off display", "blank screen" }, "Makes the smart glasses display turn off or go blank.");
        allCommands.add(blankScreenCommand);
    }

    public void registerCommand(String name, UUID id, String[] phrases, String description) {
        //add a new command
        SGMCommand newCommand = new SGMCommand(name, id, phrases, description);
        allCommands.add(newCommand);
    }
}
