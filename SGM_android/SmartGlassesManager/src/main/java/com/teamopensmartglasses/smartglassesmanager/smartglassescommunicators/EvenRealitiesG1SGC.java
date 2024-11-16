package com.teamopensmartglasses.smartglassesmanager.smartglassescommunicators;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import android.bluetooth.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class EvenRealitiesG1SGC extends SmartGlassesCommunicator {
    private static final String TAG = "WearableAi_EvenRealitiesG1SGC";

    private static final UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID UART_TX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID UART_RX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Context context;
    private BluetoothGatt leftGlassGatt;
    private BluetoothGatt rightGlassGatt;
    private BluetoothGattCharacteristic leftTxChar;
    private BluetoothGattCharacteristic rightTxChar;
    private BluetoothGattCharacteristic leftRxChar;
    private BluetoothGattCharacteristic rightRxChar;
    private final Handler handler = new Handler();
    private final Semaphore sendSemaphore = new Semaphore(1);
    private boolean isLeftConnected = false;
    private boolean isRightConnected = false;
    private int currentSeq = 0;
    private boolean stopper = false;

    private static final long DELAY_BETWEEN_SENDS_MS = 400;
    private static final long HEARTBEAT_INTERVAL_MS = 5000;

    private Handler heartbeatHandler = new Handler();
    private Runnable heartbeatRunnable;

    public EvenRealitiesG1SGC(Context context) {
        super();
        this.context = context;
        mConnectState = 0;
    }

    private final BluetoothGattCallback leftGattCallback = createGattCallback("Left");
    private final BluetoothGattCallback rightGattCallback = createGattCallback("Right");

    private BluetoothGattCallback createGattCallback(String side) {
        return new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, side + " glass connected, discovering services...");
                    gatt.discoverServices();
                    if ("Left".equals(side)) isLeftConnected = true;
                    else isRightConnected = true;
                    updateConnectionState();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if ("Left".equals(side)) isLeftConnected = false;
                    else isRightConnected = false;
                    updateConnectionState();
                    Log.d(TAG, side + " glass disconnected");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService uartService = gatt.getService(UART_SERVICE_UUID);
                    if (uartService != null) {
                        BluetoothGattCharacteristic txChar = uartService.getCharacteristic(UART_TX_CHAR_UUID);
                        BluetoothGattCharacteristic rxChar = uartService.getCharacteristic(UART_RX_CHAR_UUID);

                        if (txChar != null) {
                            if ("Left".equals(side)) leftTxChar = txChar;
                            else rightTxChar = txChar;
                            Log.d(TAG, side + " glass TX characteristic found");
                        }

                        if (rxChar != null) {
                            if ("Left".equals(side)) leftRxChar = rxChar;
                            else rightRxChar = rxChar;
                            enableNotification(gatt, rxChar);
                            Log.d(TAG, side + " glass RX characteristic found");
                        }

                        startHeartbeat();
                    } else {
                        Log.e(TAG, side + " glass UART service not found");
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, side + " glass write successful");
                } else {
                    Log.e(TAG, side + " glass write failed with status: " + status);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                if (characteristic.getUuid().equals(UART_RX_CHAR_UUID)) {
                    byte[] data = characteristic.getValue();
                    Log.d(TAG, "Received response: " + bytesToHex(data));

                    // Check if it's a heartbeat response
                    if (data.length > 0 && data[0] == 0x25) {
                        Log.d(TAG, "Heartbeat response received");
                    }
                }
            }
        };
    }

    private void enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    private void updateConnectionState() {
        if (isLeftConnected && isRightConnected) {
            mConnectState = 2;
            connectionEvent(2);
        } else if (isLeftConnected || isRightConnected) {
            mConnectState = 1;
            connectionEvent(1);
        } else {
            mConnectState = 0;
            connectionEvent(0);
        }
    }

    // Pairing functionality
    private void pairDevice(BluetoothDevice device) {
        try {
            Log.d(TAG, "Attempting to pair with device: " + device.getName());
            Method method = device.getClass().getMethod("createBond");
            method.invoke(device);
        } catch (Exception e) {
            Log.e(TAG, "Pairing failed: " + e.getMessage());
        }
    }

    private final BroadcastReceiver pairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);

                if (bondState == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "Paired with device: " + device.getName());
                    connectToGatt(device);
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "Pairing failed or unpaired: " + device.getName());
                }
            }
        }
    };

    private void connectToGatt(BluetoothDevice device) {
        if (device.getName().contains("_L_") && leftGlassGatt == null) {
            leftGlassGatt = device.connectGatt(context, false, leftGattCallback);
        } else if (device.getName().contains("_R_") && rightGlassGatt == null) {
            rightGlassGatt = device.connectGatt(context, false, rightGattCallback);
        }
    }

    @Override
    public void connectToSmartGlasses() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {
            String name = device.getName();
            if (name != null && name.contains("G1")) {
                int bondState = device.getBondState();
                if (bondState != BluetoothDevice.BOND_BONDED) {
                    pairDevice(device);
                } else {
                    connectToGatt(device);
                }
            }
        };

        bluetoothAdapter.startLeScan(leScanCallback);
        handler.postDelayed(() -> bluetoothAdapter.stopLeScan(leScanCallback), 10000);
    }

    private byte[] createTextPackage(String text, int currentPage, int totalPages, int screenStatus) {
        byte[] textBytes = text.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(9 + textBytes.length);
        buffer.put((byte) 0x4E);
        buffer.put((byte) (currentSeq++ & 0xFF));
        buffer.put((byte) 1);
        buffer.put((byte) 0);
        buffer.put((byte) screenStatus);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) currentPage);
        buffer.put((byte) totalPages);
        buffer.put(textBytes);

        return buffer.array();
    }

    private void sendDataSequentially(byte[] data) {
        if (stopper) return;
        stopper = true;

        new Thread(() -> {
            try {
                if (leftGlassGatt != null && leftTxChar != null) {
                    leftTxChar.setValue(data);
                    leftGlassGatt.writeCharacteristic(leftTxChar);
                    Thread.sleep(DELAY_BETWEEN_SENDS_MS);
                }

                if (rightGlassGatt != null && rightTxChar != null) {
                    rightTxChar.setValue(data);
                    rightGlassGatt.writeCharacteristic(rightTxChar);
                    Thread.sleep(DELAY_BETWEEN_SENDS_MS);
                }
                stopper = false;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error sending data: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void displayReferenceCardSimple(String title, String body) {
        displayReferenceCardSimple(title, body, 20);
    }

    public void displayReferenceCardSimple(String title, String body, int lingerTime) {
        if (!isConnected()) {
            Log.d(TAG, "Not connected to glasses");
            return;
        }
        String displayText = title.isEmpty() ? body : title + "\n" + body;
        byte[] data = createTextPackage(displayText, 1, 1, 0x30);
        sendDataSequentially(data);
//        data = createTextPackage(displayText, 1, 1, 0x40);
//        sendDataSequentially(data);
    }

    @Override
    public void destroy() {
        if (leftGlassGatt != null) {
            leftGlassGatt.disconnect();
            leftGlassGatt.close();
        }
        if (rightGlassGatt != null) {
            rightGlassGatt.disconnect();
            rightGlassGatt.close();
        }

        context.unregisterReceiver(pairingReceiver);
        stopHeartbeat();
    }

    @Override
    public boolean isConnected() {
        return mConnectState == 2;
    }

    // Remaining methods
    @Override
    public void displayCenteredText(String text) {}

    @Override
    public void showNaturalLanguageCommandScreen(String prompt, String naturalLanguageInput) {}

    @Override
    public void updateNaturalLanguageCommandScreen(String naturalLanguageArgs) {}

    @Override
    public void scrollingTextViewIntermediateText(String text) {}

    @Override
    public void scrollingTextViewFinalText(String text) {}

    @Override
    public void stopScrollingTextViewMode() {}

    @Override
    public void displayPromptView(String title, String[] options) {}

    @Override
    public void displayTextLine(String text) {}

    @Override
    public void displayBitmap(Bitmap bmp) {}

    public void blankScreen() {}

    public void displayDoubleTextWall(String textTop, String textBottom) {}

    public void showHomeScreen() {}

    @Override
    public void setFontSize(SmartGlassesFontSize fontSize) {}

    public void displayRowsCard(String[] rowStrings) {}

    public void displayBulletList(String title, String[] bullets) {}

    public void displayReferenceCardImage(String title, String body, String imgUrl) {}

    public void displayTextWall(String a) {}

    public void setFontSizes() {}

    // Heartbeat methods
    private byte[] constructHeartbeat() {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put((byte) 0x25);
        buffer.put((byte) 6);
        buffer.put((byte) (currentSeq & 0xFF));
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x04);
        buffer.put((byte) (currentSeq++ & 0xFF));
        return buffer.array();
    }

    private void startHeartbeat() {
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        };
        heartbeatHandler.post(heartbeatRunnable);
    }

    private void stopHeartbeat() {
        if (heartbeatHandler != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    private void sendHeartbeat() {
        byte[] heartbeatPacket = constructHeartbeat();
        Log.d(TAG, "Sending heartbeat: " + bytesToHex(heartbeatPacket));

        new Thread(() -> {
            try {
                if (leftGlassGatt != null && leftTxChar != null) {
                    leftTxChar.setValue(heartbeatPacket);
                    leftGlassGatt.writeCharacteristic(leftTxChar);
                }
                if (rightGlassGatt != null && rightTxChar != null) {
                    rightTxChar.setValue(heartbeatPacket);
                    rightGlassGatt.writeCharacteristic(rightTxChar);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending heartbeat: " + e.getMessage());
            }
        }).start();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
