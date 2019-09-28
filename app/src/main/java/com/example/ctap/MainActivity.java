package com.example.ctap;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements GattService.GattServiceDelegate {

    public MainActivity() {
    }

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final int REQUEST_ENABLE_BT = 1;

    private GattService mFido2Service;
    private GattService mDeviceInformationService;
    private Queue<GattService> mServicesToAdd;

    private HashSet<BluetoothDevice> mBluetoothDevices;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private AdvertiseData mAdvData;
    private AdvertiseData mAdvScanResponse;
    private AdvertiseSettings mAdvSettings;
    private BluetoothLeAdvertiser mAdvertiser;
    private BluetoothGattServer mGattServer;

    private static final UUID CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb");

    private final AdvertiseCallback mAdvCallback = new AdvertiseCallback() {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Unable to start advertising: " + errorCode);
            // TODO handle error code
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "Started advertising BLE services");

        }
    };

    private final BluetoothGattServerCallback mGattServerCallback =
            new BluetoothGattServerCallback() {
        private final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
                    .fromString("00002902-0000-1000-8000-00805f9b34fb");

        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices.add(device);
                    Log.i(TAG, "Connected to device: " + device.getAddress());
                    updateConnectedDevicesStatus();

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(device);
                    Log.i(TAG, "Disconnected from device: " + device.getAddress());
                    updateConnectedDevicesStatus();

                } else {
                    Log.e(TAG, "Unhandled transition from SUCCESS to " + newState);

                }
            } else {
                // Something else has happened.
                mBluetoothDevices.remove(device);
                Log.e(TAG, "Unknown error (device " + device.getAddress() + "): " + status);
                updateConnectedDevicesStatus();

            }

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));

            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET,
                        offset, null);
                return;
            }

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Service " + service.getUuid() + " was added succesfully.");

            } else {
                Log.e(TAG, "Error adding service " + service.getUuid() + ".");

            }

            super.onServiceAdded(status, service);

            addNextService();
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.v(TAG, "Notification sent, status: " + status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);

            Log.v(TAG, "Characteristic Write Request: " + Arrays.toString(value));

            //final int status = writeCharacteristic(characteristic, offset, value);
            final int status = mFido2Service.writeCharacteristic(characteristic, offset, value);
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status, 0, null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                            int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));

            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET,
                        offset, null);
                return;
            }

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite,
                    responseNeeded, offset, value);
            Log.v(TAG, "Descriptor write requested (" + descriptor.getUuid() + "): " +
                    Arrays.toString(value));

            int status = BluetoothGatt.GATT_SUCCESS;
            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                boolean supportsNotifications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                boolean supportsIndications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

                if (!(supportsNotifications || supportsIndications)) {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;

                } else if (value.length != 2) {
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;

                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    Log.d(TAG, "Notifications disabled");
                    mFido2Service.notificationsDisabled(characteristic);
                    descriptor.setValue(value);

                } else if (supportsNotifications &&
                            Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    Log.d(TAG, "Notifications enabled (notify)");
                    mFido2Service.notificationsEnabled(characteristic, false);

                } else if (supportsIndications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    Log.d(TAG, "Notifications enabled (indicate)");
                    mFido2Service.notificationsEnabled(characteristic, true);

                } else {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                    Log.w(TAG, "Unsupported GATT request: " + status);

                }
            } else {
                status = BluetoothGatt.GATT_SUCCESS;
                descriptor.setValue(value);

            }

            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status, 0, null);
            }
        }
    };

    /*
     * ACTIVITY LIFECYCLE
     */

    private void addNextService() {
        GattService service = mServicesToAdd.poll();
        if (service != null) {
            mGattServer.addService(service.getBluetoothGattService());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep the display on.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mFido2Service = new Fido2Service();
        mDeviceInformationService = new DeviceInformationService();

        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mAdvSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(true)
                .setTimeout(30000)
                .build();

        mAdvData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(mFido2Service.getServiceUUID())
                .addServiceUuid(mDeviceInformationService.getServiceUUID())
                .build();

        mAdvScanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(mFido2Service.getServiceUUID())
                .addServiceUuid(mDeviceInformationService.getServiceUUID())
                .build();


    }

    @Override
    public void onStart() {
        super.onStart();

        // If the user disabled Bluetooth when the app was in the background, this will return null.
        Log.d(TAG, "Opening GATT server");
        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);

        if (mGattServer == null) {
            Log.w(TAG, "mGattServer == null");
            ensureBleFeaturesAvailable();
            return;
        }

        // Add services (Generic Attribute & Generic Access services are present by default)
        mServicesToAdd = new ArrayDeque<>();
        mServicesToAdd.add(mFido2Service);
        mServicesToAdd.add(mFido2Service);
        addNextService();

        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.i(TAG, "Preparing to advertise");
            mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback);
        } else {
            Log.e(TAG, "BLE Advertisement is unsupported");

        }

        Log.i(TAG, "Available GATT services:");
        for (BluetoothGattService service: mGattServer.getServices()) {
            Log.i(TAG, " * " + service.getUuid());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_ENABLE_BT) {
            return;
        }

        if (resultCode == RESULT_OK) {
            if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                Toast.makeText(this, "Bluetooth advertising not supported", Toast.LENGTH_LONG).show();
                Log.e(TAG, "BLE advertising not supported");
            }

            onStart();
        } else {
            // TODO ask user to activate bluetooth
            Toast.makeText(this, "Bluetooth isn't enabled. Please activate it.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Bluetooth is not active.");
            finish();

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGattServer != null) {
            mGattServer.close();
        }

        if (mBluetoothAdapter.isEnabled() && mAdvertiser != null) {
            // if stopAdvertising gets called before close() a NPE is raised
            mAdvertiser.stopAdvertising(mAdvCallback);
        }
    }

    @Override
    public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
        boolean indicate = (characteristic.getProperties() &
                    BluetoothGattCharacteristic.PROPERTY_INDICATE)
                == BluetoothGattCharacteristic.PROPERTY_INDICATE;
        for (BluetoothDevice device: mBluetoothDevices) {
            // true for indication (ack) and false for notification (unacklowedged)
            mGattServer.notifyCharacteristicChanged(device, characteristic, indicate);
        }
    }

    private void updateConnectedDevicesStatus() {
        // TODO ui
        List<BluetoothDevice> connectedDevices = mBluetoothManager.getConnectedDevices(BluetoothGattServer.GATT);
        Log.i(TAG, "Updated list of connected devices (" + connectedDevices.size() + "):");
        for (BluetoothDevice device: connectedDevices) {
            Log.i(TAG, " * " + device.getName() + " (" + device.getAddress() + ")");

        }
    }

    private void ensureBleFeaturesAvailable() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Bluetooth not supported");
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

}
