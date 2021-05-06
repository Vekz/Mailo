package ap.mailo.main;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.math.MathUtils;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.mail.Folder;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    public static final String KEY_FolderName = "folderName";
    public static final String KEY_Acc = "ACC";
    public static final String KEY_FolderList = "folderList";

    private LoggedInUser ACC;

    // Navigation variables
    private NavigationView navigationView;
    private NavController navController;
    private BottomAppBar bottomAppBar;
    private FrameLayout scrim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get attributes passed from EntryActivity
        String folderName = getIntent().getStringExtra(KEY_FolderName);
        ACC = getIntent().getParcelableExtra(LoggedInUser.ACCOUNT_INFO);

        // Bottom App Bar, bottom drawer and navigationController
        bottomAppBar = findViewById(R.id.bottomAppBar);
        scrim = findViewById(R.id.scrim);
        navigationView = findViewById(R.id.navigationView);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Setup Bottom App Bar and bottom drawer behaviour
        setSupportActionBar(bottomAppBar);
        BottomSheetBehavior<NavigationView> bottomSheetBehavior = BottomSheetBehavior.from(navigationView);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        scrim.setVisibility(View.GONE);

        // Bottom App Bar hamburger menu listener
        bottomAppBar.setNavigationOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));

        // Scrim listener
        scrim.setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            scrim.setVisibility(View.GONE);
        });

        // On slide of bottom drawer scrim should get darker colour starting from 60% opacity black
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Unused
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                scrim.setVisibility(View.VISIBLE);
                int baseColor = Color.BLACK;
                // 60% opacity
                float baseAlpha = ResourcesCompat.getFloat(getResources(), R.dimen.material_emphasis_medium);
                float offset = (slideOffset - (-1f)) / (1f - (-1f)) * (1f - 0f) + 0f;
                int alpha = Math.round(MathUtils.lerp(0f, 255f, offset * baseAlpha));
                int color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
                scrim.setBackgroundColor(color);
            }
        });

        // Set first fragment to navigate to and pass data
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_Acc, ACC);
        bundle.putString(KEY_FolderName, folderName);
        navController.setGraph(R.navigation.nav_graph, bundle);

        navigationView.setNavigationItemSelectedListener(item -> {
            navigationView.getMenu().getItem(item.getItemId()).setChecked(true);
            String folderName1 = item.getTitle().toString();

            Log.d(getString(R.string.app_name), TAG + "> selected " + folderName1);

            Bundle bundle1 = new Bundle();
            bundle1.putParcelable(KEY_Acc, ACC);
            bundle1.putString(KEY_FolderName, folderName1);
            navController.popBackStack(R.id.messagesFragment, true);
            navController.navigate(R.id.messagesFragment, bundle1);

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            scrim.setVisibility(View.GONE);
            return true;
        });
        navigationView.bringToFront();

        populateBottomDrawer();
    }

    // Populate bottom drawer
    void populateBottomDrawer() {
        if(ACC != null) {
            // Set bottom drawer header with mailbox name and user's email
            TextView navTitle = (TextView) navigationView.getHeaderView(0).findViewById(R.id.navTitle);
            TextView navMail = (TextView) navigationView.getHeaderView(0).findViewById(R.id.navMail);
            navTitle.setText(ACC.getShortDisplayName());
            navMail.setText(ACC.getMail());

            getFolderNames(ACC).subscribe(this::setFolderNames);
        }
    }

    // Asynchronously get mailbox's folder names
    public static Single<ArrayList<String>> getFolderNames(LoggedInUser ACC) {
        return Single.fromCallable(() -> {
            ArrayList<String> names = new ArrayList<>();
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
                    names.add(folder.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return names;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    // Set folder names at bottom drawer
    void setFolderNames(ArrayList<String> folderNames){
        final Menu menu = navigationView.getMenu();
        if(folderNames != null) {
            for (int i = 0; i < folderNames.size(); i++) {
                menu.add(folderNames.get(i));
            }
        }else{
            menu.add(getString(R.string.defaultFolder));
        }

        //Set menus as checkable
        for(int i = 0; i < menu.size(); i++){
            navigationView.getMenu().getItem(i).setCheckable(true);
        }
    }

}