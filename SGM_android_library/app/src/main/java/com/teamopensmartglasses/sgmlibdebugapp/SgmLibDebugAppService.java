package com.teamopensmartglasses.sgmlibdebugapp;

import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.SGMGlobalConstants;
import com.teamopensmartglasses.sgmlib.SGMLib;
import com.teamopensmartglasses.sgmlib.SmartGlassesAndroidService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class SgmLibDebugAppService extends SmartGlassesAndroidService {
    public final String TAG = "SGMLibDebugApp_SgmLibDebugAppService";

    //our instance of the SGM library
    public SGMLib sgmLib;

    public SgmLibDebugAppService(){
        super(MainActivity.class,
                "sgmlib_debug_app",
                8362,
                "SgmLib Debug App",
                "Debug app for testing the SGMLib",
                R.drawable.ic_launcher_foreground);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //setup SGM lib
        sgmLib = new SGMLib(getApplicationContext());

        //register a basic command with the SGM
        SGMCommand helloWorldCommand = new SGMCommand("Debug One Shot", UUID.fromString(SGMGlobalConstants.DEBUG_COMMAND_ID), new String[]{"Hello world"}, "Hello world command desc");
        sgmLib.registerCommand(helloWorldCommand, this::helloWorldCallback);

        //register a command with args with the SGM
        ArrayList<String> exampleArgs = new ArrayList<String>(Arrays.asList("dog", "cat", "and other args"));
        SGMCommand helloWorldWithArgsCommand = new SGMCommand("Debug With Args", UUID.fromString(SGMGlobalConstants.DEBUG_WITH_ARGS_COMMAND_ID), new String[]{"give me args"}, "Hello world command with args", true, "Debug args:", exampleArgs);
        sgmLib.registerCommand(helloWorldWithArgsCommand, this::helloWorldWithArgsCallback);
    }

    public void helloWorldCallback(String args, long commandTime){
        sgmLib.sendReferenceCard("Debug Hello No args", "The SGM triggered the Hello World command.");
    }

    public void helloWorldWithArgsCallback(String args, long commandTime){
        sgmLib.sendReferenceCard("Debug: Hello With Args ", "The SGM triggered the Hello World With Args command. We received these args: " + args);
    }
}
