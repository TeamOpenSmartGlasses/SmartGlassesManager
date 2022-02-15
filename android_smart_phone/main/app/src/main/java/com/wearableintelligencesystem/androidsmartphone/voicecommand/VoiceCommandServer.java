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

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Arrays;

//nlp
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
    private final double wakeWordThreshold = 0.83;
    private final double commandThreshold = 0.88;
    private final double endWordThreshold = 0.90;

    //database to save voice commmands to
    public VoiceCommandRepository mVoiceCommandRepository;
    public MemoryCacheRepository mMemoryCacheRepository;

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
        voiceCommands.add(new VoiceNoteVoiceCommand(context));
        voiceCommands.add(new NaturalLanguageQueryVoiceCommand(context));
        voiceCommands.add(new SearchEngineVoiceCommand(context));
        voiceCommands.add(new SwitchModesVoiceCommand(context));
        voiceCommands.add(new ReferenceTranslateVoiceCommand(context));
        voiceCommands.add(new SelectVoiceCommand(context));

        wakeWords = new ArrayList<>(Arrays.asList(new String [] {"hey computer", "hey google", "alexa", "licklider", "lickliter", "mind extension", "mind expansion", "wearable AI", "ask wolfram"}));
        endWords = new ArrayList<>(Arrays.asList(new String [] {"finish command"}));

        startSittingCommandHitter();
    }

    private void startSittingCommandHitter(){
        //every n milliseconds, check if there is a command that has been input via voice command (voice CLI) that is ready to be run
        lastTranscriptTime = System.currentTimeMillis();
        vcHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Running hitter");
                long currTime = System.currentTimeMillis();

                if (newTranscriptSinceRun && (((currTime - lastParseTime) > voiceCommandPauseTime)) && ((currTime - lastTranscriptTime) > voiceCommandPauseTime)){
                    Log.d(TAG, "hitter executing");
                    Log.d(TAG, "4 running parsing");
                    parseTranscript(lowPassTranscriptString, true, lowPassTranscriptId);
                    restartTranscriptBuffer(0, true);
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

    //handles timing of transcripts so users have plenty of time to speak to enter commands
    private void handleNewTranscript(JSONObject data){
        long currTime = System.currentTimeMillis();

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

            Log.d(TAG, "setting to tru when: " + transcript);
            Log.d(TAG, "partiaLIdx: " + partialTranscriptBufferIdx);
            Log.d(TAG, "trans.len: " + transcript.length());

            //if there is new transcript to consider AND we have been instructed to start a new voice cli buffer, start a new voice cli buffer, starting from the given partialidx
            if (startNewVoiceCliBuffer){
                lowPassTranscriptString = "";
                startNewVoiceCliBuffer = false;
            }

            newTranscriptSinceRun = true;

            //get current passed in transcript starting from the start point
            String partialTranscript = transcript.substring(partialTranscriptBufferIdx);
            Log.d(TAG, "partial trans: " + partialTranscript);

            //the transcript to parse is the voice buffer plus the latest added in partial transcript
            String transcriptToParse = lowPassTranscriptString + " " + partialTranscript;
            Log.d(TAG, "to parse trans: " + transcriptToParse);

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

            //parse transcript - but don't run
            Log.d(TAG, "2 running parsing");
            if ((System.currentTimeMillis() - lastParseTime) > 300) {
                Log.d(TAG, "IT HAPPEN HERE");
                parseTranscript(transcriptToParse, false, lowPassTranscriptId);
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

    private void parseTranscript(String transcript, boolean run, long transcriptId) {
        Log.d(TAG, "1 running parsing");
        if (currentlyParsing) {
            return;
        }
        currentlyParsing = true;

        Log.d(TAG, "parseTranscript running with transcript: " + transcript);
        Log.d(TAG, "parseTranscript running with run: " + run);
        long currTime = System.currentTimeMillis();
        lastParseTime = currTime;

        //if we find an "end command" voice command, execute now
        //loop through all global endwords to see if any match
        for (int i = 0; i < endWords.size(); i++) {
            String currEndWord = endWords.get(i);
            int endWordLocation = nlpUtils.findNearMatches(transcript, currEndWord, endWordThreshold); //transcript.indexOf(currEndWord);
            if (endWordLocation != -1) { //if the substring "end word" is in the larger string "transcript"
                Log.d(TAG, "Detected end word");
                String transcriptSansEndWord = transcript.substring(0, endWordLocation);
                transcript = transcriptSansEndWord;
                run = true;
                int partialIdx = partialTranscriptBufferIdx + endWordLocation + currEndWord.length();
                restartTranscriptBuffer(partialIdx, null);
                break;
            }
        }

        if (run){
            Log.d(TAG, "prasing with run == true");
            newTranscriptSinceRun = false;
        }

        //loop through all voice commands to see if any of their wake words match
        for (int i = 0; i < voiceCommands.size(); i++){
            //check if any of the wake words match
            for (int j = 0; j < voiceCommands.get(i).getWakeWords().size(); j++){
                String currWakeWord = voiceCommands.get(i).getWakeWords().get(j);
                int wakeWordLocation = nlpUtils.findNearMatches(transcript, currWakeWord, wakeWordThreshold); //transcript.indexOf(currWakeWord);
                if (wakeWordLocation != -1){ //if the substring "wake word" is in the larger string "transcript"
                    //we found a command, now get its arguments and run it
                    String preArgs = transcript.substring(0, wakeWordLocation);
                    String postArgs = transcript.substring(transcript.substring(wakeWordLocation).indexOf(" ") + 1); //start at the wake word location, and parse until the after the next space
                    if (run) {
                        voiceCommands.get(i).runCommand(this, preArgs, currWakeWord, j, postArgs, currTime, transcriptId);
                    }
                    currentlyParsing = false;
                    return;
                }
            }
        }

        //loop through all global wake words to see if any match
        for (int i = 0; i < wakeWords.size(); i++){
            String currWakeWord = wakeWords.get(i);
            int wakeWordLocation = nlpUtils.findNearMatches(transcript, currWakeWord, wakeWordThreshold); //transcript.indexOf(currWakeWord);
            if (wakeWordLocation != -1){ //if the substring "wake word" is in the larger string "transcript"
                //we found a command, now get its arguments and run it
                String preArgs = transcript.substring(0, wakeWordLocation);
                String rest = transcript.substring(wakeWordLocation);
                parseCommand(preArgs, currWakeWord, rest, currTime, transcriptId, run);
                currentlyParsing = false;
                return;
            }

        }
        currentlyParsing = false;
    }

    private void parseCommand(String preArgs, String wakeWord, String rest, long commandTime, long transcriptId, boolean run){
        for (int i = 0; i < voiceCommands.size(); i++){
            for (int j = 0; j < voiceCommands.get(i).getCommands().size(); j++){
                String currCommand = voiceCommands.get(i).getCommands().get(j);
                int commandLocation = nlpUtils.findNearMatches(rest, currCommand, commandThreshold);
                if (commandLocation != -1){ //if the substring "wake word" is in the larger string "transcript"
                    String postArgs = rest.substring(commandLocation + currCommand.length());
                    if (run) {
                        Log.d(TAG, "running command with args preArgs: " + preArgs + " postArgs: " + postArgs);
                        voiceCommands.get(i).runCommand(this, preArgs, wakeWord, j, postArgs, commandTime, transcriptId);
                    }
                    currentlyParsing = false;
                    return;
                }
            }
        }
    }


//    private void runCommand(String preArgs, String wakeWord, String command, String postArgs){
//        Log.d(TAG, "RUN COMMAND with postARgs:" + postArgs);
//        //below is how we WANT to call commands, by creating command classes and calling their 'run' functions here... but for sake of time I am doing hack below
////        for (int i = 0; i < commandFuncs.size(); i++){
////            if (command.equals(commandFuncs[i])){ //if the substring "wake word" is in the larger string "transcript"
////                //commandFuncs[i].runCommand(wakeWord, voiceCommands[i], commandLocation + voiceCommands[i].length());
////            }
////        }
////
//        //save command
//        VoiceCommandCreator.create(preArgs, wakeWord, command, postArgs, "transcript_ASG", mContext, mVoiceCommandRepository);
//         try{
//            VoiceCommandEntity latestCommand = mVoiceCommandRepository.getLatestCommand("start conversation");
//            Log.d(TAG, "getLatestCommand output on 'start conversation' command");
//            Log.d(TAG, latestCommand.getCommand());
//            Log.d(TAG, latestCommand.getWakeWord());
//            Log.d(TAG, latestCommand.getPostArgs());
//         } catch (ExecutionException | InterruptedException e) {
//             e.printStackTrace();
//         }
//
//
//
//        //run command finder
//        if (command.contains("start conversation")){ //if the substring "wake word" is in the larger string "transcript"
//            convoTag(true); //this is precicely where it gets hacky, this should be generic and running "run"function in voice command class
//        } else if (command.contains("stop conversation")){
//            convoTag(false);
//        } else if ((command.contains("effective search")) || (command.contains("affective search"))){
//            affectiveSearch(postArgs);
//        }
//    }

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

}
