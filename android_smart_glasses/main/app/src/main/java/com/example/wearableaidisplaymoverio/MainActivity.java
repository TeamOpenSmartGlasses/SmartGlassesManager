package com.example.wearableaidisplaymoverio;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
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
import java.util.ArrayList;
import java.util.List;

import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity {
    WearableAiService mService;
    boolean mBound = false;

    public String TAG = "WearableAiDisplay";

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

    //wikipedia ui
    TextView wikipediaResultTitle;
    TextView wikipediaResultSummary;
    ImageView wikipediaResultImage;

    //translate ui
    ArrayList<Spanned> translateTextHolder = new ArrayList<>();
    int translateTextHolderSizeLimit = 10; // how many lines maximum in the text holder
    TextView translateText;

    //visual search ui
    TextView textListView;
    ImageView viewfinder;

    //text list ui
    ArrayList<String> textListHolder = new ArrayList<>();

    //metrics
    float eye_contact_30 = 0;
    String facial_emotion_30 = "";
    String facial_emotion_5 = "";

    //save current mode
    String curr_mode;

    long lastIntermediateMillis = 0;
    long intermediateTranscriptPeriod = 40; //milliseconds

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //set full screen for Moverio
        long FLAG_SMARTFULLSCREEN = 0x80000000;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= FLAG_SMARTFULLSCREEN;
        win.setAttributes(winParams);

        //keep the screen on throughout
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //hint, use this to allow it to turn off:


//        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
//                Intent i = new Intent(getApplicationContext(), FullImageActivity.class);
//                i.putExtra("id", position);
//                startActivity(i);
//            }
//        });


        //create the WearableAI service if it isn't already running
        startService(new Intent(this, WearableAiService.class));
        // Bind to that service
        Intent intent = new Intent(this, WearableAiService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        //start the audio service
        StartRecorder();

//        //setup camera preview
//        preview = (SurfaceView) findViewById(R.id.preview);
//        previewHolder = preview.getHolder();
//        previewHolder.addCallback(surfaceCallback);
//        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //setup take picture button
//        takePictureButton = (Button) findViewById(R.id.captureImage);
//        takePictureButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                takeAndSendPicture();
//            }
//        });
        switchMode("llc");
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
        }
        //registerReceiver(mComputeUpdateReceiver, makeComputeUpdateIntentFilter());
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
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ASPClientSocket.ACTION_RECEIVE_MESSAGE);
        intentFilter.addAction(GlboxClientSocket.ACTION_RECEIVE_TEXT);
        intentFilter.addAction(GlboxClientSocket.COMMAND_SWITCH_MODE);
        intentFilter.addAction(GlboxClientSocket.ACTION_WIKIPEDIA_RESULT);
        intentFilter.addAction(GlboxClientSocket.ACTION_TRANSLATION_RESULT);
        intentFilter.addAction(GlboxClientSocket.ACTION_VISUAL_SEARCH_RESULT);

        intentFilter.addAction(ASPClientSocket.ACTION_AFFECTIVE_MEM_TRANSCRIPT_LIST);

        return intentFilter;
    }

    private final BroadcastReceiver mComputeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (curr_mode.equals("social") && ASPClientSocket.ACTION_RECEIVE_MESSAGE.equals(action)) {
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
                    try {
                        JSONObject transcript_object = new JSONObject(intent.getStringExtra(GlboxClientSocket.FINAL_REGULAR_TRANSCRIPT));
                        JSONObject nlp = transcript_object.getJSONObject("nlp");
                        JSONArray nouns = nlp.getJSONArray("nouns");
                        String transcript = transcript_object.getString("transcript");
                        if ((nouns.length() == 0)){
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
                                if (i == (nouns.length() - 1)){
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
                    } catch (JSONException e){
                        System.out.println(e);
                    }
                } else if (intent.hasExtra(GlboxClientSocket.INTERMEDIATE_REGULAR_TRANSCRIPT)) {
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
                    textHolder.add(Html.fromHtml("<p><font color='#EE0000'>" + command_response_text.trim() + "</font></p>"));
                    if (curr_mode.equals("llc")) {
                        liveLifeCaptionsText.setText(getCurrentTranscriptScrollText());
                    }
                }
            } else if (GlboxClientSocket.COMMAND_SWITCH_MODE.equals(action)) {
                switchMode(intent.getStringExtra(GlboxClientSocket.COMMAND_ARG));
            } else if (GlboxClientSocket.ACTION_WIKIPEDIA_RESULT.equals(action)) {
                try {
                    //change the view to the wikipeida results page
                    setContentView(R.layout.wikipedia_content);
                    wikipediaResultTitle = (TextView) findViewById(R.id.wikipedia_result_title);
                    wikipediaResultSummary = (TextView) findViewById(R.id.wikipedia_result_summary);
                    wikipediaResultImage= (ImageView) findViewById(R.id.wikipedia_result_image);

                    //get content
                    JSONObject wikipedia_object = new JSONObject(intent.getStringExtra(GlboxClientSocket.WIKIPEDIA_RESULT));
                    String title = wikipedia_object.getString("title");
                    String summary = wikipedia_object.getString("summary");
                    String img_path = wikipedia_object.getString("image_path");

                    //set the text
                    wikipediaResultTitle.setText(title);
                    wikipediaResultSummary.setText(summary);
                    wikipediaResultSummary.setMovementMethod(new ScrollingMovementMethod());
                    wikipediaResultSummary.setSelected(true);

                    //open the image and display
                    File imgFile = new  File(img_path);
                    if(imgFile.exists()){
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        wikipediaResultImage.setImageBitmap(myBitmap);
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
                } catch (JSONException e){
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
                Log.d(TAG, "received visual search image results");
                String str_data = intent.getStringExtra(GlboxClientSocket.VISUAL_SEARCH_RESULT);
                thumbnailImages = new ArrayList<String>();
                visualSearchNames = new ArrayList<String>();
                try {
                    JSONArray data = new JSONArray(str_data);
                    for (int i = 0; i < data.length(); i++){
                        //get thumnail image urls
                        String thumbnailUrl = data.getJSONObject(i).getString("thumbnailUrl");
                        thumbnailImages.add(thumbnailUrl);

                        //get names of items
                        String name = data.getJSONObject(i).getString("name");
                        visualSearchNames.add(name);
                    }

                } catch (JSONException e){
                    Log.d(TAG, e.toString());
                }
                //setup gridview to view grid of visual search images
                switchMode("visualsearchgridview");
                gridviewImages = (GridView) findViewById(R.id.gridview);
                gridViewImageAdapter = new ImageAdapter(context);
                String[] simpleThumbArray = new String[ thumbnailImages.size() ];
                thumbnailImages.toArray( simpleThumbArray );
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
    }
};


private Spanned getCurrentTranscriptScrollText() {
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
            }
        });

    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WearableAiService.LocalBinder binder = (WearableAiService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection audio_service_connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            AudioService.LocalBinder binder = (AudioService.LocalBinder) service;
//            mService = binder.getService();
//            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
//            mBound = false;
        }
    };

    public void captureVisualSearchImage(){
        Log.d(TAG, "sending visual search image");

        Toast.makeText(MainActivity.this, "Capturing image...", Toast.LENGTH_SHORT).show();

        if (mBound) {
            //byte[] curr_cam_image = mService.getCurrentCameraImage();
            mService.sendGlBoxCurrImage();
        } else {
            Log.d(TAG, "Mboudn not true yo");
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
                if (curr_mode.equals("visualsearchgridview")) {
                    switchMode("llc");
                    return true;
                } else {
                    return super.onKeyUp(keyCode, event);
                }
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    //Audio recording service - maybe to be moved into WearableAI service
    public void StartRecorder() {
        Log.i(TAG, "Starting the foreground-thread");

//        Intent serviceIntent = new Intent(this.getApplicationContext(), AudioService.class);
//        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");
//        ContextCompat.startForegroundService(this, serviceIntent);
        startService(new Intent(this, AudioService.class));
        // Bind to that service
        Intent intent = new Intent(this, AudioService.class);
        bindService(intent, audio_service_connection, Context.BIND_AUTO_CREATE);
    }

    public void StopRecorder() {
        Log.i(TAG, "Stopping the foreground-thread");

        Intent serviceIntent = new Intent(this.getApplicationContext(), AudioService.class);
        this.getApplicationContext().stopService(serviceIntent);
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

}

