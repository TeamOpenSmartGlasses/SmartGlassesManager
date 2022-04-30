package com.wearableintelligencesystem.androidsmartphone.objectdetection;

import android.content.Context;
import android.graphics.RectF;
import android.util.Log;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;
import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class ObjectDetectionSystem {

    private  ObjectDetector  objectDetector;
    private String modelFile = "coco_objectdetection.tflite";
    private PublishSubject<JSONObject> dataObservable;

    public ObjectDetectionSystem(Context context){

        // Initialization
        ObjectDetector.ObjectDetectorOptions options =
                ObjectDetector.ObjectDetectorOptions.builder()
                        .setBaseOptions(BaseOptions.builder().useGpu().build())
                        .setMaxResults(5)
                        .build();
        try {
            objectDetector =
                    ObjectDetector.createFromFileAndOptions(
                            context, modelFile, options);

            Log.d("Object Detection Model setup", objectDetector.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject runInference(TensorImage image){
        // Run inference
        List<Detection> results = objectDetector.detect(image);
        Log.d("Object Detection Results", results.toString());
        JSONObject detectedObjectResult = new JSONObject();

        float closestObjectDistance = 300;
        Detection closestObject = results.get(0);

        for (Detection result: results) {
                final RectF trackedPos = new RectF(result.getBoundingBox());
                //calculating closest object that the user wants to percieve
                final float detectionConfidence = result.getCategories().get(0).getScore();
                if(detectionConfidence > 0.6 && Math.sqrt(Math.pow(240-(trackedPos.left + trackedPos.right)/2, 2) +
                        Math.pow(240 -(trackedPos.bottom + trackedPos.top)/2,2))<closestObjectDistance){

                closestObjectDistance = (float) Math.sqrt(Math.pow(240-(trackedPos.left + trackedPos.right)/2, 2) +
                        Math.pow(240 -(trackedPos.bottom + trackedPos.top)/2,2));

                closestObject = result;
            }
        }
        try {

            detectedObjectResult.put("source",closestObject.getCategories().get(0).getLabel());
            Log.d("Closest Object:", closestObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject data1 = new JSONObject();
        try {
            data1.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.OBJECT_TRANSLATION_REQUEST);
            data1.put(MessageTypes.TRANSCRIPT_TEXT, closestObject.getCategories().get(0).getLabel());
            dataObservable.onNext(data1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return detectedObjectResult;
    }

    public void setDataObservable(PublishSubject<JSONObject> observable){
          dataObservable = observable;
    }
}
