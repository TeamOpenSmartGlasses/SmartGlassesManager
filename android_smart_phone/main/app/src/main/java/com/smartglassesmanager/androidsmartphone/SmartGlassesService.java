package com.smartglassesmanager.androidsmartphone;

import com.smartglassesmanager.androidsmartphone.eventbusmessages.GlassesTapOutputEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.SmartRingButtonOutputEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.SpeechRecOutputEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class SmartGlassesService extends SmartGlassesAndroidService {

    public SmartGlassesService(){
        super(MainActivity.class,
                "example_smart_glasses_app",
                1837, //choose your own number here
                "Example Smart Glasses App",
                "An example app for smart glasses",
                com.google.android.material.R.drawable.notification_template_icon_bg);

        //register subscribers on EventBus
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onTranscript(SpeechRecOutputEvent event) {
        String text = event.text;
        long time = event.timestamp;
        boolean isFinal = event.isFinal;
    }

    @Subscribe
    public void onSmartRingButtonEvent(SmartRingButtonOutputEvent event) {
        int buttonId = event.buttonId;
        long time = event.timestamp;
    }

    @Subscribe
    public void onGlassesTapSideEvent(GlassesTapOutputEvent event) {
        int numTaps = event.numTaps;
        boolean sideOfGlasses = event.sideOfGlasses;
        long time = event.timestamp;
    }

    public void deinit(){
        EventBus.getDefault().unregister(this);
    }
}
