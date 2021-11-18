package ap.mailo.main;

import static android.Manifest.permission.RECORD_AUDIO;

import static ap.mailo.util.StyleService.setPreferedStyle;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.math.MathUtils;
import com.google.android.material.navigation.NavigationView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Locale;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import ap.mailo.util.BottomDrawerPopulator;
import ap.mailo.util.SpeechRecognitionHandler;
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
    private String folderName;

    // Navigation variables
    private NavigationView navigationView;
    private NavController navController;
    private BottomAppBar bottomAppBar;
    private FrameLayout scrim;
    private BottomSheetBehavior<NavigationView> bottomSheetBehavior;

    // Other views
    private FloatingActionButton fab;

    // Misc
    private SpeechRecognitionHandler recognitionHandler;
    private static ArrayList<String> folderNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPreferedStyle(this);
        setContentView(R.layout.activity_main);

        // Get attributes passed from EntryActivity
        folderName = getIntent().getStringExtra(KEY_FolderName);
        ACC = getIntent().getParcelableExtra(LoggedInUser.ACCOUNT_INFO);
        String mailto = getIntent().getStringExtra(WriteMessage.MAILTO_STRING);

        // Bottom App Bar, bottom drawer and navigationController
        bottomAppBar = findViewById(R.id.bottomAppBar);
        scrim = findViewById(R.id.scrim);
        navigationView = findViewById(R.id.navigationView);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Setup Bottom App Bar and bottom drawer behaviour
        setSupportActionBar(bottomAppBar);
        bottomSheetBehavior = BottomSheetBehavior.from(navigationView);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        scrim.setVisibility(View.GONE);

        // Bottom App Bar hamburger menu listener
        bottomAppBar.setNavigationOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));
        bottomAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.info) {
                navController.navigate(R.id.action_global_info);
                return true;
            }
            return false;
        });

        // Scrim listener
        scrim.setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            scrim.setVisibility(View.GONE);
        });

        // On slide of bottom drawer scrim should get darker colour starting from 60% opacity black
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetOpacityChanger);

        // Set first fragment to navigate to and pass data
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_Acc, ACC);
        bundle.putString(KEY_FolderName, folderName);
        bundle.putString(WriteMessage.MAILTO_STRING, mailto);
        navController.setGraph(R.navigation.nav_graph, bundle);

        navigationView.setNavigationItemSelectedListener(this::moveToFolder);
        navigationView.bringToFront();

        populateBottomDrawer();

        fab = findViewById(R.id.fab);
        if(fab != null) {
            recognitionHandler = new SpeechRecognitionHandler(
                                        this,
                                        navController,
                                        getSupportFragmentManager(),
                                        bottomSheetBehavior,
                                        ACC,
                                        fab
                                    );

            recognitionHandler.setCurrentFolder(folderName);
            recognitionHandler.setAvailableFolders(folderNames);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bottom_app_bar, menu);
        return true;
    }

    // Populate bottom drawer
    private void populateBottomDrawer() {
        if(ACC != null) {
            // Set bottom drawer header with mailbox name and user's email
            TextView navTitle = navigationView.getHeaderView(0).findViewById(R.id.navTitle);
            TextView navMail = navigationView.getHeaderView(0).findViewById(R.id.navMail);
            navTitle.setText(ACC.getShortDisplayName());
            navMail.setText(ACC.getMail());

            BottomDrawerPopulator.getFolderNames(ACC).subscribe((folders) -> {
                setFolderNames(folders);
                recognitionHandler.setAvailableFolders(folders);
            });
        }
    }

    // Set folder names at bottom drawer
    private void setFolderNames(ArrayList<String> folderNames){
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
            var menuItem = navigationView.getMenu().getItem(i);
            menuItem.setCheckable(true);

            if(getString(R.string.defaultFolder).equalsIgnoreCase(menuItem.getTitle().toString())){
                menuItem.setChecked(true);
            }
        }
    }

    private void ifFirstRunThenShowTutorial() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true);

        if(isFirstRun) {
            if(showFeatureOnboarding()) {
                getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                        .putBoolean("isFirstRun", false).commit();
            }
        }
    }

    private boolean showFeatureOnboarding() {
        View fab = findViewById(R.id.fab);
        Toolbar bottomAppDrawer = findViewById(R.id.bottomAppBar);
        // Get colorOnPrimary from day/night material theme
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorOnPrimary, value, true);
        if(fab != null && bottomAppDrawer != null) {
            int helpId = bottomAppDrawer.getMenu().findItem(R.id.info).getItemId();
            new TapTargetSequence(this)
                    .targets(
                            TapTarget.forView(fab, getString(R.string.onboardingMainButton_title), getString(R.string.onboardingMainButton_description))
                                    .textColorInt(value.data),
                            TapTarget.forToolbarNavigationIcon(bottomAppDrawer, getString(R.string.onboardingMenuButton_title), getString(R.string.onboardingMenuButton_description))
                                    .textColorInt(value.data),
                            TapTarget.forToolbarMenuItem(bottomAppDrawer, helpId, getString(R.string.onboardingHelpButton_title), getString(R.string.onboardingHelpButton_description))
                                    .textColorInt(value.data)
                    ).start();
            return true;
        }
        return false;
    }

    // Move to a folder
    private boolean moveToFolder(MenuItem item)
    {
        navigationView.getMenu().getItem(item.getItemId()).setChecked(true);
        folderName = item.getTitle().toString();

        Log.d(getString(R.string.app_name), TAG + "> selected " + folderName);

        recognitionHandler.setCurrentFolder(folderName);

        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_Acc, ACC);
        bundle.putString(KEY_FolderName, folderName);
        navController.navigate(R.id.messagesFragment, bundle);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        scrim.setVisibility(View.GONE);
        return true;
    }

    private final BottomSheetCallback bottomSheetOpacityChanger = new BottomSheetCallback() {
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
            float offset = (slideOffset - (-1f)) / (1f - (-1f));
            int alpha = Math.round(MathUtils.lerp(0f, 255f, offset * baseAlpha));
            int color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
            scrim.setBackgroundColor(color);
        }
    };
}