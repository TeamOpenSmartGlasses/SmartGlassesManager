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
import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
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

/** Main activity of MediaPipe basic app. */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "WearableAi";

    //socket stuff
    ServerSocket serverSocket;
    Thread SocketThread = null;
    static Thread ReceiveThread = null;
    TextView tvIP, tvPort;
    TextView tvMessages;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 4567;
    private DataOutputStream output;
    private DataInputStream input;
    ImageView wearcam_view;
    private static int mConnectState = 0;
    private static int outbound_heart_beats = 0;

    //ringtone for notification sound
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

  // Flips the camera-preview frames vertically by default, before sending them into FrameProcessor
  // to be processed in a MediaPipe graph, and flips the processed frames back when they are
  // displayed. This maybe needed because OpenGL represents images assuming the image origin is at
  // the bottom-left corner, whereas MediaPipe in general assumes the image origin is at the
  // top-left corner.
  // NOTE: use "flipFramesVertically" in manifest metadata to override this behavior.
  private static final boolean FLIP_FRAMES_VERTICALLY = true;

  // Number of output frames allocated in ExternalTextureConverter.
  // NOTE: use "converterNumBuffers" in manifest metadata to override number of buffers. For
  // example, when there is a FlowLimiterCalculator in the graph, number of buffers should be at
  // least `max_in_flight + max_in_queue + 1` (where max_in_flight and max_in_queue are used in
  // FlowLimiterCalculator options). That's because we need buffers for all the frames that are in
  // flight/queue plus one for the next frame from the camera.
  private static final int NUM_BUFFERS = 2;

  static {
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

  private static final String FOCAL_LENGTH_STREAM_NAME = "focal_length_pixel";
  private static final String OUTPUT_LANDMARKS_STREAM_NAME = "face_landmarks_with_iris";

  private boolean haveAddedSidePackets = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
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
//    PermissionHelper.checkAndRequestCameraPermissions(this);
//    // To show verbose logging, run:
//    // adb shell setprop log.tag.MainActivity VERBOSE
//    if (Log.isLoggable(TAG, Log.VERBOSE)) {
//      processor.addPacketCallback(
//          OUTPUT_LANDMARKS_STREAM_NAME,
//          (packet) -> {
//            byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
//            try {
//              NormalizedLandmarkList landmarks = NormalizedLandmarkList.parseFrom(landmarksRaw);
//              if (landmarks == null) {
//                Log.v(TAG, "[TS:" + packet.getTimestamp() + "] No landmarks.");
//                return;
//              }
////              Log.v(
////                  TAG,
////                  "[TS:"
////                      + packet.getTimestamp()
////                      + "] #Landmarks for face (including iris): "
////                      + landmarks.getLandmarkCount());
////              int maxLogSize = 500;
////              printLandmarksDebugString(landmarks);
//              processOutput(landmarks);
//            } catch (InvalidProtocolBufferException e) {
//              Log.e(TAG, "Couldn't Exception received - " + e);
//              return;
//            }
//          });
//    }

    //setup single interaction instance - later to be done dynamically based on seeing and recognizing a new face
    mSocialInteraction = new SocialInteraction();

    //moverio stuffs
    //create references to the UI
    ////comment for now because we are using the mediapipe UI
//    tvIP = findViewById(R.id.tvIP);
//    tvPort = findViewById(R.id.tvPort);
//    tvMessages = findViewById(R.id.tvMessages);
//    etMessage = findViewById(R.id.etMessage);
//    btnSend = findViewById(R.id.btnSend);
//    
    try {
        SERVER_IP = getLocalIpAddress();
    } catch (UnknownHostException e) {
        e.printStackTrace();
    }

    //start first socketThread
    mConnectState = 1;
    Log.d(TAG, "onCreate starting");
    SocketThread = new Thread(new SocketThread());
    SocketThread.start();
    Log.d(TAG, "STARTED");

    //setup handler to handle keeping connection alive, all subsequent start of SocketThread
    //start a new handler thread to send heartbeats
    HandlerThread thread = new HandlerThread("HeartBeater");
    thread.start();
    Handler handler = new Handler(thread.getLooper());
    final int delay = 3000;
    final int min_delay = 1000;
    final int max_delay = 2000;
    Random rand = new Random();
    handler.postDelayed(new Runnable() {
        public void run() {
            heartBeat();
            //random delay for heart beat so as to disallow synchronized failure between client and server
            int random_delay = rand.nextInt((max_delay - min_delay) + 1) + min_delay;
            handler.postDelayed(this, random_delay);
        }
    }, delay);

    //start a thread which send computed social data to the moverio every n seconds
//    final Handler handler = new Handler();
//    final int delay = 100; // 100 milliseconds == 0.1 second
//
//    handler.postDelayed(new Runnable() {
//        public void run() {
//            count = count + 1;
//            float eye_contact_percentage = mSocialInteraction.getEyeContactPercentage();
//            String message = String.format("%.2f", eye_contact_percentage) + "%";
//            new Thread(new SendThread(message)).start();
//            handler.postDelayed(this, delay);
//        }
//    }, delay);
//
  }

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
//    if (PermissionHelper.cameraPermissionsGranted(this)) {
//      startCamera();
//    }
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

//  protected void onCameraStarted(SurfaceTexture surfaceTexture) {
//    previewFrameTexture = surfaceTexture;
//    // Make the display view visible to start showing the preview. This triggers the
//    // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
//    previewDisplayView.setVisibility(View.VISIBLE);
//
//    // onCameraStarted gets called each time the activity resumes, but we only want to do this once.
//    if (!haveAddedSidePackets) {
//      float focalLength = cameraHelper.getFocalLengthPixels();
//      if (focalLength != Float.MIN_VALUE) {
//        Packet focalLengthSidePacket = processor.getPacketCreator().createFloat32(focalLength);
//        Map<String, Packet> inputSidePackets = new HashMap<>();
//        inputSidePackets.put(FOCAL_LENGTH_STREAM_NAME, focalLengthSidePacket);
//        processor.setInputSidePackets(inputSidePackets);
//      }
//      haveAddedSidePackets = true;
//    }
//
//  }
//
//  protected Size cameraTargetResolution() {
//    return null; // No preference and let the camera (helper) decide.
//  }
//
//  public void startCamera() {
//    cameraHelper = new CameraXPreviewHelper();
//    cameraHelper.setOnCameraStartedListener(
//        surfaceTexture -> {
//          onCameraStarted(surfaceTexture);
//        });
//    CameraHelper.CameraFacing cameraFacing =
//        applicationInfo.metaData.getBoolean("cameraFacingFront", false)
//            ? CameraHelper.CameraFacing.FRONT
//            : CameraHelper.CameraFacing.BACK;
//    cameraHelper.startCamera(
//        this, cameraFacing, /*unusedSurfaceTexture=*/ null, cameraTargetResolution());
//  }
//
//  protected Size computeViewSize(int width, int height) {
//    return new Size(width, height);
//  }

//  protected void onPreviewDisplaySurfaceChanged(
//      SurfaceHolder holder, int format, int width, int height) {
//    // (Re-)Compute the ideal size of the camera-preview display (the area that the
//    // camera-preview frames get rendered onto, potentially with scaling and rotation)
//    // based on the size of the SurfaceView that contains the display.
//    Size viewSize = computeViewSize(width, height);
//    Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
//    boolean isCameraRotated = cameraHelper.isCameraRotated();
//
//    // Connect the converter to the camera-preview frames as its input (via
//    // previewFrameTexture), and configure the output width and height as the computed
//    // display size.
//    converter.setSurfaceTextureAndAttachToGLContext(
//        previewFrameTexture,
//        isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
//        isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
//  }

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

  private static String getLandmarksDebugString(NormalizedLandmarkList landmarks) {
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

    private static void printLandmarksDebugString(NormalizedLandmarkList landmarks) {
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
  private void processOutput(NormalizedLandmarkList landmarks){
    //we convert from NormalizedLandmarks into an array list of them. I'm sure there is a great way of doing this within mediapipe, but I am pretty lost to where getLandmarkList even comes from? Can't find it if I search the whole repo. I assume it's generated, as NormalizedLandmark is generated. Need a google C++ guy to explain I guess? Well, this will work for now -cayden
    List<NormalizedLandmark> landmarksList = landmarks.getLandmarkList();

    boolean eye_contact = processEyeContact(landmarksList);

    System.out.println("UPDATING EYE CONTACT");
    mSocialInteraction.updateEyeContact(eye_contact);
    float eye_contact_percentage = mSocialInteraction.getEyeContactPercentage();
    String message = "hello w0rLd! " + String.format("%.2f", eye_contact_percentage);
    System.out.println(message);
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
     Log.v(TAG, "head_turn_ratio = " + head_turn_ratio);
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
     Log.v(TAG, "Head angle: " + head_angle);
//     Log.v(TAG, "Right eye ratio: " + right_eye_turn_ratio);
//     Log.v(TAG, "Left eye ratio: " + left_eye_turn_ratio);
     Log.v(TAG, "Right eye angle: " + right_eye_angle);
     Log.v(TAG, "Left eye angle: " + left_eye_angle);
     //use whichever eye we can see better. So if head turns right, use left eye. If head turns left, use right eye
     float closer_eye_angle;
     if (head_angle > 0){
         closer_eye_angle = left_eye_angle;
     } else {
         closer_eye_angle = right_eye_angle;
     }
     float line_of_sight_angle = head_angle + closer_eye_angle;
     Log.v(TAG, "line_of_sight_angle is = " + line_of_sight_angle);
     boolean eye_contact;
     if (line_of_sight_angle < 9 && line_of_sight_angle > -9){
         eye_contact = true;
     } else {
         eye_contact = false;
     }
     Log.v(TAG, "Eye contact is = " + eye_contact);
     //if eye contact is made, play an alarm
    if (eye_contact == true){
        try {
            if (r == null){
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            }
            System.out.println("PLAYING SOUND + " + r_count);
              if (!mRinging){
                r.play();
                mRinging = true;
              }
        } catch (Exception e) {
            e.printStackTrace();
        }
      } else {
          if (mRinging){
              r.stop();
            mRinging = false;
          }
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
            Log.d(TAG, "STARTED NEW SOCKET THREAD");
            Socket socket;
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        tvMessages.setText("Not connected");
//                        tvIP.setText("IP: " + SERVER_IP);
//                        tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
                    }
                });

                Log.d(TAG, "LISTENING FOR CONNECTIONS");
                serverSocket = new ServerSocket(SERVER_PORT);
                Log.d(TAG, "SOCKET MADE");
                try {
                    socket = serverSocket.accept();
                    Log.d(TAG, "CONNECTION MADE");
                    //output = new PrintWriter(socket.getOutputStream(), true);
                    output = new DataOutputStream(socket.getOutputStream());
                    input = new DataInputStream(new DataInputStream(socket.getInputStream()));
                    mConnectState = 2;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            tvMessages.setText("Connected\n");
                        }
                    });
                    if (ReceiveThread == null) { //if the thread is null, make a new one (the first one)
                        ReceiveThread = new Thread(new ReceiveThread());
                        ReceiveThread.start();
                    } else if (!ReceiveThread.isAlive()) { //if the thread is not null but it's dead, let it join then start a new one
                        Log.d(TAG, "IN SocketThread< WAITING FOR receive THREAD JOING");
                        try {
                            ReceiveThread.join(); //make sure socket thread has joined before throwing off a new one
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "receive JOINED");
                        ReceiveThread = new Thread(new ReceiveThread());
                        ReceiveThread.start();
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
            byte[] hb = {0x19, 0x20};
            new Thread(new SendThread(hb)).start();
        }
    }

    //receives messages
    private class ReceiveThread implements Runnable {
        boolean firstTime = true;
        boolean abort = false;
        @Override
        public void run() {
            System.out.println("Receive Started, mconnect: " + mConnectState);
            while (true) {
                if (mConnectState != 2){
                    break;
                }
                System.out.println("LISTENING FOR MESSAGES" + firstTime);
                firstTime = false;
                try {
                    int claimed_length = input.readInt();
                    Log.d(TAG,"CLAIMED LENGTH IS " + claimed_length);
                    byte hello1 = input.readByte(); // read hello of incoming message
                    byte hello2 = input.readByte(); // read hello of incoming message
                    byte hello3 = input.readByte(); // read hello of incoming message
                    if (hello1 != 0x01 || hello2 != 0x02 || hello3 != 0x03){
                        Log.d(TAG, "JPG stream - header broken, restarting socket");
                        break;
                    }
                    byte [] len_b = new byte[4];
                    int len = input.readInt(); // read length of incoming message, integer, so 4 bytes
                    //int len = my_bb_to_int_le(len_b);
                    System.out.println("LENGTH IS " + len);
                    if (len > 0){
                        byte[] raw_data = new byte[len];
                        input.readFully(raw_data, 0, len); // read the body
                        if (len <= 10000){ //heart beat or other information sending, not an image
                            if ((raw_data[0] == 0x19) && (raw_data[1] == 0x20)){
                                Log.d(TAG, "HEART BEAT RECEIVED");
                                outbound_heart_beats--;
                            }
                         } else if (len > 10000){ //must be an image if bigger than 10k
                            System.out.println("RECEIVED MESSAGE");
                            if (raw_data != null) {
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        System.out.println("TRYING TO DISPLAY");
//                                        //display image
//                                        Bitmap bitmap = BitmapFactory.decodeByteArray(raw_data, 0, raw_data.length);
//                                        //wearcam_view.setImageBitmap(bitmap); //Must.jpg present in any of your drawable folders.
//                                        System.out.println("SET DISPLAYED");
//                                    }
//                                });

                                //ping back the client to let it know we received the message
                                byte[] ack = {0x13, 0x37};
                                new Thread(new SendThread(ack)).start();

                                //convert to bitmap
                                Bitmap bitmap = BitmapFactory.decodeByteArray(raw_data, 0, raw_data.length);
                                //send through mediapipe
                                bitmapProducer.newFrame(bitmap);

                                //save image
                                //savePicture(raw_data);
                            }
                        }
                        byte goodbye1 = input.readByte(); // read goodbye of incoming message
                        byte goodbye2 = input.readByte(); // read goodbye of incoming message
                        byte goodbye3 = input.readByte(); // read goodbye of incoming message
                        System.out.println("GOODBYE 1 IS " + goodbye1);
                        System.out.println("GOODBYE 2 IS " + goodbye2);
                        System.out.println("GOODBYE 3 IS " + goodbye3);
                        if (goodbye1 != 0x03 || goodbye2 != 0x02 || goodbye3 != 0x01) {
                            Log.d(TAG, "JPG stream - footer broken, restarting socket");
                            break;
                        }
                    } else {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "FAILED TO RECEIVE");
                    break;
                }

            }
            mConnectState = 0;
        }
    }

    private void restartSocket(){
        Log.d(TAG, "RESARTING SOCKET");
        mConnectState = 1;

        outbound_heart_beats = 0;

        //close the previous socket now that it's broken/being restarted
        try {
            if (serverSocket != null && (!serverSocket.isClosed())) {
                output.close();
                input.close();
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //make sure socket thread has joined before throwing off a new one
        Log.d(TAG, "IN RESTART< WAITING FOR SOCKET THREAD JOING");
        try {
            SocketThread.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        Log.d(TAG, "JOINED");

        //start a new socket thread
        SocketThread = new Thread(new SocketThread());
        SocketThread.start();
    }

    //this sends messages
    class SendThread implements Runnable {
        private byte [] message;
        SendThread(byte [] message) {
            this.message = message;
        }
        @Override
        public void run() {
            try {
                output.write(message);
                output.flush();
            } catch (IOException e){
                Log.d(TAG, "SOCKET DEAD - FAILED TO WRITE OUT DATA");
                mConnectState = 0; //set to "trying" state if this fails
            }
        }
    }

    private void savePicture(byte[] data){
//        byte[] data = Base64.encodeToString(ata, Base64.DEFAULT).getBytes();
        File pictureFileDir = getDir();
        System.out.println("TRYING TO SAVE AT LOCATION: " + pictureFileDir.toString());

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            Log.d("PHOTO_HANDLER", "Can't create directory to save image.");
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
            Log.d(TAG, "File" + filename + "not saved: "
                    + error.getMessage());
        }
    }

    private File getDir() {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return sdDir; //new File(sdDir, "WearableAiMobileCompute");
    }

}
