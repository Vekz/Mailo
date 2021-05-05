package ap.mailo.auth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import ap.mailo.R;
import ap.mailo.store.InboxViewModel;
import ap.mailo.store.InboxViewModelFactory;
import ap.mailo.util.AccountAuthenticatorAppCompatActivity;

public class Login extends AccountAuthenticatorAppCompatActivity {
    private static final String TAG = Login.class.getSimpleName();

    private LoginViewModel loginViewModel;

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        loadingProgressBar = findViewById(R.id.loading);

        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) {
                return;
            }
            loginButton.setEnabled(loginFormState.isDataValid());
            if (loginFormState.getUsernameError() != null) {
                usernameEditText.setError(getString(loginFormState.getUsernameError()));
            }
            if (loginFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(loginFormState.getPasswordError()));
            }
        });

        loginViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) {
                return;
            }
            loadingProgressBar.setVisibility(View.GONE);
            if (loginResult.getError() != null) {
                showLoginFailed(loginResult.getError());
            }
            if (loginResult.getSuccess() != null) {
                updateUiWithUser(loginResult.getSuccess());
                finishLogin(loginResult.getSuccess());
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };

        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login();
            }
            return false;
        });

        loginButton.setOnClickListener(view -> login());
    }

    public void login() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        loginViewModel.login(usernameEditText.getText().toString(),
                passwordEditText.getText().toString(),
                getApplicationContext());
    }

    public void finishLogin(LoggedInUser success) {
        loginViewModel.finishLogin(success, getApplicationContext());
        Bundle bundle = new Bundle();
        bundle.putParcelable(LoggedInUser.ACCOUNT_INFO, success);
        setAccountAuthenticatorResult(bundle);
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void updateUiWithUser(LoggedInUser model) {
        String welcome = getString(R.string.welcome) + model.getMail();
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
        clearView();
    }

    private void clearView(){
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        loadingProgressBar = findViewById(R.id.loading);

        usernameEditText.setText(null);
        passwordEditText.setText(null);
        loginButton.setEnabled(false);
        loadingProgressBar.setVisibility(View.GONE);
    }
}