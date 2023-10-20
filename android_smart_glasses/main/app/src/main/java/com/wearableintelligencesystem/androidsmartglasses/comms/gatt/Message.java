package com.wearableintelligencesystem.androidsmartglasses.comms.gatt;

public class Message {

    private final String text;


    public Message(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static class LocalMessage extends Message {
        public LocalMessage(String text) {
            super(text);
        }
    }

    public static class RemoteMessage extends Message {
        public RemoteMessage(String text) {
            super(text);
        }
    }

}
