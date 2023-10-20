package com.smartglassesmanager.androidsmartphone.smartglassescommunicators.gatt

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartglassesmanager.androidsmartphone.eventbusmessages.SmartGlassesConnectionEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean


@SuppressLint("MissingPermission")
object BleServer {
    
    const val TAG = "BleServer"

    // hold reference to app context to run the chat server
    private var app: Application? = null
    private lateinit var bluetoothManager: BluetoothManager

    // BluetoothAdapter should never be null if the app is installed from the Play store
    // since BLE is required per the <uses-feature> tag in the AndroidManifest.xml.
    // If the app is installed on an emulator without bluetooth then the app will crash
    // on launch since installing via Android Studio bypasses the <uses-feature> flags
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    // This property will be null if bluetooth is not enabled or if advertising is not
    // possible on the device
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private lateinit var advertiseSettings: AdvertiseSettings
    private lateinit var advertiseData: AdvertiseData

    // LiveData for reporting the messages sent to the device
    private val _messages = MutableLiveData<Message>()
    val messages = _messages as LiveData<Message>

    // LiveData for reporting connection requests
    private val _connectionRequest = MutableLiveData<BluetoothDevice>()
    val connectionRequest = _connectionRequest as LiveData<BluetoothDevice>

    // LiveData for reporting the messages sent to the device
    private val _requestEnableBluetooth = MutableLiveData<Boolean>()
    val requestEnableBluetooth = _requestEnableBluetooth as LiveData<Boolean>

    private var gattServer: BluetoothGattServer? = null
    private var gattServerCallback: BluetoothGattServerCallback? = null

    private var gattClient: BluetoothGatt? = null
    private var gattClientCallback: BluetoothGattCallback? = null

    // Properties for current chat device connection
    private var currentDevice: BluetoothDevice? = null
    private val _deviceConnection =
        MutableLiveData<DeviceConnectionState>()
    val deviceConnection = _deviceConnection as LiveData<DeviceConnectionState>
    private var messageCharacteristic: BluetoothGattCharacteristic? = null

    private var recMessageCharacteristic:BluetoothGattCharacteristic? = null

    private lateinit var mRequestQueue :GattUtil.RequestQueue
    private var mMessageBuffer: ByteArray? = null
    private lateinit var bleConfig: BleConfig

    private val isStart = AtomicBoolean(false)
    fun startServer(app: Context,bleConfig: BleConfig) {
        if( isStart.get() ){
            return
        }
        isStart.set( true )
        this.bleConfig = bleConfig
        EventBus.getDefault().register(this)
        mRequestQueue = GattUtil.RequestQueue(bleConfig.serverReceiveMessageUUID)
        advertiseData = buildAdvertiseData()
        advertiseSettings = buildAdvertiseSettings()
        bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!adapter.isEnabled) {
            // prompt the user to enable bluetooth
            Log.e(TAG,"BluetoothAdapter is disabled!!")
            _requestEnableBluetooth.value = true
        } else {
            _requestEnableBluetooth.value = false
            setupGattServer(app)
            startAdvertisement()
        }
    }

    fun stopServer() {

        if(!isStart.get() ){
            return
        }
        isStart.set(false)
        stopAdvertising()
        EventBus.getDefault().unregister(this)
        gattServer?.close()
    }

    /**
     * The questions of how to obtain a device's own MAC address comes up a lot. The answer is
     * you cannot; it would be a security breach. Only system apps can get that permission.
     * Otherwise apps might use that address to fingerprint a device (e.g. for advertising, etc.)
     * A user can find their own MAC address through Settings, but apps cannot find it.
     * This method, which some might be tempted to use, returns a default value,
     * usually 02:00:00:00:00:00
     */
    fun getYourDeviceAddress(): String = bluetoothManager.adapter.address

    fun setCurrentChatConnection(device: BluetoothDevice) {
        currentDevice = device
        // Set gatt so BluetoothChatFragment can display the device data
        _deviceConnection.value =
            DeviceConnectionState.Connected(device)
        connectToChatDevice(device)
    }

    fun disconnectDevice(){
        if( currentDevice != null ) {
            gattServer?.cancelConnection(currentDevice)
        }
        currentDevice = null
        _deviceConnection.value =
            DeviceConnectionState.Disconnected
    }

    @Subscribe
    fun onConnectionStateChanged( smartGlassesConnectionEvent: SmartGlassesConnectionEvent){
        if( smartGlassesConnectionEvent.connectionStatus == DeviceConnectionState.Disconnected.state ){
            mMessageBuffer = null
            when( deviceConnection.value ){
                is DeviceConnectionState.Connected->{
                    disconnectDevice()
                }
                DeviceConnectionState.Connecting->{
                    disconnectDevice()
                }
                DeviceConnectionState.Disconnected->{
                }
                else ->{

                }
            }
        }
    }

    @Subscribe
    fun onMessageArrived(message:Message.LocalMessage){
        sendMessage( message.text )
    }


    private fun connectToChatDevice(device: BluetoothDevice) {
//        gattClientCallback = GattClientCallback()
//        gattClient = device.connectGatt(app, false, gattClientCallback)
    }

    fun sendMessage(message: String): Boolean {
        Log.d(TAG, "Send a message $message")
        if( currentDevice == null ){
            return false
        }
        messageCharacteristic?.let { characteristic ->
            val data = BLEDataUtil.encode(message)
            if (data == null) {
                Log.d(TAG, "encode message error")
                return false
            }
            mRequestQueue.clear()
           data.forEach {
               characteristic.setValue( it )
               mRequestQueue.addNotifyCharacteristic( gattServer, currentDevice,characteristic,it)
           }
            mRequestQueue.execute()

//            data.forEach { mRequestQueue.addWriteCharacteristic(gattClient, characteristic, it) }
//            mRequestQueue.execute()
            _messages.postValue( Message.LocalMessage(message))
            return true
        }
        return false
    }

    /**
     * Function to setup a local GATT server.
     * This requires setting up the available services and characteristics that other devices
     * can read and modify.
     */
    private fun setupGattServer(app: Context) {
        gattServerCallback = GattServerCallback()
        Log.d(TAG,"openGattServer")
        gattServer = bluetoothManager.openGattServer(
            app,
            gattServerCallback
        ).apply {
            addService(setupGattService())
        }
    }

    /**
     * Function to create the GATT Server with the required characteristics and descriptors
     */
    private fun setupGattService(): BluetoothGattService {
        // Setup gatt service
        val service = BluetoothGattService(
            bleConfig.serviceUUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        // need to ensure that the property is writable and has the write permission
        messageCharacteristic = BluetoothGattCharacteristic(
            bleConfig.serverSendMessageUUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE  or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        messageCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        val descriptor = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_WRITE
        )
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        messageCharacteristic?.addDescriptor(descriptor)
        service.addCharacteristic(messageCharacteristic)

        recMessageCharacteristic = BluetoothGattCharacteristic(
            bleConfig.serverReceiveMessageUUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ ,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PROPERTY_READ
        )
        recMessageCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        service.addCharacteristic(recMessageCharacteristic)
        val confirmCharacteristic = BluetoothGattCharacteristic(
            bleConfig.confirmUUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PROPERTY_READ
        )
        service.addCharacteristic(confirmCharacteristic)

        return service
    }

    /**
     * Start advertising this device so other BLE devices can see it and connect
     */
    private fun startAdvertisement() {
        advertiser = adapter.bluetoothLeAdvertiser
        Log.d(
            TAG,
            "startAdvertisement: with advertiser $advertiser"
        )

        if (advertiseCallback == null) {
            advertiseCallback = DeviceAdvertiseCallback()

            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }

    /**
     * Stops BLE Advertising.
     */
    private fun stopAdvertising() {
        Log.d(
            TAG,
            "Stopping Advertising with advertiser $advertiser"
        )
        advertiser?.stopAdvertising(advertiseCallback)
        advertiseCallback = null
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private fun buildAdvertiseData(): AdvertiseData {
        val dataBuilder = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(bleConfig.serviceUUID))
            .setIncludeDeviceName(false)
        return dataBuilder.build()
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTimeout(0)
            .build()
    }

    /**
     * Custom callback for the Gatt Server this device implements
     */
    private class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED
            Log.d(
                TAG,
                "onConnectionStateChange: Server $device ${device.name} success: $isSuccess connected: $isConnected"
            )
            if (isSuccess && isConnected) {
                _connectionRequest.postValue(device)
            } else {
                _deviceConnection.postValue(DeviceConnectionState.Disconnected)
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            Log.d(TAG,"onDescriptorWriteRequest ${descriptor?.uuid}")
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
            Log.d("GattUtil","onNotificationSent")
            mRequestQueue.next()
        }
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?,
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            if (characteristic.uuid.equals( bleConfig.serverReceiveMessageUUID)) {
                if (value != null) {
                    mMessageBuffer = BLEDataUtil.decode(
                        value,
                        mMessageBuffer
                    )
                    var fullString: String? = null
                    if (BLEDataUtil.isEnd(value)) {
                        fullString = String(mMessageBuffer!!)
                        Log.d(TAG,"get message :$fullString")
                        fullString?.let {
                            _messages.postValue(
                                Message.RemoteMessage(
                                    it
                                )
                            )
                        }
                        mMessageBuffer = null
                    }
                }
            }
        }
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class DeviceAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            // Send error state to display
            val errorMessage = "Advertise failed with error: $errorCode"
            Log.d(TAG, "Advertising failed $errorMessage")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
        }
    }
}