package com.example.ctap

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.ParcelUuid
import android.util.Log
import com.example.ctap.fido2.Authenticator
import com.google.common.primitives.Shorts
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture

class Fido2Service internal constructor(
        private val mDelegate: GattServiceDelegate,
        private val mAuthenticator: Authenticator
) : GattService(), FidoToken {

    private val mFidoService: BluetoothGattService
    private val mFidoControlPoint: BluetoothGattCharacteristic
    private val mFidoStatus: BluetoothGattCharacteristic
    private val mFidoControlPointLength: BluetoothGattCharacteristic
    private val mFidoServiceRevisionBitfield: BluetoothGattCharacteristic
    private val mFidoStatusDescriptor: BluetoothGattDescriptor
    private var mPending = false
    private var mPendingCommand: Byte = 0
    private var mPendingDataLength = 0
    private var mPendingDataReceived = 0
    private var mPendingDataFragmentsReceived = 0
    private var mPendingData: ByteArrayOutputStream? = null
    private var mPendingRequest: CompletableFuture<ByteArray>? = null

    override fun getBluetoothGattService(): BluetoothGattService = mFidoService
    override fun getServiceUUID(): ParcelUuid = ParcelUuid.fromString(FIDO_SERVICE_UUID.toString())

    override suspend fun writeCharacteristic(
            characteristic: BluetoothGattCharacteristic, offset: Int, value: ByteArray): Int {
        if (offset > 0) {
            return BluetoothGatt.GATT_INVALID_OFFSET
        }

        return when (characteristic.uuid) {
            FIDO_CONTROL_POINT_UUID -> {
                receivedFragment(value)
            }

            FIDO_SERVICE_REVISION_BITFIELD_UUID -> {
                if (!value.contentEquals(FIDO_SERVICE_REVISION_BITFIELD_RESPONSE)) {
                    Log.e(TAG, "The client tried to negotiate an unsupported U2F/FIDO protocol: " + Arrays.toString(value))
                    return BluetoothGatt.GATT_FAILURE
                }
                Log.i(TAG, "The client negotiated FIDO2 Rev1.")
                BluetoothGatt.GATT_SUCCESS
            }

            else -> {
                Log.e(TAG, "Unsupported operation requested" + characteristic.uuid)
                return BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
            }
        }
    }

    private suspend fun receivedFragment(value: ByteArray): Int {
        return if (BigInteger.valueOf(value[0].toLong()).testBit(7)) {
            receivedInitialFragment(value)
        } else receivedContinuationFragment(value)
    }

    private suspend fun receivedContinuationFragment(value: ByteArray): Int {
        return try {
            receivedData(value.copyOfRange(1, value.size))

        } catch (e: IOException) {
            Log.e(TAG, "Failed whilst saving continuation fragment", e)
            discardPendingCommand()
            BluetoothGatt.GATT_FAILURE
        }

    }

    private suspend fun receivedInitialFragment(value: ByteArray): Int {
        discardPendingCommand()
        mPending = true
        mPendingCommand = value[0]
        val lengthBuffer = ByteBuffer.wrap(Arrays.copyOfRange(value, 1, 3))
        mPendingDataLength = lengthBuffer.short.toInt()
        return try {
            receivedData(Arrays.copyOfRange(value, 3, value.size))
        } catch (e: IOException) {
            Log.e(TAG, "Failed whilst saving initial fragment", e)
            discardPendingCommand()
            BluetoothGatt.GATT_FAILURE
        }
    }

    private suspend fun receivedData(newData: ByteArray): Int {
        mPendingData!!.write(newData)
        mPendingDataReceived += newData.size
        mPendingDataFragmentsReceived += 1
        Log.d(TAG, String.format("Received fragment %d (%d bytes of %d bytes)",
                mPendingDataFragmentsReceived, mPendingDataReceived, mPendingDataLength))
        return if (mPendingDataReceived >= mPendingDataLength) {
            Log.d(TAG, "Finished command")
            executeCommand()

        } else {
            Log.d(TAG, "More data is expected.")
            BluetoothGatt.GATT_SUCCESS
        }
    }

    private suspend fun executeCommand(): Int {
        // TODO catch non-MSG commands
        // TODO decode MSG commands
        // https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#message-encoding
        var responseCode = BluetoothGatt.GATT_SUCCESS
        when (mPendingCommand) {
            FIDO_CTAP_MSG -> {
                responseCode = try {
                    val response = mAuthenticator.request(mPendingData!!.toByteArray(), this)
                    respond(response)

                    // TODO frame response
                    // https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#ble-authenticator-to-client
                    BluetoothGatt.GATT_SUCCESS
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing authenticator command", e)
                    BluetoothGatt.GATT_FAILURE

                }
            }
            else -> {
                Log.i(TAG, "Unhandled message type:$mPendingCommand")
                responseCode = BluetoothGatt.GATT_SUCCESS
            }
        }
        discardPendingCommand()
        return responseCode

        // TODO asyncify
        // TODO wrap completableFuture in keepalive wrapper:
        //      https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#ble-responses
    }

    private fun respond(response: ByteArray) {
        Log.i(TAG, String.format("Response: %s",
                Utils.byteArrayToHex(response)))

        val chunkSize = 80;
        val chunks = response.toList().chunked(chunkSize);

        chunks.forEachIndexed { i, chunk ->
            val output = if (i == 0) {
                byteArrayOf( // First packet
                        FIDO_CTAP_MSG,
                        *Shorts.toByteArray(response.size.toShort()),
                        *chunk.toByteArray()
                )
            } else {
                byteArrayOf( // Continuatino packet
                        (i - 1).toByte(), // sequence number,
                        *chunk.toByteArray()
                )
            }

            send(output);
        }
    }

    private fun send(output: ByteArray) {
        mFidoStatus.value = output
        mDelegate.sendNotificationToDevices(mFidoStatus)
        Log.i(TAG, String.format("Sending: mFidoStatus(%d)=%s",
                mFidoStatus.value.size,
                Utils.byteArrayToHex(mFidoStatus.value)))
    }

    private fun discardPendingCommand() {
        mPending = false
        mPendingCommand = 0x00
        mPendingDataLength = 0
        mPendingDataReceived = 0
        mPendingDataFragmentsReceived = -1
        mPendingData = ByteArrayOutputStream()
    }

    companion object {
        private val TAG = Fido2Service::class.java.canonicalName
        private val FIDO_SERVICE_UUID = UUID
                .fromString("0000fffd-0000-1000-8000-00805f9b34fb")
        private val FIDO_CONTROL_POINT_UUID = UUID.fromString("F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB") // write
        private val FIDO_SERVICE_REVISION_BITFIELD_UUID = UUID.fromString("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB") // read, write
        private val FIDO_STATUS_UUID = UUID.fromString("F1D0FFF2-DEAA-ECEE-B42F-C9BA7ED623BB") // notify
        private val FIDO_CONTROL_POINT_LENGTH_UUID = UUID.fromString("F1D0FFF3-DEAA-ECEE-B42F-C9BA7ED623BB") // read
        private val FIDO_STATUS_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        private val FIDO_CONTROL_POINT_LENGTH_RESPONSE = byteArrayOf(0x00, 0x5A) // 512 bytes.
        //private static final byte[] FIDO_CONTROL_POINT_LENGTH_RESPONSE = new byte[] {0x02, 0x00}; // 512 bytes.
        private val FIDO_SERVICE_REVISION_BITFIELD_RESPONSE = byteArrayOf(0x20) // FIDO2 Rev1 only.
        // https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#ble-constants
        private const val FIDO_CTAP_PING = 0x81.toByte()
        private const val FIDO_CTAP_KEEPALIVE = 0x82.toByte()
        private const val FIDO_CTAP_MSG = 0x83.toByte()
        private const val FIDO_CTAP_CANCEL = 0xBE.toByte()
        private const val FIDO_CTAP_ERROR = 0xBF.toByte()
    }

    init {
        mFidoControlPoint = BluetoothGattCharacteristic(FIDO_CONTROL_POINT_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED)
        mFidoStatus = BluetoothGattCharacteristic(FIDO_STATUS_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE)
        // TODO fidoStatus notify
        mFidoControlPointLength = BluetoothGattCharacteristic(FIDO_CONTROL_POINT_LENGTH_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED)
        mFidoControlPointLength.value = FIDO_CONTROL_POINT_LENGTH_RESPONSE
        mFidoServiceRevisionBitfield = BluetoothGattCharacteristic(FIDO_SERVICE_REVISION_BITFIELD_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or
                        BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED)
        mFidoServiceRevisionBitfield.value = FIDO_SERVICE_REVISION_BITFIELD_RESPONSE
        mFidoStatusDescriptor = BluetoothGattDescriptor(FIDO_STATUS_DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        mFidoStatusDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mFidoStatus.addDescriptor(mFidoStatusDescriptor)
        val characteristics = arrayOf(
                mFidoControlPoint,
                mFidoStatus,
                mFidoControlPointLength,
                mFidoServiceRevisionBitfield)
        mFidoService = BluetoothGattService(FIDO_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        for (characteristic in characteristics) {
            if (!mFidoService.addCharacteristic(characteristic)) {
                throw RuntimeException("Unable to add characteristic: " + characteristic.uuid)
            }
        }
    }

    override suspend fun sendKeepAlive() {
        val message = byteArrayOf(
                FIDO_CTAP_KEEPALIVE,
                0x00, 0x01, // short length
                0x02);      // user presence needed
        send(message);
    }


}