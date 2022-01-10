package com.google.mediapipe.apps.wearableai.speechrecvosk;

//Vosk ASR
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

//vosk needs
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

//android
import android.util.Base64;
import android.content.Context;
import android.util.Log;
import android.content.Context;
import android.os.Handler;
import java.io.IOException;
import java.lang.InterruptedException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.mediapipe.apps.wearableai.database.phrase.PhraseRepository;
import com.google.mediapipe.apps.wearableai.database.phrase.PhraseCreator;
import com.google.mediapipe.apps.wearableai.speechrecvosk.SpeechStreamQueueServiceVosk;
import com.google.mediapipe.apps.wearableai.comms.MessageTypes;

//queue
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;


//rxjava
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class SpeechRecVosk implements RecognitionListener {
    public String TAG = "WearableAi_SpeechRecVosk";

    private Context mContext;

    private Model model;
    //private SpeechService speechService;
    //private SpeechStreamService speechStreamService;
    private SpeechStreamQueueServiceVosk speechStreamService;
    private PhraseRepository mPhraseRepository = null;

    private VoskAudioBytesStream voskAudioBytesStream;
    //private PipedOutputStream audioAdderStreamVosk;
    //private InputStream audioSenderStreamVosk;
    private BlockingQueue<byte []> audioSenderStreamVosk;
    final Handler main_handler;

    //receive/send data stream
    PublishSubject<JSONObject> dataObservable;
    Disposable dataSub;
    //receive audio stream
    PublishSubject<byte []> audioObservable;
    Disposable audioSub;

    public SpeechRecVosk(Context context, PublishSubject<byte []> audioObservable, PublishSubject<JSONObject> dataObservable, PhraseRepository mPhraseRepository){
        mContext = context;

        //to save trancript
        this.mPhraseRepository = mPhraseRepository;

        //receive/send data
        this.dataObservable = dataObservable;
        dataSub = this.dataObservable.subscribe(i -> handleDataStream(i));

        //receive audio
        //this.audioObservable = audioObservable;
        //audioSub = this.audioObservable.subscribe(i -> handleDataStream(i));

        //setup the object which will pass audio bytes to vosk
        audioSenderStreamVosk = new ArrayBlockingQueue(1024);

        //start vosk ASR
        LibVosk.setLogLevel(LogLevel.INFO);
        initModel();

        //start recognizing audio after delay, must first wait for model to load
        main_handler = new Handler();
        final int delay = 500;
        main_handler.postDelayed(new Runnable() {
            public void run() {
                if (model != null){
                    recognizeSpeech();
                } else {
                    main_handler.postDelayed(this, delay);
                }
            }
        }, delay);
    }

    private void initModel() {
        Log.d(TAG, "Initing ASR model...");
        StorageService.unpack(mContext, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
        Log.d(TAG, "ASR Model loaded.");
    }

    private void setErrorState(String message) {
        Log.d(TAG, "VOSK error: " + message);
    }

    private void recognizeSpeech() {
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            Log.d(TAG, "VOSK MAKE RECOGNIZER");
            Recognizer rec = new Recognizer(model, 16000.0f);
            Log.d(TAG, "VOSK MAKE SPEECH SERVICE");
            //speechService = new SpeechService(rec, 16000.0f);
            //6416 is hard coded - same as chunk_len - size of buffer used on ASG
            speechStreamService = new SpeechStreamQueueServiceVosk(rec, audioSenderStreamVosk, 16000.0f, 6416);
            Log.d(TAG, "VOSK START LISTENING");
            //speechService.startListening(rec);
            speechStreamService.start(this);
        }
    }

    private void pause(boolean checked) {
//        if (speechStreamService != null) {
//            speechStreamService.setPause(checked);
//        }
    }

    public void destroy() {
        Log.d(TAG, "Destroying VOSK");
        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }


    //receive audio and send to vosk
    private void handleDataStream(JSONObject data){
        try {
            String dataType = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (dataType.equals(MessageTypes.AUDIO_CHUNK_DECRYPTED)){
                String encodedPlainData = data.getString(MessageTypes.AUDIO_DATA);
                byte [] decodedPlainData = Base64.decode(encodedPlainData, Base64.DEFAULT);
                audioSenderStreamVosk.put(decodedPlainData);
            }
        } catch (InterruptedException | JSONException e) {
            setErrorState(e.getMessage());
        }
    }

    //make our own InputStream class we can fill with audio to pass to vosk
    class VoskAudioBytesStream extends InputStream {
        public byte [] data;

        public int read() {
            byte [] tmp = {0x01, 0x01};
            return 0;
        }

        public void write(byte [] inputData){
            data = inputData;
        }
    }

    //vosk listener implementation
    @Override
    public void onResult(String hypothesis) {
        //start recognizing audio after delay, must first wait for model to load
        main_handler.post(new Runnable() {
            public void run() {
                handleResult(hypothesis);
            }
        });
    }

    public void handleResult(String hypothesis){
        long transcriptTime = System.currentTimeMillis();
        handleTranscript(hypothesis, MessageTypes.FINAL_TRANSCRIPT, transcriptTime);
        }

    public void handleTranscript(String hypothesis, String transcriptType, long transcriptTime){
        //save transcript then send to other services in app
        try {
            //Below, we do a parsing of Vosk's silly string output
            //https://github.com/alphacep/vosk-android-demo/issues/81
            JSONObject voskResponse = new JSONObject(hypothesis);
            String transcript;
            if (transcriptType.equals(MessageTypes.FINAL_TRANSCRIPT)){
                transcript = voskResponse.getString("text");
            } else if (transcriptType.equals(MessageTypes.INTERMEDIATE_TRANSCRIPT)) {
                transcript = voskResponse.getString("partial");
            } else {
                return;
            }

            //don't save null or empty transcripts
            if (transcript == null || transcript.trim().isEmpty()){
                return;
            }

            //don't save transcripts that always appear incorrect
            String [] badHitWords = {"huh", "her", "hit", "cut", "this", "if", "but", "by", "hi", "ha", "a", "the", "det", "it", "you"};
            for (int i = 0; i < badHitWords.length; i++){
                if (transcript.equals(badHitWords[i])){
                    return;
                }
            }

            //save transcript if final
            long transcriptId = 0l;
            if (transcriptType.equals(MessageTypes.FINAL_TRANSCRIPT)){
                transcriptId = PhraseCreator.create(transcript, "transcript_ASG", mContext, mPhraseRepository);
            }

            //send to other services
            JSONObject transcriptObj = new JSONObject();
            transcriptObj.put(MessageTypes.MESSAGE_TYPE_LOCAL, transcriptType);
            transcriptObj.put(MessageTypes.TRANSCRIPT_TEXT, transcript);
            if (transcriptType.equals(MessageTypes.FINAL_TRANSCRIPT)){
                transcriptObj.put(MessageTypes.TRANSCRIPT_ID, transcriptId);
            }
            transcriptObj.put(MessageTypes.TIMESTAMP, transcriptTime);
            dataObservable.onNext(transcriptObj);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        long transcriptTime = System.currentTimeMillis();
        handleTranscript(hypothesis, MessageTypes.INTERMEDIATE_TRANSCRIPT, transcriptTime);
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.d(TAG, "VOSK: timeout");
    }

}
