package com.google.mediapipe.apps.wearableai.database.voicecommand;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class VoiceCommandCreator {
    public static final String LOG_TAG = VoiceCommandCreator.class.getName();

    public static void create(String preArgs, String wakeWord, String command, String postArgs, String medium, Context context, VoiceCommandRepository repo) {
        /*
        Location may return right away or in a minute, null or not
        Because of this, insert each voicecommand without location synchronously, and after getting id back
            get location and update the voicecommand with location results whenever they arrive.
         */

        Date time = new Date();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        VoiceCommandEntity voiceCommand = new VoiceCommandEntity(preArgs, wakeWord, command, postArgs, time, medium);
        long id = repo.insert(voiceCommand);  // This insert blocks until database write has completed

        // Using getLastLocation is not always totally accurate. Good to update this at some point.
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if(location != null) {
                String address = null;
                voiceCommand.setLocation(location);

                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if(addresses.size() > 0){
                        address = addresses.get(0).getAddressLine(0);
                        voiceCommand.setAddress(address);
                    }
                    else {
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                repo.update(id, location, address);
            }
            else{
            }
        });
    }
}
