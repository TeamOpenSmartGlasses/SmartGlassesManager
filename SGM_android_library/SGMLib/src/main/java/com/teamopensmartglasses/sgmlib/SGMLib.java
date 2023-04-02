package com.teamopensmartglasses.sgmlib;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextRequestEvent;
import com.teamopensmartglasses.sgmlib.events.FocusChangedEvent;
import com.teamopensmartglasses.sgmlib.events.FocusRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStopRequestEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecFinalOutputEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecIntermediateOutputEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;

public class SGMLib {
    public String TAG = "SGMLib_SGMLib";

    private TPABroadcastReceiver sgmReceiver;
    private TPABroadcastSender sgmSender;
    private Context mContext;
    private SGMCallbackMapper sgmCallbackMapper;
    private FocusCallback focusCallback;

    public HashMap<DataStreamType, TranscriptCallback> subscribedDataStreams;

    private SmartGlassesAndroidService smartGlassesAndroidService;

    public SGMLib(Context context){
        this.mContext = context;
        sgmCallbackMapper = new SGMCallbackMapper();
        sgmReceiver = new TPABroadcastReceiver(context);
        sgmSender = new TPABroadcastSender(context);
        subscribedDataStreams = new HashMap<DataStreamType, TranscriptCallback>();

        //register subscribers on EventBus
        EventBus.getDefault().register(this);
    }

    //register a new command
    public void registerCommand(SGMCommand sgmCommand, SGMCallback callback){
        sgmCommand.packageName = mContext.getPackageName();
        sgmCommand.serviceName = mContext.getClass().getName();

        sgmCallbackMapper.putCommandWithCallback(sgmCommand, callback);
        EventBus.getDefault().post(new RegisterCommandRequestEvent(sgmCommand));
    }

    public void subscribe(DataStreamType dataStreamType, TranscriptCallback callback){
        subscribedDataStreams.put(dataStreamType, callback);
    }

    //TPA request to be the app in focus - SGM has to grant this request
    public void requestFocus(FocusCallback callback){
        focusCallback = callback;
        EventBus.getDefault().post(new FocusRequestEvent(true));
    }

    //register our app with the SGM
    public void registerApp(String appName, String appDescription) {
    }

    //show a reference card on the smart glasses with title and body text
    public void sendReferenceCard(String title, String body) {
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    }

    public void startScrollingText(String title){
        EventBus.getDefault().post(new ScrollingTextViewStartRequestEvent(title));
    }

    public void pushScrollingText(String text){
        EventBus.getDefault().post(new FinalScrollingTextRequestEvent(text));
    }

    public void stopScrollingText(){
        EventBus.getDefault().post(new ScrollingTextViewStopRequestEvent());
    }

    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent){
        SGMCommand command = receivedEvent.command;
        String args = receivedEvent.args;
        long commandTriggeredTime = receivedEvent.commandTriggeredTime;
        Log.d(TAG, " " + command.getId());
        Log.d(TAG, " " + command.getDescription());

        //call the callback
        SGMCallback callback = sgmCallbackMapper.getCommandCallback(command);
        if (callback != null){
            callback.runCommand(args, commandTriggeredTime);
        }
        Log.d(TAG, "Callback called");
    }

    @Subscribe
    public void onIntermediateTranscript(SpeechRecIntermediateOutputEvent event) {
        String text = event.text;
        long time = event.timestamp;
        if (subscribedDataStreams.containsKey(DataStreamType.TRANSCRIPTION_ENGLISH_STREAM)) {
            subscribedDataStreams.get(DataStreamType.TRANSCRIPTION_ENGLISH_STREAM).call(text, time, false);
        }
    }

    @Subscribe
    public void onFinalTranscript(SpeechRecFinalOutputEvent event) {
        String text = event.text;
        long time = event.timestamp;
        if (subscribedDataStreams.containsKey(DataStreamType.TRANSCRIPTION_ENGLISH_STREAM)) {
            subscribedDataStreams.get(DataStreamType.TRANSCRIPTION_ENGLISH_STREAM).call(text, time, true);
        }
    }

    @Subscribe
    public void onFocusChange(FocusChangedEvent event) {
        if (focusCallback != null){
            focusCallback.runFocusChange(event.focusState);
        }
    }

    public void deinit(){
        EventBus.getDefault().unregister(this);
        if (sgmReceiver != null) {
            sgmReceiver.destroy();
        }
        if (sgmSender != null) {
            sgmSender.destroy();
        }
    }
}
