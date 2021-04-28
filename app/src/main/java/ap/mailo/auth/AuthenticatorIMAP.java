package ap.mailo.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import ap.mailo.R;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;

/*
Copyrights: Artur Porowski, 2021

Modified example from: http://blog.udinic.com/2013/04/24/write-your-own-android-authenticator/

This authenticator authenticates based on password stored not oAuth2
 */
public class AuthenticatorIMAP extends AbstractAccountAuthenticator {

    private final String TAG = this.getClass().getSimpleName();
    private final Context mContext;

    public AuthenticatorIMAP(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
        Log.d(mContext.getString(R.string.app_name), TAG + "> addAccount");

        final Intent intent = new Intent(mContext, Login.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


    @Override
    public String getAuthTokenLabel(String s) {
        return s + " (Label)";
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) {
        final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        LoggedInUser user = new LoggedInUser(account, mContext);

        try {
            Session session = Session.getInstance(user.getSMTPProperties(),
                new jakarta.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user.getMail(), user.getPassword());
                    }
                }
            );

            Transport transport = session.getTransport("smtp");
            transport.connect(user.getHostSMTP(), user.getMail(), user.getPassword());
            transport.close();

            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_PASSWORD, user.getPassword());
            return result;
        } catch (Exception e) {
            final Intent intent = new Intent(mContext, Login.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(LoginViewModel.ARG_ACCOUNT_TYPE, account.type);
            intent.putExtra(LoginViewModel.ARG_AUTH_TYPE, authTokenType);
            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }
    }

    /*UNSUPPORTED*/
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) {
        return null;
    }
    /*UNSUPPORTED*/
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) {
        return null;
    }
    /*UNSUPPORTED*/
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        return null;
    }
}
