package com.google.mediapipe.apps.wearableai.utils;

import java.io.File;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BitmapJavaUtils {
    private static final String TAG = "WearableAi_BitmapJavaUtils";

    public static Bitmap loadImageFromStorage(String path){
        Log.d(TAG, "Path in bitmap is: " + path);
        File imgFile = new  File(path);
        Log.d(TAG, "absolute path is: " + imgFile.getAbsolutePath());
        if(imgFile.exists()){
            Log.d(TAG, "image exists");
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            return myBitmap;
        } else {
            Log.d(TAG, "image doesn't exist");
            return null;
        }
    }
}
