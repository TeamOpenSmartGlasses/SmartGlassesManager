package com.smartglassesmanager.androidsmartphone.smartglassescommunicators.gatt


import java.util.UUID

class BleConfig(
    val serviceUUID: UUID,
    val serverSendMessageUUID: UUID,
    var serverReceiveMessageUUID: UUID,
    val confirmUUID: UUID,
) {

    class Builder {
        var serviceUUID: UUID = SERVICE_UUID
        var serverSendmessageUUID: UUID = SERVER_SEND_MESSAGE_UUID
        var serverReceiveMessageUUID :UUID = SERVER_REC_MESSAGE_UUID
        var confirmUUID: UUID = CONFIRM_UUID
        fun setServiceUuid(uuid: String): Builder {
            serviceUUID = UUID.fromString(uuid)
            return this
        }

        fun setServerSendMessageUUid(uuid: String): Builder {
            serverSendmessageUUID = UUID.fromString(uuid)
            return this
        }

        fun setConfirmUuid(uuid: String): Builder {
            confirmUUID = UUID.fromString(uuid)
            return this
        }

        fun setServerReceiveMessageUUID(uuid:String ):Builder{
            serverReceiveMessageUUID = UUID.fromString( uuid )
            return this
        }

        fun build(): BleConfig {
            return BleConfig(
                serviceUUID,
                serverSendmessageUUID,
                serverReceiveMessageUUID,
                confirmUUID
            )
        }

    }

    companion object {
        private const val TAG = "BleConfig"
    }
}