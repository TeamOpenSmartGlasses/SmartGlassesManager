package com.wearableintelligencesystem.androidsmartglasses;

import static com.wearableintelligencesystem.androidsmartglasses.ASPClientSocket.RAW_MESSAGE_JSON_STRING;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.wearableintelligencesystem.androidsmartglasses.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartglasses.comms.gatt.BleConfig;
import com.wearableintelligencesystem.androidsmartglasses.comms.gatt.BleScanner;
import com.wearableintelligencesystem.androidsmartglasses.comms.gatt.DataUtil;
import com.wearableintelligencesystem.androidsmartglasses.comms.gatt.DeviceConnectionState;
import com.wearableintelligencesystem.androidsmartglasses.comms.gatt.GattUtil;
import com.wearableintelligencesystem.androidsmartglasses.comms.gatt.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import io.reactivex.rxjava3.subjects.PublishSubject;

@SuppressLint("MissingPermission")
public class BleClient {

    private static final String TAG = "BleClient";

    private BleScanner mBleScanner;
    private Context mContext;
    private GattClientCallback clientCallback;
    private DeviceConnectionState deviceConnectionState = new DeviceConnectionState.Disconnected();
    private BluetoothDevice mConnectedDevice;
    private BluetoothGatt mGatt;
    private BleConfig mBleConfig;

    private GattUtil.RequestQueue requestQueue;

    private BluetoothGattCharacteristic serverSendMessageCharacteristic;
    private BluetoothGattCharacteristic serverRecMessageCharacteristic;
    private byte[] messageBuffer;
    private String mySourceName;

    PublishSubject<JSONObject> dataObservable;


    private Handler bleHandler;

    public BleClient(Context context, BleConfig bleConfig) {
        mContext = context;
        mBleScanner = new BleScanner(context, bleConfig, new Consumer<List<BluetoothDevice>>() {
            @Override
            public void accept(List<BluetoothDevice> bluetoothDevices) {
                if (bluetoothDevices.size() > 0) {
                    connectToGatt(bluetoothDevices.get(0));
                }
            }
        });
        this.requestQueue = new GattUtil.RequestQueue(bleConfig.recMessageUUID);
        this.mBleConfig = bleConfig;
        mySourceName = "ble_client";

    }

    public void startScan() {
        if (deviceConnectionState.getState() == 0) {
            updateConnection(new DeviceConnectionState.Connecting());
            mBleScanner.startScan();
        }
    }

    public boolean isConnected() {
        return deviceConnectionState.getState() == 2;
    }

    public void stop() {
        mBleScanner.stopScan();
        updateConnection(new DeviceConnectionState.Disconnected());
    }

    public void setObservable(PublishSubject<JSONObject> dataObservable) {
        this.dataObservable = dataObservable;
    }

    private void broadcast(JSONObject data) {
        try {
            String typeOf = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);

            //the first bit does a bunch of if statement. The new, better way of doing things is our syncing of data across ASP and ASG with MessageTypes shared class. Now, we just pass a JSON string through to the UI if it's a MessageType that it needs to see, and let the UI deal with how to parse/handle it
            String[] uiMessages = new String[]{MessageTypes.NATURAL_LANGUAGE_QUERY, MessageTypes.REFERENCE_CARD_SIMPLE_VIEW, MessageTypes.REFERENCE_CARD_IMAGE_VIEW, MessageTypes.ACTION_SWITCH_MODES, MessageTypes.VOICE_COMMAND_STREAM_EVENT, MessageTypes.SCROLLING_TEXT_VIEW_START, MessageTypes.SCROLLING_TEXT_VIEW_STOP};
            for (String uiMessage : uiMessages) {
                if (typeOf.equals(uiMessage)) {
                    final Intent intent = new Intent();
                    intent.setAction(ASPClientSocket.ACTION_UI_DATA);
                    intent.putExtra(RAW_MESSAGE_JSON_STRING, data.toString());
                    mContext.sendBroadcast(intent); //eventually, we won't need to use the activity context, as our service will have its own context to send from

                    //new method
                    final Intent nintent = new Intent();
                    nintent.setAction(typeOf);
                    nintent.putExtra(RAW_MESSAGE_JSON_STRING, data.toString());
                    mContext.sendBroadcast(nintent); //eventually, we won't need to use the activity context, as our service will have its own context to send from
                }
            }

            //manual parsing, kept here until someone updates the ui to handle like above
            if (typeOf.equals(MessageTypes.SCROLLING_TEXT_VIEW_INTERMEDIATE)) {
                String intermediate_transcript = data.getString(MessageTypes.SCROLLING_TEXT_VIEW_TEXT);
                final Intent intent = new Intent();
                intent.putExtra(MessageTypes.SCROLLING_TEXT_VIEW_TEXT, intermediate_transcript);
                intent.setAction(MessageTypes.SCROLLING_TEXT_VIEW_INTERMEDIATE);
                mContext.sendBroadcast(intent); //eventually, we won't need to use the activity context, as our service will have its own context to send from
            } else if (typeOf.equals(MessageTypes.SCROLLING_TEXT_VIEW_FINAL)) {
                String final_transcript = data.getString(MessageTypes.SCROLLING_TEXT_VIEW_TEXT);
                final Intent intent = new Intent();
                intent.putExtra(MessageTypes.SCROLLING_TEXT_VIEW_TEXT, final_transcript);
                intent.setAction(MessageTypes.SCROLLING_TEXT_VIEW_FINAL);
                mContext.sendBroadcast(intent); //eventually, we won't need to use the activity context, as our service will have its own context to send from
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void connectToGatt(BluetoothDevice bluetoothDevice) {
        clientCallback = new GattClientCallback();
        mConnectedDevice = bluetoothDevice;
        mGatt = bluetoothDevice.connectGatt(mContext, false, clientCallback);
    }

    public void updateConnection(DeviceConnectionState deviceConnectionState) {
        Log.d(TAG, "connection change to " + deviceConnectionState.getState());
        if (deviceConnectionState.getState() == 0) {
            mGatt.disconnect();
            mBleScanner.startScan();
            this.mConnectedDevice = null;
        }
        if (deviceConnectionState.getState() == 2) {
            mBleScanner.stopScan();
        }
        this.deviceConnectionState = deviceConnectionState;
    }

    public void sendData(Message.LocalMessage localMessage) {
        if (deviceConnectionState.getState() == 2) {
            Log.d(TAG, "client send data :" + localMessage.getText());
            byte[][] data = DataUtil.encode(localMessage.getText());
            for (int i = 0; data != null && i < data.length; i++) {
                requestQueue.addWriteCharacteristic(mGatt, serverRecMessageCharacteristic, data[i]);
            }
            requestQueue.execute();
        }
    }


    private class GattClientCallback extends BluetoothGattCallback {
        private static final String TAG = "GattClientCallback";

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            boolean isSuccess = status == BluetoothGatt.GATT_SUCCESS;
            boolean isConnected = newState == BluetoothProfile.STATE_CONNECTED;
            Log.d(TAG, "onConnectionStateChange " + isSuccess + " " + isConnected);
            if (isSuccess && isConnected) {
                synchronized (deviceConnectionState) {
                    if (deviceConnectionState.getState() != 2) {
                        deviceConnectionState = new DeviceConnectionState.Connected(mConnectedDevice);
                        mGatt = gatt;
                        updateConnection(new DeviceConnectionState.Connected(mConnectedDevice));
                        gatt.discoverServices();
                    }
                }
            } else if (deviceConnectionState.getState() == 2) {
                updateConnection(new DeviceConnectionState.Disconnected());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(BleClient.TAG, "onCharacteristicChanged " + characteristic.getUuid());
            if (characteristic.getUuid().equals(mBleConfig.messageUUID)) {
                // handle message
                byte[] data = characteristic.getValue();
                if( DataUtil.isStart(data)){
                    messageBuffer = null;
                }
                messageBuffer = DataUtil.decode(data, messageBuffer);
                if (DataUtil.isEnd(data)) {
                    String fullString = new String(messageBuffer);
                    onMessageArrived(fullString);
                    messageBuffer = null;
                }

            }
        }

        private void onMessageArrived(String fullString) {
            Log.d(TAG, "client receive data :" + fullString);
            JSONObject json_message = null;
            try {
                json_message = new JSONObject(fullString);
                json_message.put("local_source", mySourceName); //ad our set name so rest of program knows the source of this message
                broadcast(json_message);
                parseData(json_message);
                dataObservable.onNext(json_message);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }

        private void parseData(JSONObject data) {
            try {
                String typeOf = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
                if (typeOf.equals(MessageTypes.AUDIO_CHUNK_DECRYPTED)) {
                    //sendString(data.getString(MessageTypes.AUDIO_DATA));
                    sendJson(data);
                } else if (typeOf.equals(MessageTypes.VISUAL_SEARCH_QUERY)) {
                    sendJson(data);
                } else if (typeOf.equals(MessageTypes.POV_IMAGE)) {
                    sendJson(data);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void sendJson(JSONObject data) {
            sendData(new Message.LocalMessage(data.toString()));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(mBleConfig.serviceUUID);
                if (service != null) {
                    Log.d(TAG, "service:" + mBleConfig.serviceUUID + " is discovered");
                    serverSendMessageCharacteristic = service.getCharacteristic(mBleConfig.messageUUID);
                    if (serverSendMessageCharacteristic != null) {
                        gatt.setCharacteristicNotification(serverSendMessageCharacteristic, true);
                        BluetoothGattDescriptor descriptor = serverSendMessageCharacteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        if (descriptor != null) {
                            Log.d(BleClient.TAG, "read descriptor " + descriptor.getUuid());
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            boolean isWrite = gatt.writeDescriptor(descriptor);
                            Log.e(BleClient.TAG, "Descriptor write!"+isWrite);

                        } else {
                            Log.e(BleClient.TAG, "Descriptor not found");
                        }
                        serverRecMessageCharacteristic = service.getCharacteristic(UUID.fromString("7E7A7486-16F7-9ABC-DEF1-48616F796E6B"));
                        sendData(new Message.LocalMessage("hello"));
                    } else {
                        Log.e(TAG, "Characteristic not found");
                    }
                } else {
                    Log.e(TAG, "Service not found");
                }
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            requestQueue.next();
        }
    }
}
