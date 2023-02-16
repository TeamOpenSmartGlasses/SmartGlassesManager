package com.wearableintelligencesystem.androidsmartphone.eventbusmessages;

public class ReferenceCardSimpleViewRequestEvent {
    public String title;
    public String body;

    public ReferenceCardSimpleViewRequestEvent(String title, String body) {
        this.title = title;
        this.body = body;
    }
}
