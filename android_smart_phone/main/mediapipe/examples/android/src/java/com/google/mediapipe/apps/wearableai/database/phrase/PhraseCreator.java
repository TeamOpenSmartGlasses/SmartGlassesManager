package com.google.mediapipe.apps.wearableai.database.phrase;

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
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class PhraseCreator {
    public static final String LOG_TAG = PhraseCreator.class.getName();

    public static long create(String words, String medium, Context context, PhraseRepository repo) {
        /*
        Location may return right away or in a minute, null or not
        Because of this, insert each phrase without location synchronously, and after getting id back
            get location and update the phrase with location results whenever they arrive.
         */

        long time = System.currentTimeMillis();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        Phrase phrase = new Phrase(words, time, medium);
        long id = repo.insert(phrase);  // This insert blocks until database write has completed

        // Using getLastLocation is not always totally accurate. Good to update this at some point.
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if(location != null) {
                String address = null;
                phrase.setLocation(location);

                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if(addresses.size() > 0){
                        address = addresses.get(0).getAddressLine(0);
                        phrase.setAddress(address);
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
        return id;
    }
}
