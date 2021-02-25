package com.google.mediapipe.apps.wearableai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;




public class BmpProducer extends Thread {

    CustomFrameAvailableListener customFrameAvailableListener;

    public int height = 513,width = 513;
    Bitmap bmp;
    Boolean first;

    BmpProducer(Context context){
        first = false;
        bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.img2);
        bmp = Bitmap.createScaledBitmap(bmp,480,640,true);
        height = bmp.getHeight();
        width = bmp.getWidth();
        start();
    }

    public void setCustomFrameAvailableListener(CustomFrameAvailableListener customFrameAvailableListener){
        this.customFrameAvailableListener = customFrameAvailableListener;
    }

    public static final String TAG="BmpProducer";
    @Override
    public void run() {
        super.run();
        while ((true)){
            if(first == true || bmp==null || customFrameAvailableListener == null)
                continue;
            Log.d(TAG,"Writing frame");
            first = true;
            customFrameAvailableListener.onFrame(bmp);
            /*OTMainActivity.imageView.post(new Runnable() {
                @Override
                public void run() {
                    OTMainActivity.imageView.setImageBitmap(bg);
                }
            });*/
            try{
                Thread.sleep(10);
            }catch (Exception e){
                Log.d(TAG,e.toString());
            }
        }
    }
}
