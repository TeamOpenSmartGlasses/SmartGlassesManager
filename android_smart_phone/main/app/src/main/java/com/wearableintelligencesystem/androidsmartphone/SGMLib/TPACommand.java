package com.wearableintelligencesystem.androidsmartphone.SGMLib;

import java.util.ArrayList;

public class TPACommand {
    String commandName;
    String commandDescription;
    ArrayList<String> callPhrases;

    TPACommand(String commandName, String commandDescription, ArrayList<String> callPhrases)
    {
        this.commandName = commandName;
        this.commandDescription = commandDescription;
        this.callPhrases = callPhrases;
    }
}
