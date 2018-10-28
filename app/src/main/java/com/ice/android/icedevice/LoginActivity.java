package com.ice.android.icedevice;

import android.app.Activity;
import android.content.Intent;
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

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    @BindView(R.id.btn_login) Button btnLogin;
    @BindView(R.id.input_email) EditText inputEmail;
    @BindView(R.id.input_password) EditText inputPassword;
    @BindView(R.id.link_signup) TextView linkSignup;
    @BindView(R.id.pb_login) ProgressBar progressBar;

    private int requestSignupKey = 0;
    private Intent resultIntent;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        btnLogin.setOnClickListener((View v) -> {
            login(inputEmail.getText().toString(), inputPassword.getText().toString());
        });

        resultIntent = new Intent(this, MainActivity.class);

        linkSignup.setOnClickListener((View v) -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivityForResult(intent, requestSignupKey);
        });

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == requestSignupKey) {
            if (resultCode == Activity.RESULT_OK) {
                navigateToHomeAfterSignup();
            }
        }
    }

    private void login(String email, String password) {
        if (!validate(email, password)) {
            onLoginFailed();
            return;
        }

        onLoginWithEmail(email, password);
    }

    public boolean validate(String email, String password) {
        boolean valid = true;

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

    public void onLoginWithEmail(String email, String password) {
        disableButton();
        showProgress();
        userLogin(email, password);
    }

    public void onLoginFailed() {
        hideProgress();
        showToast("Authentication failed.");
        enableButton();
    }

    public void onLoginSuccess() {
        hideProgress();
        enableButton();
        navigateToHome();
    }

    public void navigateToHome() {
        finish();
    }

    public void navigateToHomeAfterSignup() {
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    public void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }
    public void hideProgress() {
        progressBar.setVisibility(View.INVISIBLE);
    }
    public void enableButton() {
        btnLogin.setEnabled(true);
    }
    public void disableButton() {
        btnLogin.setEnabled(false);
    }
    public void setEmailError(String message) {
        inputEmail.setError(message);
    }
    public void setPasswordError(String message) {
        inputPassword.setError(message);
    }
    public void showToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    public void userLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            onLoginSuccess();
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            onLoginFailed();
                        }
                    }
                });
    }
}
