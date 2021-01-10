package com.example.wearableaidisplaymoverio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class PhotoHandler implements PictureCallback {

    private final Context context;
    private String DEBUG_TAG = "Debug, PhotoHandler: ";
    private final String router = "web"; //if "web", send to http, if "file", save to file system
    //socket class instance
    ClientSocket clientsocket;

    //camera data

    public PhotoHandler(Context context) {
        clientsocket = ClientSocket.getInstance();
        this.context = context;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (router == "web"){
            uploadImage(data);
        } else if (router =="file"){
            savePicture(data);
        } else {
            uploadImage(data);
        }
    }

    private void uploadImage(byte[] image_data){
        //upload the image using async task
//        new SendImage().execute(data);
        clientsocket.sendBytes(image_data);
    }

    private void savePicture(byte[] data){
//        byte[] data = Base64.encodeToString(ata, Base64.DEFAULT).getBytes();
        File pictureFileDir = getDir();

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

            Log.d("PHOT_HANDLER", "Can't create directory to save image.");
            Toast.makeText(context, "Can't create directory to save image.",
                    Toast.LENGTH_LONG).show();
            return;

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "Picture_" + date + ".jpg";

        String filename = pictureFileDir.getPath() + File.separator + photoFile;

        File pictureFile = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            Toast.makeText(context, "New Image saved:" + photoFile,
                    Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Log.d(DEBUG_TAG, "File" + filename + "not saved: "
                    + error.getMessage());
            Toast.makeText(context, "Image could not be saved.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(sdDir, "CameraAPIDemo");
    }



}
