package com.wearableintelligencesystem.androidsmartglasses.comms.gatt;

import android.bluetooth.BluetoothDevice;

public abstract class DeviceConnectionState {

    private int state;

    private DeviceConnectionState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public static class Connected extends DeviceConnectionState {

        private BluetoothDevice device;

        public Connected(BluetoothDevice device) {
            super(2);
            this.device = device;
        }

        public BluetoothDevice getDevice() {
            return device;
        }
    }

    public static class Connecting extends DeviceConnectionState {
        public Connecting() {
            super(1);
        }
    }

    public static class Disconnected extends DeviceConnectionState {
        public Disconnected() {
            super(0);
        }
    }
}
