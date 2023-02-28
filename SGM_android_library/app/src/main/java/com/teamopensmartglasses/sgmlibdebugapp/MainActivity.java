package com.teamopensmartglasses.sgmlibdebugapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.SGMLib;
import com.teamopensmartglasses.sgmlib.TPABroadcastReceiver;
import com.teamopensmartglasses.sgmlib.TPABroadcastSender;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public SGMLib sgmLib;
    public TPABroadcastReceiver sgmReceiver;
    public TPABroadcastSender sgmSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sgmLib = new SGMLib();
        sgmReceiver = new TPABroadcastReceiver(this);
        sgmSender = new TPABroadcastSender(getApplicationContext());
        new TPABroadcastSender(this);

        String helloWorldUuid = "d7e9c6e2-8f50-4c56-8feb-6c826e789d86";
        SGMCommand helloWorldCommand = new SGMCommand("Hello World Command Name", UUID.fromString(helloWorldUuid), new String[]{"Hello world"}, "Hello world command desc", this::helloWorldCallback);
        sgmLib.registerCommand(helloWorldCommand);
    }

    public void helloWorldCallback(){
        sgmLib.sendReferenceCard("Hello World!", "The SGM triggered the Hello World command.");
    }

    public void broadcastTestClicked(View v) {
        sgmLib.sendReferenceCard("TPA Card", "This is the content body of a card that was sent from a TPA using the SGMLib.");
    }
}