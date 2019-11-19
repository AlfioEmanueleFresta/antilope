package com.example.ctap

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.ParcelUuid

abstract class GattService {
    abstract fun getBluetoothGattService(): BluetoothGattService
    abstract fun getServiceUUID(): ParcelUuid

    /**
     * Function to communicate to the ServiceFragment that a device wants to write to a
     * characteristic.
     *
     * The ServiceFragment should check that the value being written is valid and
     * return a code appropriately. The ServiceFragment should update the UI to reflect the change.
     * @param characteristic Characteristic to write to
     * @param value Value to write to the characteristic
     * @return [android.bluetooth.BluetoothGatt.GATT_SUCCESS] if the write operation
     * was completed successfully. See [android.bluetooth.BluetoothGatt] for GATT return codes.
     */
    open suspend fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, offset: Int, value: ByteArray): Int {
        throw UnsupportedOperationException("Method writeCharacteristic not overridden")
    }

    /**
     * Function to notify to the ServiceFragment that a device has disabled notifications on a
     * CCC descriptor.
     *
     * The ServiceFragment should update the UI to reflect the change.
     * @param characteristic Characteristic written to
     */
    fun notificationsDisabled(characteristic: BluetoothGattCharacteristic) {
        throw UnsupportedOperationException("Method notificationsDisabled not overridden")
    }

    /**
     * Function to notify to the ServiceFragment that a device has enabled notifications on a
     * CCC descriptor.
     *
     * The ServiceFragment should update the UI to reflect the change.
     * @param characteristic Characteristic written to
     * @param indicate Boolean that says if it's indicate or notify.
     */
    fun notificationsEnabled(characteristic: BluetoothGattCharacteristic, indicate: Boolean) {
        throw UnsupportedOperationException("Method notificationsEnabled not overridden")
    }

    /**
     * This interface must be implemented by activities that contain a ServiceFragment to allow an
     * interaction in the fragment to be communicated to the activity.
     */
    interface GattServiceDelegate {
        fun sendNotificationToDevices(characteristic: BluetoothGattCharacteristic?)
    }
}