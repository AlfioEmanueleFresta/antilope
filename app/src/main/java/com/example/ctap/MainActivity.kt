package com.example.ctap

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertiseSettings.Builder
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ctap.GattService.GattServiceDelegate
import com.example.ctap.fido2.Authenticator
import com.example.ctap.fido2.AuthenticatorDisplay
import com.example.ctap.fido2.PublicKeyCredentialRpEntity
import com.example.ctap.fido2.PublicKeyCredentialUserEntity
import com.example.ctap.keystore.impl.AndroidKeyStore
import com.example.ctap.ui.CompletableDialog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.security.KeyStoreException
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashSet

class MainActivity : AppCompatActivity(), GattServiceDelegate, AuthenticatorDisplay {
    private var mKeyStore: AndroidKeyStore? = null
    private var mAuthenticator: Authenticator? = null
    private var mFido2Service: GattService? = null
    private var mDeviceInformationService: GattService? = null
    private var mServicesToAdd: Queue<GattService?>? = null
    private var mBluetoothDevices: HashSet<BluetoothDevice>? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mAdvData: AdvertiseData? = null
    private val mAdvScanResponse: AdvertiseData? = null
    private var mAdvSettings: AdvertiseSettings? = null
    private var mAdvertiser: BluetoothLeAdvertiser? = null
    private var mGattServer: BluetoothGattServer? = null
    private val mAdvCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Unable to start advertising: $errorCode")
            super.onStartFailure(errorCode)
            // TODO handle error code
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "Started advertising BLE services. Settings: $settingsInEffect")
            super.onStartSuccess(settingsInEffect)
        }
    }
    private val mGattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
                .fromString("00002902-0000-1000-8000-00805f9b34fb")

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.i(TAG, "onConnectionStateChange, device=" + device.address + ", status=" + status + ", newState=" + newState)
            super.onConnectionStateChange(device, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices!!.add(device)
                    Log.i(TAG, "Connected to device: " + device.address)
                    updateConnectedDevicesStatus()
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices!!.remove(device)
                    Log.i(TAG, "Disconnected from device: " + device.address)
                    updateConnectedDevicesStatus()
                } else {
                    Log.e(TAG, "Unhandled transition from SUCCESS to $newState")
                }
            } else { // Something else has happened.
                mBluetoothDevices!!.remove(device)
                Log.e(TAG, "Unknown error (device " + device.address + "): " + status)
                updateConnectedDevicesStatus()
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            super.onMtuChanged(device, mtu)
            Log.d(TAG, "onMtuChanged device=" + device.address + ", mtu=" + mtu)
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                                 characteristic: BluetoothGattCharacteristic) {
            Log.d(TAG, "onCharacteristicReadRequest - requestId=" + requestId + ", device=" + device.address + " (" + device.name + ")")
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.uuid)
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.value))
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            if (offset != 0) {
                mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET,
                        offset, null)
                return
            }
            mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.value)
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Service " + service.uuid + " was added succesfully.")
            } else {
                Log.e(TAG, "Error adding service " + service.uuid + ".")
            }
            super.onServiceAdded(status, service)
            addNextService()
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            Log.v(TAG, "Notification sent, status: $status")
            super.onNotificationSent(device, status)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int,
                                                  characteristic: BluetoothGattCharacteristic,
                                                  preparedWrite: Boolean, responseNeeded: Boolean,
                                                  offset: Int, value: ByteArray) {
            Log.v(TAG, "Characteristic Write Request: " + Arrays.toString(value))
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value)
            //final int status = writeCharacteristic(characteristic, offset, value);

            GlobalScope.launch {
                val status = mFido2Service!!.writeCharacteristic(characteristic, offset, value)
                mGattServer!!.sendResponse(device, requestId, status, 0, null)
            }
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int,
                                             offset: Int, descriptor: BluetoothGattDescriptor) {
            Log.d(TAG, "onDescriptorReadRequest - requestId=" + requestId + ", device=" + device.address + " (" + device.name + ")")
            Log.d(TAG, "Device tried to read descriptor: " + descriptor.uuid)
            Log.d(TAG, "Value: " + Arrays.toString(descriptor.value))
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            if (offset != 0) {
                Log.e(TAG, "Reqested non-zero offset: $offset")
                mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET,
                        offset, null)
                return
            }
            mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, descriptor.value)
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int,
                                    execute: Boolean) {
            super.onExecuteWrite(device, requestId, execute)
            mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    0, byteArrayOf())
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int,
                                              descriptor: BluetoothGattDescriptor,
                                              preparedWrite: Boolean, responseNeeded: Boolean,
                                              offset: Int, value: ByteArray) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite,
                    responseNeeded, offset, value)
            Log.v(TAG, "Descriptor write requested (" + descriptor.uuid + "): " +
                    Arrays.toString(value))
            var status = BluetoothGatt.GATT_SUCCESS
            if (descriptor.uuid === CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
                val characteristic = descriptor.characteristic
                val supportsNotifications = characteristic.properties and
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                val supportsIndications = characteristic.properties and
                        BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
                if (!(supportsNotifications || supportsIndications)) {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                } else if (value.size != 2) {
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS
                    Log.d(TAG, "Notifications disabled")
                    mFido2Service!!.notificationsDisabled(characteristic)
                    descriptor.value = value
                } else if (supportsNotifications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS
                    Log.d(TAG, "Notifications enabled (notify)")
                    mFido2Service!!.notificationsEnabled(characteristic, false)
                } else if (supportsIndications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS
                    Log.d(TAG, "Notifications enabled (indicate)")
                    mFido2Service!!.notificationsEnabled(characteristic, true)
                } else {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                    Log.w(TAG, "Unsupported GATT request: $status")
                }
            } else {
                status = BluetoothGatt.GATT_SUCCESS
                descriptor.value = value
            }
            if (responseNeeded) {
                mGattServer!!.sendResponse(device, requestId, status, 0, null)
            }
        }
    }

    /*
     * ACTIVITY LIFECYCLE
     */
    private fun addNextService() {
        val service = mServicesToAdd!!.poll()
        if (service != null) {
            Log.d(TAG, "addNextService(): Requesting addition of next service...")
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            mGattServer!!.addService(service.getBluetoothGattService())
        } else {
            Log.d(TAG, "addNextService(): This was the last service, now starting advertising")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Keep the display on.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        try {
            mKeyStore = AndroidKeyStore()
        } catch (e: KeyStoreException) {
            Log.e(TAG, "mKeyStore could not be initialised", e)
            e.printStackTrace()
        }
        Log.i(TAG, "mKeyStore was intitialised succesfully.")
        mAuthenticator = Authenticator(mKeyStore!!, this)
        mFido2Service = Fido2Service(this, mAuthenticator!!)
        mDeviceInformationService = DeviceInformationService()
        mBluetoothDevices = HashSet()
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter
        mAdvSettings = Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()
        mAdvData = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(mFido2Service!!.getServiceUUID())
                /*
                 * Some magic (https://fidoalliance.org/specs/fido-v2.0-id-20180227/fido-client-to-authenticator-protocol-v2.0-id-20180227.html#ble-pairing-authnr-considerations)
                 * required because the Android BLE API does not allow setting the required advertising flags (LE Limited Mode or LE General Discoverable bits).
                 */
                .addServiceData(mFido2Service!!.getServiceUUID(), byteArrayOf(192.toByte(), 192.toByte(), 192.toByte()))
                .addServiceUuid(mDeviceInformationService!!.getServiceUUID())
                .build()
    }

    public override fun onStart() {
        super.onStart()
        // If the user disabled Bluetooth when the app was in the background, this will return null.
        Log.d(TAG, "Opening GATT server")
        mGattServer = mBluetoothManager!!.openGattServer(this, mGattServerCallback)
        if (mGattServer == null) {
            Log.w(TAG, "mGattServer == null")
            ensureBleFeaturesAvailable()
            return
        }
        // Add services (Generic Attribute & Generic Access services are present by default)
        mServicesToAdd = ArrayDeque()
        mServicesToAdd!!.add(mDeviceInformationService)
        mServicesToAdd!!.add(mFido2Service)
        addNextService()
        startAdvertising()
    }

    private fun startAdvertising() {
        if (mBluetoothAdapter!!.isMultipleAdvertisementSupported) {
            Log.i(TAG, "Preparing to advertise")
            mAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
            mAdvertiser!!.startAdvertising(mAdvSettings, mAdvData, mAdvCallback)
        } else {
            Log.e(TAG, "BLE Advertisement is unsupported")
        }
        Log.i(TAG, "Available GATT services:")
        for (service in mGattServer!!.services) {
            Log.i(TAG, " * " + service.uuid)
        }
        Log.i(TAG, "Adapter - isEnabled=" + mBluetoothAdapter!!.isEnabled)
        Log.i(TAG, "Adapter - scanMode=" + mBluetoothAdapter!!.scanMode)
        if (mBluetoothAdapter!!.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Log.e(TAG, "!! ADAPTER IS IN UNEXPECTED NIDE !! scanMode=" + mBluetoothAdapter!!.scanMode)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_ENABLE_BT) {
            return
        }
        if (resultCode == Activity.RESULT_OK) {
            if (!mBluetoothAdapter!!.isMultipleAdvertisementSupported) {
                Toast.makeText(this, "Bluetooth advertising not supported", Toast.LENGTH_LONG).show()
                Log.e(TAG, "BLE advertising not supported")
            }
            onStart()
        } else { // TODO ask user to activate bluetooth
            Toast.makeText(this, "Bluetooth isn't enabled. Please activate it.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Bluetooth is not active.")
            finish()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (mGattServer != null) {
            mGattServer!!.close()
        }
        if (mBluetoothAdapter!!.isEnabled && mAdvertiser != null) { // if stopAdvertising gets called before close() a NPE is raised
            mAdvertiser!!.stopAdvertising(mAdvCallback)
        }
    }

    override fun sendNotificationToDevices(characteristic: BluetoothGattCharacteristic?) {
        val indicate = (characteristic!!.properties and
                BluetoothGattCharacteristic.PROPERTY_INDICATE
                == BluetoothGattCharacteristic.PROPERTY_INDICATE)
        for (device in mBluetoothDevices!!) { // true for indication (ack) and false for notification (unacklowedged)
            Log.v(TAG, String.format("Sending to %s (indicate=%b): [%s]",
                    device.address, indicate,
                    Utils.byteArrayToHex(characteristic.value)
            ))
            mGattServer!!.notifyCharacteristicChanged(device, characteristic, indicate)
        }
    }

    private fun updateConnectedDevicesStatus() { // TODO ui
        val connectedDevices = mBluetoothManager!!.getConnectedDevices(BluetoothGattServer.GATT)
        Log.i(TAG, "Updated list of connected devices (" + connectedDevices.size + "):")
        for (device in connectedDevices) {
            Log.i(TAG, " * " + device.name + " (" + device.address + ")")
        }
    }

    private fun ensureBleFeaturesAvailable() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Bluetooth not supported")
            finish()
            return
        }
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun confirmMakeCredentials(
            rp: PublicKeyCredentialRpEntity,
            user: PublicKeyCredentialUserEntity): CompletableFuture<Boolean> {
        val message = String.format("%s (%s) would like to create new credentials " +
                "for user %s (%s)", rp.name, rp.id, user.displayName, user.name)
        val future = CompletableFuture<Boolean>()
        val dialog = CompletableDialog(future, message, "Make Credentials", "Cancel")
        dialog.show(this.supportFragmentManager, "make-credentials")
        return future
    }

    companion object {
        private val TAG = MainActivity::class.java.canonicalName
        private const val REQUEST_ENABLE_BT = 1
        private val CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
                .fromString("00002901-0000-1000-8000-00805f9b34fb")
    }
}