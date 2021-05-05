package ap.mailo.store;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "MessageHeaders", primaryKeys = {"UID", "folder"})
public class MessageHeader implements Parcelable {
    @NonNull
    private long UID;
    @NonNull
    private String folder;
    private String from;
    private String subject;

    public MessageHeader() {}

    protected MessageHeader(Parcel in) {
        UID = in.readLong();
        from = in.readString();
        subject = in.readString();
        folder = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(UID);
        dest.writeString(from);
        dest.writeString(subject);
        dest.writeString(folder);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MessageHeader> CREATOR = new Creator<MessageHeader>() {
        @Override
        public MessageHeader createFromParcel(Parcel in) {
            return new MessageHeader(in);
        }

        @Override
        public MessageHeader[] newArray(int size) {
            return new MessageHeader[size];
        }
    };

    public void setUID(long UID){ this.UID = UID; }
    public long getUID(){ return UID; }
    public void setFrom(String from){ this.from = from; }
    public String getFrom(){ return from; }
    public void setSubject(String subject){ this.subject = subject; }
    public String getSubject(){ return subject; }
    public void setFolder(String folderName){ this.folder = folderName; }
    public String getFolder() { return folder; }
}
