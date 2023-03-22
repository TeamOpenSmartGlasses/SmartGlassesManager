package com.smartglassesmanager.androidsmartphone.utils;

import java.io.File;
import android.util.Log;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.io.FileOutputStream;
import android.os.Environment;
import com.smartglassesmanager.androidsmartphone.database.mediafile.MediaFileCreator;
import com.smartglassesmanager.androidsmartphone.database.mediafile.MediaFileRepository;
import java.util.Date;

public class FileUtils {
    public static final String TAG = "WearableAi_FileUtils";

    public static Long savePicture(Context context, byte[] data, long imageTime, MediaFileRepository mMediaFileRepository){
        File pictureFileDir = getDir(context);
        //System.out.println("TRYING TO SAVE AT LOCATION: " + pictureFileDir.toString());

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            Log.d(TAG, "Save picture failed because dir was not acceptable: " + pictureFileDir.getPath());
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_mm_dd_hh_mm_ss_SSS");
        String date = dateFormat.format(new Date());
        String photoFile = "WearableIntelligenceSystem_POVcamera_" + date + ".jpg";

        String filename = pictureFileDir.getPath() + File.separator + photoFile;

        File pictureFile = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (Exception error) {
            error.printStackTrace();
        }

        //now save a reference to this media file image to the room database
        Long imageId = savePictureToDatabase(filename, imageTime, mMediaFileRepository);
        return imageId;
    }


    private static long savePictureToDatabase(String localPath, long timestamp, MediaFileRepository mMediaFileRepository){
        long imageId = MediaFileCreator.create(localPath, "image", timestamp, timestamp, mMediaFileRepository);
        return imageId;
    }


    private static File getDir(Context context) {
        //name of dir where we save images
        String dirName = "WearableAiMobileCompute";

        //return spot on SD card IF it's available, otherwise, return internal pictures directory
        File saveDir;
        if (canWriteOnExternalStorage()){
            // get the path to sdcard
            //saveDir = this.getExternalStorageDirectory();
            saveDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        } else {
            //saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            saveDir = context.getFilesDir();
        }
        // to this path add a new directory path
        File dir = new File(saveDir.getAbsolutePath(), dirName);

        // create this directory if not already created
        dir.mkdir();

        return dir;
    }

    public static boolean canWriteOnExternalStorage() {
       // get the state of your external storage
       String state = Environment.getExternalStorageState();
       if (Environment.MEDIA_MOUNTED.equals(state)) {
        // if storage is mounted return true
          return true;
       }
       return false;
    }


}
