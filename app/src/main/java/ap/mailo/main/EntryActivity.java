package ap.mailo.main;

import static ap.mailo.util.StyleService.setPreferedStyle;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;

public class EntryActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    private AccountManager mAccountManager;
    private LoggedInUser acc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPreferedStyle(this);
        setContentView(R.layout.splash_screen);
        Log.d(getString(R.string.app_name), TAG + "> Entry Point");

        mAccountManager = AccountManager.get(getBaseContext());
        launchAppOrLogin();

        Intent intent;
        if(acc != null) {
            intent = new Intent(this, MainActivity.class);
            String mailto = getIntent().getDataString();
            intent.putExtra(WriteMessage.MAILTO_STRING, mailto);
            intent.putExtra(LoggedInUser.ACCOUNT_INFO, acc);
            intent.putExtra(MainActivity.KEY_FolderName, getString(R.string.defaultFolder));
        } else {
            intent = new Intent(this, EntryActivity.class);
        }

        (new Handler()).postDelayed(() -> {
                startActivity(intent);
                finish();
        }, 500); // Wait to show user a splash screen with PB logo etc.
    }

    void launchAppOrLogin() {
        Account[] accounts = mAccountManager.getAccountsByType(getString(R.string.ACCOUNT_TYPE));
        if(accounts.length == 0) {
            mAccountManager.addAccount(getString(R.string.ACCOUNT_TYPE), null, null, null, EntryActivity.this, null, null);
        } else {
            acc = new LoggedInUser(accounts[0], getBaseContext());
            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String syncTime_str = mSharedPreferences.getString(getString(R.string.pref_syncTime), "900");
            int syncTime = Integer.parseInt(syncTime_str);
            ContentResolver.addPeriodicSync(accounts[0], getString(R.string.ACCOUNT_TYPE), Bundle.EMPTY, syncTime);
        }
    }

}