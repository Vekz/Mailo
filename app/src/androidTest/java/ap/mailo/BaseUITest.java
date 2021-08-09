package ap.mailo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import ap.mailo.auth.LoginNetwork;
import ap.mailo.auth.LoginResult;
import ap.mailo.auth.LoginViewModel;

public abstract class BaseUITest
{

    protected static Context appContext;
    protected static Context testContext;
    protected static LoginViewModel loginViewModel;
    protected static LoginResult loginResult;

    protected static String mail;
    protected static String password;

    protected static LoginResult getLoginResult() {
        return loginResult;
    }

    public static void loadCredentialsFromJSON() throws JSONException
    {
        String jsonString = testContext.getClassLoader().getResource("account.json").toString();

        JSONObject obj = new JSONObject(jsonString);
        mail = obj.getString("mail");
        password = obj.getString("password");
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        appContext = ApplicationProvider.getApplicationContext();
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        loginViewModel = new LoginViewModel();

        loadCredentialsFromJSON();
        LoginNetwork.loginUser(mail, password).subscribe(result -> loginResult = result);
        assert getLoginResult().getSuccess() != null;
        loginViewModel.finishLogin(getLoginResult().getSuccess(), appContext);

        Log.i("BASE TEST: ","Logged in before test cases");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
        AccountManager am = AccountManager.get(appContext);
        Account[] accounts = am.getAccounts();
        if (accounts.length > 0) {
            Account accountToRemove = accounts[0];
            am.removeAccount(accountToRemove, null, null, null);
            Log.i("BASE TEST: ", "Logged out after test cases");
        }
    }

}
