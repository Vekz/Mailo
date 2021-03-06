package ap.mailo.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import ap.mailo.main.WriteMessage;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import static jakarta.mail.internet.MimeUtility.decodeText;

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

    public static Single<String[]> loadMessage(LoggedInUser user, long UID, String folderName) {
        return Single.fromCallable(() -> {
            try {
                //Connect to IMAP server
                Session emailSession = Session.getInstance(user.getIMAPProperties(),
                        new jakarta.mail.Authenticator() {
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

                Message message = folder.getMessageByUID(UID);

                String[] parts = new String[4];
                parts[0] = message.getSubject();
                parts[1] = decodeText(message.getFrom()[0].toString());
                parts[2] = "";

                Address[] recipients = message.getRecipients(Message.RecipientType.TO);
                if(recipients != null) {
                    for (Address ad : recipients) {
                        parts[2] += decodeText(ad.toString());
                    }
                }

                parts[3] = getText(message);

                store.close();

                return parts;
            } catch (Exception e) {
                e.printStackTrace();
                return new String[0];
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static Single<Boolean> sendMessage(LoggedInUser user, Address[] recipients, String subject, String content){
        return Single.fromCallable(() -> {
            try {
                //Connect to IMAP server
                Session emailSession = Session.getInstance(user.getSMTPProperties(),
                        new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(user.getMail(), user.getPassword());
                            }
                        });

                try {
                    Message message = new MimeMessage(emailSession);
                    message.setFrom(new InternetAddress(user.getMail()));
                    message.setRecipients(Message.RecipientType.TO, recipients);
                    message.setSubject(subject);

                    Multipart multipart = new MimeMultipart();
                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setText(content);
                    multipart.addBodyPart(messageBodyPart);

                    message.setContent(multipart);
                    message.setSentDate(new Date());

                    Transport.send(message);
                    return true;
                } catch (MessagingException e) {
                    e.printStackTrace();
                    throw e;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static Single<Boolean> setMessagesFlagAsDeleted(LoggedInUser user, long[] uids, String folderName){
        return Single.fromCallable(() -> {
            try {
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
                folder.open(Folder.READ_WRITE);

                Message[] messagesList = folder.getMessagesByUID(uids);
                for (var message : messagesList) {
                    message.setFlag(Flags.Flag.DELETED, true);
                }

                folder.close(true);
                store.close();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
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

    /* Code from https://gist.github.com/winterbe/5958387 */
    private static String getText(Part p) throws MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text += getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }

}
