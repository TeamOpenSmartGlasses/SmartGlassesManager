package com.smartglassesmanager.androidsmartphone.hci.smartrings;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.SmartRingButtonOutputEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class SmartRingBLE {
    private final static String TAG = "WearableIntelligence_SmartRingBLE";

    public static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID TCL_PRESET_CHARACTERISTIC = UUID.fromString("0000ae02-0000-1000-8000-00805f9b34fb");
    public static final UUID TCL_STREAM_CHARACTERISTIC = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");

    public static final UUID OSSR_STREAM_SERVICE = UUID.fromString("000000ff-0000-1000-8000-00805f9b34fb");
    public static final UUID OSSR_STREAM_CHARACTERISTIC = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");

    private boolean mScanning;
    private Handler mHandler;

    private boolean debugLightStatus = false;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private boolean shouldStream=true;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public Boolean lock = false; //check if bluetooth writer/reader is locked before trying to read/write
    public int attempts = 0; //number of attempts to keep retrying connection to bluetooth
    public Boolean tryStill = false; //should we keep retrying to connect, this option is for "Croc'ing" - reconnecting on disconnec
    private NotificationManager mNotificationManager; //manage notifications...
    private Context mContext;

    private boolean screenTouched;
    private boolean screenPressed;

    public SmartRingBLE(Context mContext) {
        this.mContext = mContext;

        screenTouched = false;
        screenPressed = false;
    }

    public void destroy(){
        Log.d(TAG, "destroying Smart Ring connection");
        tryStill = false;
        mHandler.removeCallbacksAndMessages(this);
        disconnect();
        close();
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //tryStill = false;
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                //broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from TCL Smart Ring.");
                attempts++;
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                if (tryStill.equals(true)) {
                    oldConn(mBluetoothDeviceAddress);
                    //broadcastUpdate(intentAction);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered received: " + status);
                //Attempts to setup TCL ring
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        tclRing();
                        ossrRing();
                    }
                }, 50);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            Log.d(TAG, "Char write success");
            lock = false;
        }

        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            lock = false;
        }

        public String byteArrayToHex(byte[] a) {
            StringBuilder sb = new StringBuilder(a.length * 2);
            for(byte b: a)
                sb.append(String.format("%02x", b));
            return sb.toString();
        }

        public void parseButtonStream(byte [] values){
            int buttonByte = 13;
            int SCREEN_TOUCHED_MASK = 0x20;
            int SCREEN_PRESSED_MASK = 0x01;

            int pos = 0;
            for (byte b : values){
                pos++;
                if (pos == buttonByte){
                    boolean currScreenTouched = (b & SCREEN_TOUCHED_MASK) != 0;
                    boolean currScreenPressed = (b & SCREEN_PRESSED_MASK) != 0;

                    if (screenTouched != currScreenTouched){
                        throwScreenTouchedEvent(currScreenTouched);
                        screenTouched = currScreenTouched;
                    }

                    if (screenPressed != currScreenPressed){
                        throwScreenPressedEvent(currScreenPressed);
                        screenPressed = currScreenPressed;
                    }
                }
            }
        }

        private void throwScreenTouchedEvent(boolean touched){
            Log.d(TAG, "Screen touch is: " + touched);
            EventBus.getDefault().post(new SmartRingButtonOutputEvent(0, System.currentTimeMillis(), touched));
        }

        private void throwScreenPressedEvent(boolean pressed){
            Log.d(TAG, "Screen press is: " + pressed);
            //Single press
            EventBus.getDefault().post(new SmartRingButtonOutputEvent(1, System.currentTimeMillis(), pressed));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //if this is a stream from the button, parse it
            if (characteristic.getUuid().equals(TCL_STREAM_CHARACTERISTIC)) {
                byte[] values = characteristic.getValue();
                parseButtonStream(values);
//                Log.d(TAG, byteArrayToHex(values));
            }
        }
    };

    public boolean start() {

        mHandler = new Handler();

        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                connectTclRing();
            }
        });

        return true;
    }

    private void connectTclRing(){
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        Log.d(TAG, "Scanning...");
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 10000);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (device != null) {
                        if (device.getName() != null) {
//                            if (device.getName().contains("RayNeo")) { //the TCL RayNeo X2 Ring
//                                Log.d(TAG, "FOUND TCL RING!!!");
//                                connect(device.getAddress());
//                            }

                            if (device.getName().contains("OSSR")) { //the TCL RayNeo X2 Ring
                                Log.d(TAG, "FOUND OSSR Ring!!!");
                                connect(device.getAddress());
                            }
                        }
                    }
                }
            };

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        Boolean result;

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

//        result = oldConn(address); //first try to connect with an old connection
//
//        if (result.equals(true)){
//            return true;
//        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        attempts = 0; //set attempts to 0 so the callback will keep retrying connection (3 times)
        tryStill = true;

        return true;
    }

    public boolean oldConn(final String address){
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void tclRing(){
        //we have to setup the TCL Ring notification
        List serve = getSupportedGattServices();
        Log.d(TAG, "starting TCL setup");
        Log.d(TAG, "services available: " + getSupportedGattServices());
        for (int i = 0; i < serve.size(); i++){
            BluetoothGattService currServe = (BluetoothGattService) serve.get(i);
            ArrayList chars = giveCharacteristics(currServe);

            //subscribe to preset and streaming characteristics
            for (int j = 0; j < chars.size(); j++){
                BluetoothGattCharacteristic currChar = (BluetoothGattCharacteristic) chars.get(j);
                Log.d(TAG, "Checking: " + currChar.getUuid().toString());
                if ((currChar.getUuid().equals(TCL_PRESET_CHARACTERISTIC)) || (currChar.getUuid().equals(TCL_STREAM_CHARACTERISTIC))) {
                    Log.d(TAG, "Subscribing to TCL Ring streaming characteristic...");
                    mBluetoothGatt.setCharacteristicNotification(currChar, true); //enable on android
                    BluetoothGattDescriptor descriptor = currChar.getDescriptor(CONFIG_DESCRIPTOR);
                    while (lock == true) {
                        //spinning block
                    }
                    lock = true;
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    Log.d(TAG, "subscribe result: " + Boolean.toString(mBluetoothGatt.writeDescriptor(descriptor))); //enable on TCL
                    Log.d(TAG, "subscribed to: " + currChar.getUuid().toString());
                }
            }
        }
    }

    public void ossrRing(){
        //we have to setup the TCL Ring notification
        List serve = getSupportedGattServices();
        Log.d(TAG, "starting OSSR setup");
        Log.d(TAG, "services available: " + getSupportedGattServices());
        for (int i = 0; i < serve.size(); i++){
            BluetoothGattService currServe = (BluetoothGattService) serve.get(i);
            ArrayList chars = giveCharacteristics(currServe);

            //subscribe to preset and streaming characteristics
            for (int j = 0; j < chars.size(); j++){
                BluetoothGattCharacteristic currChar = (BluetoothGattCharacteristic) chars.get(j);
                Log.d(TAG, "Checking: " + currChar.getUuid().toString());
                if (currChar.getUuid().equals(OSSR_STREAM_CHARACTERISTIC)) {
                    Log.d(TAG, "Subscribing to OSSR TOSG Ring streaming characteristic...");
                    mBluetoothGatt.setCharacteristicNotification(currChar, true); //enable on android
                    BluetoothGattDescriptor descriptor = currChar.getDescriptor(CONFIG_DESCRIPTOR);
                    while (lock == true) {
                        //spinning block
                    }
                    lock = true;
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    Log.d(TAG, "subscribe result: " + Boolean.toString(mBluetoothGatt.writeDescriptor(descriptor))); //enable on TCL
                    Log.d(TAG, "subscribed to: " + currChar.getUuid().toString());
                }
            }
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        tryStill = false;
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        close();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            Log.d(TAG, "it was null");
            return null;
        }

        return mBluetoothGatt.getServices();
    }

    private void discoverCharacteristics(BluetoothGattService service) {
        for (BluetoothGattCharacteristic gattCharacteristic : service.getCharacteristics()) {
//            Log.d(TAG, "Discovered UUID: " + gattCharacteristic.getUuid());
        }
    }

    private ArrayList<BluetoothGattCharacteristic> giveCharacteristics(BluetoothGattService service) {
        ArrayList<BluetoothGattCharacteristic> chars = new ArrayList<BluetoothGattCharacteristic>();
        for (BluetoothGattCharacteristic gattCharacteristic : service.getCharacteristics()) {
//            Log.d(TAG, "Discovered UUID: " + gattCharacteristic.getUuid());
            chars.add(gattCharacteristic);
        }

        return chars;
    }

    public int getConnectionState(){
        return mConnectionState;
    }

    public void debugLightToggle(){
        debugLightStatus = !debugLightStatus;
        Log.d(TAG,"Debug light toggle to: " + debugLightStatus);
        int toWrite = 0;
        if (!debugLightStatus){
            toWrite = 1;
        } else {
            toWrite = 0;
        }

        //write that to the bluetooth ring stream characteristic
        Log.d(TAG,"Writing started");
        BluetoothGattService mCustomService = mBluetoothGatt.getService(OSSR_STREAM_SERVICE);
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(OSSR_STREAM_CHARACTERISTIC);
        byte[] byteArray = new byte[4];
        // Little Endian: Least significant byte first
        byteArray[0] = (byte) (toWrite & 0xFF);
        mWriteCharacteristic.setValue(byteArray);
        if(!mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)){
            Log.e(TAG, "Failed to write characteristic");
        }
    }
}
