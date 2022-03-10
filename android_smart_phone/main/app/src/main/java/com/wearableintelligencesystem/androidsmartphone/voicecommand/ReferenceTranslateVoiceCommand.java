package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.content.Context;
import android.util.Log;

import com.wearableintelligencesystem.androidsmartphone.WearableAiAspService;
import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandCreator;
import com.wearableintelligencesystem.androidsmartphone.nlp.FuzzyMatch;
import com.wearableintelligencesystem.androidsmartphone.speechrecognition.NaturalLanguage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


class ReferenceTranslateVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_RefrenceTranslateVoiceCommand";

    ReferenceTranslateVoiceCommand(Context context){
        super(context);
        this.commandName = "translate word";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"translate"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {}));
        ArrayList argOptions = new ArrayList<>(Arrays.asList(new String [] {"french"}));
        setRequiredArg("Language", argOptions);
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, preArgs, postArgs, "asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //String translate text
        String textToBeTranslated = postArgs.substring(postArgs.trim().indexOf(" ")+ 1);

        //parse the argument to find which language they want to translate

        List<NaturalLanguage> languagesList = WearableAiAspService.supportedLanguages;

        int lanugageMatchIdx = -1;
        String naturalLanguageMode = "";
        String selectedTargetLanguage = "";
        Log.d(TAG, "Finding near matches");
        for (int i = 0; i < languagesList.size(); i++){
            naturalLanguageMode = languagesList.get(i).getNaturalLanguageName();
            Log.d(TAG, "args: " + getFirstArg(postArgs) + "; mode: " + languagesList.get(i));
            FuzzyMatch modeMatchChar = nlpUtils.findNearMatches(getFirstArg(postArgs), naturalLanguageMode, 0.8);
            if (modeMatchChar != null && modeMatchChar.getIndex() != -1){
                lanugageMatchIdx = i;
                selectedTargetLanguage = languagesList.get(i).getCode();
                Log.d(TAG, "Found language match: " +  languagesList.get(i).getNaturalLanguageName());
                break;
            }
        }

        if (lanugageMatchIdx == -1){
            //send user update
            String displayStringFail = "No language detected. Please say the target language as the first argument to the command.";
            sendResult(vcServer, false, this.commandName, displayStringFail);
            return false;
        }

        //send user update
        String displayString = "Translating: " + textToBeTranslated + "\nTo: " + naturalLanguageMode;
        sendResult(vcServer, true, this.commandName, displayString);

        //ask system to do natural language query and send to ASG on result
        JSONObject data = new JSONObject();
        try{
            data.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.REFERENCE_TRANSLATE_SEARCH_QUERY);
            data.put(MessageTypes.REFERENCE_TRANSLATE_DATA, textToBeTranslated);
            data.put(MessageTypes.REFERENCE_TRANSLATE_TARGET_LANGUAGE_CODE, selectedTargetLanguage);
            vcServer.dataObservable.onNext(data);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return true;
        }

}
