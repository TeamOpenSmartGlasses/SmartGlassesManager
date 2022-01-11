package com.google.mediapipe.apps.wearableai.affectivecomputing;


import java.util.*;
import android.util.Log;
import java.lang.Math;

import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;

//import com.google.mediapipe.apps.wearableai.LandmarksTranslate.PoseAndHandsTranslator;
//import com.google.mediapipe.apps.wearableai.PoseAndHandsTranslator;

//takes in NormalizedLandmarkList with a stack of output vectors from Pose estimation landmarks, left hand landmarks, right hand landmarks
class BodyLanguageDecode {
    private static final String TAG = "WearableAi_BodyLanguageDecode";

    //holds a time series of landmarks arrays
    private List<Long> timestamps = new ArrayList<>();
    private long last_timestamp = 0;

    //holds time series of landmarks lists
    private List<NormalizedLandmarkList> metrics = new ArrayList<>();
    private NormalizedLandmarkList current_metric;

    public void updateMetric(NormalizedLandmarkList metric, long timestamp){
        //add metrics to arrays
        this.metrics.add(metric);
        this.timestamps.add(timestamp);

        //update last processed time to now
        this.setLastProcessedTime(timestamp);

        //set new current metric
        this.setCurrentMetric(metric);
    }

    public List<Object> computeBodyLanguage(long start_time){
        //make data holder
        List<NormalizedLandmarkList> lms_sub = new ArrayList<>();

        //get all metrics that fall within the request time window
        for (int i = (timestamps.size() - 1); i > 0; i--){
            //get time
            long it = this.timestamps.get(i);

            //if older than request window, break and don't add
            if (it < start_time){
                break;
            }

            //get normalized landmark list
            NormalizedLandmarkList clms = this.metrics.get(i);

            lms_sub.add(0, clms);
        }
            //get body language from landmarks
            int head_touch = getHeadTouch(lms_sub);
        
        return Arrays.asList(head_touch, head_touch);
    }

    public void setCurrentMetric(NormalizedLandmarkList current_metric){
        this.current_metric = current_metric;
    }

    public void setLastProcessedTime(long last_timestamp){
        this.last_timestamp = last_timestamp;
    }

    public long getLastProcessedTime(){
        return this.last_timestamp;
    }

    private float getScaler(NormalizedLandmarkList lms){ //calculates a scaling factor based upon a very rough idea of how far away someone is
        float avgHeight = 173f; //centimeter
        float avgTorsoProportion = 3f / 8f; //torso is 3 / 8ths of height, https://www.researchgate.net/figure/Proportions-of-the-Human-Body-with-Respect-to-the-Height-of-the-Head_fig4_228867476
        float avgTorsoHeight = avgHeight * avgTorsoProportion; //this doesn't actually mean anything in the real world until we map the camera output to real world numbers. Right now, just a number that will help us scale

        
       // NormalizedLandmark [] lms_arr = lms.getLandmarkList();

        List<NormalizedLandmark> lms_arr = lms.getLandmarkList();
        //Log.d(TAG, lms);
        if (lms_arr.size() < 1) {
            return -1;
        }

        float noseHeight = lms_arr.get(PoseAndHandsTranslator.NOSE).getY();
        float hipHeight = (lms_arr.get(PoseAndHandsTranslator.RIGHT_HIP).getY() + lms_arr.get(PoseAndHandsTranslator.RIGHT_HIP).getY()) / 2;
        float torsoHeight = noseHeight - hipHeight;
        float scaler = (torsoHeight / avgTorsoHeight) * -1; //negative because y increases from top to bottom, so we flip it here
        return scaler;
    }

    public int getHeadTouch(List<NormalizedLandmarkList> lms_list){ //this is face or head touch
        for (int i = 0; i < lms_list.size(); i++){
            NormalizedLandmarkList lms = lms_list.get(i);
            List<NormalizedLandmark> lms_arr = lms.getLandmarkList();

            float scaler = this.getScaler(lms);
            
            //get nose and hands
            //nose
            NormalizedLandmark nose_landmark = lms_arr.get(PoseAndHandsTranslator.NOSE);
            NormalizedLandmark left_index_landmark = lms_arr.get(PoseAndHandsTranslator.LEFT_INDEX);
            NormalizedLandmark right_index_landmark = lms_arr.get(PoseAndHandsTranslator.RIGHT_INDEX);
            //left hand
//            NormalizedLandmark left_hand_wrist_landmark = lms_arr.get(PoseAndHandsTranslator.LEFT_HAND_WRIST);
//            NormalizedLandmark left_hand_middle_finger_landmark = lms_arr.get(PoseAndHandsTranslator.LEFT_HAND_MIDDLE_FINGER_TIP);
//            //right hand
//            NormalizedLandmark right_hand_wrist_landmark = lms_arr.get(PoseAndHandsTranslator.RIGHT_HAND_WRIST);
//            NormalizedLandmark right_hand_middle_finger_landmark = lms_arr.get(PoseAndHandsTranslator.RIGHT_HAND_MIDDLE_FINGER_TIP);
            //supernastral notch - at y of shoulders, halfway between the should in x

            //are we touching head?
            float simple_dist = nose_landmark.getX() - left_index_landmark.getX(); 
//            float head_left_hand_dist = Math.sqrt((Math.pow((nose_landmark.getX() - left_hand_middle_finger_landmark.getX()), 2)) + (Math.pow((nose_landmark.getY() - left_hand_middle_finger_landmark.getY()), 2))); 
            //Log.d(TAG, "DIST LEFT HAND HEAD : " + head_left_hand_dist);

        }

        return 3;
    }

//    public boolean getNeckTouch(){ //this is face or head touch
//    }
//
//    public boolean getSuprasternalNotchCover(){ //more common in women, has to cover for more at least 2 seconds
//    }
//
//
//    public boolean getArmCross(){ //discomfort, defense - but can just be comfortable position
//    }
//
//    public boolean getLegsCross(){ //comfort
//    }
//
//    public boolean getFeetAngle(){ //angle of feet can tell a lot of information
//    }

}
