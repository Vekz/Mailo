package ap.mailo.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class InboxSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();
    private static InboxSyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null)
                sSyncAdapter = new InboxSyncAdapter(getApplication(), true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
