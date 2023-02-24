package com.teamopensmartglasses.sgmlibdebugapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.teamopensmartglasses.sgmlib.SGMLib;
import com.teamopensmartglasses.sgmlib.TPABroadcastReceiver;
import com.teamopensmartglasses.sgmlib.TPABroadcastSender;

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
    }

    public void broadcastTestClicked(View v) {
        sgmLib.sendReferenceCard("TPA Card", "This is the content body of a card that was sent from a TPA using the SGMLib.");
    }
}