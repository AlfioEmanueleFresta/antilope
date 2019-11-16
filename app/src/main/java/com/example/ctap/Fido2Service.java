package com.example.ctap;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.ctap.fido2.Authenticator;
import com.google.common.primitives.Shorts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.example.ctap.Utils.byteArrayToHex;


public class Fido2Service extends GattService {
    private static final String TAG = Fido2Service.class.getCanonicalName();

    private static final UUID FIDO_SERVICE_UUID = UUID
            .fromString("0000fffd-0000-1000-8000-00805f9b34fb");

    private static final UUID FIDO_CONTROL_POINT_UUID = UUID.fromString("F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB"); // write
    private static final UUID FIDO_SERVICE_REVISION_BITFIELD_UUID = UUID.fromString("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB"); // read, write
    private static final UUID FIDO_STATUS_UUID = UUID.fromString("F1D0FFF2-DEAA-ECEE-B42F-C9BA7ED623BB"); // notify
    private static final UUID FIDO_CONTROL_POINT_LENGTH_UUID = UUID.fromString("F1D0FFF3-DEAA-ECEE-B42F-C9BA7ED623BB"); // read
    private static final UUID FIDO_STATUS_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private static final byte[] FIDO_CONTROL_POINT_LENGTH_RESPONSE = new byte[] {0x00, 0x5A}; // 512 bytes.
    //private static final byte[] FIDO_CONTROL_POINT_LENGTH_RESPONSE = new byte[] {0x02, 0x00}; // 512 bytes.
    private static final byte[] FIDO_SERVICE_REVISION_BITFIELD_RESPONSE = new byte[] {0x20};  // FIDO2 Rev1 only.

    // https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#ble-constants
    private static final byte FIDO_CTAP_PING = (byte) 0x81;
    private static final byte FIDO_CTAP_KEEPALIVE = (byte) 0x82;
    private static final byte FIDO_CTAP_MSG = (byte) 0x83;
    private static final byte FIDO_CTAP_CANCEL = (byte) 0xBE;
    private static final byte FIDO_CTAP_ERROR = (byte) 0xBF;


    private final GattServiceDelegate mDelegate;
    private final Authenticator mAuthenticator;
    private final BluetoothGattService mFidoService;
    private final BluetoothGattCharacteristic mFidoControlPoint;
    private final BluetoothGattCharacteristic mFidoStatus;
    private final BluetoothGattCharacteristic mFidoControlPointLength;
    private final BluetoothGattCharacteristic mFidoServiceRevisionBitfield;
    private final BluetoothGattDescriptor mFidoStatusDescriptor;

    private boolean mPending = false;
    private byte mPendingCommand;
    private int mPendingDataLength;
    private int mPendingDataReceived;
    private int mPendingDataFragmentsReceived;
    private ByteArrayOutputStream mPendingData;
    private CompletableFuture<byte[]> mPendingRequest;

    @Override
    public BluetoothGattService getBluetoothGattService() {
        return mFidoService;
    }

    @Override
    public ParcelUuid getServiceUUID() {
        return ParcelUuid.fromString(FIDO_SERVICE_UUID.toString());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
        // For now, we don't support any offsets.
        if (offset > 0) {
            return BluetoothGatt.GATT_INVALID_OFFSET;
        }

        if (characteristic.getUuid().equals(FIDO_CONTROL_POINT_UUID)) {
            return receivedFragment(value);

        } else if (characteristic.getUuid().equals(FIDO_SERVICE_REVISION_BITFIELD_UUID)) {
            if (!Arrays.equals(value, FIDO_SERVICE_REVISION_BITFIELD_RESPONSE)) {
                Log.e(TAG, "The client tried to negotiate an unsupported U2F/FIDO protocol: " + Arrays.toString(value));
                return BluetoothGatt.GATT_FAILURE;
            }
            Log.i(TAG, "The client negotiated FIDO2 Rev1.");

        } else {
            Log.e(TAG, "Unsupported operation requested" + characteristic.getUuid());
            return BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;

        }

        return BluetoothGatt.GATT_SUCCESS;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private int receivedFragment(byte[] value) {
        if (BigInteger.valueOf(value[0]).testBit(7)) {
            return receivedInitialFragment(value);
        }
        return receivedContinuationFragment(value);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private int receivedContinuationFragment(byte[] value) {
        /*
        byte segmentNo = value[0];
        if (segmentNo != mPendingDataFragmentsReceived + 1) {
            Log.e(TAG, String.format("Unexpected continuation fragment %d", (int) segmentNo));
            discardPendingCommand();
            return BluetoothGatt.GATT_FAILURE;
        }*/

        try {
            return receivedData(Arrays.copyOfRange(value, 1, value.length));

        } catch (IOException e) {
            Log.e(TAG, "Failed whilst saving continuation fragment", e);
            discardPendingCommand();
            return BluetoothGatt.GATT_FAILURE;

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private int receivedInitialFragment(byte[] value) {
        discardPendingCommand();

        mPending = true;
        mPendingCommand = value[0];

        ByteBuffer lengthBuffer = ByteBuffer.wrap(Arrays.copyOfRange(value, 1, 3));
        mPendingDataLength = lengthBuffer.getShort();

        try {
            return receivedData(Arrays.copyOfRange(value, 3, value.length));

        } catch (IOException e) {
            Log.e(TAG, "Failed whilst saving initial fragment", e);
            discardPendingCommand();
            return BluetoothGatt.GATT_FAILURE;

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private int receivedData(byte[] newData) throws IOException {
        mPendingData.write(newData);
        mPendingDataReceived += newData.length;
        mPendingDataFragmentsReceived += 1;

        Log.d(TAG, String.format("Received fragment %d (%d bytes of %d bytes)",
                mPendingDataFragmentsReceived, mPendingDataReceived, mPendingDataLength));

        if (mPendingDataReceived >= mPendingDataLength) {
            Log.d(TAG, "Finished command");
            return executeCommand(); // TODO asyncify

        } else {
            Log.d(TAG, "More data is expected.");
            return BluetoothGatt.GATT_SUCCESS;

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private int executeCommand() {
        // TODO catch non-MSG commands

        // TODO decode MSG commands
        //      https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#message-encoding

        int responseCode = BluetoothGatt.GATT_SUCCESS;
        switch (mPendingCommand) {
            case FIDO_CTAP_MSG: {
                try {
                    mPendingRequest = mAuthenticator.request(mPendingData.toByteArray());
                    byte[] response = mPendingRequest.get();
                    respond(response);

                    // TODO frame response
                    //      https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#ble-authenticator-to-client
                    responseCode = BluetoothGatt.GATT_SUCCESS;

                } catch (ExecutionException | InterruptedException | IOException e) {
                    Log.e(TAG, "Error executing authenticator command", e);
                    responseCode = BluetoothGatt.GATT_FAILURE;

                }
                break;
            }

            default: {
                Log.i(TAG, "Unhandled message type:" + mPendingCommand);
                responseCode = BluetoothGatt.GATT_SUCCESS;

            }
        }

        discardPendingCommand();
        return responseCode;

        // TODO asyncify
        // TODO wrap completableFuture in keepalive wrapper:
        //      https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#ble-responses
    }

    private void respond(byte[] response) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(FIDO_CTAP_MSG);
        out.write(Shorts.toByteArray((short) response.length));
        out.write(response);
        byte[] outputArray = out.toByteArray();

        mFidoStatus.setValue(outputArray);
        mDelegate.sendNotificationToDevices(mFidoStatus);
        Log.i(TAG, String.format("Responding: mFidoStatus(%d)=%s",
                outputArray.length, byteArrayToHex(outputArray)));
    }

    private void discardPendingCommand() {
        mPending = false;
        mPendingCommand = 0x00;
        mPendingDataLength = 0;
        mPendingDataReceived = 0;
        mPendingDataFragmentsReceived = -1;
        mPendingData = new ByteArrayOutputStream();
    }

    Fido2Service(final GattServiceDelegate delegate, final Authenticator authenticator) {
        mAuthenticator = authenticator;
        mDelegate = delegate;

        mFidoControlPoint = new BluetoothGattCharacteristic(FIDO_CONTROL_POINT_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);

        mFidoStatus = new BluetoothGattCharacteristic(FIDO_STATUS_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        // TODO fidoStatus notify

        mFidoControlPointLength = new BluetoothGattCharacteristic(FIDO_CONTROL_POINT_LENGTH_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        mFidoControlPointLength.setValue(FIDO_CONTROL_POINT_LENGTH_RESPONSE);

        mFidoServiceRevisionBitfield = new BluetoothGattCharacteristic(FIDO_SERVICE_REVISION_BITFIELD_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED |
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        mFidoServiceRevisionBitfield.setValue(FIDO_SERVICE_REVISION_BITFIELD_RESPONSE);

        mFidoStatusDescriptor = new BluetoothGattDescriptor(FIDO_STATUS_DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mFidoStatusDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mFidoStatus.addDescriptor(mFidoStatusDescriptor);


        BluetoothGattCharacteristic[] characteristics = new BluetoothGattCharacteristic[]{
                mFidoControlPoint,
                mFidoStatus,
                mFidoControlPointLength,
                mFidoServiceRevisionBitfield,
        };

        mFidoService = new BluetoothGattService(FIDO_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        for (BluetoothGattCharacteristic characteristic: characteristics) {
            if (!mFidoService.addCharacteristic(characteristic)) {
                throw new RuntimeException("Unable to add characteristic: " + characteristic.getUuid());
            }
        }

    }


}
