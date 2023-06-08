package com.smartglassesmanager.androidsmartphone.speechrecognition.google;

import static com.google.audio.asr.SpeechRecognitionModelOptions.SpecificModel.DICTATION_DEFAULT;
import static com.google.audio.asr.SpeechRecognitionModelOptions.SpecificModel.VIDEO;
import static com.google.audio.asr.TranscriptionResultFormatterOptions.TranscriptColoringStyle.NO_COLORING;

import android.content.Context;
import android.util.Log;

import com.google.audio.CodecAndBitrate;
import com.google.audio.asr.CloudSpeechSessionParams;
import com.google.audio.asr.CloudSpeechStreamObserverParams;
import com.google.audio.asr.SpeechRecognitionModelOptions;
import com.google.audio.asr.TranscriptionResultFormatterOptions;
import com.smartglassesmanager.androidsmartphone.speechrecognition.SpeechRecFramework;
import com.smartglassesmanager.androidsmartphone.speechrecognition.google.asr.RepeatingRecognitionSession;
import com.smartglassesmanager.androidsmartphone.speechrecognition.google.asr.SafeTranscriptionResultFormatter;
import com.smartglassesmanager.androidsmartphone.speechrecognition.google.asr.TranscriptionResultUpdatePublisher;
import com.smartglassesmanager.androidsmartphone.speechrecognition.google.asr.asrhelpers.NetworkConnectionChecker;
import com.smartglassesmanager.androidsmartphone.speechrecognition.google.gcloudspeech.CloudSpeechSessionFactory;
import com.smartglassesmanager.androidsmartphone.speechrecognition.vad.VadGateSpeechPolicy;

import org.greenrobot.eventbus.EventBus;

public class SpeechRecGoogle extends SpeechRecFramework {
    public String TAG = "WearableAi_SpeechRecGoogle";

    private Context mContext;

    public SpeechRecGoogle(Context mContext) {
        this.mContext = mContext;

        initLanguageLocale();

//        EventBus.getDefault().register(this);
    }

//    @Subscribe
//    public void onGoogleAudioChunkNewEvent(GoogleAudioChunkNewEvent receivedEvent) {
//    }

    @Override
    public void ingestAudioChunk(byte[] audioChunk) {
        recognizer.processAudioBytes(audioChunk);
    }

    @Override
    public void start(){
        //start text to speech
        constructRepeatingRecognitionSession();
        recognizer.init(1024*3);
    }

    @Override
    public void destroy(){
        if (recognizer != null) {
            recognizer.unregisterCallback(transcriptUpdater);
            networkChecker.unregisterNetworkCallback();
            recognizer.stop();
        }
    }

    private int currentLanguageCodePosition;
    private String currentLanguageCode;

    // This class was intended to be used from a thread where timing is not critical (i.e. do not
    // call this in a system audio callback). Network calls will be made during all of the functions
    // that RepeatingRecognitionSession inherits from SampleProcessorInterface.
    private RepeatingRecognitionSession recognizer;
    private NetworkConnectionChecker networkChecker;

    private final TranscriptionResultUpdatePublisher transcriptUpdater =
            (formattedTranscript, updateType) -> {
                if (updateType == TranscriptionResultUpdatePublisher.UpdateType.TRANSCRIPT_FINALIZED){
                    Log.d(TAG, "GOT FINAL TRANSCRIPT: " + formattedTranscript.toString());
                }
            };

    private void initLanguageLocale() {
        // The default locale is en-US.
        currentLanguageCode = "en-US";
        currentLanguageCodePosition = 22;
    }

    private void constructRepeatingRecognitionSession() {
        SpeechRecognitionModelOptions options =
                SpeechRecognitionModelOptions.newBuilder()
                        .setLocale(currentLanguageCode)
                        // As of 7/18/19, Cloud Speech's video model supports en-US only.
                        .setModel(currentLanguageCode.equals("en-US") ? VIDEO : DICTATION_DEFAULT) //this is overwritten to use 'latest_long' in cloud/CloudSpeechSession.java
                        .build();
        CloudSpeechSessionParams cloudParams =
                CloudSpeechSessionParams.newBuilder()
                        .setObserverParams(
                                CloudSpeechStreamObserverParams.newBuilder().setRejectUnstableHypotheses(false))
                        .setFilterProfanity(false)
                        .setEncoderParams(
                                CloudSpeechSessionParams.EncoderParams.newBuilder()
                                        .setEnableEncoder(true)
                                        .setAllowVbr(true)
                                        .setCodec(CodecAndBitrate.OGG_OPUS_BITRATE_32KBPS))
                        .build();
        networkChecker = new NetworkConnectionChecker(mContext);
        networkChecker.registerNetworkCallback();

        // There are lots of options for formatting the text. These can be useful for debugging
        // and visualization, but it increases the effort of reading the transcripts.
        TranscriptionResultFormatterOptions formatterOptions =
                TranscriptionResultFormatterOptions.newBuilder()
                        .setTranscriptColoringStyle(NO_COLORING)
                        .build();
        RepeatingRecognitionSession.Builder recognizerBuilder =
                RepeatingRecognitionSession.newBuilder()
//                        .setSpeechSessionFactory(new CloudSpeechSessionFactory(cloudParams, getApiKey(this)))
//                        .setSpeechSessionFactory(new CloudSpeechSessionFactory(cloudParams, apiKey))
                        .setSpeechSessionFactory(new CloudSpeechSessionFactory(cloudParams, "myAPIkeyhere"))
                        .setSampleRateHz(16000)
                        .setTranscriptionResultFormatter(new SafeTranscriptionResultFormatter(formatterOptions))
                        .setSpeechRecognitionModelOptions(options)
                        .setSpeechDetectionPolicy(new VadGateSpeechPolicy(mContext))
                        .setNetworkConnectionChecker(networkChecker);
        recognizer = recognizerBuilder.build();
        recognizer.registerCallback(transcriptUpdater, TranscriptionResultUpdatePublisher.ResultSource.MOST_RECENT_SEGMENT);
    }
}
