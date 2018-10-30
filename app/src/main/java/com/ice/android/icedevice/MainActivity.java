package com.ice.android.icedevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationFragment.OnConnectionRequestListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.main_fragment_container) View fragmentContainer;

    static final int USER_INFO_REQUEST = 1;
    static final int REQUEST_ENABLE_BT = 11;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBleScanner;
    private Map<String, BluetoothDevice> mBleScanResults;
    private BleScanCallback mBleScanCallback;
    private boolean mScanning;
    private Handler mHandler;
    private Handler mLogHandler;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public UserData mUserData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        // If no fragment has been committed to the activity's ViewGroup then the NewGameFragment is
        // committed.
        if (fragmentContainer != null) {
            if (savedInstanceState == null) {
                NavigationFragment newNavFragment = new NavigationFragment();

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.main_fragment_container, newNavFragment).commit();
            }
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        initiateUi(currentUser);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == USER_INFO_REQUEST) {
            Log.i(TAG, "Just logged in");
        } else if (requestCode == REQUEST_ENABLE_BT) {
            Log.i(TAG, "Bluetooth turned on");
            onBleConnectionRequest();
        }
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

    private void updateUserData(UserData userData) {
        mUserData = userData;
    }
    private void updateUserName(String username) {
        mUserData.setUsername(username);
    }

    @Override
    public void onBleConnectionRequest() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startBleScan();
        }
    }

    public void startBleScan() {
        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        mBleScanResults = new HashMap<>();
        mBleScanCallback = new BleScanCallback();
        mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBleScanner.startScan(filters, settings, mBleScanCallback);

        mHandler = new Handler();
        mHandler.postDelayed(this::stopScan, SCAN_PERIOD);

        mScanning = true;
        Log.i(TAG, "Started scanning.");
    }

    private void stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBleScanner != null) {
            mBleScanner.stopScan(mBleScanCallback);
            scanComplete();
        }

        mBleScanCallback = null;
        mScanning = false;
        mHandler = null;
        Log.i(TAG, "Stopped scanning.");
    }

    private void scanComplete() {
        if (mBleScanResults.isEmpty()) {
            Log.i(TAG, "No device found");
            return;
        }

        for (String deviceAddress : mBleScanResults.keySet()) {
            BluetoothDevice device = mBleScanResults.get(deviceAddress);
            Log.i(TAG, "The address is " + mBleScanResults.get(deviceAddress).toString());
            /*
            GattServerViewModel viewModel = new GattServerViewModel(device);

            ViewGattServerBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                    R.layout.view_gatt_server,
                    mBinding.serverListContainer,
                    true);
            binding.setViewModel(viewModel);
            binding.connectGattServerButton.setOnClickListener(v -> connectDevice(device));*/
        }
    }

    private class BleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }
        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            mBleScanResults.put(deviceAddress, device);
        }
    };
}
