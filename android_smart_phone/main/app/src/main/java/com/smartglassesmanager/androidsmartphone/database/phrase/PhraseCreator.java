package com.smartglassesmanager.androidsmartphone.database.phrase;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.content.Context;
import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.util.Locale;

public class PhraseCreator {
    public static final String TAG = "WearableAi_PhraseCreator";

    public static Phrase init(String medium, Context context, PhraseRepository repo) {
        long time = System.currentTimeMillis();
        Phrase phrase = new Phrase("", time, medium); //init empty phrase
        long id = repo.insert(phrase);  // This insert blocks until database write has completed
        phrase.setId(id);
        return phrase;
    }

    public static long create(Phrase phrase, String words, Context context, PhraseRepository repo) {
        /*
        Location may return right away or in a minute, null or not
        Because of this, insert each phrase without location synchronously, and after getting id back
            get location and update the phrase with location results whenever they arrive.
         */

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        // Using getLastLocation is not always totally accurate. Good to update this at some point.
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if(location != null) {
                String address = null;
                phrase.setLocation(location);

                //geocoder failing without GMS
//                List<Address> addresses = null;
//                try {
//                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
//                    if(addresses.size() > 0){
//                        address = addresses.get(0).getAddressLine(0);
//                        phrase.setAddress(address);
//                    }
//                    else {
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                repo.update(phrase.getId(), words, location, address);
            }
            else{
                repo.update(phrase.getId(), words, null, null);
            }
        });
        return phrase.getId();
    }
}
