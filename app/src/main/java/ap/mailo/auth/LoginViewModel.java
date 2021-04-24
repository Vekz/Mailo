package ap.mailo.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.preference.PreferenceManager;

import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import ap.mailo.R;
import ap.mailo.util.DomainParser;
import ap.mailo.util.UserDomainHandler;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;

public class LoginViewModel extends ViewModel {

    private final String TAG = this.getClass().getSimpleName();

    public static final String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public static final String ARG_AUTH_TYPE = "AUTH_TYPE";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACC";

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }
    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    private boolean isUserNameValid(String username) {
        return Patterns.EMAIL_ADDRESS.matcher(username).matches();
    }

    private boolean isPasswordValid(String password) {
        return password != null;
    }

    public void login(String mail, String password, Context context){
        Log.d(context.getString(R.string.app_name), TAG + "> login");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // Get domain for user's mail
                    String domain = DomainParser.parseDomain(mail);

                    // Check ISPDB for MX server settings
                    String url = "https://autoconfig.thunderbird.net/v1.1/" + domain;

                    // Setup SAX Parser and our XML handler
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser saxParser = factory.newSAXParser();
                    UserDomainHandler handler = new UserDomainHandler(mail, password, domain);

                    // Then parse XML
                    InputStream uri = new URL(url).openStream();
                    saxParser.parse(uri, handler);

                    LoggedInUser user = handler.getUser();

                    Session session = Session.getInstance(user.getSMTPProperties(),
                            new jakarta.mail.Authenticator() {
                                @Override
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(mail, password);
                                }
                            }
                    );

                    Transport transport = session.getTransport("smtp");
                    transport.connect(user.getHostSMTP(), mail, password);
                    transport.close();

                    loginResult.postValue(new LoginResult(user));
                    return null;
                } catch (Exception e) {
                    loginResult.postValue(new LoginResult(R.string.login_failed));
                    return null;
                }
            }
        }.execute();
    }

    public void finishLogin(LoggedInUser success, Context context) {
        Log.d(context.getString(R.string.app_name), TAG + "> finishLogin");

        AccountManager mAccountManager = AccountManager.get(context);

        String accountName = success.getMail();
        String accountPassword = success.getPassword();
        String accountType = context.getString(R.string.ACCOUNT_TYPE);

        final Account account = new Account(accountName, accountType);
        mAccountManager.addAccountExplicitly(account, accountPassword, null);

        //Set sync vars and request first sync
        ContentResolver.setSyncAutomatically(account, context.getString(R.string.ACCOUNT_TYPE), true);

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int sync_time = mSharedPreferences.getInt(context.getString(R.string.pref_sync_time), 900);
        ContentResolver.addPeriodicSync(account, context.getString(R.string.ACCOUNT_TYPE), Bundle.EMPTY, sync_time);

        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean( ContentResolver.SYNC_EXTRAS_MANUAL, true );
        settingsBundle.putBoolean( ContentResolver.SYNC_EXTRAS_EXPEDITED, true );
        ContentResolver.requestSync(account, context.getString(R.string.ACCOUNT_TYPE), settingsBundle);

        mAccountManager.setUserData(account, LoggedInUser.ACCOUNT_portSMTP, success.getHostSMTP());
        mAccountManager.setUserData(account, LoggedInUser.ACCOUNT_portSMTP, success.getPortSMTP());
        mAccountManager.setUserData(account, LoggedInUser.ACCOUNT_hostIMAP, success.getHostIMAP());
        mAccountManager.setUserData(account, LoggedInUser.ACCOUNT_portIMAP, success.getPortIMAP());
        mAccountManager.setUserData(account, LoggedInUser.ACCOUNT_socketTypeSMTP, success.getSocketTypeSMTP());
        mAccountManager.setUserData(account, LoggedInUser.ACCOUNT_socketTypeIMAP, success.getSocketTypeIMAP());
        mAccountManager.setUserData(account, LoggedInUser.ACCOUNT_shortDisplayName, success.getShortDisplayName());
        mAccountManager.setUserData(account, LoggedInUser.ACCOUNT_messageAmount, "0");
    }
}
