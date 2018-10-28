package com.ice.android.icedevice;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.main_fragment_container) View fragmentContainer;

    static final int USER_INFO_REQUEST = 1;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

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

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        initiateUi(currentUser);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void setActionBarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
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
}
