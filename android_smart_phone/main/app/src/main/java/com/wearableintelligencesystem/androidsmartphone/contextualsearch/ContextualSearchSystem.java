package com.wearableintelligencesystem.androidsmartphone.contextualsearch;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

import org.json.JSONException;
import org.json.JSONObject;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class ContextualSearchSystem {
    private PublishSubject<JSONObject> dataObservable;
    private Disposable dataSubscriber;
    private boolean iAmActive = false;

    public void setDataObservable(PublishSubject<JSONObject> observable){
        dataObservable = observable;
        dataSubscriber = dataObservable.subscribe(i -> handleDataStream(i));
    }

    public void setActive(){
        iAmActive = true;
    }

    public void setInactive(){
        iAmActive = false;
    }

    private void handleDataStream(JSONObject data){
        try {
            String dataType = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (dataType.equals(MessageTypes.START_CONTEXTUAL_SEARCH)) {
                setActive();
            } else if (dataType.equals(MessageTypes.STOP_CONTEXTUAL_SEARCH)) {
                setInactive();
            }

            //then, if we're active, check for things to do
            if (iAmActive && dataType.equals(MessageTypes.FINAL_TRANSCRIPT)) {
                //build and send a contextual search request to the GLBOX
                JSONObject contextualSearchRequest = new JSONObject();
                contextualSearchRequest.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.CONTEXTUAL_SEARCH_REQUEST);
                contextualSearchRequest.put(MessageTypes.TRANSCRIPT_TEXT, data.getString(MessageTypes.TRANSCRIPT_TEXT).toLowerCase());
                contextualSearchRequest.put(MessageTypes.TIMESTAMP, data.getString(MessageTypes.TIMESTAMP));
                contextualSearchRequest.put(MessageTypes.TRANSCRIPT_ID, data.getString(MessageTypes.TRANSCRIPT_ID));
                dataObservable.onNext(contextualSearchRequest);
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

}
