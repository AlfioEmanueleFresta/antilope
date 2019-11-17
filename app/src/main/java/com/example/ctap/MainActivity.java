package com.example.ctap;

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

import androidx.appcompat.app.AppCompatActivity;

import com.example.ctap.fido2.Authenticator;
import com.example.ctap.fido2.AuthenticatorDisplay;
import com.example.ctap.fido2.PublicKeyCredentialRpEntity;
import com.example.ctap.fido2.PublicKeyCredentialUserEntity;
import com.example.ctap.keystore.impl.AndroidKeyStore;
import com.example.ctap.ui.CompletableDialog;

import java.security.KeyStoreException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.example.ctap.Utils.byteArrayToHex;


public class MainActivity extends AppCompatActivity
        implements GattService.GattServiceDelegate, AuthenticatorDisplay {

    public MainActivity() {
    }

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final int REQUEST_ENABLE_BT = 1;

    private AndroidKeyStore mKeyStore;
    private Authenticator mAuthenticator;
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
            Log.e(TAG, "Unable to start advertising: " + errorCode);
            super.onStartFailure(errorCode);
            // TODO handle error code
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Started advertising BLE services. Settings: " + settingsInEffect);
            super.onStartSuccess(settingsInEffect);

        }
    };

    private final BluetoothGattServerCallback mGattServerCallback =
            new BluetoothGattServerCallback() {
        private final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
                    .fromString("00002902-0000-1000-8000-00805f9b34fb");

        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            Log.i(TAG, "onConnectionStateChange, device=" + device.getAddress() + ", status=" + status + ", newState=" + newState);
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
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.d(TAG, "onMtuChanged device=" + device.getAddress() + ", mtu=" + mtu);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicReadRequest - requestId=" + requestId + ", device=" + device.getAddress() + " (" + device.getName() + ")");
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

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
            Log.v(TAG, "Notification sent, status: " + status);
            super.onNotificationSent(device, status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            Log.v(TAG, "Characteristic Write Request: " + Arrays.toString(value));
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);


            //final int status = writeCharacteristic(characteristic, offset, value);
            final int status = mFido2Service.writeCharacteristic(characteristic, offset, value);
            mGattServer.sendResponse(device, requestId, status, 0, null);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                            int offset, BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "onDescriptorReadRequest - requestId=" + requestId + ", device=" + device.getAddress() + " (" + device.getName() + ")");
            Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            if (offset != 0) {
                Log.e(TAG, "Reqested non-zero offset: " + offset);
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET,
                        offset, null);
                return;
            }

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, descriptor.getValue());
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId,
                                   boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    0, new byte[] {});
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
            Log.d(TAG, "addNextService(): Requesting addition of next service...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mGattServer.addService(service.getBluetoothGattService());
        } else {
            Log.d(TAG, "addNextService(): This was the last service, now starting advertising");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep the display on.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            mKeyStore = new AndroidKeyStore();
        } catch (KeyStoreException e) {
            Log.e(TAG, "mKeyStore could not be initialised", e);
            e.printStackTrace();
        }

        Log.i(TAG, "mKeyStore was intitialised succesfully.");
        mAuthenticator = new Authenticator(this, mKeyStore, this);

        mFido2Service = new Fido2Service(this, mAuthenticator);
        mDeviceInformationService = new DeviceInformationService();

        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mAdvSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        mAdvData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(mFido2Service.getServiceUUID())
                /*
                 * Some magic (https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#ble-pairing-authnr-considerations)
                 * required because the Android BLE API does not allow setting the required advertising flags (LE Limited Mode or LE General Discoverable bits).
                 */
                .addServiceData(mFido2Service.getServiceUUID(), new byte[] {(byte) 192, (byte) 192, (byte) 192})
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
        mServicesToAdd.add(mDeviceInformationService);
        mServicesToAdd.add(mFido2Service);
        addNextService();

        startAdvertising();
    }

    private void startAdvertising() {
        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.i(TAG, "Preparing to advertise");
            mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvCallback);
        } else {
            Log.e(TAG, "BLE Advertisement is unsupported");

        }

        Log.i(TAG, "Available GATT services:");
        for (BluetoothGattService service: mGattServer.getServices()) {
            Log.i(TAG, " * " + service.getUuid());
        }

        Log.i(TAG, "Adapter - isEnabled=" + mBluetoothAdapter.isEnabled());
        Log.i(TAG, "Adapter - scanMode=" + mBluetoothAdapter.getScanMode());

        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Log.e(TAG, "!! ADAPTER IS IN UNEXPECTED NIDE !! scanMode=" + mBluetoothAdapter.getScanMode());
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
            Log.v(TAG, String.format("Sending to %s (indicate=%b): [%s]",
                        device.getAddress(), indicate,
                        byteArrayToHex(characteristic.getValue())
                    ));
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

    @Override
    public CompletableFuture<Boolean> confirmMakeCredentials(
            final PublicKeyCredentialRpEntity rp,
            final PublicKeyCredentialUserEntity user) {
        final String message = String.format("%s (%s) would like to create new credentials " +
                "for user %s (%s)", rp.getName(), rp.getId(), user.getDisplayName(), user.getName());
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableDialog dialog = new CompletableDialog(future, message, "Make Credentials", "Cancel");
        dialog.show(this.getSupportFragmentManager(), "make-credentials");
        return future;
    }
}
