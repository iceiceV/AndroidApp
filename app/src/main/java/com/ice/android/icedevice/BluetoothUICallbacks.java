package com.ice.android.icedevice;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;

import java.util.List;

public interface BluetoothUICallbacks {
    void connectingToDevice();
    void connectedToDevice();
    void disconnectedFromDevice();
    void displayData(String data);
    void noDeviceFound();
}
