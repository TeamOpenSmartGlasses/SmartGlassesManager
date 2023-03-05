package com.teamopensmartglasses.sgmlibdebugapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import java.util.UUID;

import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.SGMGlobalConstants;
import com.teamopensmartglasses.sgmlib.SGMLib;

public class MainActivity extends AppCompatActivity {
    public SGMLib sgmLib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup SGM lib
        sgmLib = new SGMLib(this);

        //register a command with the SGM
        SGMCommand helloWorldCommand = new SGMCommand("Hello World Command Name", UUID.fromString(SGMGlobalConstants.DEBUG_COMMAND_ID), new String[]{"Hello world"}, "Hello world command desc", this::helloWorldCallback);
        sgmLib.registerCommand(helloWorldCommand);
    }

    public void helloWorldCallback(){
        sgmLib.sendReferenceCard("Hello World!", "The SGM triggered the Hello World command.");
    }

    public void broadcastTestClicked(View v) {
        sgmLib.sendReferenceCard("TPA Button Clicked", "Button was clicked. This is the content body of a card that was sent from a TPA using the SGMLib.");
    }
}