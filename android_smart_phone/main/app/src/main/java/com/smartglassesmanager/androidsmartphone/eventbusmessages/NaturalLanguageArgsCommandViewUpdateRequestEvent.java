package com.smartglassesmanager.androidsmartphone.eventbusmessages;

public class NaturalLanguageArgsCommandViewUpdateRequestEvent {
    public String naturalLanguageInput;
    public static final String eventId = "naturalLanguageUpdateRequest";

    public NaturalLanguageArgsCommandViewUpdateRequestEvent(String naturalLanguageInput) {
        this.naturalLanguageInput = naturalLanguageInput;
    }
}
