package ap.mailo.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.List;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.mail.Authenticator;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;

public class MessageNetwork {

    private static final String TAG = MessageNetwork.class.getSimpleName();

    public static final String KEY_MessagesToSave = "MessSave";
    public static final String KEY_shouldNuke = "Purge";
    public static final String KEY_UIDsToDelete = "UIDsDelete";

    private MessageNetwork() {}

    public static Single<Bundle> updateFolderMessages(String folderName, LoggedInUser user, List<MessageHeader> messagesDB, Context context) {
        return Single.fromCallable(() -> {
            try {
                SharedPreferences mSharedPreferences = context.getSharedPreferences(context.getString(R.string.syncMessPrefs)+folderName, Context.MODE_PRIVATE);
                long UIDValidity = mSharedPreferences.getLong(context.getString(R.string.UIDValidity), Long.MIN_VALUE);
                long lastUIDSynced = mSharedPreferences.getLong(context.getString(R.string.lastUID), -1);

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
                Message[] messages;
                Boolean shouldNuke;

                // If UIDValidity is equal, then perform partial synchronization only for the messages deleted and newly added
                if(UIDValidity == folderUIDValidity) {
                    Log.d(context.getString(R.string.app_name), TAG + "> Getting only newest messages " + folderName);
                    // Get messages in range for new UIDs
                    messages = folder.getMessagesByUID(lastUIDSynced, UIDFolder.LASTUID);
                    shouldNuke = false;
                } else { // UIDValidity has changed which means that UIDs have to be set again and every message has to be fetched again
                    Log.d(context.getString(R.string.app_name), TAG + "> Syncing all messages " + folderName);
                    messages = folder.getMessages();
                    shouldNuke = true;
                }

                // Get headers from messages
                MessageHeader[] messagesToSave = getHeadersFromMessages(messages, folder);
                // Get UIDs of messages to delete
                long[] UIDsToDelete = getUIDsToDelete(messages, messagesDB, folder);

                // Set output bundle with all data
                Bundle bundle = new Bundle();
                bundle.putParcelableArray(KEY_MessagesToSave, messagesToSave);
                bundle.putBoolean(KEY_shouldNuke, shouldNuke);
                bundle.putLongArray(KEY_UIDsToDelete, UIDsToDelete);

                // Set the UIDValidity and lastUID
                mSharedPreferences.edit().putLong(context.getString(R.string.UIDValidity), folderUIDValidity).apply();
                lastUIDSynced = folder.getUID(messages[messages.length-1]);
                mSharedPreferences.edit().putLong(context.getString(R.string.lastUID), lastUIDSynced).apply();
                // Close folder connection
                folder.close(false);
                // Close server connection
                store.close();

                return bundle;
            } catch (Exception e) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArray(KEY_MessagesToSave, new MessageHeader[0]);
                bundle.putBoolean(KEY_shouldNuke, false);
                bundle.putLongArray(KEY_UIDsToDelete, new long[0]);

                return bundle;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    // Get headers from messages
    private static MessageHeader[] getHeadersFromMessages(Message[] messages, IMAPFolder folder) {
        List<MessageHeader> headers = new ArrayList<>();
        for (Message message : messages) {
            try {
                MessageHeader sm = new MessageHeader();
                sm.setUID(folder.getUID(message));
                sm.setFrom(message.getFrom()[0].toString());
                sm.setSubject(message.getSubject());
                sm.setFolder(folder.getName().toUpperCase());
                headers.add(sm);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        MessageHeader[] savedMessages = new MessageHeader[headers.size()];
        savedMessages = headers.toArray(savedMessages);
        return savedMessages;
    }

    // Get UIDs of messages to delete from database
    private static long[] getUIDsToDelete(Message[] messages, List<MessageHeader> messagesDB, IMAPFolder folder) {
        List<Long> UIDsToDelete = new ArrayList<>();
        for( MessageHeader mh : messagesDB ) {
            Message mess = null;
            try {
                mess = folder.getMessageByUID(mh.getUID());
                if(mess == null || mess.getFlags().contains(Flags.Flag.DELETED)){
                    UIDsToDelete.add(mh.getUID());
                }
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }


        long[] uids = new long[UIDsToDelete.size()];
        int index = 0;
        for(Long val : UIDsToDelete) {
            uids[index++] = val;
        }
        return uids;
    }
}
