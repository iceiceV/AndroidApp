package com.ice.android.icedevice;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NavigationFragment extends Fragment
        implements BluetoothUICallbacks {
    private static final String TAG = NavigationFragment.class.getSimpleName();

    private boolean deviceConnected = false;

    OnConnectionRequestListener mCallback;
    Button connectDeviceButton;
    TextView displayDataText;
    ListView listViewLE;
    List<BluetoothDevice> listBluetoothDevice;
    ListAdapter adapterLeScanResult;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private ExpandableListView mGattServicesList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_navigation, container, false);

        //ButterKnife.bind(mView);
        connectDeviceButton = mView.findViewById(R.id.connect_device_button);
        displayDataText = mView.findViewById(R.id.display_data);
        //listViewLE = mView.findViewById(R.id.lelist);
        //listBluetoothDevice = new ArrayList<>();
        //adapterLeScanResult = new ArrayAdapter<BluetoothDevice>(
        //        getActivity(), android.R.layout.simple_list_item_1, listBluetoothDevice);
        //listViewLE.setAdapter(adapterLeScanResult);
        //mGattServicesList = mView.findViewById(R.id.gatt_services_list);
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);

        connectDeviceButton.setOnClickListener((View v) -> {
            // TODO: Connect to device
            if (deviceConnected) {
                mCallback.onBleDisconnectRequest();
            } else {
                mCallback.onBleConnectionRequest();
            }
        });

        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnConnectionRequestListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnConnectionRequestListener");
        }
    }

    @Override
    public void connectedToDevice() {
        deviceConnected = true;
        connectDeviceButton.setText(getResources().getString(R.string.disconnect_button_text));
        connectDeviceButton.setBackgroundColor(getResources().getColor(R.color.buttonRed));
    }

    @Override
    public void connectingToDevice() {
        // TODO: Put a loading icon
    }

    @Override
    public void disconnectedFromDevice() {
        deviceConnected = false;
        connectDeviceButton.setText(getResources().getString(R.string.connect_button_text));
        connectDeviceButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
    }

    @Override
    public void displayData(String data) {
        displayDataText.setText(data);
    }

    @Override
    public void noDeviceFound() {

    }

    private static HashMap<String, String> attributes = new HashMap();

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public interface OnConnectionRequestListener {
        void onBleConnectionRequest();
        void onBleDisconnectRequest();
    }
}
