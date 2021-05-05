package ap.mailo.store;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import ap.mailo.auth.LoggedInUser;

public class InboxViewModelFactory implements ViewModelProvider.Factory {
    private Application mApplication;
    private String mFolderName;
    private LoggedInUser mUser;


    public InboxViewModelFactory(Application application, String folderName, LoggedInUser user) {
        mApplication = application;
        mFolderName = folderName;
        mUser = user;
    }


    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new InboxViewModel(mApplication, mFolderName, mUser);
    }
}
