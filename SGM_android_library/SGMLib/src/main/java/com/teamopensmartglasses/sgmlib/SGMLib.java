package com.teamopensmartglasses.sgmlib;

import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

public class SGMLib {
    public SGMLib(){
    }

    //register a new command
    public void registerCommand(SGMCommand sgmCommand){
        SGMData.commandsToRegister.add(sgmCommand);
    }

    //register our app with the SGM
    public void registerApp(String appName, String appDescription) throws JSONException {
//        ThirdPartyApp newApp = new ThirdPartyApp(appName, appDescription);
//        JSONObject obj = new JSONObject();
//
//        obj.put(UniversalMessageTypes.MESSAGE_TYPE, UniversalMessageTypes.SGM_REGISTER_APP);
//        obj.put(UniversalMessageTypes.SGM_TPA_DATA, newApp); //todo: serialize this
//
//        SGMData.TPABroadcastSender.broadcastData(obj.toString());
//        SGMData.commandsToRegister.clear();
    }

    public void sendReferenceCard(String title, String body) {
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    }
}
