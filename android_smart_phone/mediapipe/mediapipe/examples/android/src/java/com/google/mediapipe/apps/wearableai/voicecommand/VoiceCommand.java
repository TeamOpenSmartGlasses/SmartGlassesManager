package com.google.mediapipe.apps.wearableai.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import android.content.Context;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.mediapipe.apps.wearableai.comms.MessageTypes;
import com.google.mediapipe.apps.wearableai.nlp.NlpUtils;

//parse interpret
public abstract class VoiceCommand {
    public String TAG = "WearableAi_VoiceCommand";

    protected String commandName;
    protected ArrayList<String> commandList;
    protected ArrayList<String> wakeWordList;

    protected NlpUtils nlpUtils;

    public VoiceCommand(Context context){
        nlpUtils = NlpUtils.getInstance(context);
    }

    public abstract boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId);

    public void sendResult(VoiceCommandServer vcServer, boolean success, String commandName, String displayString){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_RESPONSE);
            commandResponseObject.put(MessageTypes.COMMAND_RESULT, success);

            //build the display string
            String commandResponseDisplayString = "";
            if (success){
                commandResponseDisplayString = "COMMAND SUCCESS: " + commandName;
            } else {
                commandResponseDisplayString = "COMMAND FAILED: " + commandName;
            }
            if (displayString != null){
                 commandResponseDisplayString = commandResponseDisplayString + "\n" + displayString;
            }

            commandResponseObject.put(MessageTypes.COMMAND_RESPONSE_DISPLAY_STRING, commandResponseDisplayString);

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
