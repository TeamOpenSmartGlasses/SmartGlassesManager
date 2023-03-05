package com.teamopensmartglasses.sgmlibdebugapp;

import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.SGMGlobalConstants;
import com.teamopensmartglasses.sgmlib.SGMLib;
import com.teamopensmartglasses.sgmlib.SmartGlassesAndroidService;

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

        //register a command with the SGM
        SGMCommand helloWorldCommand = new SGMCommand("Hello World Command Name", UUID.fromString(SGMGlobalConstants.DEBUG_COMMAND_ID), new String[]{"Hello world"}, "Hello world command desc", this::helloWorldCallback);
        sgmLib.registerCommand(helloWorldCommand);
    }

    public void helloWorldCallback(){
        sgmLib.sendReferenceCard("Hello World!", "The SGM triggered the Hello World command.");
    }
}
