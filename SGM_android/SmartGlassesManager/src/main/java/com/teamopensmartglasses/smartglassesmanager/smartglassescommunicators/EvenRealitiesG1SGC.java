package com.teamopensmartglasses.smartglassesmanager.smartglassescommunicators;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

public class EvenRealitiesG1SGC extends SmartGlassesCommunicator {
    private static final String TAG = "WearableAi_EvenRealitiesG1SGC";

    // BLE UUIDs from the protocol
    private static final UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID UART_TX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID UART_RX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    private Context context;
    private BluetoothGatt leftGlassGatt;
    private BluetoothGatt rightGlassGatt;
    private BluetoothGattCharacteristic leftTxChar;
    private BluetoothGattCharacteristic rightTxChar;
    private final Handler handler = new Handler();
    private final Semaphore sendSemaphore = new Semaphore(1);
    private boolean isLeftConnected = false;
    private boolean isRightConnected = false;
    private int currentSeq = 0;
    private static final int DEFAULT_LINGER_TIME = 15;

    public EvenRealitiesG1SGC(Context context) {
        super();
        this.context = context;
        mConnectState = 0;
    }

    private final BluetoothGattCallback rightGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Right glass connected, discovering services...");
                gatt.discoverServices();
                isRightConnected = true;
                updateConnectionState();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isRightConnected = false;
                updateConnectionState();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService uartService = gatt.getService(UART_SERVICE_UUID);
                if (uartService != null) {
                    rightTxChar = uartService.getCharacteristic(UART_TX_CHAR_UUID);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Right glass write successful");
            }
        }
    };

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

    // Add this as a class field
    private BluetoothAdapter.LeScanCallback leScanCallback;

    @Override
    public void connectToSmartGlasses() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Create the callback
        leScanCallback = (device, rssi, scanRecord) -> {
            String name = device.getName();
            if (name != null && name.contains("G1")) {
                if (name.contains("_L_") && leftGlassGatt == null) {
                    leftGlassGatt = device.connectGatt(context, false, leftGattCallback);
                } else if (name.contains("_R_") && rightGlassGatt == null) {
                    rightGlassGatt = device.connectGatt(context, false, rightGattCallback);
                }
            }
        };

        // Start scanning with the callback
        bluetoothAdapter.startLeScan(leScanCallback);

        // Stop scanning after 10 seconds
        handler.postDelayed(() -> bluetoothAdapter.stopLeScan(leScanCallback), 10000);
    }

//    private byte[] createTextPackage(String text, int currentPage, int totalPages) {
//        byte[] textBytes = text.getBytes();
//        int totalPackages = (textBytes.length + 180) / 181; // Max 181 bytes per package
//
//        ByteBuffer buffer = ByteBuffer.allocate(9 + textBytes.length);
//        buffer.put((byte)0x4E);                    // Command
//        buffer.put((byte)(currentSeq++ & 0xFF));   // Sequence number
//        buffer.put((byte)totalPackages);           // Total packages
//        buffer.put((byte)0);                       // Current package
//        buffer.put((byte)0x31);                    // Screen status (0x30 | 0x01)
//        buffer.put((byte)0);                       // new_char_pos0
//        buffer.put((byte)0);                       // new_char_pos1
//        buffer.put((byte)currentPage);             // Current page
//        buffer.put((byte)totalPages);              // Max pages
//        buffer.put(textBytes);                     // Text data
//
//        return buffer.array();
//    }

    public boolean stopper = false;
//    private void sendDataSequentially(byte[] data) {
//        if (stopper){
//            return;
//        }
//        stopper = true;
//        Log.d(TAG, "Sending data to G1...");
//        try {
//            // First send to left glass
//            sendSemaphore.acquire();
//            if (leftGlassGatt != null && leftTxChar != null) {
//                leftTxChar.setValue(data);
//                leftGlassGatt.writeCharacteristic(leftTxChar);
//            }
//
//            // Wait for left glass acknowledgment before sending to right
//            sendSemaphore.acquire();
//            if (rightGlassGatt != null && rightTxChar != null) {
//                rightTxChar.setValue(data);
//                rightGlassGatt.writeCharacteristic(rightTxChar);
//            }
//        } catch (InterruptedException e) {
//            Log.e(TAG, "Interrupted while sending data", e);
//        }
//    }

    @Override
    public void displayReferenceCardSimple(String title, String body) {
        displayReferenceCardSimple(title, body, DEFAULT_LINGER_TIME);
    }

//    public void displayReferenceCardSimple(String title, String body, int lingerTime) {
//        if (!isConnected()) {
//            Log.d(TAG, "Not connected to glasses");
//            return;
//        }
//
//        String displayText = title.isEmpty() ? body : title + "\n" + body;
//        byte[] data = createTextPackage(displayText, 1, 1);
//        sendDataSequentially(data);
//    }

    public void displayReferenceCardSimple(String title, String body, int lingerTime) {
        if (!isConnected()) {
            Log.d(TAG, "Not connected to glasses");
            return;
        }

        // Format text
        String displayText = title.isEmpty() ? body : title + "\n" + body;

        // Create and send package
        byte[] data = createTextPackage(displayText, 1, 1);
        Log.d(TAG, "Sending package: " + bytesToHex(data));
        sendDataSequentially(data);
    }







    ///////newwwwwwwwwwwwwwwwww
    // Add this at class level
    private static final long WRITE_TIMEOUT_MS = 1000;
    private static final long DELAY_BETWEEN_SENDS_MS = 400; // 0.4 seconds delay between sends

    private byte[] createTextPackage(String text, int currentPage, int totalPages) {
        byte[] textBytes = text.getBytes();
        Log.d(TAG, "Creating text package for text of length: " + textBytes.length);

        ByteBuffer buffer = ByteBuffer.allocate(9 + textBytes.length);
        buffer.put((byte)0x4E);                    // Command
        buffer.put((byte)(currentSeq++ & 0xFF));   // Sequence number
        buffer.put((byte)1);                       // Total packages (just sending as 1 for now)
        buffer.put((byte)0);                       // Current package
//        buffer.put((byte)0x31);                    // Screen status (0x30 | 0x01)
        buffer.put((byte)0x51);  // Only display new content without Even AI
        buffer.put((byte)0);                       // new_char_pos0
        buffer.put((byte)0);                       // new_char_pos1
        buffer.put((byte)currentPage);             // Current page number
        buffer.put((byte)totalPages);              // Total pages
        buffer.put(textBytes);                     // Text data

        byte[] result = buffer.array();
        Log.d(TAG, "Created package: " + bytesToHex(result));
        return result;
    }

    private void sendDataSequentially(byte[] data) {
        if (stopper) {
            return;
        }
        stopper = true;

        new Thread(() -> {
            try {
                if (leftGlassGatt != null && leftTxChar != null) {
                    leftTxChar.setValue(data);
                    if (!leftGlassGatt.writeCharacteristic(leftTxChar)) {
                        stopper = false;
                        return;
                    }
                    Thread.sleep(DELAY_BETWEEN_SENDS_MS);
                }

                if (rightGlassGatt != null && rightTxChar != null) {
                    rightTxChar.setValue(data);
                    if (!rightGlassGatt.writeCharacteristic(rightTxChar)) {
                        stopper = false;
                        return;
                    }
                }
                Thread.sleep(DELAY_BETWEEN_SENDS_MS);

                byte[] completeData = data.clone();
                completeData[4] = 0x40;

                if (leftGlassGatt != null && leftTxChar != null) {
                    leftTxChar.setValue(completeData);
                    leftGlassGatt.writeCharacteristic(leftTxChar);
                    Thread.sleep(DELAY_BETWEEN_SENDS_MS);
                }

                if (rightGlassGatt != null && rightTxChar != null) {
                    rightTxChar.setValue(completeData);
                    rightGlassGatt.writeCharacteristic(rightTxChar);
                }

                Thread.sleep(DELAY_BETWEEN_SENDS_MS);
                stopper = false;

            } catch (Exception e) {
                Log.e(TAG, "Error sending data: " + e.getMessage());
                stopper = false;
            }
        }).start();
    }

    // Updated callbacks to include better logging
    private final BluetoothGattCallback leftGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Left glass connection state change: status=" + status + " newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Left glass connected, discovering services...");
                gatt.discoverServices();
                isLeftConnected = true;
                updateConnectionState();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isLeftConnected = false;
                updateConnectionState();
                Log.d(TAG, "Left glass disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Left glass services discovered: status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService uartService = gatt.getService(UART_SERVICE_UUID);
                if (uartService != null) {
                    leftTxChar = uartService.getCharacteristic(UART_TX_CHAR_UUID);
                    if (leftTxChar != null) {
                        Log.d(TAG, "Left glass TX characteristic found");
                    } else {
                        Log.e(TAG, "Left glass TX characteristic not found");
                    }
                } else {
                    Log.e(TAG, "Left glass UART service not found");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "Left glass characteristic write: status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Left glass write successful");
            } else {
                Log.e(TAG, "Left glass write failed");
            }
        }
    };

    // Similar updates for rightGattCallback...
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    @Override
    public void displayTextWall(String text) {
        if (!isConnected()) {
            Log.d(TAG, "Not connected to glasses");
            return;
        }

        byte[] data = createTextPackage(text, 1, 1);
        sendDataSequentially(data);
    }

    @Override
    protected void setFontSizes() {
        // Empty implementation
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
    }

    @Override
    public boolean isConnected() {
        return mConnectState == 2;
    }

    // Empty implementations of other required methods
    public void displayCenteredText(String text) {}
    public void showNaturalLanguageCommandScreen(String prompt, String naturalLanguageInput) {}
    public void updateNaturalLanguageCommandScreen(String naturalLanguageArgs) {}

    @Override
    public void scrollingTextViewIntermediateText(String text) {

    }

    @Override
    public void scrollingTextViewFinalText(String text) {

    }

    @Override
    public void stopScrollingTextViewMode() {

    }

    @Override
    public void displayPromptView(String title, String[] options) {

    }

    @Override
    public void displayTextLine(String text) {

    }

    @Override
    public void displayBitmap(Bitmap bmp) {

    }

    public void blankScreen() {}
    public void displayDoubleTextWall(String textTop, String textBottom) {}
    public void showHomeScreen() {}

    @Override
    public void setFontSize(SmartGlassesFontSize fontSize) {

    }

    public void displayRowsCard(String[] rowStrings) {}
    public void displayBulletList(String title, String[] bullets) {}
    public void displayReferenceCardImage(String title, String body, String imgUrl) {}
}