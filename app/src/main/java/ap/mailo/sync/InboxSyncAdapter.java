package ap.mailo.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.sun.mail.imap.IMAPFolder;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import ap.mailo.main.EntryActivity;
import ap.mailo.store.InboxViewModel;
import ap.mailo.store.InboxViewModelFactory;
import jakarta.mail.Authenticator;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;

public class InboxSyncAdapter extends AbstractThreadedSyncAdapter {
    private final String TAG = this.getClass().getSimpleName();
    private final String CHANNEL_ID;

    private final NotificationManagerCompat notificationManager;
    private final AccountManager mAccountManager;
    private final Application app;
    private String folderName;

    public InboxSyncAdapter(Application application, boolean autoInitialize) {
        super(application, autoInitialize);
        app = application;
        mAccountManager = AccountManager.get(application);
        folderName = app.getString(R.string.defaultFolder);
        CHANNEL_ID  = app.getString(R.string.app_name);
        notificationManager = NotificationManagerCompat.from(application);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d(app.getString(R.string.app_name), TAG + "> Performs Synchronization");
        try {
            mAccountManager.blockingGetAuthToken(account, app.getString(R.string.ACCOUNT_TYPE), true);
            LoggedInUser user = new LoggedInUser(account, app);

            createNotificationChannel();

            SharedPreferences mSharedPreferences = app.getSharedPreferences(app.getString(R.string.syncMessPrefs)+folderName, Context.MODE_PRIVATE);
            long UIDValidity = mSharedPreferences.getLong(app.getString(R.string.UIDValidity), Long.MIN_VALUE);
            long lastUIDSynced = mSharedPreferences.getLong(app.getString(R.string.lastUID), -1);

            //Connect to IMAP server
            Session emailSession = Session.getInstance(user.getIMAPProperties(),
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(user.getMail(), user.getPassword());
                        }
                    });
            Store store = emailSession.getStore("imaps");
            store.connect(user.getHostIMAP(), user.getMail(), user.getPassword());

            //Get folder
            IMAPFolder folder = (IMAPFolder) store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            long folderUIDValidity = folder.getUIDValidity();
            Message[] messages = null;

            // If UIDValidity is equal, then perform partial synchronization only for the messages deleted and newly added
            if(UIDValidity == folderUIDValidity) {
                Log.d(app.getString(R.string.app_name), TAG + "> Checking for new messages " + folderName);
                // Get messages in range for new UIDs
                messages = folder.getMessagesByUID(lastUIDSynced + 1, UIDFolder.MAXUID);
            }

            if(messages != null) {
                Log.d(app.getString(R.string.app_name), TAG + "> There are " + messages.length + " new messages");

                // Build notifications
                for(Message mess : messages) {
                    // Create an explicit intent for an Activity in your app
                    Intent intent = new Intent(app, EntryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(app, 0, intent, 0);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(app, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle(mess.getSubject())
                            .setContentText(mess.getFrom()[0].toString())
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            // Set the intent that will fire when the user taps the notification
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    notificationManager.notify(mess.getMessageNumber(), builder.build());
                }
            } else {
                Log.d(app.getString(R.string.app_name), TAG + "> There are " + 0 + " new messages");
            }

            // Close folder connection
            folder.close(false);
            // Close server connection
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = app.getString(R.string.channel_name);
            String description = app.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = app.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}