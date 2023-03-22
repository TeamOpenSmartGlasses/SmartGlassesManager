package com.smartglassesmanager.androidsmartphone.database;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.location.Location;
import android.location.LocationManager;

import androidx.room.TypeConverter;

import com.google.gson.Gson;

import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;

public class Converters {

    @TypeConverter
    public static Date fromTimestamp(Long value){
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long toTimestamp(Date time){
        return time == null ? null : time.getTime();
    }

    @TypeConverter
    public static Location fromLocation(String value){
        if(value != null) {
            Gson gson = new Gson();
            Hashtable stash = gson.fromJson(value, Hashtable.class);
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude((Double) stash.get("lat"));
            location.setLongitude((Double) stash.get("lon"));
            location.setAltitude((Double) stash.get("altitude"));
            return location;
        }
        else{
            return null;
        }
    }

    @TypeConverter
    public static String toLocation(Location location){
        if(location != null) {
            Dictionary stash = new Hashtable();
            stash.put("lat", location.getLatitude());
            stash.put("lon", location.getLongitude());
            stash.put("altitude", location.getAltitude());
            Gson gson = new Gson();
            return gson.toJson(stash);
        }
        else{
            return null;
        }
    }
}
