package com.wearableintelligencesystem.androidsmartphone.nlp;

import java.util.Arrays;

import android.os.Handler;
import android.util.Log;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import android.content.Context;
import android.util.Pair;

//rxjava internal comms
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;

//CSV
import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WearableReferencerAutocite {
    private static final String TAG = "WearableIntelligenceSystem_WearableReferencer";

    private boolean isActive = false; //should this class be processing data?
    private String phoneNumber; //what phone number should be sending SMS to?

    private final String referencesFileName = "wearable_referencer_references.csv";
    private List<String []> myReferences;
    private Context context;

    private boolean processing = false;

    //selectors
    String [] selectorChars = new String[] {"α", "β", "γ", "Δ", "π"};
    String [] selectorCharNames = new String[] {"alpha", "beta", "gamma", "delta", "pie"};
    List<MutableTriple<Integer,Long, Integer>> potentialReferenceBuffer = new ArrayList<MutableTriple<Integer,Long, Integer>>(); // reference index, timestamp, selector index //stores all the references we sent out, so that know which one was selected (maps a reference to the selector we gave it)
    private int potentialReferenceDecayTime = 20000; //amount of time before we disapear a reference

    private boolean awaitingSelection;

    //voice command fuzzy search threshold
    private final double referenceThreshold = 0.95;
    private final double selectorCharNameThreshold = 0.90;
    private final int titleIndex = 1;
    private final int yearIndex = 2;
    private final int authorsIndex = 3;
    private final int linkIndex = 4;
    private final int keywordIndex = 5;
    private final int titleMinWordLen = 5;
    private final int refCountThresh = 4; //how many reference hits before we suggest the reference

    private NlpUtils nlpUtils;

    //receive/send data stream
    PublishSubject<JSONObject> dataObservable;
    Disposable dataSub;
    Handler handler;

    public WearableReferencerAutocite(Context context){
        this.context = context;
        //open our references file so we can search for references
        openReferencesFile();

        nlpUtils = NlpUtils.getInstance(context);

        handler = new Handler();
    }

    private void openReferencesFile(){
        try{
            InputStream csvFileStream = context.getAssets().open(referencesFileName);
            InputStreamReader csvStreamReader = new InputStreamReader(csvFileStream);
            CSVReader reader = new CSVReader(csvStreamReader);
            myReferences = reader.readAll();
            for (int i = 0; i < myReferences.size(); i++){
//                Log.d(TAG, "Printing references at index: " + i);
//                for (int j = 0; j < myReferences.get(i).length; j++){
//                    Log.d(TAG, "--- " + myReferences.get(i)[j]);
//                }
            }
        } catch (IOException e){
            Log.d(TAG, "openReferenecsFile failed:");
            e.printStackTrace();
        }
    }

    //receive observable to send and receive data
    public void setObservable(PublishSubject<JSONObject> observable){
        dataObservable = observable;
        dataSub = dataObservable.subscribe(i -> handleDataStream(i));
    }

    public void setActive(String phoneNumber){
        Log.d(TAG, "SETTING ACTIVE");
        this.phoneNumber = phoneNumber;
        isActive = true;
    }

    public void setInactive(){
        Log.d(TAG, "SETTING INACTIVE");
        isActive = false;
    }

    //this receives data from the data observable. This function decides what to parse
    private void handleDataStream(JSONObject data){
        //first check if it's a type we should handle
        try{
            String type = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (type.equals(MessageTypes.AUTOCITER_START)){
                String phoneNumberLocal = data.getString(MessageTypes.AUTOCITER_PHONE_NUMBER);
                setActive(phoneNumberLocal);
            } else if (type.equals(MessageTypes.AUTOCITER_STOP)){
                setInactive();
            }
            if (isActive){ //only look for transcripts and parse the if we are currently active
                if (type.equals(MessageTypes.INTERMEDIATE_TRANSCRIPT)){
                } else if (type.equals(MessageTypes.ACTION_SELECT_COMMAND)){
                    Log.d(TAG, "GOT SELECTION COMMAND RESPONSE");
                    //get the referenced object selection code
                    String selection = data.getString(MessageTypes.SELECTION);
                    Log.d(TAG, "SELECTION WAS: " + selection);
                    //fuzzy match that with our possible selection
                    int selIdx = -1;
                    for (int i = 0; i < selectorCharNames.length; i++){
                       String currSel = selectorCharNames[i];
                       FuzzyMatch find = nlpUtils.findNearMatches(selection, currSel, selectorCharNameThreshold);
                       if (find != null && find.getIndex() != -1){
                           if (i < potentialReferenceBuffer.size()) {
                               MutableTriple ref = getFromBuffer(i);
                               if (ref != null) {
                                   sendReferenceToConversationPartner(myReferences.get((int) ref.getLeft()));
                               }
                           }
                           break;
                       }
                    }
                } else if (type.equals(MessageTypes.FINAL_TRANSCRIPT)){
                    Log.d(TAG, "WearableReferencer got FINAL_TRANSCRIPT, parsing");
                    try{
                        String transcript = data.getString(MessageTypes.TRANSCRIPT_TEXT);
                        handleTranscript(transcript);
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void handleTranscript(String transcript){
        //setup a handler to process data
        if (processing){
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                processing = true;
                parseString(transcript);
                processing = false;
            }
        });
    }

    //searches incoming transcript to see if it contains any keywords
    private void parseString(String toReference){
        for (int i = 1; i < myReferences.size(); i++){ //start at 1 to skip title line of CSV
            int matchCount = 0;
            String [] keywords = myReferences.get(i)[keywordIndex].toLowerCase().split(";");
            String [] authors = myReferences.get(i)[authorsIndex].toLowerCase().split(";");
            String [] title = myReferences.get(i)[titleIndex].toLowerCase().split(" ");
            title = removeElementsLessThanN(titleMinWordLen, title); //title shouldn't match "the", "for", "is", etc.
            matchCount = matchCount + nlpUtils.findNumMatches(toReference, keywords, referenceThreshold);
            matchCount = matchCount + nlpUtils.findNumMatches(toReference, authors, referenceThreshold);
            matchCount = matchCount + nlpUtils.findNumMatches(toReference, title, referenceThreshold);
            if (matchCount >= refCountThresh){ //if this is a match
                //add to our buffer
                addToBuffer(i);
            }
        }
        //send directly over SMS
//        for (Integer i : potentialReferences){
//            Log.d(TAG, "Possible reference: " + myReferences.get(i)[titleIndex]);
//            sendReferenceToConversationPartner(myReferences.get(i));
//        }
        sendPotentialReferencesToAsg();
    }

    private MutableTriple getFromBuffer(int selectionIdx){
        for (int i = 0; i < potentialReferenceBuffer.size(); i++) {
            MutableTriple currReference = potentialReferenceBuffer.get(i);
            if (selectionIdx == (int) currReference.getRight()){
                return currReference;
            }
        }
        return null;
    }

        private void addToBuffer(int refIdx){
        long currTime = System.currentTimeMillis();

        //find an available selector index
        int selIdx = -1;
        for (int i = 0; i < selectorCharNames.length; i++) {
            boolean found = true;
            //if we check every reference in the buffer and none of them have the selector i, then we can use that selector
            for (int j = 0; j < potentialReferenceBuffer.size(); j++) {
                if (i == potentialReferenceBuffer.get(j).getRight()){
                    found = false;
                }
            }
            if (found){
                selIdx = i;
                break;
            }
        }

        //there is no more room in the UI for more references, later this should pop oldest reference and replace it
        int popIdx = -1;
        if (selIdx == -1){
            long oldestTime = System.currentTimeMillis() + 1000;
            //go through, find oldest entry, and replace the oldest entry with the new reference we detected
            for (int i = 0; i < potentialReferenceBuffer.size(); i++) {
                long refTime = potentialReferenceBuffer.get(i).getMiddle();
                if (refTime < oldestTime){
                    oldestTime = refTime;
                    selIdx = potentialReferenceBuffer.get(i).getRight();
                    popIdx = i;
                }
            }
            //we found the buffer index of the
            potentialReferenceBuffer.remove(popIdx);
        }

        //before adding, check if the refIdx is already in the buffer
        int alreadyShownIdx = -1;
        for (int i = 0; i < potentialReferenceBuffer.size(); i++) {
            if (refIdx == potentialReferenceBuffer.get(i).getLeft()){
                alreadyShownIdx = i;
                break;
            }
        }
        //If it is already in the buffer, just update the time and leave it in there
        if (alreadyShownIdx != -1){
           potentialReferenceBuffer.get(alreadyShownIdx).setMiddle(System.currentTimeMillis());
        } else {
            potentialReferenceBuffer.add(new MutableTriple(refIdx, currTime, selIdx));
        }

        //setup a handler to pop stale references after they expire
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                popStaleReferences();
            }
        }, potentialReferenceDecayTime+1);
    }

    private void popStaleReferences(){
        long currTime = System.currentTimeMillis();

        //drop all stale references
        //use a toRemove list so we can remove things while iterating through the list - this is ugly and weird, maybe we should copy the list instead...?
        List<Integer> toRemove = new ArrayList<Integer>();
        for (int i = 0; i < potentialReferenceBuffer.size(); i++){
            MutableTriple<Integer, Long, Integer> refP = potentialReferenceBuffer.get(i);
            if ((refP.getMiddle() + potentialReferenceDecayTime) < currTime){
                toRemove.add(i);
            }
        }

        //actually remove them
        int count = 0;
        for (Integer i : toRemove){
            potentialReferenceBuffer.remove(i - count);
            count++;
        }

        //update ASG with new list
        sendPotentialReferencesToAsg();
    }

    private void sendPotentialReferencesToAsg(){
        try{
            //build an object to hold the references
            JSONArray refArr = new JSONArray();
            for (int i = 0; i < potentialReferenceBuffer.size(); i++){
                int refIdx = potentialReferenceBuffer.get(i).getLeft();
                int selIdx = potentialReferenceBuffer.get(i).getRight();
                String [] ref = myReferences.get(refIdx);
                Log.d(TAG, "Possible reference: " + ref[titleIndex]);
                JSONObject refObj = new JSONObject();
                refObj.put("selector_char", selectorChars[selIdx]);
                refObj.put("selector_name", selectorCharNames[selIdx]);
                refObj.put("title", ref[titleIndex]);
                refObj.put("keyword_list", ref[keywordIndex]);
                refObj.put("authors", ref[authorsIndex]);
                refArr.put(refObj);
            }

            //send that object
            JSONObject sendRefs = new JSONObject();
            sendRefs.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.REFERENCE_SELECT_REQUEST);
            sendRefs.put(MessageTypes.REFERENCES, refArr);
            dataObservable.onNext(sendRefs);
            awaitingSelection = true;
        } catch (JSONException e){
            e.printStackTrace();
        }
    }


    private void sendReferenceToConversationPartner(String [] ref){
        Log.d(TAG, "Sending reference to conversation partner...");
        String prettyReference = ref[titleIndex] + ", " + ref[authorsIndex] + ", " + ref[linkIndex];
        try{
            JSONObject sendRef = new JSONObject();
            sendRef.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.SMS_REQUEST_SEND);
            sendRef.put(MessageTypes.SMS_PHONE_NUMBER, phoneNumber);
            sendRef.put(MessageTypes.SMS_MESSAGE_TEXT, prettyReference);

            dataObservable.onNext(sendRef);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private String [] removeElementsLessThanN(int n, String [] myStringArray){
        List<String> myList = new ArrayList();

        for (String l : myStringArray) {
            if (!(l.length() < n)) {
                myList.add(l);
            }
        }

        String[] newStringArray = new String[myList.size()];
        myStringArray = myList.toArray(newStringArray);
        return myStringArray;
    }

    public boolean getIsActive(){
        return isActive;
    }

    public String getPhoneNumber(){
        return phoneNumber;
    }

}
