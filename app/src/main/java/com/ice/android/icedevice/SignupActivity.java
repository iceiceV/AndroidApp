package com.ice.android.icedevice;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    @BindView(R.id.btn_signup) Button signupButton;
    @BindView(R.id.input_name) EditText nameText;
    @BindView(R.id.input_email) EditText emailText;
    @BindView(R.id.input_password) EditText passwordText;
    @BindView(R.id.link_login) TextView loginLink;
    @BindView(R.id.pb_signup) ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        ButterKnife.bind(this);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        signupButton.setOnClickListener((View v) -> {
            signup(nameText.getText().toString(),
                    emailText.getText().toString(),
                    passwordText.getText().toString());
        });

        loginLink.setOnClickListener((View v) -> {
            finish();
        });
    }

    public void signup(String name, String email, String password) {
        if (!validate(name, email, password)) {
            onSignupFailed();
            return;
        }

        onCreatingUser(name, email, password);
    }

    public boolean validate(String name, String email, String password) {
        boolean valid = true;

        if (!InputValidations.isValidUsername(name)) {
            setUsernameError("enter a username, up to 16 characters");
            valid = false;
        } else {
            setUsernameError(null);
        }

        if (!InputValidations.isValidEmail(email)) {
            setEmailError("enter a valid email address");
            valid = false;
        } else {
            setEmailError(null);
        }

        if (!InputValidations.isValidPassword(password)) {
            setPasswordError("enter a password, at least 6 characters");
            valid = false;
        } else {
            setPasswordError(null);
        }

        return valid;
    }

    public void onSignupSuccess() {
        enableButton();
        navigateToHome();
    }

    public void onSignupFailed() {
        hideProgress();
        showToast("Authentication failed.");
        enableButton();
    }

    public void onCreatingUser(String name, String email, String password) {
        disableButton();
        showProgress();
        createNewUser(name, email, password);
    }

    public void finishedCreatingUser() {
        hideProgress();
    }

    public void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }
    public void hideProgress() {
        progressBar.setVisibility(View.INVISIBLE);
    }
    public void enableButton() {
        signupButton.setEnabled(true);
    }
    public void disableButton() {
        signupButton.setEnabled(false);
    }
    public void setUsernameError(String message) {
        nameText.setError(message);
    }
    public void setEmailError(String message) {
        emailText.setError(message);
    }
    public void setPasswordError(String message) {
        passwordText.setError(message);
    }

    public void showToast(String message) {
        Toast.makeText(SignupActivity.this, message,
                Toast.LENGTH_SHORT).show();
    }

    public void navigateToHome() {
        setResult(RESULT_OK, null);
        finish();
    }

    public void createNewUser(final String username,
                              String email,
                              String password) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                UserData userData = new UserData(
                                        username, user.getEmail());
                                dbRef.child("users").child(user.getUid()).setValue(userData);
                            }

                            onSignupSuccess();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            onSignupFailed();
                        }

                        finishedCreatingUser();
                    }
                });
    }
}
