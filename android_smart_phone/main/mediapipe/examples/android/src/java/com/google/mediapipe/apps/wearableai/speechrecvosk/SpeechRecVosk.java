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
import android.content.Context;
import android.util.Log;
import android.content.Context;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;

//rxjava
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class SpeechRecVosk implements RecognitionListener {
    public String TAG = "WearableAi_SpeechRecVosk";

    private Context mContext;

    private Model model;
    //private SpeechService speechService;
    private SpeechStreamService speechStreamService;

    private VoskAudioBytesStream voskAudioBytesStream;
    private PipedOutputStream audioAdderStreamVosk;
    private InputStream audioSenderStreamVosk;

    //receive audio stream
    PublishSubject<byte []> audioObservable;

    public SpeechRecVosk(Context context, PublishSubject<byte []> audioObservable){
        mContext = context;

        this.audioObservable = audioObservable;
        Disposable audioSub = this.audioObservable.subscribe(i -> handleAudioStream(i));

        //setup the object which will pass audio bytes to vosk
        //VoskAudioBytesStream voskAudioBytesStream = new VoskAudioBytesStream();
        try{
            audioAdderStreamVosk = new PipedOutputStream();
            audioSenderStreamVosk = new PipedInputStream(audioAdderStreamVosk);
        } catch (IOException e){
            e.printStackTrace();
        }

        //start vosk ASR
        LibVosk.setLogLevel(LogLevel.INFO);
        initModel();

        //start recognizing audio after delay, must first wait for model to load
        final Handler main_handler = new Handler();
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
            speechStreamService = new SpeechStreamService(rec, audioSenderStreamVosk, 16000.0f);
            Log.d(TAG, "VOSK START LISTENING");
            speechStreamService.start(this);
        }
    }

    private void pause(boolean checked) {
//        if (speechStreamService != null) {
//            speechStreamService.setPause(checked);
//        }
    }

    public void destroy() {
//        if (speechService != null) {
//            speechService.stop();
//            speechService.shutdown();
//        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }


    //receive audio and send to vosk
    private void handleAudioStream(byte [] data){
        try {
            audioAdderStreamVosk.write(data);
        } catch (IOException e) {
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
        Log.d(TAG, "VOSK: " + hypothesis + "\n");
    }

    @Override
    public void onFinalResult(String hypothesis) {
        Log.d(TAG, "VOSK, final: " + hypothesis + "\n");
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        Log.d(TAG, "VOSK, partial: " + hypothesis + "\n");
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
