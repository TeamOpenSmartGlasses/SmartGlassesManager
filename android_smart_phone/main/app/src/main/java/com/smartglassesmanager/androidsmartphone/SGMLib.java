package com.smartglassesmanager.androidsmartphone;

import android.content.Context;

import com.smartglassesmanager.androidsmartphone.eventbusmessages.BulletPointListViewRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.CenteredTextViewRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.FinalScrollingTextRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.ReferenceCardImageViewRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.ReferenceCardSimpleViewRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.ScrollingTextViewStartRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.ScrollingTextViewStopRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.TextLineViewRequestEvent;

import org.greenrobot.eventbus.EventBus;

public class SGMLib {
    public String TAG = "SGMLib_SGMLib";

    //show a reference card on the smart glasses with title and body text
    public void sendReferenceCard(String title, String body) {
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    }

    //show a bullet point list card on the smart glasses with title and bullet points
    public void sendBulletPointList(String title, String [] bullets) {
        EventBus.getDefault().post(new BulletPointListViewRequestEvent(title, bullets));
    }

    public void sendReferenceCard(String title, String body, String imgUrl) {
        EventBus.getDefault().post(new ReferenceCardImageViewRequestEvent(title, body, imgUrl));
    }

    public void startScrollingText(String title){
        EventBus.getDefault().post(new ScrollingTextViewStartRequestEvent(title));
    }

    public void pushScrollingText(String text){
        EventBus.getDefault().post(new FinalScrollingTextRequestEvent(text));
    }

    public void stopScrollingText(){
        EventBus.getDefault().post(new ScrollingTextViewStopRequestEvent());
    }

    public void sendTextLine(String text) {
        EventBus.getDefault().post(new TextLineViewRequestEvent(text));
    }

    public void sendCenteredText(String text){
        EventBus.getDefault().post(new CenteredTextViewRequestEvent(text));
    }
}
