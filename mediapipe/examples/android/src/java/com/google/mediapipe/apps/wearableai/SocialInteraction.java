package com.google.mediapipe.apps.wearableai;

import com.google.mediapipe.apps.wearableai.SocialMetricBoolean;

//this class is instantiated for every individual we have an interaction with. A series of social states becomes a a history and live state of an individual we know
//eventually there will values this is instantiated with that we get from our memories of that indivudal, so we can do anomoly detection easily and form archetypes
class SocialInteraction {
    //timing
    private long interaction_start_time;

    //state
    private SocialMetricBoolean eye_contact;

    SocialInteraction(){
        this.eye_contact = new SocialMetricBoolean(3000); //eye contact max time is about 3 seconds
    }

    public float getEyeContactPercentage(){
        return this.eye_contact.getMetricPercentage();
    }

    //updaters are different than just setters - because we will save the last state to a running sum of previous states based on how long it's been since last update
    public void updateEyeContact(boolean eye_contact){
        //set new metric
        this.eye_contact.updateMetric(eye_contact);
    }

}
