package com.google.mediapipe.apps.wearableai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;




public class BmpProducer extends Thread {

    CustomFrameAvailableListener customFrameAvailableListener;

    public int height = 513,width = 513;
    public boolean newFrame = false;
    Bitmap bmp;
    Boolean first;

    BmpProducer(Context context){
        first = false;
//        bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.img2);
//        bmp = Bitmap.createScaledBitmap(bmp,480,640,true);
//        height = bmp.getHeight();
//        width = bmp.getWidth();
        start();
    }

    public void newFrame(Bitmap b){
        bmp = b;
        height = bmp.getHeight();
        width = bmp.getWidth();
        newFrame = true;
    }


    public void setCustomFrameAvailableListener(CustomFrameAvailableListener customFrameAvailableListener){
        this.customFrameAvailableListener = customFrameAvailableListener;
    }

    public static final String TAG="BmpProducer";
    @Override
    public void run() {
        super.run();
        while ((true)){
            if(bmp==null || customFrameAvailableListener == null){
                try{
                    Thread.sleep(20);
                }catch (Exception e){
                    Log.d(TAG,e.toString());
                }
                continue;
            }
            if (newFrame == true){
                Log.d(TAG,"Sending frame into MediaPipe WearableAI graph");
                customFrameAvailableListener.onFrame(bmp);
                newFrame = false;
            }
        }
    }
}
