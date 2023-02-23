package com.teamopensmartglasses.sgmlib;

import org.json.JSONException;
import org.json.JSONObject;

public class SGMLibCommands {
    public static void registerCommand(TPACommand TPACommand){
        SGMData.commandsToRegister.add(TPACommand);
    }

    public static void registerApp(String appName, String appDescription) throws JSONException {
        ThirdPartyApp newApp = new ThirdPartyApp(appName, appDescription, SGMData.commandsToRegister);
        JSONObject obj = new JSONObject();

        obj.put(UniversalMessageTypes.MESSAGE_TYPE_LOCAL, UniversalMessageTypes.SGM_REGISTER_APP);
        obj.put(UniversalMessageTypes.SGM_TPA_DATA, newApp); //todo: serialize this

        SGMData.sgmBroadcastSender.broadcastData(obj.toString());
        SGMData.commandsToRegister.clear();
    }

    public static void sendReferenceCard(String title, String body) throws JSONException {
        JSONObject commandResponseObject = new JSONObject();
        commandResponseObject.put(UniversalMessageTypes.MESSAGE_TYPE_LOCAL, UniversalMessageTypes.REFERENCE_CARD_SIMPLE_VIEW);
        commandResponseObject.put(UniversalMessageTypes.REFERENCE_CARD_SIMPLE_VIEW_TITLE, title);
        commandResponseObject.put(UniversalMessageTypes.REFERENCE_CARD_SIMPLE_VIEW_BODY, body);
        SGMData.sgmBroadcastSender.broadcastData(commandResponseObject.toString());
    }
}
