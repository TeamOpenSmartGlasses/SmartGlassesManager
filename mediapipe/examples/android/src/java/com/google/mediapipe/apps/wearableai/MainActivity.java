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

import java.util.List;
import java.util.ArrayList;

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

/** Main activity of MediaPipe basic app. */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "WearableAi_MainActivity";

    //socket stuff
    ServerSocket serverSocket;
    Thread SocketThread = null;
    String message;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 4567;
    private PrintWriter output;
    private DataInputStream input;
    byte [] image_data_rcv_jpg;
    TextView tvIP, tvPort;
    TextView tvMessages;
    EditText etMessage;
    Button btnSend;
    ImageView wearcam_view;

    //temp, update this on repeat and send to wearable to show connection is live
    private int count = 10;

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

  // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
  // frames onto a {@link Surface}.
  protected FrameProcessor processor;
  // Handles camera access via the {@link CameraX} Jetpack support library.
  protected CameraXPreviewHelper cameraHelper;
  
  // {@link SurfaceTexture} where the camera-preview frames can be accessed.
  private SurfaceTexture previewFrameTexture;
  // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
  private SurfaceView previewDisplayView;

  // Creates and manages an {@link EGLContext}.
  private EglManager eglManager;
  // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
  // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
  private ExternalTextureConverter converter;

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

    PermissionHelper.checkAndRequestCameraPermissions(this);
    // To show verbose logging, run:
    // adb shell setprop log.tag.MainActivity VERBOSE
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      processor.addPacketCallback(
          OUTPUT_LANDMARKS_STREAM_NAME,
          (packet) -> {
            byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
            try {
                System.out.println("FUCK1");
                Log.v(TAG, "FUCK2");
              NormalizedLandmarkList landmarks = NormalizedLandmarkList.parseFrom(landmarksRaw);
              if (landmarks == null) {
                Log.v(TAG, "[TS:" + packet.getTimestamp() + "] No landmarks.");
                Log.v(TAG, "FUCK3");
                return;
              }
              Log.v(
                  TAG,
                  "[TS:"
                      + packet.getTimestamp()
                      + "] #Landmarks for face (including iris): "
                      + landmarks.getLandmarkCount());
              Log.v(TAG, getLandmarksDebugString(landmarks));
              processOutput(landmarks);
            } catch (InvalidProtocolBufferException e) {
            Log.v(TAG, "FUCK4");
              Log.e(TAG, "Couldn't Exception received - " + e);
              return;
            }
          });
    }

    //moverio stuffs
    //create references to the UI
    ////comment for now because we are using the mediapipe UI
//    tvIP = findViewById(R.id.tvIP);
//    tvPort = findViewById(R.id.tvPort);
//    tvMessages = findViewById(R.id.tvMessages);
//    etMessage = findViewById(R.id.etMessage);
//    btnSend = findViewById(R.id.btnSend);

    //get local IP
    try {
        SERVER_IP = getLocalIpAddress();
    } catch (UnknownHostException e) {
        e.printStackTrace();
    }

//        //create socket handler
//        mSocket = new SocketHandler();

    SocketThread = new Thread(new SocketThread());
    SocketThread.start();
//    btnSend.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            message = etMessage.getText().toString().trim();
//            if (!message.isEmpty()) {
//                new Thread(new SendThread(message)).start();
//            }
//        }
//    });

    //setup image view
    // New Code
//    wearcam_view = (ImageView)findViewById(R.id.imageView); //Assuming an ImgView is there in your layout activity_main

    //start a thread which send random data to the moverio every n seconds, this is for testing
    final Handler handler = new Handler();
    final int delay = 2000; // 2000 milliseconds == 2 second

    handler.postDelayed(new Runnable() {
        public void run() {
            count = count + 1;
            String message = "hello w0rLd!" + Integer.toString(count);
            new Thread(new SendThread(message)).start();
            handler.postDelayed(this, delay);
        }
    }, delay);

  }

  // Used to obtain the content view for this application. If you are extending this class, and
  // have a custom layout, override this method and return the custom layout.
  protected int getContentViewLayoutResId() {
    return R.layout.activity_main;
  }

  @Override
  protected void onResume() {
    super.onResume();
    converter =
        new ExternalTextureConverter(
            eglManager.getContext(),
            applicationInfo.metaData.getInt("converterNumBuffers", NUM_BUFFERS));
    converter.setFlipY(
        applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));
    converter.setConsumer(processor);
    if (PermissionHelper.cameraPermissionsGranted(this)) {
      startCamera();
    }
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

  protected void onCameraStarted(SurfaceTexture surfaceTexture) {
    previewFrameTexture = surfaceTexture;
    // Make the display view visible to start showing the preview. This triggers the
    // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
    previewDisplayView.setVisibility(View.VISIBLE);

    // onCameraStarted gets called each time the activity resumes, but we only want to do this once.
    if (!haveAddedSidePackets) {
      float focalLength = cameraHelper.getFocalLengthPixels();
      if (focalLength != Float.MIN_VALUE) {
        Packet focalLengthSidePacket = processor.getPacketCreator().createFloat32(focalLength);
        Map<String, Packet> inputSidePackets = new HashMap<>();
        inputSidePackets.put(FOCAL_LENGTH_STREAM_NAME, focalLengthSidePacket);
        processor.setInputSidePackets(inputSidePackets);
      }
      haveAddedSidePackets = true;
    }

  }

  protected Size cameraTargetResolution() {
    return null; // No preference and let the camera (helper) decide.
  }

  public void startCamera() {
    cameraHelper = new CameraXPreviewHelper();
    cameraHelper.setOnCameraStartedListener(
        surfaceTexture -> {
          onCameraStarted(surfaceTexture);
        });
    CameraHelper.CameraFacing cameraFacing =
        applicationInfo.metaData.getBoolean("cameraFacingFront", false)
            ? CameraHelper.CameraFacing.FRONT
            : CameraHelper.CameraFacing.BACK;
    cameraHelper.startCamera(
        this, cameraFacing, /*unusedSurfaceTexture=*/ null, cameraTargetResolution());
  }

  protected Size computeViewSize(int width, int height) {
    return new Size(width, height);
  }

  protected void onPreviewDisplaySurfaceChanged(
      SurfaceHolder holder, int format, int width, int height) {
    // (Re-)Compute the ideal size of the camera-preview display (the area that the
    // camera-preview frames get rendered onto, potentially with scaling and rotation)
    // based on the size of the SurfaceView that contains the display.
    Size viewSize = computeViewSize(width, height);
    Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
    boolean isCameraRotated = cameraHelper.isCameraRotated();

    // Connect the converter to the camera-preview frames as its input (via
    // previewFrameTexture), and configure the output width and height as the computed
    // display size.
    converter.setSurfaceTextureAndAttachToGLContext(
        previewFrameTexture,
        isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
        isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
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
                onPreviewDisplaySurfaceChanged(holder, format, width, height);
              }

              @Override
              public void surfaceDestroyed(SurfaceHolder holder) {
                processor.getVideoSurfaceOutput().setSurface(null);
              }
            });
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

  //mediapipe handles the AI processing of data, here we implement the hardcoded data processing
  private void processOutput(NormalizedLandmarkList landmarks){
      System.out.println("Received output from mediapipe graph");
//    List<NormalizedLandmark> rel = ; //landmarks for right eye
//    List<NormalizedLandmark> lel = ; //landmarks for right eye
    int leftEyeLocationStart = 469;
    int leftEyeLocationEnd = 473;
    int rightEyeLocationStart = 474;
    int rightEyeLocationEnd = 478;
    //we convert from NormalizedLandmarks into an array list of them. I'm sure there is a great way of doing this within mediapipe, but I am pretty lost to where getLandmarkList even comes from? Can't find it if I search the whole repo. I assume it's generated, as NormalizedLandmark is generated. Need a google C++ guy to explain I guess? Well, this will work for now -cayden
      List<NormalizedLandmark> lll = landmarks.getLandmarkList();
      List<NormalizedLandmark> leftEyeLandmarks = new ArrayList<NormalizedLandmark>();
      List<NormalizedLandmark> rightEyeLandmarks = new ArrayList<NormalizedLandmark>();
      int landmarkIndex = 0;
    for (NormalizedLandmark landmark : lll) {
        if ((landmarkIndex >= leftEyeLocationStart) && (landmarkIndex <= leftEyeLocationEnd)){
            leftEyeLandmarks.add(landmark);
        }
        else if ((landmarkIndex >= rightEyeLocationStart) && (landmarkIndex <= rightEyeLocationEnd)){
            rightEyeLandmarks.add(landmark);
        }
        landmarkIndex++;
    }
    
    processEyeContact(leftEyeLandmarks, rightEyeLandmarks);
  }

  private void processEyeContact(List<NormalizedLandmark> leftEyeLandmarks, List<NormalizedLandmark> rightEyeLandmarks){
      System.out.println("Processing eye contact from iris landmarks...");
    for (NormalizedLandmark landmark : leftEyeLandmarks) {
        System.out.println("LANDMARK: " + landmark.getX());
    }
  }

 private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    //socket exists on this thread, socket gets started and persists here
    class SocketThread implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        tvMessages.setText("Not connected");
//                        tvIP.setText("IP: " + SERVER_IP);
//                        tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
                    }
                });
                try {
                    socket = serverSocket.accept();
                    mConnectionState = true;
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new DataInputStream(new DataInputStream(socket.getInputStream()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            tvMessages.setText("Connected\n");
                        }
                    });
                    new Thread(new ReceiveThread()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //receives messages
    private class ReceiveThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                System.out.println("LISTENING FOR MESSAGES");
                try {
                    int length = input.readInt();                    // read length of incoming message
                    byte[] raw_data = new byte[length];
                    if(length>0) {
                        input.readFully(raw_data, 0, raw_data.length); // read the message
                    }

                    if (raw_data != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //display image
                                Bitmap bitmap = BitmapFactory.decodeByteArray(raw_data, 0, raw_data.length);
//                                Packet imagePacket = packetCreator.createRgbaImageFrame(yuv_converted_bimap);
//                                wearcam_view.setImageBitmap(bitmap); //Must.jpg present in any of your drawable folders.
                            }
                        });
                    } else {
                        SocketThread = new Thread(new SocketThread());
                        SocketThread.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    //this sends messages
    class SendThread implements Runnable {
        private String message;
        SendThread(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            if (mConnectionState) {
                output.write(message + "\n");
                output.flush();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        tvMessages.append("server: " + message + "\n");
//                        etMessage.setText("");
                    }
                });
            }
        }
        }

}
