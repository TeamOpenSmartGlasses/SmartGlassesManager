package com.wearableintelligencesystem.androidsmartphone.comms;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import androidx.lifecycle.LifecycleService;
import android.util.Log;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import androidx.lifecycle.Observer;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.method.ScrollingMovementMethod;
import android.util.Size;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import com.wearableintelligencesystem.androidsmartphone.MainActivity;
import com.wearableintelligencesystem.androidsmartphone.WearableAiAspService;
import com.wearableintelligencesystem.androidsmartphone.facialrecognition.model.FaceNetModel;
import com.wearableintelligencesystem.androidsmartphone.facialrecognition.model.Models;
import com.wearableintelligencesystem.androidsmartphone.facialrecognition.Prediction;
import java.io.*;
//rxjava
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

//database
import com.wearableintelligencesystem.androidsmartphone.database.person.PersonCreator;
import com.wearableintelligencesystem.androidsmartphone.database.person.PersonRepository;
import com.wearableintelligencesystem.androidsmartphone.database.person.PersonEntity;
import com.wearableintelligencesystem.androidsmartphone.database.person.PersonViewModel;

import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.wearableintelligencesystem.androidsmartphone.MainActivity;
import com.wearableintelligencesystem.androidsmartphone.R;

public class SGMBroadcastReceiver extends BroadcastReceiver {
    private Context context;
    private WearableAiAspService mService;

    public SGMBroadcastReceiver(Context myContext, WearableAiAspService myService){
        this.mService = myService;
        this.context = myContext;
        IntentFilter intentFilter = new IntentFilter("com.teamsmartglasses.from3pa");
        this.context.registerReceiver(this, intentFilter);
    }

    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String data = bundle.getString("data");

        Log.i("Broadcastreceiver", "BroadcastReceiver Received");
        Toast.makeText(context, "Broadcast message is received", Toast.LENGTH_LONG).show();

        try {
            //For now, send generic card
            String testCardImg = "https://ichef.bbci.co.uk/news/976/cpsprodpb/7727/production/_103330503_musk3.jpg";
            String testCardTitle = "3pa title";
            String testCardContent = "Woah dude this is from a 3pa";

            this.mService.sendTestCard(testCardTitle, testCardContent, testCardImg);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}

