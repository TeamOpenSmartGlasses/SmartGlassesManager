package com.google.mediapipe.apps.wearableai.affectivecomputing;

import android.util.Log;
import java.lang.Math;
import java.util.*;

//holds information for one metric that is held in a social interaction
class SocialMetricBoolean {
  private static final String TAG = "WearableAi_MainActivity";
    //timing
    private long last_timestamp = 0;
    private List<Long> timestamps = new ArrayList<>();
    private long max_time = 0; //the maximum time that our last state can be considered correct when we update to a new one

    //state
    private List<Boolean> metrics = new ArrayList<>();
    private boolean current_metric;

    //history - save moving metric
    private long on_time = 0;
    private long off_time = 0;

    SocialMetricBoolean(int max_time){
        this.max_time = max_time;
    }

    public void setCurrentMetric(boolean current_metric){
        this.current_metric = current_metric;
    }

    public void setLastProcessedTime(long last_timestamp){
        this.last_timestamp = last_timestamp;
    }

    public long getLastProcessedTime(){
        return this.last_timestamp;
    }

    //return percentage of time Boolean metric is true
    public float getMetricPercentage(long start_time){
        float percentage;
        List<Object> times = this.getMetricsTime(start_time);
        long total_time = (long) times.get(0);
        long on_time = (long) times.get(1);

        if (total_time == 0){
            return -1f;
        } else {
            percentage = ((float)on_time / total_time) * 100f;
        }

        return percentage;
    }

    private List<Object> getMetricsTime(long start_time){
        float percentage;
        long total_time = 0;
        long on_time = 0;
        
        //add up on and off times
        for (int i = (this.metrics.size() - 1); i > 0; i--){
            //get time
            long it = this.timestamps.get(i);


            //ensure we haven't gone further into the past than our start time permits

            if (it < start_time){
                break;
            }

            //add to proper sum
            if (this.metrics.get(i)){
                on_time += it;
            }
            //add to total sum
            total_time += it;
        }

        return Arrays.asList(total_time, on_time);
    }

//    public float getMetricPercentage(){
//        float percentage;
//        if (off_time != 0){
//            percentage = ((float)on_time / (on_time + off_time)) * 100f;
//        } else{
//            percentage = -1f;
//        }
////        System.out.println("ONTIME " + Long.toString(on_time));
////        System.out.println("OFFTIME " + Long.toString(off_time));
////        System.out.println("percentage" + Float.toString(percentage));
//        return percentage;
//    }

    //updaters are different than just setters - because we will save the last state to a running sum of previous states based on how long it's been since last update
    public void updateMetric(boolean metric, long timestamp){
        //add metrics to arrays
        this.metrics.add(metric);
        this.timestamps.add(timestamp);

//        if (this.last_timestamp != 0){ //don't save metric if this is the first time we are given data
//            //get elapsed time since last update, so we know how long the user was in the current state
//            long elapsed_time = timestamp - this.last_timestamp; 
//            elapsed_time = Math.min(max_time, elapsed_time); //only allowed to consider that last state correct for max_time milliseconds
//            //save last metric and how long it was the case/true
//            if (this.current_metric == true){
//                on_time = on_time + elapsed_time;
//            } else {
//                off_time = off_time + elapsed_time;
//            }
//        }

        //update last processed time to now
        this.setLastProcessedTime(timestamp);

        //set new current metric
        this.setCurrentMetric(metric);
    }
}
