package ap.mailo.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import ap.mailo.util.DomainParser;
import ap.mailo.util.UserDomainHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser implements Parcelable {

    public static final String ACCOUNT_INFO = "ACC";
    public static final String ACCOUNT_hostSMTP = "host_SMTP";
    public static final String ACCOUNT_portSMTP = "port_SMTP";
    public static final String ACCOUNT_hostIMAP = "host_IMAP";
    public static final String ACCOUNT_portIMAP = "port_IMAP";
    public static final String ACCOUNT_socketTypeSMTP = "socketTypeSMTP";
    public static final String ACCOUNT_socketTypeIMAP = "socketTypeIMAP";
    public static final String ACCOUNT_shortDisplayName = "shortDisplayName";
    public static final String ACCOUNT_messageAmount = "messageAmount";

    private String mail;
    private String password;
    private String hostSMTP;
    private String portSMTP;
    private String hostIMAP;
    private String portIMAP;
    private String socketTypeSMTP;
    private String socketTypeIMAP;
    private String shortDisplayName;

    public String getMail() {
        return mail;
    }
    public String getPassword() {
        return password;
    }
    public String getHostSMTP() {
        return hostSMTP;
    }
    public String getPortSMTP() {
        return portSMTP;
    }
    public String getHostIMAP() {
        return hostIMAP;
    }
    public String getPortIMAP() {
        return portIMAP;
    }
    public String getSocketTypeSMTP() {
        return socketTypeSMTP;
    }
    public String getSocketTypeIMAP() {
        return socketTypeIMAP;
    }
    public String getShortDisplayName() {
        return shortDisplayName;
    }

    public Properties getSMTPProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        if(socketTypeSMTP.equalsIgnoreCase("STARTTLS")){
            properties.put("mail.smtp.starttls.enable", "true");
        }
        if(socketTypeSMTP.equalsIgnoreCase("SSL")){
            properties.setProperty("mail.smtp.ssl.enable", "true");
        }
        properties.put("mail.smtp.host",hostSMTP);
        properties.put("mail.smtp.port",portSMTP);
        return properties;
    }

    public Properties getIMAPProperties() {
        Properties properties = new Properties();
        if(socketTypeSMTP.equalsIgnoreCase("STARTTLS")){
            properties.put("mail.imap.starttls.enable", "true");
        }
        if(socketTypeSMTP.equalsIgnoreCase("SSL")){
            properties.setProperty("mail.imap.ssl.enable", "true");
        }
        properties.put("mail.mime.allowutf8", true);
        properties.put("mail.imap.host",hostIMAP);
        properties.put("mail.imap.port",portIMAP);
        return properties;
    }

    public LoggedInUser(String mail, String password, String hostSMTP, String portSMTP, String socketTypeSMTP, String hostIMAP, String portIMAP, String socketTypeIMAP, String shortDisplayName) {
        this.mail = mail;
        this.password = password;
        this.hostSMTP = hostSMTP;
        this.portSMTP = portSMTP;
        this.hostIMAP = hostIMAP;
        this.portIMAP = portIMAP;
        this.socketTypeSMTP = socketTypeSMTP;
        this.socketTypeIMAP = socketTypeIMAP;
        this.shortDisplayName = shortDisplayName;
    }

    public LoggedInUser(Account account, Context mContext){
        final AccountManager am = AccountManager.get(mContext);
        this.mail = account.name;
        this.password = am.getPassword(account);
        this.hostSMTP = am.getUserData(account, ACCOUNT_hostSMTP);
        this.portSMTP = am.getUserData(account, ACCOUNT_portSMTP);
        this.hostIMAP = am.getUserData(account, ACCOUNT_hostIMAP);
        this.portIMAP = am.getUserData(account, ACCOUNT_portIMAP);
        this.socketTypeSMTP = am.getUserData(account, ACCOUNT_socketTypeSMTP);
        this.socketTypeIMAP = am.getUserData(account, ACCOUNT_socketTypeIMAP);
        this.shortDisplayName = am.getUserData(account, ACCOUNT_shortDisplayName);
    }

    public LoggedInUser(LoggedInUser user)
    {
        this.mail = user.getMail();
        this.password = user.getPassword();
        this.hostSMTP = user.getHostSMTP();
        this.portSMTP = user.getPortSMTP();
        this.hostIMAP = user.getHostIMAP();
        this.portIMAP = user.getPortIMAP();
        this.socketTypeSMTP = user.getSocketTypeSMTP();
        this.socketTypeIMAP = user.getSocketTypeIMAP();
        this.shortDisplayName = user.getShortDisplayName();
    }

    // Implement Parcelable interface
    public LoggedInUser(Parcel in)
    {
        this.mail = in.readString();
        this.password = in.readString();
        this.hostSMTP = in.readString();
        this.portSMTP = in.readString();
        this.hostIMAP = in.readString();
        this.portIMAP = in.readString();
        this.socketTypeSMTP = in.readString();
        this.socketTypeIMAP = in.readString();
        this.shortDisplayName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeString(mail);
        parcel.writeString(password);
        parcel.writeString(hostSMTP);
        parcel.writeString(portSMTP);
        parcel.writeString(hostIMAP);
        parcel.writeString(portIMAP);
        parcel.writeString(socketTypeSMTP);
        parcel.writeString(socketTypeIMAP);
        parcel.writeString(shortDisplayName);
    }

    public static final Parcelable.Creator<LoggedInUser> CREATOR = new Parcelable.Creator<LoggedInUser>() {
        public LoggedInUser createFromParcel(Parcel in) {
            return new LoggedInUser(in);
        }
        public LoggedInUser[] newArray(int size) {
            return new LoggedInUser[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    // Implement equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoggedInUser that = (LoggedInUser) o;

        if (!mail.equals(that.mail)) return false;
        if (!password.equals(that.password)) return false;
        if (!hostSMTP.equals(that.hostSMTP)) return false;
        if (!portSMTP.equals(that.portSMTP)) return false;
        if (!hostIMAP.equals(that.hostIMAP)) return false;
        if (!portIMAP.equals(that.portIMAP)) return false;
        if (!socketTypeSMTP.equals(that.socketTypeSMTP)) return false;
        if (!socketTypeIMAP.equals(that.socketTypeIMAP)) return false;
        return shortDisplayName.equals(that.shortDisplayName);
    }

    @Override
    public int hashCode() {
        int result = mail != null ? mail.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (hostSMTP != null ? hostSMTP.hashCode() : 0);
        result = 31 * result + (portSMTP != null ? portSMTP.hashCode() : 0);
        result = 31 * result + (hostIMAP != null ? hostIMAP.hashCode() : 0);
        result = 31 * result + (portIMAP != null ? portIMAP.hashCode() : 0);
        result = 31 * result + (socketTypeSMTP != null ? socketTypeSMTP.hashCode() : 0);
        result = 31 * result + (socketTypeIMAP != null ? socketTypeIMAP.hashCode() : 0);
        result = 31 * result + (shortDisplayName != null ? shortDisplayName.hashCode() : 0);
        return result;
    }
}
