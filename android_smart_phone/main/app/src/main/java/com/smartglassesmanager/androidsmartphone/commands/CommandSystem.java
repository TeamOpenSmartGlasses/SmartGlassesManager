package com.smartglassesmanager.androidsmartphone.commands;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.smartglassesmanager.androidsmartphone.eventbusmessages.HomeScreenEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.TPARequestEvent;
import com.teamopensmartglasses.sgmlib.SGMCallbackMapper;
import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.SGMCallback;
import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextRequestEvent;
import com.teamopensmartglasses.sgmlib.events.FocusRevokedEvent;
import com.teamopensmartglasses.sgmlib.events.IntermediateScrollingTextRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.StartLiveCaptionsEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.StopLiveCaptionsEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStopRequestEvent;

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

    //command timeout
    class AppPrivilegeTimeout {
        public String appId;
        public long timeStart;

        AppPrivilegeTimeout(String appId, long timeStart) {
            this.appId = appId;
            this.timeStart = timeStart;
        }
    }

    private AppPrivilegeTimeout appPrivilegeTimeout;
    private String focusedAppPackage; //if an app was triggered and took focus by starting a mode, then this will tell us which app is currently in focus

    Context mContext;

    final String commandStartString = "command--";
    final int commandResponseWindowTime = 2000; //how long a TPA has to send a response request event

    public CommandSystem(Context context) {
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

    public void launchLiveCaptions(String args, long commandTime) {
        EventBus.getDefault().post(new StartLiveCaptionsEvent());
    }

    public void launchTestCard(String args, long commandTime) {
        EventBus.getDefault().post(new StopLiveCaptionsEvent());
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent("Test Card", "This is a test card triggered by voice command."));
    }

    public void goHome(String args, long commandTime) {
        focusedAppPackage = null;
        EventBus.getDefault().post(new HomeScreenEvent());
        EventBus.getDefault().post(new FocusRevokedEvent());
    }

    public void loadDefaultCommands() {
        //live life captions - test scrolling view text
//        registerCommand("Live Captions", UUID.fromString("933b8950-412e-429e-8fb6-430f973cc9dc"), new String[]{"live captions", "live life captions", "captions", "transcription"}, "Starts streaming captions live to the glasses display.", this::launchLiveCaptions);

        //test reference card
        registerCommand("Test", UUID.fromString("f4290426-18d5-431a-aea4-21844b832735"), new String[]{"show me a test card", "test card", "test", "testing", "show test card"}, "Shows a test Reference Card on the glasses display.", this::launchTestCard);

        //go home
        registerCommand("Go Home", UUID.fromString("07111c55-b8e0-41d2-a6dd-d994ab946d1e"), new String[]{"go home", "quit", "stop"}, "Exits current mode and goes back to the home screen.", this::goHome);

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
        String commandId = commandStartString + command.getId();
        editor.remove(commandId);
        editor.putString(commandId, commandSerialized);
        editor.apply();
    }

    public void loadCommands() {
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

    private void updateInterfaceCommands() {
        voiceCommandServer.updateVoiceCommands(sgmCallbackMapper.getCommandsList());
    }

    @Subscribe
    public void onRegisterCommandRequestEvent(RegisterCommandRequestEvent receivedEvent) {
        Log.d(TAG, "Command was registered");
        saveCommand(receivedEvent.command); //save the command to be loaded later
        updateInterfaceCommands();
    }

    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent) {
        focusedAppPackage = null; //clear the focused app

        SGMCommand command = receivedEvent.command;
        String args = receivedEvent.args;
        long commandTriggeredTime = receivedEvent.commandTriggeredTime;
        if (command != null) {
            //start the app privilege timer as user triggered this command
            Log.d(TAG, "Starting app privilege timer for app: " + receivedEvent.command.packageName);
            appPrivilegeTimeout = new AppPrivilegeTimeout(receivedEvent.command.packageName, System.currentTimeMillis());

            //run if it's a locally registered command
            SGMCallback callback = sgmCallbackMapper.getCommandCallback(command);
            if (callback != null) {
                Log.d(TAG, "Running command");
                callback.runCommand(args, commandTriggeredTime);
            }
        }
    }

    public void destroy() {
        if (voiceCommandServer != null) {
            voiceCommandServer.destroy();
        }
        EventBus.getDefault().unregister(this);
    }

    //check is an app making a request should be granted that specific request right now
    private boolean checkAppHasPrivilege(String eventId, String sendingPackage){
        //check if app has focus - if so, it can do whatever it wants
        if (sendingPackage.equals(focusedAppPackage)){
            return true;
        }

        //if the app doesn't have focus, then we have to check if its been recently triggered and thus allowed to do something
        if (eventId.equals(ReferenceCardSimpleViewRequestEvent.eventId) | eventId.equals(ScrollingTextViewStartRequestEvent.eventId)) {
            //if the app took too long to respond, don't allow it to run anything
            if (appPrivilegeTimeout == null) {
                return false;
            }

            //if the app responded in good time (and it's the same app that was actually triggered, then allow the request
            if (sendingPackage.equals(appPrivilegeTimeout.appId)) {
                if ((System.currentTimeMillis() - appPrivilegeTimeout.timeStart) < commandResponseWindowTime) {
                    return true;
                }
            }
        }

        //if not focused and not recently triggered, the app does not have permission to run anything
        return false;
    }

    //respond and approve events below
    @Subscribe
    public void onTPARequestEvent(TPARequestEvent receivedEvent) {
        Log.d(TAG, "onTPARequestEvent");

        //map from id to event for all events that don't need permissions
        switch (receivedEvent.eventId) {
            case RegisterCommandRequestEvent.eventId:
                Log.d(TAG, "Resending register command request event");
                EventBus.getDefault().post((RegisterCommandRequestEvent) receivedEvent.serializedEvent);
                return;
        }

        //check if the app making the request has privilege, only run it if it does have privilege
        if (checkAppHasPrivilege(receivedEvent.eventId, receivedEvent.sendingPackage)) {
            Log.d(TAG, "Allowing and resending event requested by: " + receivedEvent.sendingPackage);
            //map from id to event for all events that need permissions
            switch (receivedEvent.eventId) {
                case ReferenceCardSimpleViewRequestEvent.eventId:
                    EventBus.getDefault().post((ReferenceCardSimpleViewRequestEvent) receivedEvent.serializedEvent);
                    break;
                case ScrollingTextViewStartRequestEvent.eventId: //mode start command - gives app focus
                    focusedAppPackage = receivedEvent.sendingPackage;
                    EventBus.getDefault().post((ScrollingTextViewStartRequestEvent) receivedEvent.serializedEvent);
                    break;
                case ScrollingTextViewStopRequestEvent.eventId:
                    EventBus.getDefault().post((ScrollingTextViewStopRequestEvent) receivedEvent.serializedEvent);
                    break;
                case FinalScrollingTextRequestEvent.eventId:
                    EventBus.getDefault().post((FinalScrollingTextRequestEvent) receivedEvent.serializedEvent);
                    break;
                case IntermediateScrollingTextRequestEvent.eventId:
                    EventBus.getDefault().post((IntermediateScrollingTextRequestEvent) receivedEvent.serializedEvent);
                    break;
            }
        } else {
            Log.d(TAG, "Denied event requested by: " + receivedEvent.sendingPackage);
        }
    }

}
