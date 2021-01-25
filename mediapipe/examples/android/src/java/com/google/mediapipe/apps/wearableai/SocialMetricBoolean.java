package com.google.mediapipe.apps.wearableai;

import java.util.ArrayList;
import android.util.Log;
import java.lang.Math;

//holds information for one metric that is held in a social interaction
class SocialMetricBoolean {
  private static final String TAG = "WearableAi_MainActivity";
    //timing
    private long last_processed_time = 0;
    //state
    private boolean metric;
    private long max_time = 0; //the maximum time that our last state can be considered correct when we update to a new one
    //history
    private long on_time = 0;
    private long off_time = 0;

    SocialMetricBoolean(int max_time){
        this.max_time = max_time;
    }

    public void setMetric(boolean metric){
        this.metric = metric;
    }

    public void setLastProcessedTime(long last_processed_time){
        this.last_processed_time = last_processed_time;
    }

    public long getLastProcessedTime(){
        return this.last_processed_time;
    }

    public float getMetricPercentage(){
        float percentage;
        if (off_time != 0){
            percentage = ((float)on_time / (on_time + off_time)) * 100f;
        } else{
            percentage = -1f;
        }
        System.out.println("ONTIME " + Long.toString(on_time));
        System.out.println("OFFTIME " + Long.toString(off_time));
        System.out.println("percentage" + Float.toString(percentage));
        return percentage;
    }

    //updaters are different than just setters - because we will save the last state to a running sum of previous states based on how long it's been since last update
    public void updateMetric(boolean metric){
        if (this.last_processed_time != 0){ //don't save metric if this is the first time we are given data
            //get elapsed time since last update, so we know how long the user was in the current state
            long elapsed_time = System.currentTimeMillis() - this.last_processed_time; 
            elapsed_time = Math.min(max_time, elapsed_time); //only allowed to consider that last state correct for max_time milliseconds
            //save last metric and how long it was the case/true
            if (this.metric == true){
                on_time = on_time + elapsed_time;
            } else {
                off_time = off_time + elapsed_time;
            }
        }

        //update last processed time to now
        this.setLastProcessedTime(System.currentTimeMillis());
        //set new metric
        this.metric = metric;
    }
}
