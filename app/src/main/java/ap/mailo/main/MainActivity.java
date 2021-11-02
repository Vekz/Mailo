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
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.math.MathUtils;
import com.google.android.material.navigation.NavigationView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Locale;

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
    private boolean isLongClickPressed = false;
    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;
    private boolean isSpeechActivated = false;
    private TextToSpeech textToSpeech;

    private static ArrayList<String> folderNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        navigationView.setNavigationItemSelectedListener(this::moveToFolder);
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

            getFolderNames(ACC).subscribe(this::setFolderNames);
        }
    }

    // Asynchronously get mailbox's folder names
    private static Single<ArrayList<String>> getFolderNames(LoggedInUser ACC) {
        return Single.fromCallable(() -> {
            folderNames = new ArrayList<>();
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

    // Move to a folder
    public boolean moveToFolder(MenuItem item)
    {
        navigationView.getMenu().getItem(item.getItemId()).setChecked(true);
        folderName = item.getTitle().toString();

        Log.d(getString(R.string.app_name), TAG + "> selected " + folderName);

        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_Acc, ACC);
        bundle.putString(KEY_FolderName, folderName);
        navController.navigate(R.id.messagesFragment, bundle);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        scrim.setVisibility(View.GONE);
        return true;
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
            result = result.substring(1, result.length()-1);
            result = result.toLowerCase(Locale.ROOT);
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            handleRecognitionResults(result);
        }

        @Override
        public void onError(int i) {
            handleRecognitionError();
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

    private void handleRecognitionError() {
        // Text-To-Speech "Sorry I did not understand you"
        if(isSpeechActivated){
            textToSpeech.speak(getString(R.string.speechError), TextToSpeech.QUEUE_FLUSH, null, "");
        }else {
            Toast.makeText(getApplicationContext(), getString(R.string.speechError), Toast.LENGTH_SHORT).show();
        }
    }

    // Voice commands
    private void handleRecognitionResults(String result) {
        if(result.startsWith(getString(R.string.COMM_OpenNo))) {
            String[] resultSplit = result.split(" ");
            if (resultSplit.length == 3) {
                String messNumber = resultSplit[2];
                int index = stringToInt(messNumber);
                // Open message with that index
                if (index != -1) {
                    var fragment = getForegroundFragment();
                    if(fragment.getClass().getName().equals(MessagesFragment.class.getName())) {
                        var messageListFragment = (MessagesFragment)fragment;
                        messageListFragment.openMessageAt(index);
                    }
                }
                return;
            }
        }
        if(result.startsWith(getString(R.string.COMM_Open))) {
            String[] resultSplit = result.split(" ");
            if (resultSplit.length >= 2) {
                String folderName = "";
                for(int i=1; i< resultSplit.length; i++) {
                    String space = " ";
                    if(i == resultSplit.length - 1){
                        space = "";
                    }
                    folderName = String.format("%s%s%s", folderName, resultSplit[i], space);
                }

                if(folderNames.stream().anyMatch(folderName::equalsIgnoreCase)) {
                    // Open appropriate folder
                    var name = folderNames.stream().filter(folderName::equalsIgnoreCase).toArray()[0].toString();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(KEY_Acc, ACC);
                    bundle.putString(KEY_FolderName, name);
                    navController.navigate(R.id.messagesFragment, bundle);
                    return;
                }
            }
        }
        if(result.startsWith(getString(R.string.COMM_ReplyNo))) {
            String[] resultSplit = result.split(" ");
            if (resultSplit.length == 4) {
                String messNumber = resultSplit[3];
                int index = stringToInt(messNumber);
                // Reply to message with that index
                if (index != -1) {
                    var fragment = getForegroundFragment();
                    if(fragment.getClass().getName().equals(MessagesFragment.class.getName())) {
                        Log.d(TAG, "Not yet implemented ;C");
                    }
                }
                return;
            }
        }
        if(result.equalsIgnoreCase(getString(R.string.COMM_Reply)))
        {
            // Reply to this message
            var fragment = getForegroundFragment();
            if(fragment.getClass().getName().equals(ReadMessage.class.getName()))
            {
                var readFragment = (ReadMessage)fragment;
                readFragment.reply();
                return;
            }
        }
        if(result.startsWith(getString(R.string.COMM_DeleteNo))) {
            String[] resultSplit = result.split(" ");
            if (resultSplit.length == 3) {
                String messNumber = resultSplit[2];
                int index = stringToInt(messNumber);
                // Delete message with that index
                if (index != -1) {
                    var fragment = getForegroundFragment();
                    if(fragment.getClass().getName().equals(MessagesFragment.class.getName())) {
                        var messageListFragment = (MessagesFragment)fragment;
                        messageListFragment.deleteMessageAt(index);
                    }
                }
                return;
            }
        }
        if(result.equalsIgnoreCase(getString(R.string.COMM_Delete)))
        {
            // Reply to this message
            var fragment = getForegroundFragment();
            if(fragment.getClass().getName().equals(ReadMessage.class.getName()))
            {
                var readFragment = (ReadMessage)fragment;
                readFragment.delete();
                return;
            }
        }
        if(result.equalsIgnoreCase(getString(R.string.COMM_Send)))
        {
            // Send this message
            var fragment = getForegroundFragment();
            if(fragment.getClass().getName().equals(WriteMessage.class.getName()))
            {
                var writeFragment = (WriteMessage)fragment;
                writeFragment.sendMessage();
                return;
            }
        }
        if(result.equalsIgnoreCase(getString(R.string.COMM_NewMessage))) {
            // Start composing new message
            Bundle bundle = new Bundle();
            bundle.putParcelable(MainActivity.KEY_Acc, ACC);
            bundle.putString(MainActivity.KEY_FolderName, folderName);
            navController.navigate(R.id.writeMessage, bundle);
            return;
        }
        if(result.equalsIgnoreCase(getString(R.string.COMM_GoBack))) {
            // Move to previous fragment on stack
            var fragment = getForegroundFragment();
            if(!(folderName.equalsIgnoreCase(getString(R.string.defaultFolder)) && fragment.getClass().getName().equals(MessagesFragment.class.getName())) )
                navController.popBackStack();
            return;
        }
        if(result.equalsIgnoreCase(getString(R.string.COMM_Menu))) {
            // Move to previous fragment on stack
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            return;
        }

        handleRecognitionError();
    }

    private int stringToInt(String toParse) {

            if(toParse.equalsIgnoreCase(getString(R.string.Numeral1)) || toParse.equals("1"))
                return 1;
            if(toParse.equalsIgnoreCase(getString(R.string.Numeral2)) || toParse.equals("2"))
                return 2;
            if(toParse.equalsIgnoreCase(getString(R.string.Numeral3)) || toParse.equals("3"))
                return 3;
            if(toParse.equalsIgnoreCase(getString(R.string.Numeral4)) || toParse.equals("4"))
                return 4;
            if(toParse.equalsIgnoreCase(getString(R.string.Numeral5)) || toParse.equals("5"))
                return 5;
            if(toParse.equalsIgnoreCase(getString(R.string.Numeral6)) || toParse.equals("6"))
                return 6;
            if(toParse.equalsIgnoreCase(getString(R.string.Numeral7)) || toParse.equals("7"))
                return 7;
            if(toParse.equalsIgnoreCase(getString(R.string.Numeral8)) || toParse.equals("8"))
                return 8;
            if(toParse.equalsIgnoreCase(getString(R.string.Numeral9)) || toParse.equals("9"))
                return 9;

            handleRecognitionError();
            return -1;
    }

    private Fragment getForegroundFragment(){
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return navHostFragment == null ? null : navHostFragment.getChildFragmentManager().getFragments().get(0);
    }
}