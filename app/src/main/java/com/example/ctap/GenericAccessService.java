package com.example.ctap;

import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

import java.util.UUID;

public class GenericAccessService extends GattService {
    private static final UUID GENERIC_ACCESS_SERVICE_UUID = UUID
            .fromString("00001800-0000-1000-8000-00805f9b34fb");

    private static final UUID DEVICE_NAME_UUID = UUID
            .fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private static final UUID APPEAREANCE_UUID = UUID
            .fromString("00002a01-0000-1000-8000-00805f9b34fb");

    @Override
    public BluetoothGattService getBluetoothGattService() {
        return null;
    }

    @Override
    public ParcelUuid getServiceUUID() {
        return null;
    }
}
