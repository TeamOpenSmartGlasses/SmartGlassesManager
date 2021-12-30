package com.google.mediapipe.apps.wearableai.affectivecomputing;

//import com.google.mediapipe.apps.wearableai.SocialMetricBoolean;
//import com.google.mediapipe.apps.wearableai.SocialMetricClass;
//import com.google.mediapipe.apps.wearableai.BodyLanguageDecode;

import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;

import java.util.*;
import android.util.Log;

//this class is instantiated for every individual we have an interaction with. A series of social states becomes a a history and live state of an individual we know
//eventually there will values this is instantiated with that we get from our memories of that indivudal, so we can do anomoly detection easily and form archetypes
public class SocialInteraction {
    private  final String TAG = "WearableAi_SocialInteraction";

    //timing of interactions
    private long interaction_start_time;

    //various social metrics
    private SocialMetricBoolean eye_contact; // eye contact == true, non-eye contact == false
    private SocialMetricClass facial_emotion; //facial emotion classes ['Angry', 'Disgusted', 'Fearful', 'Happy', 'Sad', 'Surprised', 'Neutral']
    private BodyLanguageDecode body_language; //facial emotion classes ['Angry', 'Disgusted', 'Fearful', 'Happy', 'Sad', 'Surprised', 'Neutral']

    public SocialInteraction(){
        this.eye_contact = new SocialMetricBoolean(3000); 
        this.facial_emotion = new SocialMetricClass(7, 3000);
        this.body_language = new BodyLanguageDecode();
    }

    public float getEyeContactPercentage(long start_time){
        return this.eye_contact.getMetricPercentage(start_time);
    }

    public int getFacialEmotionMostFrequent(long start_time){
        return this.facial_emotion.getMostFrequent(start_time);
    }

    public int getBodyLanguage(long start_time){
        List<Object> bls = this.body_language.computeBodyLanguage(start_time);

        int head_touch = (int) bls.get(0);
        return head_touch;
    }

    //updaters are different than just setters - because we will save the last state to a running sum of previous states based on how long it's been since last update
    public void updateEyeContact(boolean eye_contact, long timestamp){
        //set new metric
        this.eye_contact.updateMetric(eye_contact, timestamp);
    }

    public void updateFaceEmotion(float [] facial_emotion, long timestamp){
        //set new metric
        this.facial_emotion.updateMetric(facial_emotion, timestamp);
    }

    public void updateBodyLanguageLandmarks(NormalizedLandmarkList lms, long timestamp){
        //set new metric
        this.body_language.updateMetric(lms, timestamp);
    }


}
