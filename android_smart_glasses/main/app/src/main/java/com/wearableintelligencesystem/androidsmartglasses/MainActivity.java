package com.wearableintelligencesystem.androidsmartglasses;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.widget.Toast;

import com.example.wearableintelligencesystemandroidsmartglasses.BuildConfig;
import com.example.wearableintelligencesystemandroidsmartglasses.R;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity {
    WearableAiService mService;
    boolean mBound = false;

    public static String TAG = "WearableAiDisplay_MainActivity";

    public long lastFaceUpdateTime = 0;
    public long faceUpdateInterval = 5000; //milliseconds


    public final static String ACTION_UI_UPDATE = "com.example.wearableaidisplaymoverio.UI_UPDATE";
    public final static String PHONE_CONN_STATUS_UPDATE = "com.example.wearableaidisplaymoverio.PHONE_CONN_STATUS_UPDATE";
    public final static String WIFI_CONN_STATUS_UPDATE = "com.example.wearableaidisplaymoverio.WIFI_CONN_STATUS_UPDATE";
    public final static String BATTERY_CHARGING_STATUS_UPDATE = "com.example.wearableaidisplaymoverio.BATTERY_CHARGIN_STATUS_UPDATE";
    public final static String BATTERY_LEVEL_STATUS_UPDATE = "com.example.wearableaidisplaymoverio.BATTERY_LEVEL_STATUS_UPDATE";

    //current person recognized's names
    public ArrayList<String> faceNames = new ArrayList<String>();

    //store information from visual search response
    List<String> thumbnailImages;
    List<String> visualSearchNames;

    //visual search gridview ui
    GridView gridviewImages;
    ImageAdapter gridViewImageAdapter;

    //social UI
    TextView messageTextView;
    TextView eyeContactMetricTextView;
    TextView eyeContact5MetricTextView;
    TextView eyeContact30MetricTextView;
    TextView facialEmotionMetricTextView;
    TextView facialEmotion5MetricTextView;
    TextView facialEmotion30MetricTextView;
    Button toggleCameraButton;
    private PieChart chart;

    //live life captions ui
    ArrayList<Spanned> textHolder = new ArrayList<>();
    int textHolderSizeLimit = 10; // how many lines maximum in the text holder
    TextView liveLifeCaptionsText;

    //referenceCard ui
    TextView referenceCardResultTitle;
    TextView referenceCardResultSummary;
    ImageView referenceCardResultImage;

    //translate ui
    ArrayList<Spanned> translateTextHolder = new ArrayList<>();
    int translateTextHolderSizeLimit = 10; // how many lines maximum in the text holder
    TextView translateText;

    //visual search ui
    ImageView viewfinder;

    //text list ui
    ArrayList<String> textListHolder = new ArrayList<>();
    TextView textListView;

    //text list ui
    private String textBlockHolder = "";
    TextView textBlockView;

    //metrics
    float eye_contact_30 = 0;
    String facial_emotion_30 = "";
    String facial_emotion_5 = "";

    //save current mode
    String curr_mode;

    long lastIntermediateMillis = 0;
    long intermediateTranscriptPeriod = 40; //milliseconds

    //HUD ui
    private TextView mClockTextView;
    private boolean clockRunning = false;
    private ImageView mWifiStatusImageView;
    private ImageView mPhoneStatusImageView;
    private ImageView mBatteryStatusImageView;

    //device status
    private TextView mBatteryStatusTextView;
    boolean phoneConnected = false;
    boolean wifiConnected;
    boolean batteryFull;
    float batteryLevel;
    boolean batteryCharging = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate running");
        //help us find potential issues in dev
        if (BuildConfig.DEBUG){
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        super.onCreate(savedInstanceState);

        //setup main view
        switchMode("llc");

        //setup the HUD ui
        setupHud();

        //keep the screen on throughout
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //create the WearableAI service if it isn't already running
        startService(new Intent(this, WearableAiService.class));
        bindWearableAiService();
        Log.d(TAG, "onCreate complete");
    }

    private void setupHud(){
        Log.d(TAG, "setupHud running");
        //setup wifi status
//        updateWifiHud();
//
        //setup phone connect status
        updatePhoneHud();
//
//        //setup battery connect status
//        updateBatteryHud();

        //setup clock
        if (! clockRunning) {
            clockRunning = true;
            Handler handler = new Handler();
            handler.post(new Runnable() {
                public void run() {
                    // Default time format for current locale, with respect (on API 22+) to user's 12/24-hour
                    // settings. I couldn't find any simple way to respect it back to API 14.
                    Log.d(TAG, "Setting time...");
                    //String prettyTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()) + "-" + DateFormat.getDateInstance(DateFormat.SHORT).format(new Date());
                    String prettyTime = new SimpleDateFormat("kk:mm").format(new Date())  + "-" + DateFormat.getDateInstance(DateFormat.SHORT).format(new Date());
                    mClockTextView = (TextView) findViewById(R.id.clock_text_view);
                    if (mClockTextView != null) {
                        mClockTextView.setText(prettyTime);
                    }
                    Log.d(TAG, "Time set.");
                    handler.postDelayed(this, 1000);
                }
            });
        }
        Log.d(TAG, "setupHud complete");
    }

    private void updateWifiHud(){
        Log.d(TAG, "updateWifiHud start...");
        mWifiStatusImageView = (ImageView) findViewById(R.id.wifi_image_view);

        if (mWifiStatusImageView == null){
            return;
        }
        //wifiConnected = WifiUtils.checkWifiOnAndConnected(this); //check wifi status - don't need now that we have listener in service
        Drawable wifiOnDrawable = this.getDrawable(R.drawable.wifi_on_green);
        Drawable wifiOffDrawable = this.getDrawable(R.drawable.wifi_off_red);
        if (wifiConnected) {
            mWifiStatusImageView.setImageDrawable(wifiOnDrawable);
        } else {
            mWifiStatusImageView.setImageDrawable(wifiOffDrawable);
        }
        Log.d(TAG, "updateWifiHud complete.");
    }

    private void updatePhoneHud(){
        Log.d(TAG, "updatePhoneHud start...");
        mPhoneStatusImageView = (ImageView) findViewById(R.id.phone_status_image_view);
        if (mPhoneStatusImageView == null){
            return;
        }
        Drawable phoneOnDrawable = this.getDrawable(R.drawable.phone_connected_green);
        Drawable phoneOffDrawable = this.getDrawable(R.drawable.phone_disconnected_red);
        if (phoneConnected) {
            mPhoneStatusImageView.setImageDrawable(phoneOnDrawable);
        } else {
            mPhoneStatusImageView.setImageDrawable(phoneOffDrawable);
        }
        Log.d(TAG, "updatePhoneHud complete.");
    }

    private void updateBatteryHud(){
        Log.d(TAG, "updateBatteryHud start.");
        //set the icon
        mBatteryStatusImageView = (ImageView) findViewById(R.id.battery_status_image_view);
        if (mBatteryStatusImageView == null){
            return;
        }
        Drawable batteryFullDrawable = this.getDrawable(R.drawable.full_battery_green);
        Drawable batteryFullChargingDrawable = this.getDrawable(R.drawable.full_battery_charging_green);
        Drawable batteryLowDrawable = this.getDrawable(R.drawable.low_battery_red);
        Drawable batteryLowChargingDrawable = this.getDrawable(R.drawable.low_battery_charging_red);
        if (batteryFull) {
            if (batteryCharging) {
                mBatteryStatusImageView.setImageDrawable(batteryFullChargingDrawable);
            } else {
                mBatteryStatusImageView.setImageDrawable(batteryFullDrawable);
            }
        } else {
            mBatteryStatusImageView.setImageDrawable(batteryLowDrawable);
            if (batteryCharging) {
                mBatteryStatusImageView.setImageDrawable(batteryLowChargingDrawable);
            } else {
                mBatteryStatusImageView.setImageDrawable(batteryFullDrawable);
            }
        }

        //set the text
        mBatteryStatusTextView = (TextView) findViewById(R.id.battery_percentage_text_view);
        mBatteryStatusTextView.setText((int)batteryLevel + "%");

        Log.d(TAG, "updateBatteryHud complete.");
    }

    private void switchMode(String mode) {
        Log.d(TAG, "SWITCH MODE RUNNING WITH NEW MODE: " + mode);
        curr_mode = mode;
        switch (mode) {
            case "social":
                setupSocialIntelligenceUi();
                break;
            case "llc":
                setupLlcUi();
                break;
            case "blank":
                blankUi();
                break;
            case "translate":
                setupTranslateUi();
                break;
            case "visualsearchviewfind":
                setupVisualSearchViewfinder();
                break;
            case "visualsearchgridview":
                setContentView(R.layout.image_gridview);
                break;
            case "textlist":
                setupTextList();
                break;
            case "textblock":
                setupTextBlock();
                break;
            case "wearablefacerecognizer":
                showWearableFaceRecognizer();
                break;
        }
        //registerReceiver(mComputeUpdateReceiver, makeComputeUpdateIntentFilter());
        Log.d(TAG, "SWITCH MODE COMPLETE");
    }

    private void showWearableFaceRecognizer(){
        setContentView(R.layout.wearable_face_recognizer);
        TextView faceRecTitle = findViewById(R.id.face_rec_title);
        faceRecTitle.setText("Face detected!\nName: " + faceNames.get(0));

//        //for now, show for n seconds and then return to llc
//        int show_time = 3000; //milliseconds
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                setupLlcUi();
//            }
//        }, show_time);
    }

    private void setupVisualSearchViewfinder() {
        //live life captions mode gui setup
        setContentView(R.layout.viewfinder);
        viewfinder = (ImageView) findViewById(R.id.camera_viewfinder);
//        if (mBound) {
//            byte[] curr_cam_image = mService.getCurrentCameraImage();
//            Bitmap curr_cam_bmp = BitmapFactory.decodeByteArray(curr_cam_image, 0, curr_cam_image.length);
//            viewfinder.setImageBitmap(curr_cam_bmp);
//        } else {
//            Log.d(TAG, "Mboudn not true yo");
//        }

        updateViewFindFrame();
    }

    //generic way to set the current enumerated list of strings and display them, scrollably, on the main UI
    private void setupTextList() {
        //live life captions mode gui setup
        setContentView(R.layout.text_list);

        //setup the text list view
        textListView = (TextView) findViewById(R.id.text_list);
        textListView.setText("");
        for (int i = 0; i < textListHolder.size(); i++) {
            textListView.append(Integer.toString(i+1) + ": " + textListHolder.get(i) + "\n");
        }
        textListView.setPadding(10, 0, 10, 0);
        textListView.setMovementMethod(new ScrollingMovementMethod());
        textListView.setSelected(true);
    }

    //generic way to set the current single string, scrollably, on the main UI
    private void setupTextBlock() {
        //live life captions mode gui setup
        setContentView(R.layout.text_block);

        //setup the text list view
        textBlockView = (TextView) findViewById(R.id.text_block);
        textBlockView.setText(textBlockHolder);
        textBlockView.setPadding(10, 0, 10, 0);
        textBlockView.setMovementMethod(new ScrollingMovementMethod());
        textBlockView.setSelected(true);
    }

    private void updateViewFindFrame(){
        if (mBound) {
            byte[] curr_cam_image = mService.getCurrentCameraImage();
            Bitmap curr_cam_bmp = BitmapFactory.decodeByteArray(curr_cam_image, 0, curr_cam_image.length);
            viewfinder.setImageBitmap(curr_cam_bmp);
        } else {
            Log.d(TAG, "Mboudn not true yo");
        }

        //update the current preview frame in
        //for now, show for n seconds and then return to llc
        int frame_delay = 50; //milliseconds
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (curr_mode.equals("visualsearchviewfind")) {
                    updateViewFindFrame();
                }
            }
        }, frame_delay);
    }

    private void setupLlcUi() {
        //live life captions mode gui setup
        setContentView(R.layout.live_life_caption_text);
        liveLifeCaptionsText = (TextView) findViewById(R.id.livelifecaptionstextview);
        liveLifeCaptionsText.setMovementMethod(new ScrollingMovementMethod());
        liveLifeCaptionsText.setText(getCurrentTranscriptScrollText());

        liveLifeCaptionsText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scrollToBottom(liveLifeCaptionsText);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        liveLifeCaptionsText.setText(getCurrentTranscriptScrollText());
        scrollToBottom(liveLifeCaptionsText);

        if (mBound){
            mService.requestUiUpdate();
        }
    }

    private void setupTranslateUi() {
        //live life captions mode gui setup
        setContentView(R.layout.translate_mode_view);
        translateText = (TextView) findViewById(R.id.translatetextview);
        translateText.setMovementMethod(new ScrollingMovementMethod());
        translateText.setText(getCurrentTranscriptScrollText());

        translateText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scrollToBottom(translateText);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        translateText.setText(getCurrentTranslateScrollText());
        scrollToBottom(translateText);
    }

    private void setupSocialIntelligenceUi() {
        //social mode ui setup
        setContentView(R.layout.social_intelligence_activity);
        messageTextView = (TextView) findViewById(R.id.message);
        eyeContactMetricTextView = (TextView) findViewById(R.id.eye_contact_metric);
        eyeContact5MetricTextView = (TextView) findViewById(R.id.eye_contact_metric_5);
        eyeContact30MetricTextView = (TextView) findViewById(R.id.eye_contact_metric_30);
        facialEmotionMetricTextView = (TextView) findViewById(R.id.facial_emotion_metric);
        facialEmotion5MetricTextView = (TextView) findViewById(R.id.facial_emotion_metric_5);
        facialEmotion30MetricTextView = (TextView) findViewById(R.id.facial_emotion_metric_30);

        //handle chart
        chart = findViewById(R.id.stress_confidence_chart);
        chart.setBackgroundColor(Color.BLACK);
        moveOffScreen(); //not sure what this does really
        setupChart();
    }


    private void blankUi() {
        setContentView(R.layout.blank_screen);
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(mComputeUpdateReceiver, makeComputeUpdateIntentFilter());

        bindWearableAiService();

        if (curr_mode == "llc"){
            scrollToBottom(liveLifeCaptionsText);
        }

        if (curr_mode == "translate"){
            scrollToBottom(translateText);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        unbindWearableAiService();

        //unregister receiver
        unregisterReceiver(mComputeUpdateReceiver);
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return (result);
    }

    //
//    private void initPreview(int width, int height) {
//        if (camera != null && previewHolder.getSurface() != null) {
//            try {
//                camera.setPreviewDisplay(previewHolder);
//            } catch (Throwable t) {
//                Log.e("PreviewDemo",
//                        "Exception in setPreviewDisplay()", t);
//                Toast
//                        .makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG)
//                        .show();
//            }
//
//            if (!cameraConfigured) {
//                Camera.Parameters parameters = camera.getParameters();
//                parameters.setRecordingHint(true);
//                Camera.Size size = getBestPreviewSize(width, height,
//                        parameters);
//
//                if (size != null) {
//                    parameters.setPreviewSize(size.width, size.height);
//                    camera.setParameters(parameters);
//                    cameraConfigured = true;
//                }
//            }
//        }
//    }
//
//    private void startPreview() {
//        if (cameraConfigured && camera != null) {
//            camera.startPreview();
//            inPreview = true;
//        }
//    }
//
//    private void takeAndSendPicture() {
//        System.out.println("TAKE AND SEND PICTURE CALLED");
//        camera.takePicture(null, null, null, new PhotoHandler(getApplicationContext()));
//        startPreview();
//    }
//
//    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
//        public void surfaceCreated(SurfaceHolder holder) {
//            // no-op -- wait until surfaceChanged()
//        }
//
//        public void surfaceChanged(SurfaceHolder holder,
//                                   int format, int width,
//                                   int height) {
//            initPreview(width, height);
//            startPreview();
//        }
//
//        public void surfaceDestroyed(SurfaceHolder holder) {
//            // no-op
//        }
//    };
//
    public void setGuiMessage(String message, TextView tv, String postfix) {
        //see if the message is generic or one of the metrics to be displayed
        messageTextView.setText("");
        tv.setText(message + postfix);
    }

    public void receiveFacialEmotionMessage(String message) {
        //see if the message is generic or one of the metrics to be displayed
        messageTextView.setText("");
        facialEmotionMetricTextView.setText(message);
    }

    private static IntentFilter makeComputeUpdateIntentFilter() {
        Log.d(TAG, "makeComputeUpdateIntentFilter running");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ASPClientSocket.ACTION_RECEIVE_MESSAGE);
        intentFilter.addAction(GlboxClientSocket.ACTION_RECEIVE_TEXT);
        intentFilter.addAction(GlboxClientSocket.COMMAND_SWITCH_MODE);
        intentFilter.addAction(GlboxClientSocket.ACTION_WIKIPEDIA_RESULT);
        intentFilter.addAction(GlboxClientSocket.ACTION_TRANSLATION_RESULT);
        intentFilter.addAction(GlboxClientSocket.ACTION_VISUAL_SEARCH_RESULT);
        intentFilter.addAction(GlboxClientSocket.ACTION_AFFECTIVE_SUMMARY_RESULT);
        intentFilter.addAction(MessageTypes.FACE_SIGHTING_EVENT);

        intentFilter.addAction(ASPClientSocket.ACTION_AFFECTIVE_MEM_TRANSCRIPT_LIST);
        intentFilter.addAction(ASPClientSocket.ACTION_AFFECTIVE_SEARCH_QUERY);

        intentFilter.addAction(ACTION_UI_UPDATE);
        intentFilter.addAction(ASPClientSocket.ACTION_UI_DATA);

        Log.d(TAG, "makeComputeUpdateIntentFilter retruning");
        return intentFilter;
    }

    private final BroadcastReceiver mComputeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "GOT BROADCAST");
            final String action = intent.getAction();
            Log.d(TAG, action);
            if (ASPClientSocket.ACTION_UI_DATA.equals(action)) {
                Log.d(TAG, "found something search engine");
                try {
                    JSONObject data = new JSONObject(intent.getStringExtra(ASPClientSocket.RAW_MESSAGE_JSON_STRING));
                    String typeOf = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
                    //run visual search results
                    if (typeOf.equals(MessageTypes.VISUAL_SEARCH_RESULT)){
                        String payloadString = data.getString("payload");
                        showVisualSearchResults(new JSONArray(payloadString));
                    } else if (typeOf.equals(MessageTypes.SEARCH_ENGINE_RESULT)){
                        showSearchEngineResults(data);
                    }
                } catch(JSONException e){
                    e.printStackTrace();
                }
            } else if (ACTION_UI_UPDATE.equals(action)) {
                Log.d(TAG, "GOT ACTION_UI_UPDATE");
                if (intent.hasExtra(PHONE_CONN_STATUS_UPDATE)) {
                    phoneConnected = intent.getBooleanExtra(PHONE_CONN_STATUS_UPDATE, false);
                    updatePhoneHud();
                }
                if (intent.hasExtra(BATTERY_CHARGING_STATUS_UPDATE)){
                    batteryCharging = intent.getBooleanExtra(BATTERY_CHARGING_STATUS_UPDATE, false);
                    updateBatteryHud();
                }
                if (intent.hasExtra(BATTERY_LEVEL_STATUS_UPDATE)){
                    batteryLevel = intent.getFloatExtra(BATTERY_LEVEL_STATUS_UPDATE, 100f);
                    if (batteryLevel > 40f) {
                        batteryFull = true;
                    } else {
                        batteryFull = false;
                    }
                    updateBatteryHud();
                }
                if (intent.hasExtra(WIFI_CONN_STATUS_UPDATE)){
                    wifiConnected = intent.getBooleanExtra(WIFI_CONN_STATUS_UPDATE, false);
                    updateWifiHud();
                }
        } else if (curr_mode.equals("social") && ASPClientSocket.ACTION_RECEIVE_MESSAGE.equals(action)) {
                if (intent.hasExtra(ASPClientSocket.EYE_CONTACT_5_MESSAGE)) {
                    String message = intent.getStringExtra(ASPClientSocket.EYE_CONTACT_5_MESSAGE);
                    setGuiMessage(message, eyeContact5MetricTextView, "%");
                } else if (intent.hasExtra(ASPClientSocket.EYE_CONTACT_30_MESSAGE)) {
                    String message = intent.getStringExtra(ASPClientSocket.EYE_CONTACT_30_MESSAGE);
                    setGuiMessage(message, eyeContact30MetricTextView, "%");
                    eye_contact_30 = Float.parseFloat(message);
                    setChartData();
                } else if (intent.hasExtra(ASPClientSocket.EYE_CONTACT_300_MESSAGE)) {
                    String message = intent.getStringExtra(ASPClientSocket.EYE_CONTACT_300_MESSAGE);
                    setGuiMessage(message, eyeContactMetricTextView, "%");
                } else if (intent.hasExtra(ASPClientSocket.FACIAL_EMOTION_5_MESSAGE)) {
                    String message = intent.getStringExtra(ASPClientSocket.FACIAL_EMOTION_5_MESSAGE);
                    setGuiMessage(message, facialEmotion5MetricTextView, "");
                    facial_emotion_5 = message;
                } else if (intent.hasExtra(ASPClientSocket.FACIAL_EMOTION_30_MESSAGE)) {
                    String message = intent.getStringExtra(ASPClientSocket.FACIAL_EMOTION_30_MESSAGE);
                    setGuiMessage(message, facialEmotion30MetricTextView, "");
                    facial_emotion_30 = message;
                } else if (intent.hasExtra(ASPClientSocket.FACIAL_EMOTION_300_MESSAGE)) {
                    String message = intent.getStringExtra(ASPClientSocket.FACIAL_EMOTION_300_MESSAGE);
                    setGuiMessage(message, facialEmotionMetricTextView, "");
                }
            } else if (GlboxClientSocket.ACTION_RECEIVE_TEXT.equals(action)) {
                if (intent.hasExtra(GlboxClientSocket.FINAL_REGULAR_TRANSCRIPT)) {
                    Log.d(TAG, "got final transcript");
                    try {
                        JSONObject transcript_object = new JSONObject(intent.getStringExtra(GlboxClientSocket.FINAL_REGULAR_TRANSCRIPT));
//                        JSONObject nlp = transcript_object.getJSONObject("nlp");
//                        JSONArray nouns = nlp.getJSONArray("nouns");
                        JSONArray nouns = new JSONArray(); //for now, since we haven't implemented NLP on ASP, we just make this empty
                        String transcript = transcript_object.getString(MessageTypes.TRANSCRIPT_TEXT);
                        if ((nouns.length() == 0)) {
                            textHolder.add(Html.fromHtml("<p>" + transcript.trim() + "</p>"));
                        } else {
                            //add text to a string and highlight properly the things we want to highlight (e.g. proper nouns)
                            String textBuilder = "<p>";
                            int prev_end = 0;
                            for (int i = 0; i < nouns.length(); i++) {
                                String noun = nouns.getJSONObject(i).getString("noun");
                                int start = nouns.getJSONObject(i).getInt("start");
                                int end = nouns.getJSONObject(i).getInt("end");

                                //dont' drop the words before the first noun
                                if (i == 0) {
                                    textBuilder = textBuilder + transcript.substring(0, start);
                                } else { //add in the transcript between previous noun and this one
                                    textBuilder = textBuilder + transcript.substring(prev_end, start);
                                }

                                //add in current colored noun
                                textBuilder = textBuilder + "<font color='#00FF00'><u>" + transcript.substring(start, end) + "</u></font>";

                                //add in end of transcript if we just added the last noun
                                if (i == (nouns.length() - 1)) {
                                    textBuilder = textBuilder + transcript.substring(end);
                                }

                                //set this noun end to be the previous noun end for next loop
                                prev_end = end;
                            }
                            textBuilder = textBuilder + "</p>";
                            textHolder.add(Html.fromHtml(textBuilder));
                        }

                        if (curr_mode.equals("llc")) {
                            liveLifeCaptionsText.setText(getCurrentTranscriptScrollText());
                        }
                    } catch (JSONException e) {
                        System.out.println(e);
                    }
                } else if (intent.hasExtra(GlboxClientSocket.INTERMEDIATE_REGULAR_TRANSCRIPT)) {
                    Log.d(TAG, "got final transcript");
                    //only update this transcript if it's been n milliseconds since the last intermediate update
                    if ((System.currentTimeMillis() - lastIntermediateMillis) > intermediateTranscriptPeriod) {
                        lastIntermediateMillis = System.currentTimeMillis();
                        String intermediate_transcript = intent.getStringExtra(GlboxClientSocket.INTERMEDIATE_REGULAR_TRANSCRIPT);
                        if (curr_mode.equals("llc")) {
                            liveLifeCaptionsText.setText(TextUtils.concat(getCurrentTranscriptScrollText(), Html.fromHtml("<p>" + intermediate_transcript.trim() + "</p>")));
                        }
                    }
                } else if (intent.hasExtra(GlboxClientSocket.COMMAND_RESPONSE)) {
                    String command_response_text = intent.getStringExtra(GlboxClientSocket.COMMAND_RESPONSE);
                    Log.d(TAG, command_response_text);
                    //change newlines to <br/>
                    command_response_text = command_response_text.replaceAll("\n", "<br/>");
                    textHolder.add(Html.fromHtml("<p><font color='#00CC00'>" + command_response_text.trim() + "</font></p>"));
                    if (curr_mode.equals("llc")) {
                        liveLifeCaptionsText.setText(getCurrentTranscriptScrollText());
                    }
                }
            } else if (GlboxClientSocket.COMMAND_SWITCH_MODE.equals(action)) {
                switchMode(intent.getStringExtra(GlboxClientSocket.COMMAND_ARG));
            } else if (GlboxClientSocket.ACTION_WIKIPEDIA_RESULT.equals(action)) {
                try {
                    //change the view to the wikipeida results page
                    setContentView(R.layout.reference_card);
                    referenceCardResultTitle = (TextView) findViewById(R.id.reference_card_result_title);
                    referenceCardResultSummary = (TextView) findViewById(R.id.reference_card_result_summary);
                    referenceCardResultImage = (ImageView) findViewById(R.id.reference_card_result_image);

                    //get content
                    JSONObject reference_card_object = new JSONObject(intent.getStringExtra(GlboxClientSocket.WIKIPEDIA_RESULT));
                    String title = reference_card_object.getString("title");
                    String summary = reference_card_object.getString("summary");
                    String img_path = reference_card_object.getString("image_path");

                    //set the text
                    referenceCardResultTitle.setText(title);
                    referenceCardResultSummary.setText(summary);
                    referenceCardResultSummary.setMovementMethod(new ScrollingMovementMethod());
                    referenceCardResultSummary.setSelected(true);

                    //open the image and display
                    File imgFile = new File(img_path);
                    if (imgFile.exists()) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        referenceCardResultImage.setImageBitmap(myBitmap);
                    }

                    //for now, show for n seconds and then return to llc
                    int wiki_show_time = 8000; //milliseconds
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            setupLlcUi();
                        }
                    }, wiki_show_time);

                } catch (JSONException e) {
                    Log.d(TAG, e.toString());
                }
            } else if (ASPClientSocket.ACTION_AFFECTIVE_MEM_TRANSCRIPT_LIST.equals(action)) {
                Log.d(TAG, "MainActivity got affective mem transcript list");
                try {
                    JSONObject affective_mem = new JSONObject(intent.getStringExtra(ASPClientSocket.AFFECTIVE_MEM_TRANSCRIPT_LIST));
                    textListHolder.clear();
                    int i = 0;
                    while (true) {
                        if (!affective_mem.has(Integer.toString(i))) {
                            break;
                        }
                        String transcript = affective_mem.getString(Integer.toString(i));
                        textListHolder.add(transcript);
                        Log.d(TAG, "MainActivity Affective mem #" + i + " is " + transcript);
                        i++;
                    }
                    switchMode("textlist");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (ASPClientSocket.ACTION_AFFECTIVE_SEARCH_QUERY.equals(action)) {
                Log.d(TAG, "MainActivity got affective mem transcript list");
                try {
                    JSONObject affective_query_result = new JSONObject(intent.getStringExtra(ASPClientSocket.AFFECTIVE_SEARCH_QUERY_RESULT));
                    textBlockHolder = "";
                    textBlockHolder = "Most " + affective_query_result.getString("emotion") + " of last conversation: \n\n" + affective_query_result.getString("phrase");
                    switchMode("textblock");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (GlboxClientSocket.ACTION_TRANSLATION_RESULT.equals(action)) {
                String translation_result_text = intent.getStringExtra(GlboxClientSocket.TRANSLATION_RESULT);
                //textHolder.add(Html.fromHtml("<p><font color='#EE0000'>TRANSLATION: " + translation_result_text + "</font></p>"));

                translateTextHolder.add(Html.fromHtml("<p>" + translation_result_text + "</p>"));

                if (curr_mode.equals("translate")) {
                    translateText.setText(getCurrentTranslateScrollText());
                }
            } else if (GlboxClientSocket.ACTION_VISUAL_SEARCH_RESULT.equals(action)) {

            } else if (MessageTypes.FACE_SIGHTING_EVENT.equals(action)) {
                String currName = intent.getStringExtra(MessageTypes.FACE_NAME);
                faceNames.clear();
                faceNames.add(currName);
                long timeSinceLastFaceUpdate = System.currentTimeMillis() - lastFaceUpdateTime;
                if (timeSinceLastFaceUpdate > faceUpdateInterval){
                    Toast.makeText(MainActivity.this, "Saw: " + currName, Toast.LENGTH_SHORT).show();
                    lastFaceUpdateTime = System.currentTimeMillis();
                }
                //switchMode("wearablefacerecognizer");
            } else if (GlboxClientSocket.ACTION_AFFECTIVE_SUMMARY_RESULT.equals(action)) {
                String str_data = intent.getStringExtra(GlboxClientSocket.AFFECTIVE_SUMMARY_RESULT);
                textBlockHolder = str_data;
                switchMode("textblock");
            }
            Log.d(TAG, "Done BROADCAST");
        }
    };


    private Spanned getCurrentTranscriptScrollText() {
        Log.d(TAG, "getCurrentTranscriptScorllText running");
        Spanned current_transcript_scroll = Html.fromHtml("<div></div>");
        //limit textHolder to our maximum size
        while ((textHolder.size() - textHolderSizeLimit) > 0){
            textHolder.remove(0);
        }
        for (int i = 0; i < textHolder.size(); i++) {
            //current_transcript_scroll = current_transcript_scroll + textHolder.get(i) + "\n" + "\n";
            if (i == 0) {
                current_transcript_scroll = textHolder.get(i);
            } else {
                current_transcript_scroll = (Spanned) TextUtils.concat(current_transcript_scroll, textHolder.get(i));
            }
        }
        Log.d(TAG, "getCurrentTranscriptScorllText returning");
        return current_transcript_scroll;
    }

    private Spanned getCurrentTranslateScrollText() {
        Spanned current_translate_scroll = Html.fromHtml("<div></div>");
        //limit textHolder to our maximum size
        while ((translateTextHolder.size() - translateTextHolderSizeLimit) > 0){
            translateTextHolder.remove(0);
        }
        for (int i = 0; i < translateTextHolder.size(); i++) {
            //current_transcript_scroll = current_transcript_scroll + textHolder.get(i) + "\n" + "\n";
            if (i == 0) {
                current_translate_scroll = translateTextHolder.get(i);
            } else {
                current_translate_scroll = (Spanned) TextUtils.concat(current_translate_scroll, translateTextHolder.get(i));
            }
        }
        return current_translate_scroll;
    }

    //stuff for the charts
    private void setChartData() {
        Log.d(TAG, "setChartData running");

        float max = 100;
        ArrayList<PieEntry> values = new ArrayList<>();

        //temporary method of deducing stress
        float input = (max - eye_contact_30);
        if ((facial_emotion_30 == "Happy") || (facial_emotion_5 == "Happy")) {
            input = Math.max(0, input - 20);
        }

//        for (int i = 0; i < count; i++) {
//            values.add(new PieEntry((float) ((Math.random() * range) + range / 5), "Stress"));
//        }
        values.add(new PieEntry((float) (input), ""));
        values.add(new PieEntry((float) (max - input), ""));

        PieDataSet dataSet = new PieDataSet(values, "Election Results");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setDrawValues(false);

        //dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setColors(Color.RED, Color.BLACK);
        //dataSet.setSelectionShift(0f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(24f);
        data.setValueTextColor(Color.BLACK);
        //data.setValueTypeface(tfLight);
        chart.setData(data);
        //chart.animateY(1400, Easing.EaseInOutQuad);

        chart.invalidate();
        Log.d(TAG, "setChartData complete");
    }

    private void moveOffScreen() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int height = displayMetrics.heightPixels;

        int offset = (int) (height * 0.65); /* percent to move */

        RelativeLayout.LayoutParams rlParams =
                (RelativeLayout.LayoutParams) chart.getLayoutParams();
        rlParams.setMargins(0, 0, 0, -offset);
        chart.setLayoutParams(rlParams);
    }

    private void setupChart() {
        chart.setUsePercentValues(false);
        chart.getDescription().setEnabled(false);

        //chart.setCenterTextTypeface(tfLight);

        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.BLACK);

        chart.setTransparentCircleColor(Color.BLACK);
        chart.setTransparentCircleAlpha(110);

        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(61f);

        chart.setDrawCenterText(true);

        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(false);

        chart.setMaxAngle(180f); // HALF CHART
        chart.setRotationAngle(180f);
        chart.setCenterTextOffset(0, -20);
        chart.setCenterTextColor(Color.WHITE);
        chart.setCenterTextSize(28);
        chart.setCenterText("Stress");

        setChartData();

        chart.animateY(1400, Easing.EaseInOutQuad);

//        Legend l = chart.getLegend();
//        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
//        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
//        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
//        l.setDrawInside(false);
//        l.setXEntrySpace(7f);
//        l.setYEntrySpace(0f);
//        l.setYOffset(0f);
//
//        // entry label styling
        chart.setEntryLabelColor(Color.BLACK);
        //chart.setEntryLabelTypeface(tfRegular);
        chart.setEntryLabelTextSize(19f);
    }

    private void scrollToBottom(TextView tv) {
        tv.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "scrollToBottom runnable running");
                int lc = tv.getLineCount();
                if (lc == 0){
                    return;
                }
                tv.scrollTo(0, tv.getBottom());
                int scrollAmount = tv.getLayout().getLineTop(lc) - tv.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    tv.scrollTo(0, scrollAmount);
                else
                    tv.scrollTo(0, 0);
                Log.d(TAG, "scrollToBottom runnable complete");
            }
        });

    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d(TAG, "onServiceConnected running");
            WearableAiService.LocalBinder binder = (WearableAiService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            //get update of ui information
            mService.requestUiUpdate();
            Log.d(TAG, "onServiceConnected complete");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };



    public void captureVisualSearchImage(){
        Log.d(TAG, "sending visual search image");

        Toast.makeText(MainActivity.this, "Capturing image...", Toast.LENGTH_SHORT).show();

        if (mBound) {
            //byte[] curr_cam_image = mService.getCurrentCameraImage();
            mService.sendGlBoxCurrImage();
        }
        Log.d(TAG, "visual search image has been sent");
    }

    private void selectVisualSearchResult(){
        Log.d(TAG, "select visual search result called");
        int pos = gridviewImages.getSelectedItemPosition();
        String name = visualSearchNames.get(pos);
        Toast.makeText(MainActivity.this, name, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "got keycode");
        Log.d(TAG, Integer.toString(keyCode));
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                Log.d(TAG, "keycode _ enter felt");
                if (curr_mode.equals("visualsearchviewfind")){
                    captureVisualSearchImage();
                    return true;
                } else if (curr_mode.equals("visualsearchgridview")){
                    selectVisualSearchResult();
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            case KeyEvent.KEYCODE_DEL:
                if (!curr_mode.equals("llc")) {
                    switchMode("llc");
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    // Note  from authour - From what I've seen you don't need the wake-lock or wifi-lock below for the audio-recorder to persist through screen-off.
    // However, to be on the safe side you might want to activate them anyway. (and/or if you have other functions that need them)
    private PowerManager.WakeLock wakeLock_partial = null;
    public void StartPartialWakeLock() {
        if (wakeLock_partial != null && wakeLock_partial.isHeld()) return;
        Log.i("vmain", "Starting partial wake-lock.");
        final PowerManager pm = (PowerManager) this.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock_partial = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.myapp:partial_wake_lock");
        wakeLock_partial.acquire();
    }
    public void StopPartialWakeLock() {
        if (wakeLock_partial != null && wakeLock_partial.isHeld()) {
            Log.i("vmain", "Stopping partial wake-lock.");
            wakeLock_partial.release();
        }
    }

    private WifiManager.WifiLock wifiLock = null;
    public void StartWifiLock() {
        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        wifiLock.acquire();
    }
    public void StopWifiLock() {
        wifiLock.release();
    }

    public void bindWearableAiService(){
        // Bind to that service
        Log.d(TAG, "Binding to WAI service.");
        Intent intent = new Intent(this, WearableAiService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Bound to WAI service.");
    }

    public void unbindWearableAiService() {
        // Bind to that service
        Log.d(TAG, "Unbinding to WAI service.");
        unbindService(connection);
        Log.d(TAG, "Unbound to WAI service.");
    }

    public void showVisualSearchResults(JSONArray data){
        Log.d(TAG, "received visual search image results");
        thumbnailImages = new ArrayList<String>();
        visualSearchNames = new ArrayList<String>();
        try {
            for (int i = 0; i < data.length(); i++) {
                //get thumnail image urls
                String thumbnailUrl = data.getJSONObject(i).getString("thumbnailUrl");
                thumbnailImages.add(thumbnailUrl);

                //get names of items
                String name = data.getJSONObject(i).getString("name");
                visualSearchNames.add(name);
            }

        } catch (JSONException e) {
            Log.d(TAG, e.toString());
        }
        //setup gridview to view grid of visual search images
        switchMode("visualsearchgridview");
        gridviewImages = (GridView) findViewById(R.id.gridview);
        gridViewImageAdapter = new ImageAdapter(this);
        String[] simpleThumbArray = new String[thumbnailImages.size()];
        thumbnailImages.toArray(simpleThumbArray);
        gridViewImageAdapter.imageTotal = simpleThumbArray.length;
        gridViewImageAdapter.mThumbIds = simpleThumbArray;
        gridviewImages.setDrawSelectorOnTop(false);
        gridviewImages.setSelector(R.drawable.selector_image_gridview);
        gridviewImages.setAdapter(gridViewImageAdapter);
        gridviewImages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Log.d(TAG, "Selected position: " + position);
                selectVisualSearchResult();
                gridViewImageAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showSearchEngineResults(JSONObject data) {
        Log.d(TAG, "showSearchEngineResults");
        try {
            //parse out the response object - one result for now
            JSONObject search_engine_result = new JSONObject(data.getString(MessageTypes.SEARCH_ENGINE_RESULT_DATA));
            //JSONArray search_engine_results = new JSONArray(data.getString(MessageTypes.SEARCH_ENGINE_RESULT_DATA));
            //JSONObject search_engine_result = search_engine_results.getJSONObject(0); //get the first result

            //change the view to the search results page
            setContentView(R.layout.reference_card);
            referenceCardResultTitle = (TextView) findViewById(R.id.reference_card_result_title);
            referenceCardResultSummary = (TextView) findViewById(R.id.reference_card_result_summary);
            referenceCardResultImage = (ImageView) findViewById(R.id.reference_card_result_image);

            //get content
            String title = search_engine_result.getString("title");
            String summary = search_engine_result.getString("summary");
            String img_url = search_engine_result.getString("image");

            //set the text
            referenceCardResultTitle.setText(title);
            referenceCardResultSummary.setText(summary);
            referenceCardResultSummary.setMovementMethod(new ScrollingMovementMethod());
            referenceCardResultSummary.setSelected(true);

            //pull image from web and display in imageview
            Picasso.with(this).load(img_url).into(referenceCardResultImage);

            //for now, show for n seconds and then return to llc
            int search_show_time = 12000; //milliseconds
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    setupLlcUi();
                }
            }, search_show_time);

        } catch (JSONException e) {
            Log.d(TAG, e.toString());
        }
    }
}

