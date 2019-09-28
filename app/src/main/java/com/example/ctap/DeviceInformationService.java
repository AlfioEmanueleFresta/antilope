package com.example.ctap;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

import java.util.UUID;


public class DeviceInformationService extends GattService {
    private static final UUID DEVICE_INFORMATION_SERVICE_UUID = UUID
            .fromString("0000180a-0000-1000-8000-00805f9b34fb");

    private static final UUID MANUFACTURER_NAME_UUID = UUID
            .fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private static final UUID MODEL_NUMBER_UUID = UUID
            .fromString("00002a24-0000-1000-8000-00805f9b34fb");
    private static final UUID FIRMWARE_REVISION_UUID = UUID
            .fromString("00002a26-0000-1000-8000-00805f9b34fb");

    private static final String MANUFACTURER_NAME = "Manufacturer Name";
    private static final String MODEL_NUMBER = "B1";
    private static final String FIRMWARE_REVISION = "1.0b";

    private BluetoothGattService mDeviceInformationService;
    private BluetoothGattCharacteristic mManufacturerNameCharacteristic;
    private BluetoothGattCharacteristic mModelNumberCharacteristic;
    private BluetoothGattCharacteristic mFirmwareRevisionCharacteristic;

    @Override
    public BluetoothGattService getBluetoothGattService() {
        return mDeviceInformationService;
    }

    @Override
    public ParcelUuid getServiceUUID() {
        return new ParcelUuid(DEVICE_INFORMATION_SERVICE_UUID);
    }

    public DeviceInformationService() {
        mManufacturerNameCharacteristic = new BluetoothGattCharacteristic(MANUFACTURER_NAME_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM);
        mManufacturerNameCharacteristic.addDescriptor(buildStringDescriptor(MANUFACTURER_NAME_UUID, MANUFACTURER_NAME));

        mModelNumberCharacteristic = new BluetoothGattCharacteristic(MODEL_NUMBER_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM);
        mModelNumberCharacteristic.addDescriptor(buildStringDescriptor(MODEL_NUMBER_UUID, MODEL_NUMBER));

        mFirmwareRevisionCharacteristic = new BluetoothGattCharacteristic(FIRMWARE_REVISION_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM);
        mFirmwareRevisionCharacteristic.addDescriptor(buildStringDescriptor(FIRMWARE_REVISION_UUID, FIRMWARE_REVISION));

        mDeviceInformationService = new BluetoothGattService(DEVICE_INFORMATION_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mDeviceInformationService.addCharacteristic(mManufacturerNameCharacteristic);
        mDeviceInformationService.addCharacteristic(mModelNumberCharacteristic);
        mDeviceInformationService.addCharacteristic(mFirmwareRevisionCharacteristic);
    }

}
