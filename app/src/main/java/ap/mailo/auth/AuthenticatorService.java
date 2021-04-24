package ap.mailo.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        AuthenticatorIMAP authenticator = new AuthenticatorIMAP(this);
        return authenticator.getIBinder();
    }
}
