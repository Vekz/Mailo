package ap.mailo.store;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {MessageHeader.class}, version = 3, exportSchema = false)
public abstract class InboxDatabase extends RoomDatabase {
    public abstract MessageHeaderDAO messageHeaderDAO();

    private static volatile InboxDatabase INSTANCE;
    public static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static InboxDatabase getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (InboxDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            InboxDatabase.class, "inbox_db").fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}