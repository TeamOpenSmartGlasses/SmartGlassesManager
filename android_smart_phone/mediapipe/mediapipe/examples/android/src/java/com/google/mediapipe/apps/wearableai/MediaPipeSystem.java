//this was pulled out of WearableAiAspService. It need to be updated to use the rxjava event bus to receive images and respond with computation
//saveFacialEmotion needs to be implemented with the event bus
//need to call the following from whoever creates this class in order to pass in a new image:
//
//        //send through mediapipe
//        if (mediaPipeSystem.isMediaPipeSetup){
//            mediaPipeSystem.bitmapProducer.newFrame(bitmap);
//        }
//then, it should be made into an aar so we may build it with bazel and include it as a dependency in the Gradle main ASP application

package com.google.mediapipe.apps.wearableai;

import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;
import com.google.mediapipe.apps.wearableai.database.WearableAiRoomDatabase;
import com.google.mediapipe.apps.wearableai.speechrecvosk.SpeechRecVosk;
import com.google.mediapipe.apps.wearableai.voicecommand.VoiceCommandServer;

import java.util.List;
import android.content.pm.PackageManager.NameNotFoundException;
import java.lang.NullPointerException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.Arrays;

//affective computing
import com.google.mediapipe.apps.wearableai.affectivecomputing.SocialInteraction;
import com.google.mediapipe.apps.wearableai.affectivecomputing.LandmarksTranslator;

//sensors
import com.google.mediapipe.apps.wearableai.sensors.BitmapConverter;
import com.google.mediapipe.apps.wearableai.sensors.BmpProducer;


import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.os.Binder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.glutil.EglManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

//lots of repeat imports...
import android.content.Context;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.mediapipe.apps.wearableai.comms.MessageTypes;

/** Handles sending frame through mediapipe*/
public class MediaPipeSystem {
    private static final String TAG = "WearableAi_MediaPipeSystem";

    // Service Binder given to clients
    public boolean isMediaPipeSetup = false;

    private Context context;

    //socket message ids
    //metrics
    final byte [] eye_contact_info_id_5 = {0x12, 0x01};
    final byte [] eye_contact_info_id_30 = {0x12, 0x02};
    final byte [] eye_contact_info_id_300 = {0x12, 0x03};
    final byte [] facial_emotion_info_id_5 = {0x13, 0x01};
    final byte [] facial_emotion_info_id_30 = {0x13, 0x02};
    final byte [] facial_emotion_info_id_300 = {0x13, 0x03};

    //other
    final byte [] ack_id = {0x13, 0x37};
    final byte [] heart_beat_id = {0x19, 0x20};
    final byte [] img_id = {0x01, 0x10}; //id for images

    //temp, update this on repeat and send to wearable to show connection is live
    private int count = 10;

    //keep track of current interaction - to be moved to it's own class and instantiated for each interaction/face recognition
    private SocialInteraction mSocialInteraction;

    //social metrics
    //facial_emotion_list
    String [] facial_emotion_list = {"Angry", "Disgusted", "Fearful", "Happy", "Sad", "Surprised", "Neutral"};
    String [] facialEmotionList = {"angry", "disgusted", "fearful", "happy", "sad", "surprised", "neutral"}; //this how the one hot encoded vector of our emotion network encodes the output, i.e. 0010000 would be Fearful


  // Flips the camera-preview frames vertically by default, before sending them into FrameProcessor
  // to be processed in a MediaPipe graph, and flips the processed frames back when they are
  // displayed. This maybe needed because OpenGL represents images assuming the image origin is at
  // the bottom-left corner, whereas MediaPipe in general assumes the image origin is at the
  // top-left corner.
  // NOTE: use "flipFramesVertically" in manifest metadata to override this behavior.
  private  final boolean FLIP_FRAMES_VERTICALLY = true;

  // Number of output frames allocated in ExternalTextureConverter.
  // NOTE: use "converterNumBuffers" in manifest metadata to override number of buffers. For
  // example, when there is a FlowLimiterCalculator in the graph, number of buffers should be at
  // least `max_in_flight + max_in_queue + 1` (where max_in_flight and max_in_queue are used in
  // FlowLimiterCalculator options). That's because we need buffers for all the frames that are in
  // flight/queue plus one for the next frame from the camera.
  private  final int NUM_BUFFERS = 2;

   {
    // Load all native libraries needed by the app.
    System.loadLibrary("mediapipe_jni");
    try {
      System.loadLibrary("opencv_java3");
    } catch (java.lang.UnsatisfiedLinkError e) {
      // Some example apps (e.g. template matching) require OpenCV 4.
      System.loadLibrary("opencv_java4");
    }
  }

  // {@link SurfaceTexture} where the camera-preview frames can be accessed.
  private SurfaceTexture previewFrameTexture;
  // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
  private SurfaceView previewDisplayView;

  // Creates and manages an {@link EGLContext}.
  private EglManager eglManager;
      // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private FrameProcessor processor;
  // Converts the GL_TEXTURE_EXTERNAL_OES texture from our bitmap or Android camera into a regular texture to be
  // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
  private BitmapConverter converter;

  //thread that we hand bitmaps and it converts them and passes to mediapipe
  public BmpProducer bitmapProducer;

  // ApplicationInfo for retrieving metadata defined in the manifest.
  private ApplicationInfo applicationInfo;

  private  final String FOCAL_LENGTH_STREAM_NAME = "focal_length_pixel";
  private  final String OUTPUT_LANDMARKS_STREAM_NAME = "face_landmarks_with_iris";
  private  final String OUTPUT_FACE_EMOTION_STREAM_NAME = "face_emotion";
  private  final String OUTPUT_BODY_LANGUAGE_LANDMARKS_STREAM_NAME = "body_language_landmarks";

  private boolean haveAddedSidePackets = false;

    //observables to send data around app
    PublishSubject<JSONObject> dataObservable;
    PublishSubject<byte []> audioObservable;

  public MediaPipeSystem(Context context) {
      this.context = context;

        //setup mediapipe
        setupMediapipe();
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

    private void startProducer(){
        bitmapProducer = new BmpProducer(context);
        previewDisplayView.setVisibility(View.VISIBLE);
        bitmapProducer.setCustomFrameAvailableListener(converter);
    }

  private  String getLandmarksDebugString(NormalizedLandmarkList landmarks) {
    int landmarkIndex = 0;
    String landmarksString = "";
    for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
      landmarksString +=
          "\t\tLandmark["
              + landmarkIndex
              + "]: ("
              + landmark.getX()
              + ", "
              + landmark.getY()
              + ", "
              + landmark.getZ()
              + ")\n";
      ++landmarkIndex;
    }
    return landmarksString;
  }

    private  void printLandmarksDebugString(NormalizedLandmarkList landmarks) {
        int landmarkIndex = 0;
        for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
            String landmarksString = "";
          landmarksString +=
              "\t\tLandmark["
                  + landmarkIndex
                  + "]: ("
                  + landmark.getX()
                  + ", "
                  + landmark.getY()
                  + ", "
                  + landmark.getZ()
                  + ")\n";
          Log.v(TAG, landmarksString);
          ++landmarkIndex;
        }
      }

  //mediapipe handles the AI processing of data, here we implement the hardcoded data processing
  private void processWearableAiOutput(NormalizedLandmarkList landmarks, long timestamp){
    //we convert from NormalizedLandmarks into an array list of them. I'm sure there is a great way of doing this within mediapipe, but I am pretty lost to where getLandmarkList even comes from? Can't find it if I search the whole repo. I assume it's generated, as NormalizedLandmark is generated. Need a google C++ guy to explain I guess? Well, this will work for now -cayden
    List<NormalizedLandmark> landmarksList = landmarks.getLandmarkList();

    boolean eye_contact = processEyeContact(landmarksList);

    //System.out.println("UPDATING EYE CONTACT " + eye_contact);
    mSocialInteraction.updateEyeContact(eye_contact, timestamp);
//    float eye_contact_percentage = mSocialInteraction.getEyeContactPercentage();
//    String message = "hello w0rLd! " + String.format("%.2f", eye_contact_percentage);
//    System.out.println(message);
  }

  private boolean processEyeContact(List<NormalizedLandmark> landmarksList){
//     Log.v(TAG, "Processing eye contact from face + iris landmarks...");
     //get irises
     NormalizedLandmark left_iris_center_landmark = landmarksList.get(LandmarksTranslator.left_iris_center);
     NormalizedLandmark right_iris_center_landmark = landmarksList.get(LandmarksTranslator.right_iris_center);
     //get eyes outer points
     NormalizedLandmark right_eye_outer_center_landmark = landmarksList.get(LandmarksTranslator.right_eye_outer_center);
     NormalizedLandmark left_eye_outer_center_landmark = landmarksList.get(LandmarksTranslator.left_eye_outer_center);
     //get eyes inner points
     NormalizedLandmark right_eye_inner_center_landmark = landmarksList.get(LandmarksTranslator.right_eye_inner_center);
     NormalizedLandmark left_eye_inner_center_landmark = landmarksList.get(LandmarksTranslator.left_eye_inner_center);
     //get nose
     NormalizedLandmark nose_bottom_landmark = landmarksList.get(LandmarksTranslator.nose_bottom);

     //x angles
     //get head angle using distance from eyes to nose
     //get distance between right eye and nose
     float eye_nose_dist_right = nose_bottom_landmark.getX() - right_iris_center_landmark.getX();
     float eye_nose_dist_left = left_iris_center_landmark.getX() - nose_bottom_landmark.getX();
//     Log.v(TAG, "Distance right eye to nose: " + eye_nose_dist_right);
//     Log.v(TAG, "Distance left eye to nose: " + eye_nose_dist_left);
     //now we get ratio of the two. Let's add 2 to both distances to "normalize" (don't let any negative numbers, don't hit a divide by zero,  makes things easier)
     float head_turn_ratio = (eye_nose_dist_right + 2) / (eye_nose_dist_left + 2);
//     Log.v(TAG, "head_turn_ratio = " + head_turn_ratio);
     //if head_turn_ratio ~= 1.12 - head turn 75 degrees right - +75deg
     //if head_turn_ratio ~= 0.88 - head turn 75 degrees left  - -75deg
     //if head_turn_ratio ~= 1.00 - head turn straight ahead
     //resting head position looking forward = 0 degrees
     //convert to degrees turn
     //
     //get eye angle using ratio of distance of iris center to outermost point and iris center to innermost point
     //inner
     float left_eye_inner_dist = left_eye_inner_center_landmark.getX() - left_iris_center_landmark.getX();
     float right_eye_inner_dist = right_iris_center_landmark.getX() - right_eye_inner_center_landmark.getX();
     //outer
     float left_eye_outer_dist = left_iris_center_landmark.getX() - left_eye_outer_center_landmark.getX();
     float right_eye_outer_dist = right_eye_outer_center_landmark.getX() - right_iris_center_landmark.getX();
     //ratios, again add 2 to make it all postive, no divide by 0 issue
     float right_eye_turn_ratio =  (right_eye_inner_dist + 2) / (right_eye_outer_dist + 2);
     float left_eye_turn_ratio =  (left_eye_inner_dist + 2) / (left_eye_outer_dist + 2);
//     Log.v(TAG, "right_eye_turn_ratio= " + right_eye_turn_ratio);
//     Log.v(TAG, "left_eye_turn_ratio= " + left_eye_turn_ratio);
     //eyes ratio - [0.998,1.002] - straight ahead
     //eyes ratio - 1.02 - full outside 30 degrees
     //     -right - outside is +30deg
     //     -left - outside is -30deg
     //eyes ratio - 0.98 - full inside 30 degrees
     //     -right - inside is -30deg
     //     -left - inside is +30deg
     //now we convert ratios to angles with linear functions - done on paper, y = mx+b and that shit
     float head_angle = calculateHeadAngle(head_turn_ratio);
     float right_eye_angle = calculateEyeAngle(right_eye_turn_ratio, false);
     float left_eye_angle = calculateEyeAngle(left_eye_turn_ratio, true);
//     Log.v(TAG, "Head angle: " + head_angle);
//     Log.v(TAG, "Right eye ratio: " + right_eye_turn_ratio);
//     Log.v(TAG, "Left eye ratio: " + left_eye_turn_ratio);
//     Log.v(TAG, "Right eye angle: " + right_eye_angle);
//     Log.v(TAG, "Left eye angle: " + left_eye_angle);
     //use whichever eye we can see better. So if head turns right, use left eye. If head turns left, use right eye
     float closer_eye_angle;
     if (head_angle > 0){
         closer_eye_angle = left_eye_angle;
     } else {
         closer_eye_angle = right_eye_angle;
     }
     float line_of_sight_angle = head_angle + closer_eye_angle;
//     Log.v(TAG, "line_of_sight_angle is = " + line_of_sight_angle);
     boolean eye_contact;
     if (line_of_sight_angle < 9 && line_of_sight_angle > -9){
         eye_contact = true;
     } else {
         eye_contact = false;
     }
    return eye_contact;
  }

  private float calculateHeadAngle(float ratio){
     float y_angle_head = (750* ratio) - 750; //this was computed, and then changed through trial and error
     return y_angle_head;
  }

  private float calculateEyeAngle(float ratio, boolean is_left){
     float y_angle_eye = (3800* ratio) - 3800f; //this was computed, and then changed through trial and error
     if (is_left){
         y_angle_eye = -1f * y_angle_eye;
     }

     return y_angle_eye;
  }

    public void destroy() {
        //kill mediapipe
        converter.close();
    }

    public void setupMediapipe(){
        //mediapipe stuffs
        Log.d(TAG, "Setting up...");
        try {
          applicationInfo =
              context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
          Log.e(TAG, "Cannot find application info: " + e);
        }

        previewDisplayView = new SurfaceView(context);

        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(context);
        eglManager = new EglManager(null);
        processor =
            new FrameProcessor(
                context,
                eglManager.getNativeContext(),
                applicationInfo.metaData.getString("binaryGraphName"),
                applicationInfo.metaData.getString("inputVideoStreamName"),
                applicationInfo.metaData.getString("outputVideoStreamName"));
        processor
            .getVideoSurfaceOutput()
            .setFlipY(
                applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));

        //set processor focal length side packet for specific camera
        float focalLength = 100; //random, I have no idea what this is supposed to be, TODO
        Packet focalLengthSidePacket = processor.getPacketCreator().createFloat32(focalLength);
        if (!haveAddedSidePackets) {
            Log.d(TAG, "Adding side packets");
            Map<String, Packet> inputSidePackets = new HashMap<>();
            inputSidePackets.put(FOCAL_LENGTH_STREAM_NAME, focalLengthSidePacket);
            processor.setInputSidePackets(inputSidePackets);
            haveAddedSidePackets = true;
        }

        //add a callback to process the holistic + iris output of the mediapipe perception pipeline
        processor.addPacketCallback(
          OUTPUT_LANDMARKS_STREAM_NAME,
          (packet) -> {
            byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
            try {
              NormalizedLandmarkList landmarks = NormalizedLandmarkList.parseFrom(landmarksRaw);
              if (landmarks == null) {
                return;
              } else {
                  processWearableAiOutput(landmarks, System.currentTimeMillis());
              }
            } catch (InvalidProtocolBufferException e) {
              Log.e(TAG, "Couldn't Exception received - " + e);
              return;
            }
          });

        //add a callback to process the facial emotion output of the mediapipe perception pipeline
        processor.addPacketCallback(
          OUTPUT_FACE_EMOTION_STREAM_NAME,
          (packet) -> {
              //extract face_emotion_vector from packet
              
                float[] face_emotion_vector = PacketGetter.getFloat32Vector(packet);
                //update face emotion
                mSocialInteraction.updateFaceEmotion(face_emotion_vector, System.currentTimeMillis());
                
                String faceEmotion = facialEmotionList[getMaxIdxFloat(face_emotion_vector)];
                //saveFacialEmotion(faceEmotion);

                //get facial emotion
    //           int most_frequent_facial_emotion = mSocialInteraction.getFacialEmotionMostFrequent(30);
    //           Log.d(TAG, "FACE EMO F. : " + most_frequent_facial_emotion);

    //        Log.d(TAG, "FACE EMOTION CALLBACK");
    //        for (int i = 0; i < face_emotion_vector.length; i++){
    //            Log.d(TAG, "Face emotion model output at " + i + "::: " + face_emotion_vector[i]);
    //        }
          });

        //add a callback to process the pose + left hand + right hand body language
        processor.addPacketCallback(
          OUTPUT_BODY_LANGUAGE_LANDMARKS_STREAM_NAME,
          (packet) -> {
            byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
            try {
    //              NormalizedLandmarkList landmarks = PacketGetter.getProto(packet, NormalizedLandmarkList.class);
              NormalizedLandmarkList landmarks = NormalizedLandmarkList.parseFrom(landmarksRaw);
              if (landmarks == null) {
                return;
              } else {
                    mSocialInteraction.updateBodyLanguageLandmarks(landmarks, System.currentTimeMillis());
              }
            } catch (InvalidProtocolBufferException e) {
              Log.e(TAG, "Couldn't Exception received - " + e);
              return;
            }
          });

        //setup mediapipe
        converter = new BitmapConverter(eglManager.getContext());
        converter.setConsumer(processor);
        startProducer();
        isMediaPipeSetup = true;

        Log.d(TAG, "Setup complete.");
    }

}
