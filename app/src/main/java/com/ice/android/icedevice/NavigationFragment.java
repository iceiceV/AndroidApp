package com.ice.android.icedevice;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class NavigationFragment extends Fragment {
    private static final String TAG = NavigationFragment.class.getSimpleName();

    OnConnectionRequestListener mCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_navigation, container, false);

        //ButterKnife.bind(mView);
        Button liveDataButton = mView.findViewById(R.id.live_data_button);
        Button viewRecordButton = mView.findViewById(R.id.view_record_button);
        Button connectDeviceButton = mView.findViewById(R.id.connect_device_button);

        liveDataButton.setOnClickListener((View v) -> {
            // TODO: Show live data graph
        });

        viewRecordButton.setOnClickListener((View v) -> {
            // TODO: Show record
        });

        connectDeviceButton.setOnClickListener((View v) -> {
            // TODO: Connect to device
            mCallback.onBleConnectionRequest();
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

    public interface OnConnectionRequestListener {
        public void onBleConnectionRequest();
    }
}
