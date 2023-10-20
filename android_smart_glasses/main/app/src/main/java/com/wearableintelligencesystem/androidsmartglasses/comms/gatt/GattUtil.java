package com.wearableintelligencesystem.androidsmartglasses.comms.gatt;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class GattUtil {

    public static class RequestQueue {
        private static class GattRequest {
            int mRequestType;
            BluetoothGatt mGatt;
            BluetoothGattCharacteristic mCharacteristic;
            byte[] mValue;

            public GattRequest(int requestType, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
                mRequestType = requestType;
                mGatt = gatt;
                mCharacteristic = characteristic;
                mValue = value;
            }

            static final int REQUEST_WRITE_CHAR = 2;
        }

        private Queue<GattRequest> mRequestQueue = new ArrayDeque<>();
        private boolean mIsRunning = false;
        private final String TAG = "GattUtil";
        private UUID uuid;

        public RequestQueue(UUID uuid) {
            this.uuid = uuid;
        }

        public synchronized void addWriteCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {

            if( gatt == null || characteristic == null ){
                return ;
            }
            mRequestQueue.add( new GattRequest( GattRequest.REQUEST_WRITE_CHAR, gatt, characteristic ,value));
        }
        public void next() {
            GattRequest request;
            synchronized (this){
                request = mRequestQueue.poll();
                Log.v(TAG,"get request");
                if( request == null ){
                    mIsRunning = false;
                    return;
                }
            }
            if(mRequestQueue.size() == 0 ){
                mIsRunning = false;
            }
            switch ( request.mRequestType ){
                case GattRequest.REQUEST_WRITE_CHAR:
                    if( request.mCharacteristic.getUuid() == uuid){
                        request.mCharacteristic.setWriteType( BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    }
                    if( request.mValue != null ){
                        request.mCharacteristic.setValue( request.mValue );
                    }
                    boolean isWrite = request.mGatt.writeCharacteristic( request.mCharacteristic );

                    Log.v(TAG,"Write value size :" + request.mValue.length + " isWrite:" + isWrite);
                    if( !isWrite ){
                        clear();
                    }
                    break;
            }
        }

        public void execute() {
            synchronized (this){
                if( mIsRunning ){
                    return;
                }
                mIsRunning = true;
            }
            next();
        }

        public void clear() {
            synchronized (this){
                mIsRunning = false;
            }
            mRequestQueue.clear();
        }
    }


}
