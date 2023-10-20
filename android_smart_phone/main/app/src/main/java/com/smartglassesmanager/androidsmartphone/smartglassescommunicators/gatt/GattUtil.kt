package com.smartglassesmanager.androidsmartphone.smartglassescommunicators.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.os.Build
import android.util.Log;
import java.util.*
import kotlin.experimental.and

/**
 * Helper functions to work with services and characteristics
 */
@SuppressLint("MissingPermission")
class GattUtil {
    /**
     * Queue for ensuring read/write requests are serialized so that each
     * read/write completes for the the next read/write request is performed
     */
    class RequestQueue( val uuid: UUID) {
        /**
         * Internal object used to manage a specific read or write request
         */
        private class GattRequest(
            requestType: Int,
            gattServer: BluetoothGattServer,
            bluetoothDevice: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic?,
            value: ByteArray? = null
        ) {
            val mRequestType: Int = requestType
            val mCharacteristic: BluetoothGattCharacteristic? = characteristic
            val mValue: ByteArray? = value
            val mGattServer:BluetoothGattServer = gattServer
            val mBluetoothDevice :BluetoothDevice = bluetoothDevice

            companion object {
                const val REQUEST_NOTIFY_CHAR =3
            }
        }

        // Queue containing requests
        private val mRequestQueue: Queue<GattRequest> = ArrayDeque()

        // If true, the request queue is currently processing read/write
        // requests
        private var mIsRunning = false

        @Synchronized
        fun addNotifyCharacteristic(
            gatt: BluetoothGattServer?,
            bluetoothDevice: BluetoothDevice?,
            characteristic: BluetoothGattCharacteristic?,
            value: ByteArray?
        ) {
//            Log.v(TAG, "addWriteCharacteristic(),characteristic: ${characteristic?.uuid}")
            if (gatt == null || characteristic == null || bluetoothDevice==null ) {
                Log.v(TAG, "addWriteCharacteristic(): invalid data")
                return
            }
            mRequestQueue.add(
                GattRequest(
                    GattRequest.REQUEST_NOTIFY_CHAR,
                    gatt,
                    bluetoothDevice,
                    characteristic,
                    value
                )
            )
        }




        /**
         * Get the next queued request, if any, and perform the requested
         * operation
         */
        fun next() {
            Log.d(TAG,"next message " +mRequestQueue.size)

            var request: GattRequest?
            synchronized(this) {
                request = mRequestQueue.poll()
                if (request == null) {
                    mIsRunning = false
                    return
                }
            }
            if (mRequestQueue.size == 0) {
                mIsRunning = false
            }
            when (request!!.mRequestType) {
                GattRequest.REQUEST_NOTIFY_CHAR->{
                    Log.d(TAG,"REQUEST_NOTIFY_CHAR ")

                    if( Build.VERSION.SDK_INT >= 33 ) {
                        val status = request!!.mGattServer.notifyCharacteristicChanged(
                            request!!.mBluetoothDevice,
                            request!!.mCharacteristic!!,
                            false,
                            request!!.mValue!!
                        )
                        Log.d(TAG, "notify ${request!!.mCharacteristic!!.uuid} "+"isSuccess:" + (status == BluetoothGatt.GATT_SUCCESS))
                    }else{
                        val status = request!!.mGattServer.notifyCharacteristicChanged(
                            request!!.mBluetoothDevice,
                            request!!.mCharacteristic!!,
                            false
                        )
                        Log.d(TAG, "isSuccess:$status")
                    }
                }
            }
        }

        /**
         * Start processing the queued requests, if not already started.
         * Otherwise, this method does nothing if processing already started
         */
        fun execute() {
            synchronized(this) {
                Log.v(
                    TAG, "execute: queue size=" + mRequestQueue.size + ", mIsRunning= "
                            + mIsRunning
                )
                if (mIsRunning) {
                    Log.w( TAG, "previous data is still running..")
                    return
                }
                mIsRunning = true
            }
            next()
        }

        fun clear() {
            synchronized(this) {
                mIsRunning = false
            }
            mRequestQueue.clear()
        }
    }

    companion object {
        private const val TAG = "GattUtil"

        /**
         * Get a characteristic with the specified service UUID and characteristic
         * UUID
         *
         * @param gatt
         * @param serviceUuid
         * @param characteristicUuid
         * @return
         */
        fun getCharacteristic(
            gatt: BluetoothGatt?,
            serviceUuid: UUID, characteristicUuid: UUID?
        ): BluetoothGattCharacteristic? {
            Log.d(TAG, "getCharacteristic,gatt: $gatt serviceUuid: $serviceUuid")
            if (gatt == null) {
                Log.d(TAG, "getCharacteristic,gatt: gatt == null")
                return null
            }
            val list = gatt.services
            Log.d(TAG, "getCharacteristic,gatt: list.size():" + list.size)
            for (i in list.indices) {
                Log.d(TAG, "getCharacteristic,gatt: ------------>" + list[i].uuid)
            }
            val service = gatt.getService(serviceUuid)
            if (service == null) {
                Log.d(TAG, "getCharacteristic,gatt: service == null")
                return null
            }
            return service.getCharacteristic(characteristicUuid)
        }

    }
}