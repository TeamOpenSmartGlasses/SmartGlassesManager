package com.wearableintelligencesystem.androidsmartphone.SGMLib;

import java.io.Serializable;
import java.util.ArrayList;

public class ThirdPartyApp implements Serializable {
    String appName;
    String appDescription;
    public ArrayList<TPACommand> commands;
    ThirdPartyApp(String appName, String appDescription, ArrayList<TPACommand> tpaCommands)
    {
        this.appName = appName;
        this.appDescription = appDescription;
        this.commands = tpaCommands;
    }
}
