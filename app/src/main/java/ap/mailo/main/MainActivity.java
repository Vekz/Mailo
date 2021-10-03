package ap.mailo.main;

import static android.Manifest.permission.RECORD_AUDIO;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

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

    // Other views
    private FloatingActionButton fab;

    // Misc
    private boolean isLongClickPressed = false;
    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;
    private boolean isSpeechActivated = false;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get attributes passed from EntryActivity
        String folderName = getIntent().getStringExtra(KEY_FolderName);
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
        bundle.putString(WriteMessage.MAILTO_STRING, mailto);
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

        // Speech recognition setup
        textToSpeech = new TextToSpeech(this, initTTSListener);

        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(speechListener);

        fab = findViewById(R.id.fab);
        if(fab != null) {
            ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);
            fab.setOnLongClickListener(speechPressed);
            fab.setOnTouchListener(speechReleased);
        }
    }

    // Populate bottom drawer
    private void populateBottomDrawer() {
        if(ACC != null) {
            // Set bottom drawer header with mailbox name and user's email
            TextView navTitle = navigationView.getHeaderView(0).findViewById(R.id.navTitle);
            TextView navMail = navigationView.getHeaderView(0).findViewById(R.id.navMail);
            navTitle.setText(ACC.getShortDisplayName());
            navMail.setText(ACC.getMail());

            getFolderNames(ACC).subscribe(this::setFolderNames);
        }
    }

    // Asynchronously get mailbox's folder names
    private static Single<ArrayList<String>> getFolderNames(LoggedInUser ACC) {
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

    // FAB - Speech recognition listeners
    private final TextToSpeech.OnInitListener initTTSListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int i) {
            isSpeechActivated = true;
        }
    };

    private final View.OnLongClickListener speechPressed = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            isLongClickPressed = true;
            // Start listening
            speechRecognizer.startListening(intentRecognizer);
            return true;
        }
    };

    private final View.OnTouchListener speechReleased = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            view.onTouchEvent(motionEvent);
            // We're only interested in when the button is released.
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // We're only interested in anything if our speech button is currently pressed.
                if (isLongClickPressed) {
                    isLongClickPressed = false;
                    // Stop listening
                    speechRecognizer.stopListening();
                }
            }
            return true;
        }
    };

    private final RecognitionListener speechListener = new RecognitionListener() {
        @Override
        public void onResults(Bundle bundle) {
            String result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).toString();
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(int i) {
            // Text-To-Speech "Sorry I did not understand you"
            if(isSpeechActivated){
                textToSpeech.speak(getString(R.string.speechError), TextToSpeech.QUEUE_FLUSH, null, "");
            }else {
                Toast.makeText(getApplicationContext(), getString(R.string.speechError), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onReadyForSpeech(Bundle bundle) { }

        @Override
        public void onBeginningOfSpeech() { }

        @Override
        public void onRmsChanged(float v) { }

        @Override
        public void onBufferReceived(byte[] bytes) { }

        @Override
        public void onEndOfSpeech() { }

        @Override
        public void onPartialResults(Bundle bundle) { }

        @Override
        public void onEvent(int i, Bundle bundle) { }
    };
}