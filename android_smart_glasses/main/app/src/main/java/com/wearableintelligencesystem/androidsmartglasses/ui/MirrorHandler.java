package com.wearableintelligencesystem.androidsmartglasses.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;

public class MirrorHandler {

    // Saves a screenshot to cache
    // Eventually may be used to mirror screen for RayNeo X2
    public static void saveMirror(View v, Context context){
        v.post(new Runnable() {

            @Override
            public void run() {
                Bitmap bm = getScreenShot(v);
                store(bm, "blah.png", context);
            }
        });
    }

    public static Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public static void store(Bitmap bm, String fileName, Context context){
        File dir = context.getExternalCacheDir();
        final String dirPath = dir.getAbsolutePath();
        File file = new File(dirPath, fileName);
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
