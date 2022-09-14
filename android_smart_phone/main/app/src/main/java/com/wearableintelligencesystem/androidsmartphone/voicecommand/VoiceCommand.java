package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import android.content.Context;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.nlp.NlpUtils;

//parse interpret
public abstract class VoiceCommand {
    public String TAG = "WearableAi_VoiceCommand";

    protected String commandName;
    protected ArrayList<String> commandList;
    protected ArrayList<String> wakeWordList;
    public boolean requiredArg = false;
    public String requiredArgString;
    public ArrayList<String> requiredArgOptions;

    protected NlpUtils nlpUtils;

    public boolean isPrimary = true; //is this a primary command that can be run at any time
    public boolean noArgs = false; //does this take no arguments?

    public VoiceCommand(Context context){
        nlpUtils = NlpUtils.getInstance(context);
    }

    public abstract boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId);

    protected void setRequiredArg(String requiredArgString, ArrayList<String> requiredArgOptions){
        this.requiredArgString = requiredArgString;
        this.requiredArg = true;
        this.requiredArgOptions = requiredArgOptions;
    }

    public void sendResult(VoiceCommandServer vcServer, boolean success, String commandName, String displayString){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_STREAM_EVENT);
            commandResponseObject.put(MessageTypes.VOICE_COMMAND_STREAM_EVENT_TYPE, MessageTypes.RESOLVE_EVENT_TYPE);
            commandResponseObject.put(MessageTypes.COMMAND_RESULT, success);
            commandResponseObject.put(MessageTypes.COMMAND_NAME, commandName);
            Log.d(TAG, "DISPLAY STRING: " + displayString);
            commandResponseObject.put(MessageTypes.COMMAND_RESPONSE_DISPLAY_STRING, displayString);

            //send the command result
            vcServer.dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }
    
    public void sendMessage(VoiceCommandServer vcServer, String displayString){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_RESPONSE);

            commandResponseObject.put(MessageTypes.COMMAND_RESPONSE_DISPLAY_STRING, displayString);

            //send the command result
            vcServer.dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public String getCommandName(){
        return commandName;
    }

    public ArrayList<String> getCommands(){
        return commandList;
    }

    public ArrayList<String> getWakeWords(){
        return wakeWordList;
    }

    public ArrayList<String> parseKeyValueArgs(String text, String key){
        ArrayList<String> hits = new ArrayList<String>();
        int hitLocation = text.indexOf(key);

        if (hitLocation != -1){
            //find the next spaces
            int firstSpaceLocation = text.substring(hitLocation).indexOf(" "); //the space after the hit word
            if (firstSpaceLocation == -1){ //if no space after hit word, then we are at end of string and thus there is no key-value pair
                return hits;
            }

            //text after hit
            String afterText = text.substring(hitLocation + key.length() + 1); //+1 for space after word

            int secondSpaceLocation = afterText.indexOf(" ");

            //word after hit
            String hit;
            String restString;
            if (secondSpaceLocation != -1){
                hit = afterText.substring(0, secondSpaceLocation);
                hits.add(hit);
                restString = afterText.substring(secondSpaceLocation + 1);
            } else { //if no space, it's the rest of the string
                hit = afterText;
                hits.add(hit);
                return hits;
            }

            //if more hits, call this function on the rest of the string again
            int newHitLocation = restString.indexOf(key);
            if (newHitLocation != -1){
                hits.addAll(parseKeyValueArgs(restString, key));
            }
        }

        return hits;
    }

    protected String getFirstArg(String toParse){
        String firstArg = toParse.trim().split("\\s+")[0];
        return firstArg;
    }

//    public void sendResults(JSONObject results){
//        try{
//            //send the command result to web socket, to send to asg
//            dataObservable.onNext(results);
//        } catch (JSONException e){
//            e.printStackTrace();
//        }
//    }
//
//    public void findMatch(String subString, String fullString){
//        return match;
//    }


}
