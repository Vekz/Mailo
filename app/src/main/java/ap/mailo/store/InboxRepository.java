package ap.mailo.store;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import ap.mailo.auth.LoggedInUser;
import ap.mailo.util.InternetChecker;
import io.reactivex.rxjava3.functions.Consumer;

public class InboxRepository {
    private MessageHeaderDAO messageHeaderDAO;
    private LiveData<List<MessageHeader>> messages;
    private LoggedInUser mUser;
    private String mFolderName;
    private Application mApplication;

    InboxRepository(Application application, String folderName, LoggedInUser user){
        InboxDatabase database = InboxDatabase.getDatabase(application);
        messageHeaderDAO = database.messageHeaderDAO();
        mUser = user;
        mFolderName = folderName;
        mApplication = application;

        messages = messageHeaderDAO.getAllFromFolder(mFolderName.toUpperCase());
    }

    LiveData<List<MessageHeader>> getAllMessages() {
        return messages;
    }


    MessageHeader getMessageById(int id) { return messageHeaderDAO.findByUID(id); }

    void insert(MessageHeader... messages) {
        InboxDatabase.databaseWriteExecutor.execute(() -> messageHeaderDAO.insertMessages(messages));
    }

    void nukeTable() {
        InboxDatabase.databaseWriteExecutor.execute(() -> messageHeaderDAO.nukeMessages());
    }

    void nukeTableByFolder(String folderName) {
        InboxDatabase.databaseWriteExecutor.execute(() -> messageHeaderDAO.nukeMessagesByFolder(folderName.toUpperCase()));
    }

    void deleteFromFolderByUIDs(long[] uids, String folderName) {
        deleteFromFolderByUIDs(null, uids, folderName);
    }

    void deleteFromFolderByUIDs(LoggedInUser user, long[] uids, String folderName) {
        InboxDatabase.databaseWriteExecutor.execute(() -> messageHeaderDAO.deleteFromFolderByUIDs(uids, folderName.toUpperCase()));
        if (user != null)
        {
            InternetChecker.hasInternetConnection()
                    .subscribe(has -> {
                        if (has) {
                            MessageNetwork.setMessagesFlagAsDeleted(user, uids, folderName).subscribe();
                        }
                    });
        }
    }

    void refreshMessages() {
        InternetChecker.hasInternetConnection()
            .subscribe(has -> {
            if(has){
                    MessageNetwork.updateFolderMessages(mFolderName, mUser, getAllMessages().getValue(), mApplication)
                        .subscribe(bundle -> {
                        long[] uids = bundle.getLongArray(MessageNetwork.KEY_UIDsToDelete);
                        if(uids.length > 0) {
                            deleteFromFolderByUIDs(uids, mFolderName);
                        }
                        if(bundle.getBoolean(MessageNetwork.KEY_shouldNuke)){
                            nukeTableByFolder(mFolderName);
                        }
                        insert((MessageHeader[]) bundle.getParcelableArray(MessageNetwork.KEY_MessagesToSave));
                    });
            }
        });
    }
}
