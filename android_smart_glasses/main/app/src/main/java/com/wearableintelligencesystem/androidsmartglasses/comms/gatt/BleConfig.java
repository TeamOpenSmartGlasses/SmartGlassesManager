package com.wearableintelligencesystem.androidsmartglasses.comms.gatt;

import static com.wearableintelligencesystem.androidsmartglasses.comms.gatt.Constants.CONFIRM_UUID;
import static com.wearableintelligencesystem.androidsmartglasses.comms.gatt.Constants.SERVER_REC_MESSAGE_UUID;
import static com.wearableintelligencesystem.androidsmartglasses.comms.gatt.Constants.SERVER_SEND_MESSAGE_UUID;
import static com.wearableintelligencesystem.androidsmartglasses.comms.gatt.Constants.SERVICE_UUID;

import java.util.UUID;

public class BleConfig {
    private static final String TAG = "BleConfig";

    public final UUID serviceUUID;
    public final  UUID messageUUID;
    public final  UUID recMessageUUID;
    public final  UUID confirmUUID;

    public BleConfig(UUID serviceUUID, UUID messageUUID, UUID recMessageUUID, UUID confirmUUID) {
        this.serviceUUID = serviceUUID;
        this.messageUUID = messageUUID;
        this.recMessageUUID = recMessageUUID;
        this.confirmUUID = confirmUUID;
    }

    public static class Builder {
        private UUID serviceUUID = SERVICE_UUID;
        private UUID messageUUID = SERVER_SEND_MESSAGE_UUID;
        private UUID recMessageUUID = SERVER_REC_MESSAGE_UUID;
        private UUID confirmUUID = CONFIRM_UUID;

        public Builder setServiceUuid(String uuid) {
            this.serviceUUID = UUID.fromString(uuid);
            return this;
        }

        public Builder setMessageUUid(String uuid) {
            this.messageUUID = UUID.fromString(uuid);
            return this;
        }

        public Builder setConfirmUuid(String uuid) {
            this.confirmUUID = UUID.fromString(uuid);
            return this;
        }

        public Builder setReceiveMessageUUID(String uuid) {
            this.recMessageUUID = UUID.fromString(uuid);
            return this;
        }

        public BleConfig build() {
            return new BleConfig(serviceUUID, messageUUID, recMessageUUID, confirmUUID);
        }
    }

}
