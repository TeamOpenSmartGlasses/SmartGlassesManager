package com.google.mediapipe.apps.wearableai.comms;

public class MessageTypes {
    //top level
    public static final String MESSAGE_TYPE_LOCAL = "MESSAGE_TYPE_LOCAL";
    public static final String MESSAGE_TYPE_ASG = "MESSAGE_TYPE_ASG";
    public static final String SEND_TO_ASG = "SEND_TO_ASG";

    //specific message types (after MESSAGE_TYPE_*)
    //TRANSCRIPTS
    public static final String FINAL_TRANSCRIPT = "FINAL_TRANSCRIPT";
    public static final String INTERMEDIATE_TRANSCRIPT = "INTERMEDIATE_TRANSCRIPT";
    public static final String TRANSCRIPT_TEXT = "TRANSCRIPT_TEXT";
    public static final String TRANSCRIPT_ID = "TRANSCRIPT_ID";
    public static final String TIMESTAMP = "TIMESTAMP";

    //VOICE COMMANDS
    public static final String VOICE_COMMAND_RESPONSE = "VOICE_COMMAND_RESPONSE";
    public static final String COMMAND_RESULT = "COMMAND_RESULT";
    public static final String COMMAND_RESPONSE_DISPLAY_STRING = "COMMAND_RESPONSE_DISPLAY_STRING";

    //FACE/PERSON SIGHTING
    public static final String FACE_SIGHTING_EVENT = "FACE_SIGHTING_EVENT";
    public static final String FACE_NAME = "FACE_NAME";

    //SMS
    public static final String SMS_REQUEST_SEND = "SMS_REQUEST_SEND";
    public static final String SMS_MESSAGE_TEXT = "SMS_MESSAGE_TEXT";
    public static final String SMS_PHONE_NUMBER = "SMS_PHONE_NUMBER";

    //AUDIO
    public static final String AUDIO_CHUNK_ENCRYPTED = "AUDIO_CHUNK_ENCRYPTED";
    public static final String AUDIO_CHUNK_DECRYPTED = "AUDIO_CHUNK_DECRYPTED";
    public static final String AUDIO_DATA = "AUDIO_DATA";

    //AUTOCITER/WEARABLE-REFERENCER
    public static final String AUTOCITER_START = "AUTOCITER_START";
    public static final String AUTOCITER_STOP = "AUTOCITER_STOP";
    public static final String AUTOCITER_PHONE_NUMBER = "AUTOCITER_PHONE_NUMBER";
}


