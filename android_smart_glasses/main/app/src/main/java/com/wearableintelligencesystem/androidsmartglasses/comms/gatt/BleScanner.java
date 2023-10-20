package com.wearableintelligencesystem.androidsmartglasses.comms.gatt;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@SuppressLint("MissingPermission")
public class BleScanner {
    
    private final Context context;
    private final BleConfig bleConfig;
    private final Consumer<List<BluetoothDevice>> scanCallback;
    private final BluetoothAdapter adapter;
    private final Map<String, BluetoothDevice> scanResults = new HashMap<>();
    private final List<ScanFilter> mScanFilters;
    private final ScanSettings mScanSettings;
    private final BluetoothLeScanner mScanner;
    private final AtomicBoolean isScan = new AtomicBoolean(false);
    private final ScanCallback deviceScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult:"+result.getDevice());

            BluetoothDevice device = result.getDevice();
            if (device != null) {
                scanResults.put(device.getAddress(), device);
            }
            scanCallback.accept(new ArrayList<>(scanResults.values()));
        }

        @Override
        public void onScanFailed(int errorCode) {

            Log.d(TAG, "Scan Failed!"+errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults" + results.size());
            for (ScanResult item : results) {
                BluetoothDevice device = item.getDevice();
                if (device != null) {
                    scanResults.put(device.getAddress(), device);
                }
            }
            scanCallback.accept(new ArrayList<>(scanResults.values()));
        }
    };

    public BleScanner(Context context, BleConfig bleConfig, Consumer<List<BluetoothDevice>> scanCallback) {
        this.context = context;
        this.bleConfig = bleConfig;
        this.scanCallback = scanCallback;
        this.adapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        this.mScanFilters = buildScanFilters();
        this.mScanSettings = buildScanSettings();
        this.mScanner = adapter.getBluetoothLeScanner();
    }

    public void startScan() {
        adapter.enable();
        Log.d("BleScanner", "BluetoothAdapter.isEnabled :" + adapter.isEnabled());
        if (mScanner != null && !isScan.get()) {
            isScan.set(true);
            Log.d("BleScanner", mScanner + " start scanning...");
            mScanner.startScan(mScanFilters, mScanSettings, deviceScanCallback);
        } else {
            Log.e("BleScanner", "BluetoothLeScanner is null");
        }
    }

    public BluetoothDevice lastDevice(String address) {
        return adapter.getRemoteDevice(address);
    }

    public void stopScan() {
        if (mScanner != null && isScan.get()) {
            isScan.set(false);
            mScanner.stopScan(deviceScanCallback);
        } else {
            Log.e("BleScanner", "BluetoothLeScanner is null");
        }
    }

    private List<ScanFilter> buildScanFilters() {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(new ParcelUuid(bleConfig.serviceUUID));
        ScanFilter filter = builder.build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);
        return filters;
    }

    private ScanSettings buildScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();
    }

    private static final String TAG = "BleScanner";
}
