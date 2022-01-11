package com.wearableintelligencesystem.androidsmartphone.database.facialemotion;

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

public class FacialEmotionCreator {
    public static final String LOG_TAG = FacialEmotionCreator.class.getName();

    public static void create(String faceEmotion, String medium, Context context, FacialEmotionRepository repo) {
        /*
        Location may return right away or in a minute, null or not
        Because of this, insert each location synchronously, and after getting id back
            get location and update the data with location results whenever they arrive.
         */

        long time = System.currentTimeMillis();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        FacialEmotion facialEmotion = new FacialEmotion(faceEmotion, time, medium);
        long id = repo.insert(facialEmotion);  // This insert blocks until database write has completed

        // Using getLastLocation is not always totally accurate. Good to update this at some point.
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if(location != null) {
                String address = null;
                facialEmotion.setLocation(location);

                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if(addresses.size() > 0){
                        address = addresses.get(0).getAddressLine(0);
                        facialEmotion.setAddress(address);
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
