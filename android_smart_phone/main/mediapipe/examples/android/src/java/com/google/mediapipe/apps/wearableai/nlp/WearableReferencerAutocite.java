package com.google.mediapipe.apps.wearableai.nlp;

import java.util.Arrays;

import android.util.Log;

import com.google.mediapipe.apps.wearableai.comms.MessageTypes;
import android.content.Context;

//rxjava internal comms
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
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


    //voice command fuzzy search threshold
    private final double referenceThreshold = 0.90;
    private final int titleIndex = 1;
    private final int yearIndex = 2;
    private final int authorsIndex = 3;
    private final int linkIndex = 4;
    private final int keywordIndex = 5;
    private final int titleMinWordLen = 5;
    private final int refCountThresh = 2; //how many reference hits before we suggest the reference

    private NlpUtils nlpUtils;

    //receive/send data stream
    PublishSubject<JSONObject> dataObservable;
    Disposable dataSub;

    public WearableReferencerAutocite(Context context){
        this.context = context;
        //open our references file so we can search for references
        openReferencesFile();

        nlpUtils = NlpUtils.getInstance(context);
    }

    private void openReferencesFile(){
        try{
            InputStream csvFileStream = context.getAssets().open(referencesFileName);
            InputStreamReader csvStreamReader = new InputStreamReader(csvFileStream);
            CSVReader reader = new CSVReader(csvStreamReader);
            myReferences = reader.readAll();
            for (int i = 0; i < myReferences.size(); i++){
                Log.d(TAG, "Printing references at index: " + i);
                for (int j = 0; j < myReferences.get(i).length; j++){
                    Log.d(TAG, "--- " + myReferences.get(i)[j]);
                }
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
                    Log.d(TAG, "WearableReferencer got INTERMEDIATE_TRANSCRIPT");
                } else if (type.equals(MessageTypes.FINAL_TRANSCRIPT)){
                    Log.d(TAG, "WearableReferencer got FINAL_TRANSCRIPT, parsing");
                    try{
                        String transcript = data.getString(MessageTypes.TRANSCRIPT_TEXT);
                        parseString(transcript);
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    //searches incoming transcript to see if it contains any keywords
    private void parseString(String toReference){
        List<Integer> potentialReferences = new ArrayList();
        for (int i = 1; i < myReferences.size(); i++){ //start at 1 to skip title line of CSV
            int matchCount = 0;
            String [] keywords = myReferences.get(i)[keywordIndex].toLowerCase().split(";");
            String [] authors = myReferences.get(i)[authorsIndex].toLowerCase().split(";");
            String [] title = myReferences.get(i)[titleIndex].toLowerCase().split(" ");
            title = removeElementsLessThanN(titleMinWordLen, title); //title shouldn't match "the", "for", "is", etc.
            matchCount = matchCount + nlpUtils.findNumMatches(toReference, keywords, referenceThreshold);
            matchCount = matchCount + nlpUtils.findNumMatches(toReference, authors, referenceThreshold);
            matchCount = matchCount + nlpUtils.findNumMatches(toReference, title, referenceThreshold);
            Log.d(TAG, "Got matchCount: " + matchCount + "; for index: " + i);
            if (matchCount >= refCountThresh){
                potentialReferences.add(i);
            }
        }
        for (Integer i : potentialReferences){
            Log.d(TAG, "Possible reference: " + myReferences.get(i)[titleIndex]);
            sendReferenceToConversationPartner(myReferences.get(i));
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
