package com.google.mediapipe.apps.wearableai.comms;

public class MessageTypes {
    //top level
    public static final String MESSAGE_TYPE_LOCAL = "MESSAGE_TYPE_LOCAL";

    //DATA TYPES
    public static final String POV_IMAGE = "POV_IMAGE";
    public static final String JPG_BYTES_BASE64 = "JPG_BYTES_BASE64";
    public static final String IMAGE_ID = "IMAGE_ID";

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

    //command responses to show
    //Natural language
    public final static String NATURAL_LANGUAGE_QUERY = "NATURAL_LANGUAGE_QUERY";
    public final static String TEXT_QUERY = "TEXT_QUERY";
    //visual search
    public final static String VISUAL_SEARCH_RESULT = "VISUAL_SEARCH_RESULT"; //this is the ASG facing term
    public final static String VISUAL_SEARCH_IMAGE= "VISUAL_SEARCH_IMAGE";
    public final static String VISUAL_SEARCH_QUERY = "VISUAL_SEARCH_QUERY"; //this is the glbox facing term
    public final static String VISUAL_SEARCH_DATA = "VISUAL_SEARCH_DATA"; //this is the payload
    //search engine
    public final static String SEARCH_ENGINE_RESULT = "SEARCH_ENGINE_RESULT";
    public final static String SEARCH_ENGINE_RESULT_DATA = "SEARCH_ENGINE_RESULT_DATA";
    public final static String TRANSLATION_RESULT = "TRANSLATION_RESULT";
    public final static String AFFECTIVE_SUMMARY_RESULT = "AFFECTIVE_SUMMARY_RESULT";
    public final static String COMMAND_SWITCH_MODE = "COMMAND_SWITCH_MODE";

    //control the current mode of the ASG
    public final static String ACTION_SWITCH_MODES = "ACTION_SWITCH_MODES";
    public final static String NEW_MODE = "NEW_MODE";
    public final static String MODE_VISUAL_SEARCH = "MODE_VISUAL_SEARCH";
    public final static String MODE_LIVE_LIFE_CAPTIONS = "MODE_LIVE_LIFE_CAPTIONS";
    public final static String MODE_SOCIAL_MODE = "MODE_SOCIAL_MODE";
    public final static String MODE_REFERENCE_GRID = "MODE_REFERENCE_GRID";
    public final static String MODE_WEARABLE_FACE_RECOGNIZER = "MODE_WEARABLE_FACE_RECOGNIZER";
    public final static String MODE_LANGUAGE_TRANSLATE = "MODE_LANGUAGE_TRANSLATE";
    public final static String MODE_TEXT_LIST = "MODE_TEXT_LIST";
    public final static String MODE_TEXT_BLOCK = "MODE_TEXT_BLOCK";
    public final static String MODE_BLANK = "MODE_BLANK";

}


