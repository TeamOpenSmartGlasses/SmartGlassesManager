//NOTES:
//there is a race condition in how the sockets are setup
//if we restartSocket() then SendThread or ReceiveThread fails, it will set the mConnectState back to 0 even though we are currently trying to connect
//need to have some handler that only send mConnectState = 0 if it's currently = 2. If it's =1, then don't change it


// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheRepository;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandRepository;

import java.util.ArrayList;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.content.Context;
import android.util.Pair;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Arrays;

//nlp
import com.wearableintelligencesystem.androidsmartphone.nlp.FuzzyMatch;
import com.wearableintelligencesystem.androidsmartphone.nlp.NlpUtils;

//parse, interpret, run commands
//low pass filter on speech to text so user can say voice command slowly
//send updates when wake words, commands, arguments are seen to ASG, so user knows that what they are trying to say has been recognized, and they can see suggestions for next commands, arguments, etc.
public class VoiceCommandServer {
    private  final String TAG = "WearableAi_VoiceCommand";
    //one stream to receive transcripts, data, etc.
    public PublishSubject<JSONObject> dataObservable;
    private Disposable dataSubscriber;

    private Handler vcHandler;
    private HandlerThread mHandlerThread;

    //nlp
    private NlpUtils nlpUtils;

    //voice command stuff
    private ArrayList<VoiceCommand> voiceCommands;
    private ArrayList<String> wakeWords;
    private ArrayList<String> endWords;

    //timing of voice command system
    private long voiceCommandPauseTime = 8000; //milliseconds //amount of time user must pause speaking before we consider the command to be finished
    private long lastParseTime = 0; //milliseconds //since last time we parse a command (executing or not)
    private long lastTranscriptTime = 0; //milliseconds since we last received a transcript
    private int partialTranscriptBufferIdx = 0; //this is set true when an end word is found, so we don't keep trying to process incoming INTERMEDIATE_TRANSCRIPTs if we have already processing the current buffer - we need to wait for the next transcript
    private boolean newTranscriptSinceRun = false; //has a new transcript come in since we last parsed a voice command?
    private boolean currentlyParsing = false; //are we currently parsing? if so, don't try to parse again at the same time

    //the current voice buffer that we are processing, with metadata about the lastest phrase/transcript which makes up that voice buffer
    private String lowPassTranscriptString = "";
    long lowPassTranscriptTime = 0l;
    long lowPassTranscriptId = 0l;

    boolean startNewVoiceCliBuffer = true;
    boolean useNextTranscriptMetaData = true;

    //private ArrayList<String> voiceCommands;
    private Context mContext;

    //voice command fuzzy search threshold
    private final double wakeWordThreshold = 0.88;
    private final double commandThreshold = 0.82;
    private final double endWordThreshold = 0.88;

    //database to save voice commmands to
    public VoiceCommandRepository mVoiceCommandRepository;
    public MemoryCacheRepository mMemoryCacheRepository;

    //flags to save the current state and info about the currenlty streaming in voice command
    boolean waked = false;
    String wakeWordGiven = "";
    int wakeWordEndIdx = -1;
    boolean commanded = false;
    String commandGiven = "";
    int commandEndIdx = -1;
    boolean requiredArged = false;
    boolean needArg = false;
    boolean ended = false;

    public VoiceCommandServer(PublishSubject<JSONObject> observable, VoiceCommandRepository voiceCommandRepository, MemoryCacheRepository memoryCacheRepository, Context context){
        //setup nlp
        nlpUtils = NlpUtils.getInstance(context);

        mVoiceCommandRepository = voiceCommandRepository;
        mMemoryCacheRepository = memoryCacheRepository;

        mContext = context;

        //setup handler for voice commands
        mHandlerThread = new HandlerThread("VoiceCommandHandler");
        mHandlerThread.start();
        vcHandler = new Handler(mHandlerThread.getLooper());

        dataObservable = observable;
        dataSubscriber = dataObservable.subscribe(i -> handleDataStream(i));

        //get all voice commands
        voiceCommands = new ArrayList<VoiceCommand>();
        voiceCommands.add(new MemoryCacheStartVoiceCommand(context));
        voiceCommands.add(new MemoryCacheStopVoiceCommand(context));
        voiceCommands.add(new SearchEngineVoiceCommand(context));
        voiceCommands.add(new NaturalLanguageQueryVoiceCommand(context));
        voiceCommands.add(new SwitchModesVoiceCommand(context));
        voiceCommands.add(new VoiceNoteVoiceCommand(context));
        voiceCommands.add(new ReferenceTranslateVoiceCommand(context));
        voiceCommands.add(new SelectVoiceCommand(context));

        wakeWords = new ArrayList<>(Arrays.asList(new String [] {"hey computer"}));
        endWords = new ArrayList<>(Arrays.asList(new String [] {"finish command"}));

//        startSittingCommandHitter(); //keep checking to see if there is a command to be run
    }

    private void startSittingCommandHitter(){
        //every n milliseconds, check if there is a command that has been input via voice command (voice CLI) that is ready to be run
        lastTranscriptTime = System.currentTimeMillis();
        vcHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long currTime = System.currentTimeMillis();

                if (newTranscriptSinceRun && (((currTime - lastParseTime) > voiceCommandPauseTime)) && ((currTime - lastTranscriptTime) > voiceCommandPauseTime)){
                    parseVoiceCommmandBuffer(lowPassTranscriptString, true, lowPassTranscriptId);
                    restartTranscriptBuffer(0, true);
                    if (waked){ //if we had already woken up, but no command has run, let ASG know to go exit the command
                        cancelVoiceCommand();
                        resetVoiceCommand();
                    }
                }

                long waitTime;
                if (!newTranscriptSinceRun) {
                    waitTime = voiceCommandPauseTime + 10;
                } else {
                    waitTime = ((voiceCommandPauseTime + lastTranscriptTime) - currTime) + 10; //plus 10 milliseconds for potential OS jitter (I don't know if it's actually needed)
                }

                vcHandler.postDelayed(this, waitTime);
            }
        }, voiceCommandPauseTime + 10);

    }
    
    public void setObservable(PublishSubject observable){
        dataObservable = observable;
    }

    private void handleDataStream(JSONObject data){
        try {
            String dataType = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (dataType.equals(MessageTypes.FINAL_TRANSCRIPT)) {
                throwHandleNewTranscript(data);
            } else if (dataType.equals(MessageTypes.INTERMEDIATE_TRANSCRIPT)){
                throwHandleNewTranscript(data);
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void throwHandleNewTranscript(JSONObject data){
        //run on new thread so we don't slow anything down
        vcHandler.post(new Runnable() {
            public void run() {
                handleNewTranscript(data);
            }
        });
    }

    //handles timing of transcripts and keeping a proper buffer that is delineated by voice commands
    // allows users to have plenty of time to speak to enter commands
    private void handleNewTranscript(JSONObject data){ long currTime = System.currentTimeMillis();

        //set last transcript heard time
        lastTranscriptTime = currTime;

        //parse incoming data object
        String transcript = "";
        try{
            //get basic data about transcript
            transcript = data.getString(MessageTypes.TRANSCRIPT_TEXT).toLowerCase();
            String dataType = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);

            //if the current partial transcript idx is greater than the current transcript, ignore this transcript
            if ((partialTranscriptBufferIdx + 1) > transcript.length()){
                //if it's the final one, reset our idx to 0, because we no longer have a buffer
                if (dataType.equals(MessageTypes.FINAL_TRANSCRIPT)) {
                    partialTranscriptBufferIdx = 0;
                }
                lowPassTranscriptString = "";
                return;
            }

            //if there is new transcript to consider AND we have been instructed to start a new voice cli buffer, start a new voice cli buffer, starting from the given partialidx
            if (startNewVoiceCliBuffer){
                lowPassTranscriptString = "";
                startNewVoiceCliBuffer = false;
            }

            newTranscriptSinceRun = true;

            //get current passed in transcript starting from the start point
            String partialTranscript = transcript.substring(partialTranscriptBufferIdx);

            //the transcript to parse is the voice buffer plus the latest added in partial transcript
            String transcriptToParse = lowPassTranscriptString + " " + partialTranscript;

            //should we update the buffer's timestamp and ID at the next transcript?
            if (useNextTranscriptMetaData){
                lowPassTranscriptTime = data.getLong(MessageTypes.TIMESTAMP);
                lowPassTranscriptId = data.getLong(MessageTypes.TRANSCRIPT_ID);
                useNextTranscriptMetaData = false;
            }

            //if a FINAL_TRANSCRIPT is getting passed in,
            if (dataType.equals(MessageTypes.FINAL_TRANSCRIPT)) {
                lowPassTranscriptString = transcriptToParse;

                //if we are grabbing a partial transcript, but now we've received the final transcript, we can stop parsing the current transcript as a partial transcript on the next incoming transcript
                partialTranscriptBufferIdx = 0;
            }

            //parse transcript - but don't run too often
            if ((System.currentTimeMillis() - lastParseTime) > 50) {
                parseVoiceCommmandBuffer(transcriptToParse, false, lowPassTranscriptId);
            }
        } catch (JSONException e){
            e.printStackTrace();
            return;
        }
    }

    private void restartTranscriptBuffer(int partialIdx, Boolean useNext){
//        lowPassTranscriptString = "";
        partialTranscriptBufferIdx = partialIdx;
        startNewVoiceCliBuffer = true;
        if (useNext != null) {
            useNextTranscriptMetaData = useNext;
        }
   }

   //run a parsing pass on the current voice transcript buffer. This depends on the current state of the machine w.r.t. what we have already found - wake word, command, arguments, end word, etc.
    private void parseVoiceCommmandBuffer(String transcript, boolean run, long transcriptId) {
        //if we have already found a wake word and a command, but the command is not yet finished, then send a stream of the argument text to the ASG
        if (waked && commanded){
            if (transcript.length() > commandEndIdx) { //don't send if there's no args
                try {
                    JSONObject commandArgsEvent = new JSONObject();
                    commandArgsEvent.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_STREAM_EVENT);
                    commandArgsEvent.put(MessageTypes.VOICE_COMMAND_STREAM_EVENT_TYPE, MessageTypes.COMMAND_ARGS_EVENT_TYPE);
                    String subString = transcript.substring(commandEndIdx); //string after the command ends
                    Log.d(TAG, "substring: " + subString);
                    String subSubString = "";
                    int firstSubStringSpace = subString.indexOf(" ");
                    if (firstSubStringSpace != -1){ //if there is no space, then there is no new word after the command word, and thus nothing to show
                        subSubString = subString.substring(firstSubStringSpace + 1);//string after the commands ends and after the next space
                    }

                    Log.d(TAG, "subsubstring: " + subSubString);
                    commandArgsEvent.put(MessageTypes.INPUT_VOICE_STRING, subSubString); //everything after the wake word and commandEnd //+1 for space
                    dataObservable.onNext(commandArgsEvent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        //return if we are currently parsing another string
        if (currentlyParsing) {
            return;
        }
        currentlyParsing = true;

        long currTime = System.currentTimeMillis();
        lastParseTime = currTime;


        //loop through all voice commands to see if any of their wake words match
        if (!waked) { //only run this if we haven't already detected a wake word
//            for (int i = 0; i < voiceCommands.size(); i++) {
//                Pair<FuzzyMatch, Integer> wakeWordBestMatch = nlpUtils.findBestMatch(transcript, voiceCommands.get(i).getWakeWords(), wakeWordThreshold);
//                if (wakeWordBestMatch != null) {
//                    FuzzyMatch wakeWordMatch = wakeWordBestMatch.first;
//                    int wakeWordMatchIdx = wakeWordBestMatch.second;
//                    String wakeWordMatchString = voiceCommands.get(i).getWakeWords().get(wakeWordMatchIdx);
//                    foundWakeWord(wakeWordMatchString, wakeWordMatch.getIndex() + wakeWordMatchString.length());
//                    //we found a command, now get its arguments and run it
//                    String preArgs = transcript.substring(0, wakeWordMatch.getIndex());
//                    String postArgs = transcript.substring(transcript.substring(wakeWordMatch.getIndex()).indexOf(" ") + 1); //start at the wake word location, and remove until the after the next space
//                    if (run) {
//                        runCommand(voiceCommands.get(i), preArgs, wakeWordMatchString, wakeWordMatchIdx, postArgs, currTime, transcriptId);
//                    }
//                    break;
//                }
//            }

            //loop through all global wake words to see if any match
            for (int i = 0; i < wakeWords.size(); i++){
                String currWakeWord = wakeWords.get(i);
                FuzzyMatch wakeWordLocation = nlpUtils.findNearMatches(transcript, currWakeWord, wakeWordThreshold); //transcript.indexOf(currWakeWord);
                if (wakeWordLocation != null && wakeWordLocation.getIndex() != -1){ //if the substring "wake word" is in the larger string "transcript"
                    //we found a command, now get its arguments and run it
                    foundWakeWord(currWakeWord, wakeWordLocation.getIndex() + currWakeWord.length());
                    String preArgs = transcript.substring(0, wakeWordLocation.getIndex());
                    String rest;
                    try {
                        rest = transcript.substring(wakeWordLocation.getIndex() + currWakeWord.length());
                    } catch (StringIndexOutOfBoundsException e){
                        rest = "";
                    }
                    break;
                }
            }
        }

        //search for command input
        String preArgs;
        String rest;
        if (waked) { //only search for a command if we've already woken
            preArgs = transcript.substring(0, wakeWordEndIdx - wakeWordGiven.length()); //get args before the wakeword
            try {
                rest = transcript.substring(wakeWordEndIdx);
            } catch (StringIndexOutOfBoundsException e){
                rest = "";
            }

            //search for a command. If it's found, we'll show the arguments screen and save what command was found
            parseCommand(preArgs, wakeWordGiven, rest, currTime, transcriptId, run);

            //if we find an "end command" voice command, execute now
            //loop through all global endwords to see if any match
            Log.d(TAG, "SEARCH FOR END");
            for (int i = 0; i < endWords.size(); i++) {
                String currEndWord = endWords.get(i);
                FuzzyMatch endWordLocation = nlpUtils.findNearMatches(transcript, currEndWord, endWordThreshold); //transcript.indexOf(currEndWord);
                if (endWordLocation != null && endWordLocation.getIndex() != -1) { //if the substring "end word" is in the larger string "transcript"
                    //we detected an end word, so act on it if we've been commanded, or cancel if not
                    Log.d(TAG, "Detected end word");
                    int partialIdx = partialTranscriptBufferIdx + endWordLocation.getIndex() + currEndWord.length();
                    if (!commanded){
                        Log.d(TAG, "Wake word detected with no command input: " + rest);
                        cancelVoiceCommand();
                        run = true;
                        restartTranscriptBuffer(partialIdx, null);
                        break;
                    }
                    String transcriptSansEndWord = transcript.substring(0, endWordLocation.getIndex());
                    rest = transcriptSansEndWord;
                    run = true;
                    ended = true;

                    //find the command and run it
                    Log.d(TAG, "RUN PARSE " + preArgs + ",  " + rest);
                    parseCommand(preArgs, wakeWordGiven, rest, currTime, transcriptId, run);

                    //restart voice transcript buffer now that we've run a command
                    restartTranscriptBuffer(partialIdx, null);
                    break;
                }
            }
        }

        if (run){
            newTranscriptSinceRun = false;
        }

        currentlyParsing = false;
    }

    private void parseCommand(String preArgs, String wakeWord, String rest, long commandTime, long transcriptId, boolean run){
        FuzzyMatch bestMatch = new FuzzyMatch(-1,0);
        int bestMatchIdx = -1;
        int vcIdx = -1;
        for (int i = 0; i < voiceCommands.size(); i++) {
            Pair<FuzzyMatch, Integer> commandBestMatch = nlpUtils.findBestMatch(rest, voiceCommands.get(i).getCommands(), commandThreshold);
            if (commandBestMatch != null) {
                FuzzyMatch commandMatch = commandBestMatch.first;
                int commandMatchIdx = commandBestMatch.second;
                if (commandMatch.getSimilarity() > bestMatch.getSimilarity()) {
                    bestMatch = commandMatch;
                    bestMatchIdx = commandMatchIdx;
                    vcIdx = i;
                }
            }
        }
        if (bestMatch.getIndex() != -1){
            FuzzyMatch commandMatch = bestMatch;
            Log.d(TAG, "BEST MATCH COMMAND: " + bestMatch);
            int commandMatchIdx = bestMatchIdx;
            String commandMatchString = voiceCommands.get(vcIdx).getCommands().get(commandMatchIdx);

            Log.d(TAG, "3FOUND: " + commandMatchString);
            String postArgs;
            try {
                postArgs = rest.substring(commandMatch.getIndex() + commandMatchString.length());
            } catch (StringIndexOutOfBoundsException e){
                postArgs = "";
            }

            int firstSpace = postArgs.indexOf(" ");
            if (firstSpace != -1){ //if there is no space, then there is no new word after the command word, and thus nothing to show
                postArgs = postArgs.substring(firstSpace + 1);//string after the commands ends and after the next space
            }

            //check if there is a full word argument yet, if so, then we have gotten our requiredArg
            Log.d(TAG, "POSTARGS: " + postArgs);
            if (postArgs.indexOf(" ") != -1){
                Log.d(TAG, "POSTARGS RAN THIS BOI");
                requiredArged = true;
            }

            //show the user the next menu
            if (commanded) { //if we've already found a command, just check if we are still waiting on a required argument
                if (voiceCommands.get(vcIdx).requiredArg && needArg && requiredArged){
                    needNaturalLanguageArgs(commandMatchString);
                    needArg = false;
                }
            } else { //else, this is the first time we found a command, and we either prompt for a required argument or send user straight to the natural language query selection
                if (voiceCommands.get(vcIdx).requiredArg) {
                    needRequiredArg(voiceCommands.get(vcIdx).requiredArgString, voiceCommands.get(vcIdx).requiredArgOptions);
                } else {
                    needNaturalLanguageArgs(commandMatchString);
                }
            }

            foundCommand(commandMatchString, wakeWordEndIdx + commandMatch.getIndex() + commandMatchString.length());

            if (run || voiceCommands.get(vcIdx).noArgs) {
                restartTranscriptBuffer(commandEndIdx, true);
                runCommand(voiceCommands.get(vcIdx), preArgs, wakeWord, commandMatchIdx, postArgs, commandTime, transcriptId);
            }
            currentlyParsing = false;
            return;
        }
    }

    private void convoTag(boolean start){
        if (start){
            Log.d(TAG, "********** starting conversation");
        } else {
            Log.d(TAG, "********** stopping conversation");
        }
    }

    private void affectiveSearch(String postArgs){
        try{
            postArgs = postArgs.trim();
            int idxSpace = postArgs.indexOf(' ');
            String emotion = "";
            if (idxSpace != -1){
                emotion = postArgs.trim().substring(0, postArgs.indexOf(' '));
            } else{
                emotion = postArgs;
            }
            Log.d(TAG, "emotion is: " + emotion);
            JSONObject affective_search_query = new JSONObject();
            affective_search_query.put(MessageTypes.MESSAGE_TYPE_LOCAL, "affective_search_query");
            affective_search_query.put("emotion", emotion);
            dataObservable.onNext(affective_search_query);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void needRequiredArg(String argName, ArrayList<String> possibleArgs){
        needArg = true;
        //tell ASG that we have found a command but now need a required argument
        try {
            //generate args list
            JSONArray argsList = new JSONArray();
            possibleArgs.forEach(argPossibility -> {
                argsList.put(argPossibility);
            });

            JSONObject wakeWordFoundEvent = new JSONObject();
            wakeWordFoundEvent.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_STREAM_EVENT);
            wakeWordFoundEvent.put(MessageTypes.VOICE_COMMAND_STREAM_EVENT_TYPE, MessageTypes.REQUIRED_ARG_EVENT_TYPE);
            wakeWordFoundEvent.put(MessageTypes.ARG_NAME, argName);
            wakeWordFoundEvent.put(MessageTypes.ARG_OPTIONS, argsList);
            dataObservable.onNext(wakeWordFoundEvent);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void needNaturalLanguageArgs(String command){
        //tell ASG that we have found this command, and what kind of input we need next
        try {
            JSONObject commandFoundEvent = new JSONObject();
            commandFoundEvent.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_STREAM_EVENT);
            commandFoundEvent.put(MessageTypes.VOICE_COMMAND_STREAM_EVENT_TYPE, MessageTypes.COMMAND_EVENT_TYPE);
            commandFoundEvent.put(MessageTypes.INPUT_VOICE_COMMAND_NAME, command);
            commandFoundEvent.put(MessageTypes.INPUT_WAKE_WORD, this.wakeWordGiven);
            commandFoundEvent.put(MessageTypes.VOICE_ARG_EXPECT_TYPE, MessageTypes.VOICE_ARG_EXPECT_NATURAL_LANGUAGE);
            dataObservable.onNext(commandFoundEvent);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void foundWakeWord(String wakeWord, int wakeWordEndIdx){
        //don't run if a wake word has already been found in the current voice buffer
        if (waked){
            return;
        }
        Log.d(TAG, "FOUND WAKE WORD");

        waked = true;
        this.wakeWordGiven = wakeWord;
        this.wakeWordEndIdx = wakeWordEndIdx;

        //generate commandlist for all commands
        JSONArray commandList = new JSONArray();
        voiceCommands.forEach(voiceCommand -> {
            if (voiceCommand.isPrimary) {
                voiceCommand.getCommands().forEach(command -> {
                    commandList.put(command);
                });
            }
        });

        //tell ASG that we have found this wake word and to display the following command options
        try {
            JSONObject wakeWordFoundEvent = new JSONObject();
            wakeWordFoundEvent.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_STREAM_EVENT);
            wakeWordFoundEvent.put(MessageTypes.VOICE_COMMAND_STREAM_EVENT_TYPE, MessageTypes.WAKE_WORD_EVENT_TYPE);
            wakeWordFoundEvent.put(MessageTypes.VOICE_COMMAND_LIST,commandList.toString());
            wakeWordFoundEvent.put(MessageTypes.INPUT_WAKE_WORD, wakeWord);
            dataObservable.onNext(wakeWordFoundEvent);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void foundCommand(String command, int commandEndIdx){
        //don't run if a wake word has already been found in the current voice buffer
        if (commanded){
            return;
        }
        Log.d(TAG, "FOUND COMMAND: " + command);

        commanded = true;
        this.commandGiven = command;
        this.commandEndIdx = commandEndIdx;
    }

    private void runCommand(VoiceCommand vc, String preArgs, String wakeWord, int commandIdx, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "running commmand: " + vc.getCommandName());
        //the voice command itself will handle sending the appropriate response to the ASG
        resetVoiceCommand();
        boolean result = vc.runCommand(this, preArgs, wakeWord, commandIdx, postArgs, commandTime, transcriptId);
        if (!result){
            cancelVoiceCommand();
        }
        //sendResultToAsg(success);
    }

    private void resetVoiceCommand(){
        Log.d(TAG, "RESET VOICE COMMAND BUFFER FLAGS");
        waked = false;
        this.wakeWordGiven = "";
        commanded = false;
        this.commandGiven = "";
        ended = false;
        requiredArged = false;
        needArg = false;
    }

    public void sendResultToAsg(boolean success){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_STREAM_EVENT);
            commandResponseObject.put(MessageTypes.VOICE_COMMAND_STREAM_EVENT_TYPE, MessageTypes.RESOLVE_EVENT_TYPE);
            commandResponseObject.put(MessageTypes.COMMAND_RESULT, success);

            //send the command result
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void cancelVoiceCommand(){
        resetVoiceCommand();
        Log.d(TAG, "CANCEL VOICE COMMAND");
        try{
            //build json object to send voice command cancel notification
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_STREAM_EVENT);
            commandResponseObject.put(MessageTypes.VOICE_COMMAND_STREAM_EVENT_TYPE, MessageTypes.CANCEL_EVENT_TYPE);

            //send the command result
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

}
