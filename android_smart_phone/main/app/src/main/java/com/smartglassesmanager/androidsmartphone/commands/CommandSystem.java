package com.smartglassesmanager.androidsmartphone.commands;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.SGMCallbackMapper;
import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.SGMCallback;
import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.StartLiveCaptionsEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.StopLiveCaptionsEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.UUID;

public class CommandSystem {
    private String TAG = "WearableAi_CommandSystem";

    //hold all registered commands, mostly from TPAs
    public SGMCallbackMapper sgmCallbackMapper;

    //voice command system
    VoiceCommandServer voiceCommandServer;

    Context mContext;

    final String commandStartString = "command--";

    public CommandSystem(Context context){
        mContext = context;

        sgmCallbackMapper = new SGMCallbackMapper();

        loadDefaultCommands();

        //start voice command server to parse transcript for voice command
        voiceCommandServer = new VoiceCommandServer(context);
        updateInterfaceCommands();

        //load the commands we saved to storage
        loadCommands();

        //subscribe to event bus events
        EventBus.getDefault().register(this);
    }

    public void launchLiveCaptions(String args, long commandTime){
        EventBus.getDefault().post(new StartLiveCaptionsEvent());
    }

    public void launchTestCard(String args, long commandTime){
        EventBus.getDefault().post(new StopLiveCaptionsEvent());
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent("Test Card", "This is a test card triggered by voice command."));
    }

    public void loadDefaultCommands(){
        //live life captions - test scrolling view text
        registerCommand("Live Captions", UUID.fromString("933b8950-412e-429e-8fb6-430f973cc9dc"), new String[] { "live captions", "live life captions", "captions", "transcription" }, "Starts streaming captions live to the glasses display.", this::launchLiveCaptions);

        //test reference card
        registerCommand("Test Card", UUID.fromString("f4290426-18d5-431a-aea4-21844b832735"), new String[] { "show me a test card", "test card", "test", "testing", "show test card" }, "Shows a test Reference Card on the glasses display.", this::launchTestCard);

//        //blank screen
//        registerCommand("Blank Screen", UUID.fromString("93401154-b4ab-4166-9aa3-58b79db41ff0"), new String[] { "turn off display", "blank screen" }, "Makes the smart glasses display turn off or go blank.", this::dummyCallback);
    }

    public void registerCommand(String name, UUID id, String[] phrases, String description, SGMCallback callback) {
        //add a new command
        SGMCommand newCommand = new SGMCommand(name, id, phrases, description);
        sgmCallbackMapper.putCommandWithCallback(newCommand, callback);
    }

    public void saveCommand(SGMCommand command) {
        //save command to memory
        sgmCallbackMapper.putCommandWithCallback(command, null);

        //save command to storage
        // Get a reference to the SharedPreferences object
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("SGMPrefs", Context.MODE_PRIVATE);

        // Add command to the SharedPreferences
        String commandSerialized = serializeObject(command);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        String commandId = commandStartString + command.getName();
        editor.remove(commandId);
        editor.putString(commandStartString + command.getName(), commandSerialized);
        editor.apply();
    }

    public void loadCommands(){
        // Get a reference to the SharedPreferences object
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("SGMPrefs", Context.MODE_PRIVATE);

        // Retrieve values from the SharedPreferences
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(commandStartString)) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    SGMCommand command = (SGMCommand) deserializeObject((String) value);
                    sgmCallbackMapper.putCommandWithCallback(command, null);
                    Log.d(TAG, "Key: " + key + ", Value: " + command.getName());
                }
            }
        }

        updateInterfaceCommands();
    }

    public String serializeObject(Object object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(out);
            outputStream.writeObject(object);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
    }

    public static Object deserializeObject(String serialized) {
        byte[] bytes = Base64.decode(serialized, Base64.DEFAULT);
        Object object = null;
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
            object = inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return object;
    }

    private void updateInterfaceCommands(){
        voiceCommandServer.updateVoiceCommands(sgmCallbackMapper.getCommandsList());
    }

    @Subscribe
    public void onRegisterCommandRequestEvent(RegisterCommandRequestEvent receivedEvent){
        saveCommand(receivedEvent.command); //save the command to be loaded later
        Log.d(TAG, "Command was registered");
        updateInterfaceCommands();
    }
    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent){
        SGMCommand command = receivedEvent.command;
        String args = receivedEvent.args;
        long commandTriggeredTime = receivedEvent.commandTriggeredTime;
        if (command != null){
            SGMCallback callback = sgmCallbackMapper.getCommandCallback(command);
            if (callback != null){
                Log.d(TAG, "Running command");
                callback.runCommand(args, commandTriggeredTime);
            }
        }
    }

    public void destroy(){
        if (voiceCommandServer != null) {
            voiceCommandServer.destroy();
        }

        EventBus.getDefault().unregister(this);
    }

}
