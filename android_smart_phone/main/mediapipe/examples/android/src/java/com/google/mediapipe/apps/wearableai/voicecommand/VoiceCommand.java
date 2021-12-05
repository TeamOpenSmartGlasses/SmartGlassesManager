package com.google.mediapipe.apps.wearableai.voicecommand;

import android.util.Log;
import java.util.ArrayList;

//parse interpret
public abstract class VoiceCommand {
    public String TAG = "WearableAi_VoiceCommand";

    protected String commandName;
    protected ArrayList<String> commandList;
    protected ArrayList<String> wakeWordList;

    public abstract boolean runCommand(VoiceCommandServer vsServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime);

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
            //text after hit
            String afterText = text.substring(hitLocation);

            //find the next spaces
            int firstSpaceLocation = afterText.indexOf(" "); //the space after the hit word
            if (firstSpaceLocation == -1){ //if no space after hit word, then there is no key-value pair
                return hits;
            }
            int secondSpaceLocation = afterText.substring(firstSpaceLocation + 1).indexOf(" ");

            //word after hit
            String hit;
            String restString;
            if (secondSpaceLocation != -1){
                hit = text.substring(hitLocation + firstSpaceLocation + 1, hitLocation + firstSpaceLocation + 1 + secondSpaceLocation); //+1 for spaces
                hits.add(hit);
                restString = afterText.substring(hitLocation + firstSpaceLocation + 1 + secondSpaceLocation);
            } else { //if no space, it's the rest of the string
                hit = text.substring(hitLocation + firstSpaceLocation + 1);
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
