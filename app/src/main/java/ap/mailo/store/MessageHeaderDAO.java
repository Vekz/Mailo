package ap.mailo.store;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageHeaderDAO {
    @Query("SELECT * FROM MessageHeaders WHERE folder == :folderName ORDER BY UID")
    public LiveData<List<MessageHeader>> getAllFromFolder(String folderName);

    @Query("SELECT * FROM MessageHeaders WHERE UID = :UID LIMIT 1")
    public MessageHeader findByUID(long UID);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertMessages(MessageHeader... messages);

    @Query("DELETE FROM MessageHeaders")
    public void nukeMessages();

    @Query("DELETE FROM MessageHeaders WHERE folder == :folderName")
    public void nukeMessagesByFolder(String folderName);

    @Query("DELETE FROM MessageHeaders WHERE folder == :folderName AND UID in (:uids)")
    public void deleteFromFolderByUIDs(long[] uids, String folderName);
}
