package com.google.mediapipe.apps.wearableai.comms;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

import android.telephony.SmsManager;

import com.google.mediapipe.apps.wearableai.R;

import org.json.JSONObject;
import org.json.JSONException;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.util.ArrayList;
import android.util.Log;

import com.google.mediapipe.apps.wearableai.comms.MessageTypes;

import java.lang.Exception;

public class SmsComms {
    private static String TAG = "WearableAi_SmsComms";
    private static SmsManager sm;
    private static SmsComms myself;

    //data observable we can tx/rx data
    private static PublishSubject<JSONObject> dataObservable;

    private SmsComms(){
        sm = SmsManager.getDefault();
    }

    public static SmsComms getInstance(){
        if (myself == null){
            myself = new SmsComms();
        }
        return myself;
    }

    // https://stackoverflow.com/questions/6361428/how-can-i-send-sms-messages-in-the-background-using-android
    public void sendSms(String phoneNumber, String message){
        //send a text message
        Log.d(TAG, "Sending SMS");
        Log.d(TAG, "-- number: " + phoneNumber);
        Log.d(TAG, "-- message: " + message);

        //below, we break up the message if it's greater than 160 chars
        if (message.length() < 160){
            sm.sendTextMessage(phoneNumber, null, message, null, null);
        } else {
            try {
                ArrayList<String> mSMSMessage = sm.divideMessage(message);
//                for (int i = 0; i < mSMSMessage.size(); i++) {
//                    sentPendingIntents.add(i, sentPI);
//                    deliveredPendingIntents.add(i, deliveredPI);
//                }
                sm.sendMultipartTextMessage(phoneNumber, null, mSMSMessage, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //receive observable to send and receive data
    public void setObservable(PublishSubject<JSONObject> observable){
        dataObservable = observable;
        Disposable dataSub = dataObservable.subscribe(i -> handleDataStream(i));
    }

    //this receives data from the data observable
    private void handleDataStream(JSONObject data){
        //first check if it's a type we should handle
        try{
            String type = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (type.equals(MessageTypes.SMS_REQUEST_SEND)){ //if it's a request to send a text, send it
                Log.d(TAG, "Got SMS request");
                String phoneNumber = data.getString(MessageTypes.SMS_PHONE_NUMBER);
                String message = data.getString(MessageTypes.SMS_MESSAGE_TEXT);
                sendSms(phoneNumber, message);
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }


}
