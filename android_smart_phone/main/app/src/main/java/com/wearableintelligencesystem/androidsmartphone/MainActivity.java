package com.wearableintelligencesystem.androidsmartphone;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.EngoTwo;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.InmoAirOne;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.SmartGlassesDevice;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.TCLRayNeoXTwo;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.VuzixShield;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.VuzixUltralite;

/** Main activity of WearableAI compute module android app. **/
/** This provides a simple UI for users to connect and setup their glasses. This activity launches the service which does all of the work to communicate with glasses and third party apps. **/
public class MainActivity extends AppCompatActivity {
    private  final String TAG = "WearableAi_MainActivity";

    //the smart glasses we're currently connecting and communicating with
    public SmartGlassesDevice selectedDevice;

    //handle the foreground service which does all of the important stuff
    public WearableAiAspService mService;
    boolean mBound = false;

    //handle permissions
    PermissionsUtils permissionsUtils;

    //UI - bottom nav bar
    private BottomNavigationView bottomNavigation;
    private NavController navController;

    //list of glasses we support
    public SmartGlassesDevice [] smartGlassesDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup action bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        //set main view
        setContentView(R.layout.activity_main);

        //setup the available smart glasses we support
        smartGlassesDevices = new SmartGlassesDevice[]{new VuzixShield(), new EngoTwo(), new InmoAirOne(), new TCLRayNeoXTwo(), new VuzixUltralite()};

        //setup the nav bar
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        Log.d(TAG, getSupportFragmentManager().getFragments().toString());
        navController = navHostFragment.getNavController();
//        setupBottomNavBar();
//        bottomNavigation.setSelectedItemId(R.id.settings_page);

        //get permissions
        permissionsUtils = new PermissionsUtils(this, TAG);
        permissionsUtils.checkPermission();

        //start wearable ai service
        startWearableAiService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "run onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //bind to WearableAi service
        bindWearableAiAspService();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //unbind wearableAi service
        unbindWearableAiAspService();
    }

    public void stopWearableAiService() {
        Log.d(TAG, "Stopping WearableAI service");
        unbindWearableAiAspService();

        if (!isMyServiceRunning(WearableAiAspService.class)) return;
        Intent stopIntent = new Intent(this, WearableAiAspService.class);
        stopIntent.setAction(WearableAiAspService.ACTION_STOP_FOREGROUND_SERVICE);
        startService(stopIntent);
    }

   public void sendWearableAiServiceMessage(String message) {
        if (!isMyServiceRunning(WearableAiAspService.class)) return;
        Intent messageIntent = new Intent(this, WearableAiAspService.class);
        messageIntent.setAction(message);
        Log.d(TAG, "Sending WearableAi Service this message: " + message);
        startService(messageIntent);
   }

   public void startWearableAiService() {
       Log.d(TAG, "Starting wearableAiService");
        if (isMyServiceRunning(WearableAiAspService.class)) return;
        Intent startIntent = new Intent(this, WearableAiAspService.class);
        startIntent.setAction(WearableAiAspService.ACTION_START_FOREGROUND_SERVICE);
        startService(startIntent);
        bindWearableAiAspService();
   }

    //check if service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //handle bottom app bar navigation
//    private void setupBottomNavBar(){
//        bottomNavigation = (BottomNavigationView) findViewById(R.id.bottom_nav_main_menu_nav);
//        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
//            @Override
//            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//                //this has to be an if-else: https://stackoverflow.com/questions/21005205/what-causes-constant-expression-required-errors-for-the-generated-r-id-xxx-val
//                int itemId = item.getItemId();
//                if(R.id.memory_page == itemId){
//                    Log.d(TAG, "Clicked memory tools");
//                    navController.navigate(R.id.nav_memory_tools);
//                } else if (R.id.smart_glasses_debug_page == itemId){
//                    Log.d(TAG, "Clicked smart glasses debug page");
//                    navController.navigate(R.id.nav_smart_glasses_debug);
//                } else if (R.id.settings_page == itemId){
//                    Log.d(TAG, "Clicked settings page");
//                    navController.navigate(R.id.nav_settings);
//                }
//                return true;
//            }
//        });
//    }

    public void bindWearableAiAspService(){
        // Bind to that service
        if (!mBound){
            Intent intent = new Intent(this, WearableAiAspService.class);
            bindService(intent, wearableAiServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindWearableAiAspService() {
        // Bind to that service
        if (mBound){
            unbindService(wearableAiServiceConnection);
            mBound = false;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection wearableAiServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WearableAiAspService.LocalBinder wearableAiServiceBinder = (WearableAiAspService.LocalBinder) service;
            mService = wearableAiServiceBinder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void connectSmartGlasses(SmartGlassesDevice device){
        this.selectedDevice = device;
        mService.connectToSmartGlasses(device);
        //startWearableAiService();
    }

    public boolean areSmartGlassesConnected(){
//        if (!isMyServiceRunning(WearableAiAspService.class)){
//            return false;
//        } else {
//            return mService.areSmartGlassesConnected();
//        }
        return false;
    }

}
