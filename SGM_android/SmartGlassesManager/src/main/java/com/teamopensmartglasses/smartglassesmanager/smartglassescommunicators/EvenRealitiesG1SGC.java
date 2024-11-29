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
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
    private boolean debugStopper = false;

    private static final long DELAY_BETWEEN_SENDS_MS = 20;
    private static final long HEARTBEAT_INTERVAL_MS = 12000;

    //heartbeat sender
    private Handler heartbeatHandler = new Handler();
    private Runnable heartbeatRunnable;

    //notification period sender
    private Handler notificationHandler = new Handler();
    private Runnable notificationRunnable;
    private boolean notifysStarted = false;
    private int notificationNum = 10;

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

                    gatt.requestMtu(251); // Request a higher MTU size
                    Log.d(TAG, "Requested MTU size: 251");

                    BluetoothGattService uartService = gatt.getService(UART_SERVICE_UUID);


                    if (uartService != null) {
                        BluetoothGattCharacteristic txChar = uartService.getCharacteristic(UART_TX_CHAR_UUID);
                        BluetoothGattCharacteristic rxChar = uartService.getCharacteristic(UART_RX_CHAR_UUID);

                        if (txChar != null) {
                            if ("Left".equals(side)) leftTxChar = txChar;
                            else rightTxChar = txChar;
                            enableNotification(gatt, rxChar, side);
                            txChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            Log.d(TAG, side + " glass TX characteristic found");
                        }

                        if (rxChar != null) {

                            // Bond the device
                            if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_BONDED) {
                                Log.d(TAG, "Creating bond with device: " + gatt.getDevice().getName());
                                gatt.getDevice().createBond();
                            } else {
                                Log.d(TAG, "Device already bonded: " + gatt.getDevice().getName());
                            }

                            if ("Left".equals(side)) leftRxChar = rxChar;
                            else rightRxChar = rxChar;
                            enableNotification(gatt, rxChar, side);
                            txChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            Log.d(TAG, side + " glass RX characteristic found");
                        }

                        // Request MTU size
                        gatt.requestMtu(251);
                        Log.d(TAG, "Requested MTU size: 251");

                        //no idea why but it's in the Even app - Cayden
                        Log.d(TAG, "Sending 0xF4 Command");
                        sendDataSequentially(new byte[] {(byte) 0xF4, (byte) 0x01});

                        //start heartbeat
                        startHeartbeat();

                        // Start MIC streaming
                        setMicEnabled(true); // Enable the MIC

                        //start sending notifications
                        startPeriodicNotifications();
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
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (characteristic.getUuid().equals(UART_RX_CHAR_UUID)) {
                        byte[] data = characteristic.getValue();

                        // Handle MIC audio data
                        if (data.length > 0 && (data[0] & 0xFF) == 0xF1) {
                            int seq = data[1] & 0xFF; // Sequence number
                            byte[] audioData = Arrays.copyOfRange(data, 2, data.length); // Extract audio data
//                            Log.d(TAG, "Audio data received. Seq: " + seq + ", Data: " + Arrays.toString(audioData));
                        } else {
                            Log.d(TAG, "Received non-audio response: " + bytesToHex(data));
                        }

                        // Check if it's a heartbeat response
                        if (data.length > 0 && data[0] == 0x25) {
                            Log.d(TAG, "Heartbeat response received");
                        }
                    }
                });
            }


        };
    }

    private void enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, String side) {
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean result = gatt.writeDescriptor(descriptor);
            if (result) {
                Log.d(TAG, side + " SIDE," + "Descriptor write successful for characteristic: " + characteristic.getUuid());
            } else {
                Log.e(TAG, side + " SIDE," + "Failed to write descriptor for characteristic: " + characteristic.getUuid());
            }
        } else {
            Log.e(TAG, side + " SIDE," + "Descriptor not found for characteristic: " + characteristic.getUuid());
        }
    }

    private void updateConnectionState() {
        if (isLeftConnected && isRightConnected) {
            mConnectState = 2;
            Log.d(TAG, "Both glasses connected");
            connectionEvent(2);
        } else if (isLeftConnected || isRightConnected) {
            mConnectState = 1;
            Log.d(TAG, "One glass connected");
            connectionEvent(1);
        } else {
            mConnectState = 0;
            Log.d(TAG, "No glasses connected");
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

    private static final int NOTIFICATION = 0x4B; // Notification command
    private String createNotificationJson(String appIdentifier, String title, String subtitle, String message) {
        long currentTime = System.currentTimeMillis() / 1000L; // Unix timestamp in seconds
        String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()); // Date format for 'date' field

        NCSNotification ncsNotification = new NCSNotification(
                notificationNum++,  // Increment sequence ID for uniqueness
                1,             // type (e.g., 1 = notification type)
                appIdentifier,
                title,
                subtitle,
                message,
                (int) currentTime,  // Cast long to int to match Python
                currentDate,        // Add the current date to the notification
                "AugmentOS:" + notificationNum // display_name
        );

        Notification notification = new Notification(ncsNotification, "Add");

        Gson gson = new Gson();
        return gson.toJson(notification);
    }


    class Notification {
        NCSNotification ncs_notification;
        String type;

        public Notification() {
            // Default constructor
        }

        public Notification(NCSNotification ncs_notification, String type) {
            this.ncs_notification = ncs_notification;
            this.type = type;
        }
    }

    class NCSNotification {
        int msg_id;
        int type;
        String app_identifier;
        String title;
        String subtitle;
        String message;
        int time_s;  // Changed from long to int for consistency
        String date; // Added to match Python's date field
        String display_name;

        public NCSNotification(int msg_id, int type, String app_identifier, String title, String subtitle, String message, int time_s, String date, String display_name) {
            this.msg_id = msg_id;
            this.type = type;
            this.app_identifier = app_identifier;
            this.title = title;
            this.subtitle = subtitle;
            this.message = message;
            this.time_s = time_s;
            this.date = date; // Initialize the date field
            this.display_name = display_name;
        }
    }




    private List<byte[]> createNotificationChunks(String json) {
        final int MAX_CHUNK_SIZE = 176; // 180 - 4 header bytes
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int totalChunks = (int) Math.ceil((double) jsonBytes.length / MAX_CHUNK_SIZE);

        List<byte[]> chunks = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            int start = i * MAX_CHUNK_SIZE;
            int end = Math.min(start + MAX_CHUNK_SIZE, jsonBytes.length);
            byte[] payloadChunk = Arrays.copyOfRange(jsonBytes, start, end);

            // Create the header
            byte[] header = new byte[] {
                    (byte) NOTIFICATION,
                    0x00, // notify_id (can be updated as needed)
                    (byte) totalChunks,
                    (byte) i
            };

            // Combine header and payload
            ByteBuffer chunk = ByteBuffer.allocate(header.length + payloadChunk.length);
            chunk.put(header);
            chunk.put(payloadChunk);

            chunks.add(chunk.array());
        }

        return chunks;
    }

    public void displayReferenceCardSimple(String title, String body, int lingerTime) {
//        if (!isConnected()) {
//            Log.d(TAG, "Not connected to glasses");
//            return;
//        }
//        if (debugStopper){
//            return;
//        }
//        debugStopper = true;
//
//        // Create JSON and chunks
////        String json = createNotificationJson("org.telegram.messenger", "Title", "This is sick", "and this is good too");
//        String json = createNotificationJson("org.telegram.messenger", "Test", "message", "Short message");
//        Log.d(TAG, "G1 Generated JSON: " + json);
//        List<byte[]> chunks = createNotificationChunks(json);
//
//        // Log chunks for debugging
//        for (byte[] chunk : chunks) {
//            Log.d(TAG, "Chunk: " + Arrays.toString(chunk));
//        }
//
//        // Send each chunk sequentially
//        for (byte[] chunk : chunks) {
//            sendDataSequentially(chunk);
//        }
    }

    @Override
    public void destroy() {
        setMicEnabled(false); // Disable the MIC
        if (leftGlassGatt != null) {
            leftGlassGatt.disconnect();
            leftGlassGatt.close();
        }
        if (rightGlassGatt != null) {
            rightGlassGatt.disconnect();
            rightGlassGatt.close();
        }

        // Stop periodic notifications
        stopPeriodicNotifications();

        if (pairingReceiver != null) {
            context.unregisterReceiver(pairingReceiver);
        }
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

    //microphone stuff
    public void setMicEnabled(boolean enable) {
        if (!isConnected()) {
            Log.d(TAG, "Tryna start mic: Not connected to glasses");
            return;
        }

        byte command = 0x0E; // Command for MIC control
        byte enableByte = (byte) (enable ? 1 : 0); // 1 to enable, 0 to disable

        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put(command);
        buffer.put(enableByte);

        sendDataSequentially(buffer.array());
        Log.d(TAG, "Sent MIC command: " + bytesToHex(buffer.array()));
    }

    //notifications
    private void startPeriodicNotifications() {
        if (notifysStarted){
            return;
        }
        notifysStarted = true;

        notificationRunnable = new Runnable() {
            @Override
            public void run() {
                // Send notification
                sendPeriodicNotification();

                // Schedule the next notification
                notificationHandler.postDelayed(this, 5000); // 5 seconds
            }
        };

        // Start the first notification after 5 seconds
        notificationHandler.postDelayed(notificationRunnable, 3000);
    }

    private void sendPeriodicNotification() {
        if (!isConnected()) {
            Log.d(TAG, "Cannot send notification: Not connected to glasses");
            return;
        }

        // Example notification data (replace with your actual data)
        String json = createNotificationJson("com.even.test", "QuestionAnswerer", "How much caffeine in dark chocolate?", "25 to 50 grams per piece");
        Log.d(TAG, "the JSON to send: " + json);
        List<byte[]> chunks = createNotificationChunks(json);
//        Log.d(TAG, "THE CHUNKS:");
//        Log.d(TAG, chunks.get(0).toString());
//        Log.d(TAG, chunks.get(1).toString());
        for (byte[] chunk : chunks) {
            Log.d(TAG, "Sent chunk to glasses: " + bytesToUtf8(chunk));
        }

        // Send each chunk with a short sleep between each send
        for (byte[] chunk : chunks) {
            sendDataSequentially(chunk);

            // Sleep for 100 milliseconds between sending each chunk
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "Sent periodic notification");
    }

    private static String bytesToUtf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void stopPeriodicNotifications() {
        if (notificationHandler != null && notificationRunnable != null) {
            notificationHandler.removeCallbacks(notificationRunnable);
            Log.d(TAG, "Stopped periodic notifications");
        }
    }

}
