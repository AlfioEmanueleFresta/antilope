package com.example.ctap;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

import java.util.UUID;


public class Fido2Service extends GattService {
    private static final UUID FIDO_SERVICE_UUID = UUID
            .fromString("0000fffd-0000-1000-8000-00805f9b34fb");

    private static final UUID FIDO_CONTROL_POINT_UUID = UUID.fromString("F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB"); // write
    private static final UUID FIDO_STATUS_UUID = UUID.fromString("F1D0FFF2-DEAA-ECEE-B42F-C9BA7ED623BB"); // notify
    private static final UUID FIDO_CONTROL_POINT_LENGTH_UUID = UUID.fromString("F1D0FFF3-DEAA-ECEE-B42F-C9BA7ED623BB"); // read
    private static final UUID FIDO_SERVICE_REVISION_BITFIELD_UUID = UUID.fromString("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB"); // read, write
    private static final UUID FIDO_SERVICE_REVISION_UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb"); // read

    private static final UUID FIDO_STATUS_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private BluetoothGattService mFidoService;
    private BluetoothGattCharacteristic mFidoControlPoint;
    private BluetoothGattCharacteristic mFidoStatus;
    private BluetoothGattCharacteristic mFidoControlPointLength;
    private BluetoothGattCharacteristic mFidoServiceRevisionBitfield;
    private BluetoothGattCharacteristic mFidoServiceRevision;
    private BluetoothGattDescriptor mFidoStatusDescriptor;

    @Override
    public BluetoothGattService getBluetoothGattService() {
        return mFidoService;
    }

    @Override
    public ParcelUuid getServiceUUID() {
        return new ParcelUuid(FIDO_SERVICE_UUID);
    }

    public Fido2Service() {
        mFidoControlPoint = new BluetoothGattCharacteristic(FIDO_CONTROL_POINT_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM);
        // TODO fidoControlPoint write

        mFidoStatus = new BluetoothGattCharacteristic(FIDO_STATUS_UUID,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM);
        // TODO fidoStatus notify

        mFidoControlPointLength = new BluetoothGattCharacteristic(FIDO_CONTROL_POINT_LENGTH_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        // TODO fidoControlPointLength read

        mFidoServiceRevisionBitfield = new BluetoothGattCharacteristic(FIDO_SERVICE_REVISION_BITFIELD_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM |
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM);
        // TODO fidoServiceRevisionBitfield read, write

        mFidoServiceRevision = new BluetoothGattCharacteristic(FIDO_SERVICE_REVISION_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        // TODO fidoServiceRevision read

        mFidoStatusDescriptor = new BluetoothGattDescriptor(FIDO_STATUS_DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM);

        mFidoService = new BluetoothGattService(FIDO_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mFidoStatus.addDescriptor(mFidoStatusDescriptor);

        mFidoService.addCharacteristic(mFidoControlPoint);
        mFidoService.addCharacteristic(mFidoStatus);
        mFidoService.addCharacteristic(mFidoControlPointLength);
        mFidoService.addCharacteristic(mFidoServiceRevisionBitfield);
        mFidoService.addCharacteristic(mFidoServiceRevision);
    }

}
