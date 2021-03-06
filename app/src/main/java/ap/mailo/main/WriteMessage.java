package ap.mailo.main;

import android.net.MailTo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import ap.mailo.store.MessageNetwork;
import jakarta.mail.Address;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteMessage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteMessage extends Fragment {

    public static final String ARG_MESS_NR = "MESS_NR";
    public static final String MAILTO_STRING = "mailto";

    private String mailto;
    private String folderName;
    private LoggedInUser ACC;

    private EditText writeRecipient;
    private EditText writeSubject;
    private EditText writeContent;
    private FloatingActionButton sendBtn;

    private Address[] recipients;
    private String subject;
    private String content;
    private ImageView backBtn;

    private NavController navController;
    private FloatingActionButton fab;

    public WriteMessage() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param ACC User details.
     * @return A new instance of fragment WriteMessage.
     */
    public static WriteMessage newInstance(LoggedInUser ACC, String mailto, String folderName) {
        WriteMessage fragment = new WriteMessage();
        Bundle args = new Bundle();
        args.putParcelable(MainActivity.KEY_Acc, ACC);
        args.putString(MainActivity.KEY_FolderName, folderName);
        args.putString(WriteMessage.MAILTO_STRING, mailto);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ACC = getArguments().getParcelable(MainActivity.KEY_Acc);
            folderName = getArguments().getString(MainActivity.KEY_FolderName);
            mailto = getArguments().getString(WriteMessage.MAILTO_STRING);
        }

        //Get NavigationController vars
        NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        recipients = null;
        subject = "";
        content = "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_write_message, container, false);

        writeRecipient = view.findViewById(R.id.writeTo);
        writeSubject = view.findViewById(R.id.writeSubject);
        writeContent = view.findViewById(R.id.writeContent);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentActivity activity = getActivity();

        if(activity != null) {
            fab = activity.findViewById(R.id.fab);
            fab.setImageResource(R.drawable.ic_baseline_send_24);
            fab.setOnClickListener(v -> {
                    sendMessage();
            });
        }

        if(mailto != null)
            loadMessage();

        writeRecipient.addTextChangedListener(new TextWatcher() {
            private String line;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                line = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkAddressee(line);
            }
        });
        writeSubject.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                subject = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        writeContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                content = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

    public void sendMessage() {
        if (recipients == null) {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.empty_recipient), Toast.LENGTH_SHORT).show();
        } else {
            MessageNetwork.sendMessage(this.ACC, this.recipients, this.subject, this.content).subscribe(send -> {
                if (send) {
                    Toast.makeText(getContext(), getString(R.string.succes_sending), Toast.LENGTH_SHORT).show();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(MainActivity.KEY_Acc, ACC);
                    bundle.putString(MainActivity.KEY_FolderName, folderName);
                    navController.navigate(R.id.messagesFragment, bundle);
                } else {
                    Toast.makeText(getContext(), getString(R.string.failure_sending), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadMessage(){
        MailTo parsedMailto = MailTo.parse(mailto);
        String subject = parsedMailto.getSubject() != null ? "Re: "+  parsedMailto.getSubject() : "";
        String to = parsedMailto.getTo();
        String content = parsedMailto.getBody();

        writeSubject.setText(subject);
        writeRecipient.setText(to);
        writeContent.setText(content);

        checkAddressee(to);
    }

    private void checkAddressee(String line) {
        if(line != null) {
            if (line.contains(", ")) {
                line = line.replace(", ", ",");
            } else if (line.contains(" ")) {
                line = line.replace(" ", ",");
            }

            try {
                recipients = InternetAddress.parse(line);
            } catch (AddressException e) {
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.comma_separation), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
}