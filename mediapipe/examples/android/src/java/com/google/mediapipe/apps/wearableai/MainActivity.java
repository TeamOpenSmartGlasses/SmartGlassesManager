//NOTES:
//there is a race condition in how the sockets are setup
//if we restartSocket() then SendThread or ReceiveThread fails, it will set the mConnectState back to 0 even though we are currently trying to connect
//need to have some handler that only send mConnectState = 0 if it's currently = 2. If it's =1, then don't change it


// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.apps.wearableai;

import com.google.mediapipe.apps.wearableai.LandmarksTranslator;
import com.google.mediapipe.apps.wearableai.SocialInteraction;

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.os.Environment;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

//lots of repeat imports...
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import android.content.Context;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;

/** Main activity of WearableAI compute module android app. */
public class MainActivity extends AppCompatActivity {
    private  final String TAG = "WearableAi";

    //SOCKET STUFF
    //acutal socket
    ServerSocket serverSocket;
    Socket socket;
    //socket threads
    Thread SocketThread = null;
    Thread ReceiveThread = null;
    Thread SendThread = null;
    //queue of data to send through the socket
    private  BlockingQueue<byte []> queue;
    //address info
    public  String SERVER_IP = "";
    public  final int SERVER_PORT = 4567;
    //i/o
    private  DataOutputStream output;
    private  DataInputStream input;
    //state information
    private  int mConnectState = 0;
    private  int outbound_heart_beats = 0;
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

    //UI
    TextView tvIP, tvPort;
    TextView tvMessages;
    ImageView wearcam_view;

    //ringtone for notification sound - testing/feedback
    private Ringtone r;
    private Uri notification;
    private int r_count = 0;
    private boolean mRinging = false;

    //temp, update this on repeat and send to wearable to show connection is live
    private int count = 10;

    //keep track of current interaction - to be moved to it's own class and instantiated for each interaction/face recognition
    private SocialInteraction mSocialInteraction;

    //holds connection state
    private boolean mConnectionState = false;

    public int PORT_NUM = 8891;
    public DatagramSocket adv_socket;
    public String adv_key = "WearableAiCyborg";

    private Context mContext;

    //social metrics
    //facial_emotion_list
    String [] facial_emotion_list = {"Angry", "Disgusted", "Fearful", "Happy", "Sad", "Surprised", "Neutral"};

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
  BmpProducer bitmapProducer;

  // ApplicationInfo for retrieving metadata defined in the manifest.
  private ApplicationInfo applicationInfo;

  private  final String FOCAL_LENGTH_STREAM_NAME = "focal_length_pixel";
  private  final String OUTPUT_LANDMARKS_STREAM_NAME = "face_landmarks_with_iris";
  private  final String OUTPUT_FACE_EMOTION_STREAM_NAME = "face_emotion";
  private  final String OUTPUT_BODY_LANGUAGE_LANDMARKS_STREAM_NAME = "body_language_landmarks";

  private boolean haveAddedSidePackets = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
      //make screen stay on for demos
      //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

      //mediapipe stuffs
    super.onCreate(savedInstanceState);
    setContentView(getContentViewLayoutResId());

    try {
      applicationInfo =
          getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
    } catch (NameNotFoundException e) {
      Log.e(TAG, "Cannot find application info: " + e);
    }

    previewDisplayView = new SurfaceView(this);
    setupPreviewDisplayView();

    // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
    // binary graphs.
    AndroidAssetUtil.initializeNativeAssetManager(this);
    eglManager = new EglManager(null);
    processor =
        new FrameProcessor(
            this,
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
        Map<String, Packet> inputSidePackets = new HashMap<>();
        inputSidePackets.put(FOCAL_LENGTH_STREAM_NAME, focalLengthSidePacket);
        processor.setInputSidePackets(inputSidePackets);
        haveAddedSidePackets = true;
    }

    //add a callback to process the holistic + iris output of the mediapipe perception pipeline
    processor.addPacketCallback(
      OUTPUT_LANDMARKS_STREAM_NAME,
      (packet) -> {
//        Log.d(TAG, "PACKET CALLBACK");
        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
        try {
          NormalizedLandmarkList landmarks = NormalizedLandmarkList.parseFrom(landmarksRaw);
          if (landmarks == null) {
//            Log.d(TAG, "[TS:" + packet.getTimestamp() + "] No landmarks.");
            return;
          } else {
//            Log.d(TAG, "[TS:" + packet.getTimestamp() + "] Timestamp.");
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

    //setup single interaction instance - later to be done dynamically based on seeing and recognizing a new face
    mSocialInteraction = new SocialInteraction();

    //create a new queue to hold outbound message
    queue = new ArrayBlockingQueue<byte[]>(50);

    //get IP address
    try {
        SERVER_IP = getLocalIpAddress();
    } catch (UnknownHostException e) {
        e.printStackTrace();
    }

    //start advertising socket thread which tells smart glasses which IP to connect to
    mContext = this;

    //open the UDP socket to broadcast our ip address
    openSocket();

    //send broadcast
    final Handler adv_handler = new Handler();
    final int delay = 1000; // 1000 milliseconds == 1 second

    adv_handler.postDelayed(new Runnable() {
        public void run() {
            new Thread(new SendAdvThread()).start();
            adv_handler.postDelayed(this, delay);
        }
    }, delay);

    //start first socketThread
    startSocket();
  }

  public void startSocket(){
        //start first socketThread
        if (socket == null) {
            mConnectState = 1;
            SocketThread = new Thread(new SocketThread());
            SocketThread.start();

            //setup handler to handle keeping connection alive, all subsequent start of SocketThread
            //start a new handler thread to send heartbeats
            HandlerThread thread = new HandlerThread("HeartBeater");
            thread.start();
            Handler heart_beat_handler = new Handler(thread.getLooper());
            final int hb_delay = 3000;
            final int min_hb_delay = 1000;
            final int max_hb_delay = 2000;
            Random rand = new Random();
            heart_beat_handler.postDelayed(new Runnable() {
                public void run() {
                    heartBeat();
                    //random hb_delay for heart beat so as to disallow synchronized failure between client and server
                    int random_hb_delay = rand.nextInt((max_hb_delay - min_hb_delay) + 1) + min_hb_delay;
                    heart_beat_handler.postDelayed(this, random_hb_delay);
                }
            }, hb_delay);

            //start a thread which send computed social data to smart glasses display every n seconds
            final Handler metrics_handler = new Handler();
            final int metrics_delay = 2500;

            metrics_handler.postDelayed(new Runnable() {
                public void run() {
                    count = count + 1;
                    //get time and start times of metrics
                    //WOW need to make this programmatic with time windows somehow - just hard coding as we don't
                    //even know what this will end up being - cayden
                    long curr_time = System.currentTimeMillis();
                    long start_time_5 = curr_time - 5000; //5 seconds window
                    long start_time_30 = curr_time - 30000; //30 seconds window
                    long start_time_300 = curr_time - 300000; //5 minutes

                    //get predicted metrics
                    float eye_contact_percentage_5 = mSocialInteraction.getEyeContactPercentage(start_time_5);
                    float eye_contact_percentage_30 = mSocialInteraction.getEyeContactPercentage(start_time_30);
                    float eye_contact_percentage_300 = mSocialInteraction.getEyeContactPercentage(start_time_300);
                    int round_eye_contact_percentage_5 = Math.round(eye_contact_percentage_5);
                    int round_eye_contact_percentage_30 = Math.round(eye_contact_percentage_30);
                    int round_eye_contact_percentage_300 = Math.round(eye_contact_percentage_300);
                    int facial_emotion_idx_5 = mSocialInteraction.getFacialEmotionMostFrequent(start_time_5);
                    int facial_emotion_idx_30 = mSocialInteraction.getFacialEmotionMostFrequent(start_time_30);
                    int facial_emotion_idx_300 = mSocialInteraction.getFacialEmotionMostFrequent(start_time_300);


                    int head_touch = mSocialInteraction.getBodyLanguage(start_time_300);
                    Log.d(TAG, "HEAD_TOUCHMAINACTIVITY: " + head_touch);

                    //load payloads and send
                    byte [] eye_contact_data_send_5 = my_int_to_bb_be(round_eye_contact_percentage_5);
                    byte [] eye_contact_data_send_30 = my_int_to_bb_be(round_eye_contact_percentage_30);
                    byte [] eye_contact_data_send_300 = my_int_to_bb_be(round_eye_contact_percentage_300);
                    byte [] facial_emotion_data_send_5 = facial_emotion_list[facial_emotion_idx_5].getBytes();
                    byte [] facial_emotion_data_send_30 = facial_emotion_list[facial_emotion_idx_30].getBytes();
                    byte [] facial_emotion_data_send_300 = facial_emotion_list[facial_emotion_idx_300].getBytes();
                    if (mConnectState == 2){
                        Log.d(TAG, "SENDING EYE CONTACT 5");
                        sendBytes(eye_contact_info_id_5, eye_contact_data_send_5);
                        sendBytes(eye_contact_info_id_30, eye_contact_data_send_30);
                        sendBytes(eye_contact_info_id_300, eye_contact_data_send_300);
                        sendBytes(facial_emotion_info_id_5, facial_emotion_data_send_5);
                        sendBytes(facial_emotion_info_id_30, facial_emotion_data_send_30);
                        sendBytes(facial_emotion_info_id_300, facial_emotion_data_send_300);
                    }
                    metrics_handler.postDelayed(this, metrics_delay);
                }
            }, metrics_delay);
        }

    }

//  public  void restartSocket() {
//        Log.d(TAG, "Restarting socket");
//        mConnectState = 1;
//        if (socket != null && (!socket.isClosed())){
//            try {
//                output.close();
//                input.close();
//                socket.close();
//            } catch (IOException e) {
//                System.out.println("FAILED TO CLOSE SOCKET, SOMETHING IS WRONG");
//            }
//        }
//        SocketThread = new Thread(new SocketThread());
//        SocketThread.start();
//    }


  // Used to obtain the content view for this application. If you are extending this class, and
  // have a custom layout, override this method and return the custom layout.
  protected int getContentViewLayoutResId() {
    return R.layout.activity_main;
  }

  @Override
  protected void onResume() {
    super.onResume();
//    converter =
//        new ExternalTextureConverter(
//            eglManager.getContext(),
//            applicationInfo.metaData.getInt("converterNumBuffers", NUM_BUFFERS));
//    converter.setFlipY(
//        applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));
    converter = new BitmapConverter(eglManager.getContext());
    converter.setConsumer(processor);
    startProducer();

  }

  @Override
  protected void onPause() {
    super.onPause();
    converter.close();

    // Hide preview display until we re-open the camera again.
    previewDisplayView.setVisibility(View.GONE);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  private void setupPreviewDisplayView() {
    previewDisplayView.setVisibility(View.GONE);
    ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
    viewGroup.addView(previewDisplayView);

    previewDisplayView
        .getHolder()
        .addCallback(
            new SurfaceHolder.Callback() {
              @Override
              public void surfaceCreated(SurfaceHolder holder) {
                processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
              }

              @Override
              public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                bitmapProducer.setCustomFrameAvailableListener(converter);
              }

              @Override
              public void surfaceDestroyed(SurfaceHolder holder) {
                processor.getVideoSurfaceOutput().setSurface(null);
              }
            });
  }

    private void startProducer(){
        bitmapProducer = new BmpProducer(this);
        previewDisplayView.setVisibility(View.VISIBLE);
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
//     Log.v(TAG, "Eye contact is = " + eye_contact);
     //if eye contact is made, play an alarm
    if (eye_contact == true){
        try {
//            if (r == null){
//                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//                r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//            }
//            System.out.println("PLAYING SOUND + " + r_count);
//              if (!mRinging){
//                r.play();
//                mRinging = true;
//              }
        } catch (Exception e) {
            e.printStackTrace();
        }
      } else {
//          if (mRinging){
//              r.stop();
//            mRinging = false;
//          }
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


 private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    class SocketThread implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                try {
                    socket = serverSocket.accept();
                    //output = new PrintWriter(socket.getOutputStream(), true);
                    output = new DataOutputStream(socket.getOutputStream());
                    input = new DataInputStream(new DataInputStream(socket.getInputStream()));
                    mConnectState = 2;
                    if (ReceiveThread == null) { //if the thread is null, make a new one (the first one)
                        ReceiveThread = new Thread(new ReceiveThread());
                        ReceiveThread.start();
                    } else if (!ReceiveThread.isAlive()) { //if the thread is not null but it's dead, let it join then start a new one
                        try {
                            ReceiveThread.join(); //make sure socket thread has joined before throwing off a new one
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ReceiveThread = new Thread(new ReceiveThread());
                        ReceiveThread.start();
                    }
                    if (SendThread == null) { //if the thread is null, make a new one (the first one)
                    SendThread = new Thread(new SendThread());
                    SendThread.start();
                } else if (!SendThread.isAlive()) { //if the thread is not null but it's dead, let it join then start a new one
                    try {
                        SendThread.join(); //make sure socket thread has joined before throwing off a new one
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    SendThread =  new Thread(new SendThread());
                    SendThread.start();
                }
                } catch (IOException e) {
                    e.printStackTrace();
                    mConnectState = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
                mConnectState = 0;
            }
        }
    }

    //receives messages
    private void heartBeat(){
        //check if we are still connected.
        //if not , reconnect,
        //if we are connected, send a heart beat to make sure we are still connected
        if (mConnectState == 0) {
            restartSocket();
        } else if (mConnectState == 2){
            //make sure we don't have a ton of outbound heart beats unresponded to
            if (outbound_heart_beats > 5) {
                restartSocket();
                return;
            }

            //increment counter
            outbound_heart_beats++;

            //send heart beat
            sendBytes(heart_beat_id, null);
        }
}

    //receives messages
    private  class ReceiveThread implements Runnable {
        @Override
        public void run() {
            //System.out.println("Receive Started, mconnect: " + mConnectState);
            while (true) {
                if (mConnectState != 2){
                    break;
                }
                byte b1, b2;
                byte [] raw_data = null;
                byte goodbye1, goodbye2, goodbye3;
                try {
                    byte hello1 = input.readByte(); // read hello of incoming message
                    byte hello2 = input.readByte(); // read hello of incoming message
                    byte hello3 = input.readByte(); // read hello of incoming message

                    //make sure header is verified
                    if (hello1 != 0x01 || hello2 != 0x02 || hello3 != 0x03){
                        break;
                    }
                    //length of body
                    int body_len = input.readInt();

                    //read in message id bytes
                    b1 = input.readByte();
                    b2 = input.readByte();

                    //read in message body (if there is one)
                    if (body_len > 0){
                        raw_data = new byte[body_len];
                        input.readFully(raw_data, 0, body_len); // read the body
                    }
                    goodbye1 = input.readByte(); // read goodbye of incoming message
                    goodbye2 = input.readByte(); // read goodbye of incoming message
                    goodbye3 = input.readByte(); // read goodbye of incoming message
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                //make sure footer is verified
                if (goodbye1 != 0x03 || goodbye2 != 0x02 || goodbye3 != 0x01) {
                    break;
                }

                //now process the data that was sent to us
                if ((b1 == heart_beat_id[0]) && (b2 == heart_beat_id[1])){ //heart beat id tag
                    outbound_heart_beats--;
                } else if ((b1 == ack_id[0]) && (b2 == ack_id[1])){ //an ack id
                } else if ((b1 == img_id[0]) && (b2 == img_id[1])){ //an img id
                    if (raw_data != null) {
                        //ping back the client to let it know we received the message
                        sendBytes(ack_id, null);

                        //convert to bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(raw_data, 0, raw_data.length);
                        //send through mediapipe
                        bitmapProducer.newFrame(bitmap);

                        //save image
                        //savePicture(raw_data);
                    }
                } else {
                    break;
                }
            }
            throwBrokenSocket();
        }
    }

    private void restartSocket(){
        mConnectState = 1;

        outbound_heart_beats = 0;

        //close the previous socket now that it's broken/being restarted
        try {
            if (serverSocket != null && (!serverSocket.isClosed())) {
                output.close();
                input.close();
                serverSocket.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //make sure socket thread has joined before throwing off a new one
        try {
            SocketThread.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        //start a new socket thread
        SocketThread = new Thread(new SocketThread());
        SocketThread.start();
    }

    public   byte[] my_int_to_bb_be(int myInteger){
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
    }

    public void sendBytes(byte[] id, byte [] data){
        //first, send hello
        byte [] hello = {0x01, 0x02, 0x03};
        //then send length of body
        byte[] len;
        if (data != null) {
             len = my_int_to_bb_be(data.length);
        } else {
            len = my_int_to_bb_be(0);
        }
        //then send id of message type
        byte [] msg_id = id;
        //then send data
        byte [] body = data;
        //then send end tag - eventually make this unique to the image
        byte [] goodbye = {0x3, 0x2, 0x1};
        //combine those into a payload
        ByteArrayOutputStream outputStream;
        try {
            outputStream = new ByteArrayOutputStream();
            outputStream.write(hello);
            outputStream.write(len);
            outputStream.write(msg_id);
            if (body != null) {
                outputStream.write(body);
            }
            outputStream.write(goodbye);
        } catch (IOException e){
            mConnectState = 0;
            return;
        }
        byte [] payload = outputStream.toByteArray();

        //send it in a background thread
        //new Thread(new SendThread(payload)).start();
        queue.add(payload);
    }

    //this sends messages
     class SendThread implements Runnable {
        SendThread() {
        }
        @Override
        public void run() {
            queue.clear();
            while (true){
                if (mConnectState != 2){
                    break;
                }
                if (queue.size() > 10){
                    break;
                }
                byte [] data;
                try {
                    data = queue.take(); //block until there is something we can pull out to send
                } catch (InterruptedException e){
                    e.printStackTrace();
                    break;
                }
                try {
                    output.write(data);           // write the message
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            throwBrokenSocket();
        }
    }

    private  void savePicture(byte[] data){
//        byte[] data = Base64.encodeToString(ata, Base64.DEFAULT).getBytes();
        File pictureFileDir = getDir();
        //System.out.println("TRYING TO SAVE AT LOCATION: " + pictureFileDir.toString());

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            return;

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_mm_dd_hh_mm_ss_SSS");
        String date = dateFormat.format(new Date());
        String photoFile = "Picture_" + date + ".jpg";

        String filename = pictureFileDir.getPath() + File.separator + photoFile;

        File pictureFile = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (Exception error) {
//            Log.d(TAG, "File" + filename + "not saved: "
//                    + error.getMessage());
        }
    }

    private  File getDir() {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return sdDir; //new File(sdDir, "WearableAiMobileCompute");
    }

    private  void throwBrokenSocket(){
        if (mConnectState == 2){
            mConnectState = 0;
        }
    }

    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public void sendBroadcast(String messageStr){
        try {
            byte[] sendData = messageStr.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(), PORT_NUM);
            adv_socket.send(sendPacket);
            //System.out.println(getClass().getName() + "Broadcast packet sent to: " + getBroadcastAddress().getHostAddress());
        } catch (IOException e){
            return ;
        }
    }

    public void openSocket() {
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            //Open a random port to send the package
            adv_socket = new DatagramSocket();
            adv_socket.setBroadcast(true);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        }
    }

    class SendAdvThread extends Thread {
        public void run() {
            //send three times as the Vuzix sucks at receiving - hit it hard and fast lol - cayden
            sendBroadcast(adv_key);
            sendBroadcast(adv_key);
            sendBroadcast(adv_key);
        }
    }

}
