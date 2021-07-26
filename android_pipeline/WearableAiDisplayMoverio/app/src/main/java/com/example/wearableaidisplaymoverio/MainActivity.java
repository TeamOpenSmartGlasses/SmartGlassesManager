package com.example.wearableaidisplaymoverio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;

import android.app.Activity;
import android.hardware.Camera;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import android.view.Window;
import android.view.WindowManager;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;


public class MainActivity extends Activity {
    public String TAG = "WearableAiDisplay";

    //tmp

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
    ArrayList<String> transcriptsHolder = new ArrayList<>();
    TextView liveLifeCaptionsText;

    //metrics
    float eye_contact_30 = 0;
    String facial_emotion_30 = "";
    String facial_emotion_5 = "";

    //save current mode
    String curr_mode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //print a bunch of stuff we can see in logcat
        for (int i = 0; i < 20; i++) {
            System.out.println("WEARABLEAI");
        }

        //set full screen for Moverio
        long FLAG_SMARTFULLSCREEN = 0x80000000;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= FLAG_SMARTFULLSCREEN;
        win.setAttributes(winParams);

        //keep the screen on throughout
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //hint, use this to allow it to turn off:


        //create the WearableAI service if it isn't already running
        startService(new Intent(this, WearableAiService.class));

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
        switch (mode) {
            case "social":
                setupSocialIntelligenceUi();
                break;
            case "llc":
                setupLlcUi();
                break;
        }
        curr_mode = mode;
        //registerReceiver(mComputeUpdateReceiver, makeComputeUpdateIntentFilter());
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
        scrollToBottom(liveLifeCaptionsText);
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

    @Override
    public void onResume() {
        Log.d(TAG, "onResumerunning");
        super.onResume();

        registerReceiver(mComputeUpdateReceiver, makeComputeUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause running");
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

        Log.d("WearableAI", "makeComputUpdateIntentFilter");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ASPClientSocket.ACTION_RECEIVE_MESSAGE);
        intentFilter.addAction(GlboxClientSocket.ACTION_RECEIVE_TEXT);
        intentFilter.addAction(GlboxClientSocket.COMMAND_SWITCH_MODE);
        return intentFilter;
    }

    private final BroadcastReceiver mComputeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "GOT ACTION: " + action);
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
                    String transcript = intent.getStringExtra(GlboxClientSocket.FINAL_REGULAR_TRANSCRIPT);
                    System.out.println("TRANSCRIPT RECEIVED IN MAIN ACTIVITY: " + transcript);
                    System.out.println("curr_mode: " + curr_mode);
                    transcriptsHolder.add(transcript.trim());
                    if (curr_mode.equals("llc")) {
                        System.out.println("TEXTSET");
                        liveLifeCaptionsText.setText(getCurrentTranscriptScrollText());
                        //scrollToBottom(liveLifeCaptionsText);
                    }
                } else if (intent.hasExtra(GlboxClientSocket.INTERMEDIATE_REGULAR_TRANSCRIPT)) {
                    String intermediate_transcript = intent.getStringExtra(GlboxClientSocket.INTERMEDIATE_REGULAR_TRANSCRIPT);
                    System.out.println("I. TRANSCRIPT RECEIVED IN MAIN ACTIVITY: " + intermediate_transcript);
                    if (curr_mode.equals("llc")) {
                        liveLifeCaptionsText.setText(getCurrentTranscriptScrollText() + "\n" + intermediate_transcript.trim());
                        //scrollToBottom(liveLifeCaptionsText);
                    }
                }
            } else if (GlboxClientSocket.COMMAND_SWITCH_MODE.equals(action)) {
                switchMode(intent.getStringExtra(GlboxClientSocket.COMMAND_ARG));
            }
        }
    };


    private String getCurrentTranscriptScrollText() {
        String current_transcript_scroll = "";
        for (int i = 0; i < transcriptsHolder.size(); i++) {
            current_transcript_scroll = current_transcript_scroll + transcriptsHolder.get(i) + "\n" + "\n";
        }
        return current_transcript_scroll;
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
                Log.d(TAG, "LCLCLC is: " + lc);
                if (lc == 0){
                    return;
                }
                Log.d(TAG, "getLineCount is : " + tv.getLineCount());
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




}

