package ap.mailo.main;

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
    public static WriteMessage newInstance(LoggedInUser ACC) {
        WriteMessage fragment = new WriteMessage();
        Bundle args = new Bundle();
        args.putParcelable(MainActivity.KEY_Acc, ACC);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ACC = getArguments().getParcelable(MainActivity.KEY_Acc);
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

        backBtn = (ImageView) view.findViewById(R.id.back);
        writeRecipient = view.findViewById(R.id.writeTo);
        writeSubject = view.findViewById(R.id.writeSubject);
        writeContent = view.findViewById(R.id.writeContent);

        backBtn.setOnClickListener(v -> navController.popBackStack());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentActivity activity = getActivity();

        if(activity != null) {
            fab = activity.findViewById(R.id.fab);
            fab.setImageResource(R.drawable.ic_baseline_send_24);
            fab.setEnabled(false);
            fab.setOnClickListener(v -> {
                if (recipients == null) {
                    Toast.makeText(activity.getApplicationContext(), getString(R.string.empty_recipient), Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage();
                }
            });
        }


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
                if(line.contains(", ")) {
                    line = line.replace(", ", ",");
                }else if(line.contains(" ")){
                    line = line.replace(" ", ",");
                }

                try {
                    recipients = InternetAddress.parse(line);
                    fab.setEnabled(true);
                } catch (AddressException e) {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.comma_separation), Toast.LENGTH_SHORT).show();
                    fab.setEnabled(false);
                    e.printStackTrace();
                }
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

        MessageNetwork.sendMessage(this.ACC, this.recipients, this.subject, this.content).subscribe(send -> {
            if (send) {
                Toast.makeText(getContext(), getString(R.string.succes_sending), Toast.LENGTH_SHORT).show();
                navController.popBackStack();
            } else {
                Toast.makeText(getContext(), getString(R.string.failure_sending), Toast.LENGTH_SHORT).show();
            }
        });
    }
}