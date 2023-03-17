package com.teamopensmartglasses.sgmlib;

import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStopEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecFinalOutputEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecIntermediateOutputEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class SGMLib {
    public String TAG = "SGMLib_SGMLib";

    private TPABroadcastReceiver sgmReceiver;
    private TPABroadcastSender sgmSender;
    private Context mContext;
    private SGMCallbackMapper sgmCallbackMapper;

    public HashMap<DataStreamType, TranscriptCallback> subscribedDataStreams;
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
        sgmCallbackMapper.putCommandWithCallback(sgmCommand, callback);
        EventBus.getDefault().post(new RegisterCommandRequestEvent(sgmCommand));
    }

    public void subscribe(DataStreamType dataStreamType, TranscriptCallback callback){
        subscribedDataStreams.put(dataStreamType, callback);
    }

    //register our app with the SGM
    public void registerApp(String appName, String appDescription) {
    }

    //show a reference card on the smart glasses with title and body text
    public void sendReferenceCard(String title, String body) {
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    }

    public void startScrollingText(String title){
        EventBus.getDefault().post(new ScrollingTextViewStartEvent(title));
    }

    public void pushScrollingText(String text){
        EventBus.getDefault().post(new FinalScrollingTextEvent(text));
    }
    public void stopScrollingText(){
        EventBus.getDefault().post(new ScrollingTextViewStopEvent());
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
        subscribedDataStreams.get(DataStreamType.TRANSCRIPTION_STREAM).call(text, time, false);
    }

    @Subscribe
    public void onFinalTranscript(SpeechRecFinalOutputEvent event) {
        String text = event.text;
        long time = event.timestamp;
        subscribedDataStreams.get(DataStreamType.TRANSCRIPTION_STREAM).call(text, time, true);
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
