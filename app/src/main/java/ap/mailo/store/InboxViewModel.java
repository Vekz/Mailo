package ap.mailo.store;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import ap.mailo.auth.LoggedInUser;

public class InboxViewModel extends AndroidViewModel {
    private InboxRepository inboxRepository;
    private LiveData<List<MessageHeader>> messages;

    public InboxViewModel(@NotNull Application application, String folderName, LoggedInUser user) {
        super(application);
        inboxRepository = new InboxRepository(application, folderName, user);
        messages = inboxRepository.getAllMessages();
    }

    public LiveData<List<MessageHeader>> getAll() {
        return messages;
    }

    public MessageHeader getById(int id) { return inboxRepository.getMessageById(id); }

    public void insert(MessageHeader... messages) {
        inboxRepository.insert(messages);
    }

    public void refreshMessages() { inboxRepository.refreshMessages(); }

    public void nukeTable() { inboxRepository.nukeTable(); }

    public void nukeTableByFolder(String folderName) { inboxRepository.nukeTableByFolder(folderName); }

    public void deleteFromFolderByUIDs(long[] uids, String folderName) { inboxRepository.deleteFromFolderByUIDs(uids, folderName); }
}
