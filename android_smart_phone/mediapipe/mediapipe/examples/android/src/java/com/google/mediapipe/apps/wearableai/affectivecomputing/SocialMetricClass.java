package com.google.mediapipe.apps.wearableai.affectivecomputing;

import android.util.Log;
import java.lang.Math;
import java.util.*;

//holds information for one metric that is held in a social interaction
class SocialMetricClass {
  private static final String TAG = "WearableAi_SocialMetricClass";
    //timing
    private long last_timestamp = 0;
    private List<Long> timestamps = new ArrayList<>();
    private long max_time = 0; //the maximum time that our last state can be considered correct when we update to a new one

    //state
    private int num_classes;
    private List<float []> metrics = new ArrayList<>();
    private float [] current_metric;

    SocialMetricClass(int num_classes, int max_time){
        this.num_classes = num_classes;
        this.max_time = max_time;
    }

    public void setCurrentMetric(float [] current_metric){
        this.current_metric = current_metric;
    }

    public void setLastProcessedTime(long last_timestamp){
        this.last_timestamp = last_timestamp;
    }

    public long getLastProcessedTime(){
        return this.last_timestamp;
    }

    //return percentage of time Boolean metric is true
    public int getMostFrequent(long start_time){
        List<Object> times = this.getMetricsTime(start_time);
        long total_time = (long) times.get(0);
        long [] class_times = (long []) times.get(1);

        return this.getMaxIdxLong(class_times);
    }

    private List<Object> getMetricsTime(long start_time){
        float percentage;
        long total_time = 0;
        long [] class_times = new long[this.num_classes]; //count the amount of time when each prediction is true

        //for each data point, find prediction (largest float) and add up times
        for (int i = (this.metrics.size() - 1); i > 0; i--){
            //get time
            long it = this.timestamps.get(i);

            //ensure we haven't gone further into the past than our start time permits
            if (it < start_time ){
                break;
            }

            //add to sum in predictions counter vector
            int predict_idx = this.getMaxIdxFloat(this.metrics.get(i));
            class_times[predict_idx] += it;

            //add to total sum
            total_time += it;
        }

        return Arrays.asList(total_time, class_times);
    }

    private int getMaxIdxFloat(float [] arr){
        float maxi = 0;
        int maxi_idx = 0;

        for (int i = 0; i < arr.length; i++){
            if (arr[i] > maxi){
                maxi = arr[i];
                maxi_idx = i;
            }
        }

        return maxi_idx;
    }

    private int getMaxIdxLong(long [] arr){
        long maxi = 0;
        int maxi_idx = 0;

        for (int i = 0; i < arr.length; i++){
            if (arr[i] > maxi){
                maxi = arr[i];
                maxi_idx = i;
            }
        }

        return maxi_idx;
    }


    //updaters are different than just setters - because we will save the last state to a running sum of previous states based on how long it's been since last update
    public void updateMetric(float [] metric, long timestamp){
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
