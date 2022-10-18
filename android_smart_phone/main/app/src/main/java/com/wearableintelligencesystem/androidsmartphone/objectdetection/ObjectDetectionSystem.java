package com.wearableintelligencesystem.androidsmartphone.objectdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.util.Base64;
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

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class ObjectDetectionSystem {

    private  ObjectDetector  objectDetector;
    private String modelFile = "coco_objectdetection.tflite";
    private PublishSubject<JSONObject> dataObservable;
    private Disposable dataSubscriber;
    private boolean iAmActive = false;

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

    private void parseImageForObjectDetection(JSONObject data){
        try{
            String jpgImageString = data.getString(MessageTypes.JPG_BYTES_BASE64);
            byte [] jpgImage = Base64.decode(jpgImageString, Base64.DEFAULT);

            //convert to bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpgImage, 0, jpgImage.length);

            //send through object detection system
            runInference(TensorImage.fromBitmap(bitmap));
        } catch (JSONException e){
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
            if (dataType.equals(MessageTypes.START_OBJECT_DETECTION)) {
                setActive();
            } else if (dataType.equals(MessageTypes.STOP_OBJECT_DETECTION)) {
                setInactive();
            }

            //then, if we're active, check for things to do
            if (iAmActive && dataType.equals(MessageTypes.POV_IMAGE)) {
                parseImageForObjectDetection(data);
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

}
