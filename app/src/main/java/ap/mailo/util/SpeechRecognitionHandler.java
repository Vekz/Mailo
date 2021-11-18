package ap.mailo.util;

import static android.Manifest.permission.RECORD_AUDIO;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.List;
import java.util.Locale;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import ap.mailo.main.MainActivity;
import ap.mailo.main.MessagesFragment;
import ap.mailo.main.ReadMessage;
import ap.mailo.main.WriteMessage;

public class SpeechRecognitionHandler {
    private final String TAG = getClass().getSimpleName();

    private final Context context;
    private final NavController navController;
    private final LoggedInUser ACC;
    private final FragmentManager fragmentManager;
    private final BottomSheetBehavior<NavigationView> bottomSheetBehavior;
    private final SpeechRecognizer speechRecognizer;
    private final Intent intentRecognizer;
    private final TextToSpeech textToSpeech;

    private List<String> folderNames;
    private String folderName;
    private boolean isLongClickPressed = false;
    private boolean isSpeechActivated = false;


    public SpeechRecognitionHandler(Activity activity,
                                    NavController controller,
                                    FragmentManager manager,
                                    BottomSheetBehavior<NavigationView> behavior,
                                    LoggedInUser user,
                                    FloatingActionButton input)
    {
        context = activity.getApplicationContext();
        navController = controller;
        fragmentManager = manager;
        bottomSheetBehavior = behavior;
        ACC = user;

        // Speech recognition setup
        textToSpeech = new TextToSpeech(context, initTTSListener);

        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(speechListener);

        ActivityCompat.requestPermissions(activity, new String[]{RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);
        input.setOnLongClickListener(speechPressed);
        input.setOnTouchListener(speechReleased);
    }

    public void setCurrentFolder(String name){ folderName = name; };
    public void setAvailableFolders(List<String> folders){ folderNames = folders; };

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
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
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
            textToSpeech.speak(context.getString(R.string.speechError), TextToSpeech.QUEUE_FLUSH, null, "");
        }else {
            Toast.makeText(context, context.getString(R.string.speechError), Toast.LENGTH_SHORT).show();
        }
    }

    // Voice commands
    private void handleRecognitionResults(String result) {
        if(result.startsWith(context.getString(R.string.COMM_OpenNo))) {
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
        if(result.startsWith(context.getString(R.string.COMM_Open))) {
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
                    bundle.putParcelable(MainActivity.KEY_Acc, ACC);
                    bundle.putString(MainActivity.KEY_FolderName, name);
                    navController.navigate(R.id.messagesFragment, bundle);
                    return;
                }
            }
        }
        if(result.startsWith(context.getString(R.string.COMM_ReplyNo))) {
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
        if(result.equalsIgnoreCase(context.getString(R.string.COMM_Reply)))
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
        if(result.startsWith(context.getString(R.string.COMM_DeleteNo))) {
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
        if(result.equalsIgnoreCase(context.getString(R.string.COMM_Delete)))
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
        if(result.equalsIgnoreCase(context.getString(R.string.COMM_Send)))
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
        if(result.equalsIgnoreCase(context.getString(R.string.COMM_NewMessage))) {
            // Start composing new message
            Bundle bundle = new Bundle();
            bundle.putParcelable(MainActivity.KEY_Acc, ACC);
            bundle.putString(MainActivity.KEY_FolderName, folderName);
            navController.navigate(R.id.writeMessage, bundle);
            return;
        }
        if(result.equalsIgnoreCase(context.getString(R.string.COMM_GoBack))) {
            // Move to previous fragment on stack
            var fragment = getForegroundFragment();
            if(!(folderName.equalsIgnoreCase(context.getString(R.string.defaultFolder)) && fragment.getClass().getName().equals(MessagesFragment.class.getName())) )
                navController.popBackStack();
            return;
        }
        if(result.equalsIgnoreCase(context.getString(R.string.COMM_Menu))) {
            // Move to previous fragment on stack
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            return;
        }
        if(result.equalsIgnoreCase(context.getString(R.string.COMM_Help))) {
            // Move to previous fragment on stack
            Bundle bundle = new Bundle();
            navController.navigate(R.id.infoFragment, bundle);
            return;
        }

        handleRecognitionError();
    }

    private int stringToInt(String toParse) {

        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral1)) || toParse.equals("1"))
            return 1;
        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral2)) || toParse.equals("2"))
            return 2;
        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral3)) || toParse.equals("3"))
            return 3;
        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral4)) || toParse.equals("4"))
            return 4;
        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral5)) || toParse.equals("5"))
            return 5;
        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral6)) || toParse.equals("6"))
            return 6;
        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral7)) || toParse.equals("7"))
            return 7;
        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral8)) || toParse.equals("8"))
            return 8;
        if(toParse.equalsIgnoreCase(context.getString(R.string.Numeral9)) || toParse.equals("9"))
            return 9;

        handleRecognitionError();
        return -1;
    }

    private Fragment getForegroundFragment(){
        Fragment navHostFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        return navHostFragment == null ? null : navHostFragment.getChildFragmentManager().getFragments().get(0);
    }
}
