package ap.mailo.main;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
        setContentView(R.layout.splash_screen);
        Log.d(getString(R.string.app_name), TAG + "> Entry Point");

        mAccountManager = AccountManager.get(getBaseContext());
        launchAppOrLogin();

        Intent intent;
        if(acc != null) {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra(LoggedInUser.ACCOUNT_INFO, acc);
            intent.putExtra(MainActivity.KEY_FolderName, getString(R.string.defaultFolder));
        } else {
            intent = new Intent(this, EntryActivity.class);
        }

        startActivity(intent);
        finish();
    }

    void launchAppOrLogin() {
        Account[] accounts = mAccountManager.getAccountsByType(getString(R.string.ACCOUNT_TYPE));
        if(accounts.length == 0) {
            mAccountManager.addAccount(getString(R.string.ACCOUNT_TYPE), null, null, null, EntryActivity.this, null, null);
        } else {
            acc = new LoggedInUser(accounts[0], getBaseContext());
            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            int syncTime = mSharedPreferences.getInt(getString(R.string.pref_syncTime), 900);
            ContentResolver.addPeriodicSync(accounts[0], getString(R.string.ACCOUNT_TYPE), Bundle.EMPTY, syncTime);
        }
    }

}