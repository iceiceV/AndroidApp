package com.ice.android.icedevice;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationFragment.OnConnectionRequestListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.main_fragment_container) View fragmentContainer;

    NavigationFragment mNavFragment;

    static final int USER_INFO_REQUEST = 1;
    static final int REQUEST_ENABLE_BT = 11;
    static final int REQUEST_ENABLE_LOC = 12;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBleScanner;
    private boolean mScanning;
    private boolean deviceConnected = false;
    private BleService mBleService;
    private BluetoothDevice mBleDevice;
    private BluetoothGattCharacteristic mDataMDLP, mControlMLDP;

    private final String bleDeviceName = "ICETest";

    private Handler mHandler;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public UserData mUserData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mHandler = new Handler();

        // If no fragment has been committed to the activity's ViewGroup then the NewGameFragment is
        // committed.
        if (fragmentContainer != null) {
            if (savedInstanceState == null) {
                mNavFragment = new NavigationFragment();

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.main_fragment_container, mNavFragment).commit();
            }
        }

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        initiateUi(currentUser);
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBleService != null && mBleDevice != null) {
            final boolean result = mBleService.connect(mBleDevice.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        deviceConnected = false;
        mBleService = null;
    }

    @Override
    public void onBleConnectionRequest() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ENABLE_LOC);
        } else if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else if (mBleService != null && mBleDevice != null) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            final boolean result = mBleService.connect(mBleDevice.getAddress());
            deviceConnected = true;
            mNavFragment.connectedToDevice();
            Log.i(TAG, "Connect request result=" + result);
        } else {
            startBleScan();
        }
    }

    @Override
    public void onBleDisconnectRequest() {
        mBleService.disconnect();
        unregisterReceiver(mGattUpdateReceiver);
        deviceConnected = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == USER_INFO_REQUEST) {
            Log.i(TAG, "Just logged in");
        } else if (requestCode == REQUEST_ENABLE_BT) {
            Log.i(TAG, "Bluetooth turned on");
            getBleAdapterAndScanner();
            onBleConnectionRequest();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ENABLE_LOC && grantResults.length > 0) {
            Log.i(TAG, "Location access granted");
            onBleConnectionRequest();
        } else {
            Log.w(TAG, "Location access not granted! Needed for bluetooth access!");
        }
    }

    private void getBleAdapterAndScanner(){
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanning = false;
    }

    private void updateUserData(UserData userData) {
        mUserData = userData;
    }
    private void updateUserName(String username) {
        mUserData.setUsername(username);
    }

    public void startBleScan() {
        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBleScanner.stopScan(mBleScanCallback);
                mScanning = false;
                Log.i(TAG, "Stopped scanning.");
            }
        }, SCAN_PERIOD);

        ScanFilter scanFilter =
                new ScanFilter.Builder()
                        .setServiceUuid(BleService.PARCEL_UUID_BLE_SERVICE)
                        .build();
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(scanFilter);

        ScanSettings scanSettings =
                new ScanSettings.Builder().build();

        mScanning = true;
        Log.i(TAG, "Started scanning.");
        mBleScanner.startScan(scanFilters, scanSettings, mBleScanCallback);
    }

    private ScanCallback mBleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            checkDeviceAndConnect(result.getDevice());
            //Log.i(TAG, "" + result.getDevice().getName());
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.w(TAG, "BLE Scan Failed with code " + errorCode);
        }

        private void checkDeviceAndConnect(BluetoothDevice device) {
            Log.i(TAG, device.getName());
            if (device.getName().equals(bleDeviceName)) {
                mBleDevice = device;
                mBleScanner.stopScan(mBleScanCallback);
                mScanning = false;
                connectBluetoothDevice();
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleService = ((BleService.LocalBinder) service).getService();
            if (!mBleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBleService.connect(mBleDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleService = null;
        }
    };

    private void connectBluetoothDevice() {
        Intent gattServiceIntent = new Intent(this, BleService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
                deviceConnected = true;
                mNavFragment.connectedToDevice();
            } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                deviceConnected = false;
                mNavFragment.disconnectedFromDevice();
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                findMldpGattService(mBleService.getSupportedGattServices());
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BleService.EXTRA_DATA));
                String dataValue = intent.getStringExtra(BleService.EXTRA_DATA);
                mNavFragment.displayData(dataValue);
                // TODO: Do what needs to be done for incoming data
            }
        }
    };

    // ----------------------------------------------------------------------------------------------------------------
    // Iterate through the supported GATT Services/Characteristics to see if the MLDP srevice is supported
    private void findMldpGattService(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {                                                     //Verify that list of GATT services is valid
            Log.d(TAG, "findMldpGattService found no Services");
            return;
        }
        String uuid;                                                                    //String to compare received UUID with desired known UUIDs
        mDataMDLP = null;                                                               //Searching for a characteristic, start with null value

        for (BluetoothGattService gattService : gattServices) {                         //Test each service in the list of services
            uuid = gattService.getUuid().toString();//Get the string version of the service's UUID
            Log.i(TAG, "The uuid service: " + uuid);
            if (uuid.equals(BleService.MLDP_PRIVATE_SERVICE)) {                                    //See if it matches the UUID of the MLDP service
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics(); //If so then get the service's list of characteristics
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) { //Test each characteristic in the list of characteristics
                    uuid = gattCharacteristic.getUuid().toString();                     //Get the string version of the characteristic's UUID
                    if (uuid.equals(BleService.MLDP_DATA_PRIVATE_CHAR)) {                          //See if it matches the UUID of the MLDP data characteristic
                        mDataMDLP = gattCharacteristic;                                 //If so then save the reference to the characteristic
                        Log.i(TAG, "Found MLDP data characteristics");
                    } else if (uuid.equals(BleService.MLDP_CONTROL_PRIVATE_CHAR)) {                  //See if UUID matches the UUID of the MLDP control characteristic
                        mControlMLDP = gattCharacteristic;                              //If so then save the reference to the characteristic
                        Log.i(TAG, "Found MLDP control characteristics");
                    }
                    final int characteristicProperties = gattCharacteristic.getProperties(); //Get the properties of the characteristic
                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //See if the characteristic has the Notify property

                        mBleService.setCharacteristicNotification(gattCharacteristic, true); //If so then enable notification in the BluetoothGatt
                    }
                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_INDICATE)) > 0) { //See if the characteristic has the Indicate property
                        mBleService.setCharacteristicIndication(gattCharacteristic, true); //If so then enable notification (and indication) in the BluetoothGatt
                    }
                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //See if the characteristic has the Write (acknowledged) property
                        gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with acknowledge) in the BluetoothGatt
                    }
                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                        gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                    }
                }
                break;
            }
        }
        if (mDataMDLP == null) {                                                        //See if the MLDP data characteristic was not found
            Toast.makeText(this, "MLDP not supported!", Toast.LENGTH_SHORT).show(); //If so then show an error message
            Log.i(TAG, "findMldpGattService found no MLDP service");
            finish();                                                                   //and end the activity
        }
        mHandler.postDelayed(new Runnable() {                                           //Create delayed runnable that will send a roll of the die after a delay
            @Override
            public void run() {
                readDataFromDevice();                                                       //Update the state of the die with a new roll and send over BLE
            }
        }, 200);                                                                        //Do it after 200ms delay to give the RN4020 time to configure the characteristic

    }

    private void readDataFromDevice() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBleService.readCharacteristic(mDataMDLP);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void initiateUi(final FirebaseUser currentUser) {
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, USER_INFO_REQUEST);
        } else {

            dbRef.child("users").child(currentUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            UserData userData = dataSnapshot.getValue(UserData.class);
                            updateUserData(userData);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.w(TAG, "Failed to read user data", databaseError.toException());
                        }
                    });

            dbRef.child("users").child(currentUser.getUid()).child("username")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            // TODO: Handle cases if username is changed
                            if (mUserData != null) {
                                updateUserName((String)dataSnapshot.getValue());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.w(TAG, "Failed to read username", databaseError.toException());
                        }
                    });
        }
    }
}
