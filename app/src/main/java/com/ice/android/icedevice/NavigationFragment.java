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

import butterknife.BindView;
import butterknife.ButterKnife;

public class NavigationFragment extends Fragment {
    private static final String TAG = NavigationFragment.class.getSimpleName();

    //@BindView(R.id.live_data_button) Button liveDataButton;
    //@BindView(R.id.view_record_button) Button viewRecordButton;

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

        liveDataButton.setOnClickListener((View v) -> {
            // TODO: Show live data graph
        });

        viewRecordButton.setOnClickListener((View v) -> {
            // TODO: Show record
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
    }
}
