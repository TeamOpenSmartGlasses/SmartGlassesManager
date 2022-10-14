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
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Main activity of WearableAI compute module android app. */
public class MainActivity extends AppCompatActivity {

    private  final String TAG = "WearableAi_MainActivity";

    public WearableAiAspService mService;
    boolean mBound = false;

    //bottom nav bar
    private BottomNavigationView bottomNavigation;
    private NavController navController;

    //permissions
    //file storage and SMS permissions
    public final String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SEND_SMS};
    public final int EXTERNAL_REQUEST = 138;
    //location permissions
    private int LOCATION_PERMISSION_CODE = 1;
    private int BACKGROUND_LOCATION_PERMISSION_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup action bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        //set main view
        setContentView(R.layout.activity_main);

        //setup the nav bar
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        Log.d(TAG, getSupportFragmentManager().getFragments().toString());
        navController = navHostFragment.getNavController();
        setupBottomNavBar();
        bottomNavigation.setSelectedItemId(R.id.settings_page);

        //before service runs, get permissions
        //requestForPermission(); //files
        checkPermission(); //location

        //start wearable ai service
        startWearableAiService();
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
//       Log.d(TAG, "Kill all the children of service");
//       unbindWearableAiAspService();

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

    //handle permissions
    public boolean requestFilesPermission() {
        boolean isPermissionOn = true;
        final int version = Build.VERSION.SDK_INT;
        if (version >= 23) {
            if (!canAccessExternalSd()) {
                isPermissionOn = false;
                requestPermissions(EXTERNAL_PERMS, EXTERNAL_REQUEST);
            }
        }

        return isPermissionOn;
    }

    public boolean canAccessExternalSd() {
        return (hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private boolean hasPermission(String perm) {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm));

    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Fine Location permission is granted
            // Check if current android version >= 11, if >= 11 check for Background Location permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Background Location Permission is granted so do your work here
                    //now, get file permissions
                    requestFilesPermission();
                } else {
                    // Ask for Background Location Permission
                    askPermissionForBackgroundUsage();
                }
            }
        } else {
            // Fine Location Permission is not granted so ask for permission
            askForLocationPermission();
        }
    }

    private void askForLocationPermission() {
        Log.d(TAG, "run askForLocationPermission");
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed!")
                    .setMessage("Location Permission Needed!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Permission is denied by the user
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    private void askPermissionForBackgroundUsage() {
        Log.d(TAG, "run askPermissionForBackgroundUsage");
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Location permissions needed.")
                    .setMessage("To tag memories and data based on where you were, the WIS app needs to collect location data. Many features won't work without location permissions. To allow, tap \"Allow all the time\" on the next screen.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // User declined for Background Location Permission.
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "run onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted location permission
                // Now check if android version >= 11, if >= 11 check for Background Location Permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Background Location Permission is granted so do your work here
                        //now, get file permissions
                        requestFilesPermission();
                    } else {
                        // Ask for Background Location Permission
                        askPermissionForBackgroundUsage();
                    }
                }
            } else {
                // User denied location permission
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            //now, get file permissions
            requestFilesPermission();
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
        }

    }
    //^^^ handle permissions

    //handle bottom app bar navigation
    private void setupBottomNavBar(){
        bottomNavigation = (BottomNavigationView) findViewById(R.id.bottom_nav_main_menu_nav);
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //this has to be an if-else: https://stackoverflow.com/questions/21005205/what-causes-constant-expression-required-errors-for-the-generated-r-id-xxx-val
                int itemId = item.getItemId();
                if(R.id.memory_page == itemId){
                    Log.d(TAG, "Clicked memory tools");
                    navController.navigate(R.id.nav_memory_tools);
                } else if (R.id.social_page == itemId){
                    Log.d(TAG, "Clicked social tools");
                    navController.navigate(R.id.nav_social_tools);
                } else if (R.id.smart_glasses_debug_page == itemId){
                    Log.d(TAG, "Clicked smart glasses debug page");
                    navController.navigate(R.id.nav_smart_glasses_debug);
                } else if (R.id.settings_page == itemId){
                    Log.d(TAG, "Clicked settings page");
                    navController.navigate(R.id.nav_settings);
                }
                return true;
            }
        });
    }

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

}
