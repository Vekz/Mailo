package ap.mailo.util;

import java.util.ArrayList;
import java.util.List;

import ap.mailo.auth.LoggedInUser;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.mail.Folder;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;

public class BottomDrawerPopulator {
    // Asynchronously get mailbox's folder names
    public static Single<ArrayList<String>> getFolderNames(LoggedInUser ACC) {
        return Single.fromCallable(() -> {
            ArrayList<String> folderNames = new ArrayList<>();
            try {
                Session emailSession = Session.getInstance(ACC.getIMAPProperties(),
                        new jakarta.mail.Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(ACC.getMail(), ACC.getPassword());
                            }
                        });
                Store store = emailSession.getStore("imaps");
                store.connect(ACC.getHostIMAP(), ACC.getMail(), ACC.getPassword());

                //Get folders
                Folder[] folders = store.getDefaultFolder().listSubscribed();

                for (Folder folder : folders) {
                    folderNames.add(folder.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return folderNames;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }
}
