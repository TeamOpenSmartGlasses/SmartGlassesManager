package com.teamopensmartglasses.smartglassesmanager.eventbusmessages;

import org.json.JSONObject;

public class DiarizationOutputEvent {
    public JSONObject diarizationData;

    public DiarizationOutputEvent(JSONObject diarizationData){
        this.diarizationData = diarizationData;
    }
}