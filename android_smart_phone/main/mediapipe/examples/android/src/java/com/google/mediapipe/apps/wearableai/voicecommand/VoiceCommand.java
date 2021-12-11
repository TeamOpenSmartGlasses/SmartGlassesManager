package com.google.mediapipe.apps.wearableai.voicecommand;

import android.util.Log;
import java.util.ArrayList;

//parse interpret
public abstract class VoiceCommand {
    public String TAG = "WearableAi_VoiceCommand";

    protected String commandName;
    protected ArrayList<String> commandList;
    protected ArrayList<String> wakeWordList;

    public abstract boolean runCommand(VoiceCommandServer vsServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId);

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

}
