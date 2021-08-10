package ap.mailo;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import ap.mailo.auth.LoginNetwork;
import ap.mailo.auth.LoginResult;
import ap.mailo.auth.LoginViewModel;

public abstract class BaseUITest
{

    protected static Context appContext;
    protected static Context testContext;
    protected static LoginViewModel loginViewModel;

    protected static String mail;
    protected static String password;

    @Ignore("Setup function")
    public static void loadCredentialsFromJSON() throws JSONException
    {
        InputStream jsonInput = appContext.getResources().openRawResource(R.raw.account);
        Writer writer = new StringWriter();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(jsonInput, StandardCharsets.UTF_8));
            String line = reader.readLine();
            while (line != null) {
                writer.write(line);
                line = reader.readLine();
            }
        } catch (Exception e) {
            Log.e("BASE TEST: ", "Unhandled exception while using JSONResourceReader", e);
        } finally {
            try {
                jsonInput.close();
            } catch (Exception e) {
                Log.e("BASE TEST: ", "Unhandled exception while using JSONResourceReader", e);
            }
        }
        String jsonString = writer.toString();

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
        LoginNetwork.loginUser(mail, password).subscribe(result -> {
            if (result.getSuccess() != null)
                loginViewModel.finishLogin(result.getSuccess(), appContext);
        });

        Log.w("BASE TEST: ","Logged in before test cases");

        Thread.sleep(1500);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
        AccountManager am = AccountManager.get(appContext);
        Account[] accounts = am.getAccounts();
        if (accounts.length > 0) {
            Account accountToRemove = accounts[0];
            am.removeAccount(accountToRemove, null, null, null);
            Log.w("BASE TEST: ", "Logged out after test cases");
        }
    }

}
