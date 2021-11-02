package ap.mailo.main;

import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import ap.mailo.store.InboxViewModel;
import ap.mailo.store.InboxViewModelFactory;
import ap.mailo.store.MessageNetwork;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReadMessage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadMessage extends Fragment {

    public static final String ARG_MESS_NR = "MESS_NR";

    private LoggedInUser ACC;
    private String folderName;
    private long messnr;

    private ImageView backBtn;
    private TextView subjectView;
    private TextView fromView;
    private TextView toView;
    private WebView contentView;

    private String mailtoReply;

    private NavController navController;

    public ReadMessage() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param messnr Message number to read from folder.
     * @param folderName Name of a folder to read from.
     * @param ACC User details.
     * @return A new instance of fragment ReadMessage.
     */
    public static ReadMessage newInstance(long messnr, String folderName, LoggedInUser ACC) {
        ReadMessage fragment = new ReadMessage();
        Bundle args = new Bundle();
        args.putLong(ARG_MESS_NR, messnr);
        args.putString(MainActivity.KEY_FolderName, folderName);
        args.putParcelable(MainActivity.KEY_Acc, ACC);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            ACC = getArguments().getParcelable(MainActivity.KEY_Acc);
            folderName = getArguments().getString(MainActivity.KEY_FolderName);
            messnr = getArguments().getLong(ARG_MESS_NR);
        }

        //Get NavigationController vars
        NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        loadMessage();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        MenuItem delete = menu.add("delete");
        delete.setIcon(R.drawable.delete);
        delete.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        delete.setOnMenuItemClickListener(menuItem -> {
            delete();
            return true;
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_read_message, container, false);

        subjectView = (TextView) view.findViewById(R.id.readSubject);
        fromView = (TextView) view.findViewById(R.id.readFrom);
        toView = (TextView) view.findViewById(R.id.readTo);
        contentView = (WebView) view.findViewById(R.id.readBody);

        contentView.getSettings().setBuiltInZoomControls(true);
        contentView.getSettings().setSupportZoom(true);
        contentView.getSettings().setDisplayZoomControls(false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentActivity activity = getActivity();

        if(activity != null) {
            FloatingActionButton fab = activity.findViewById(R.id.fab);
            fab.setImageResource(R.drawable.ic_baseline_reply_24);
            fab.setEnabled(true);
            fab.setOnClickListener(v -> { reply(); });
        }

        contentView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    }

    public void delete()
    {
        var inboxVM = new ViewModelProvider(this, new InboxViewModelFactory(getActivity().getApplication(), folderName, ACC)).get(InboxViewModel.class);
        inboxVM.deleteFromFolderByUIDs(ACC, new long[] {messnr}, folderName);
        navController.popBackStack();
    }

    public void reply()
    {
        Bundle bundle = new Bundle();
        bundle.putParcelable(MainActivity.KEY_Acc, ACC);
        bundle.putString(MainActivity.KEY_FolderName, folderName);
        bundle.putString(WriteMessage.MAILTO_STRING, mailtoReply);
        navController.navigate(R.id.writeMessage, bundle);
    }

    private void loadMessage(){
        MessageNetwork.loadMessage(ACC, messnr, folderName).subscribe(body -> {
            if (body.length > 0) {
                setContent(body);
            }
        });
    }

    private void setContent(String[] body){
        String subject = body[0];
        String from = body[1];
        String to = body[2];
        String content = "<div id='content'>" + body[3] + "</div>";

        subjectView.setText(subject);
        fromView.setText(from);
        toView.setText(to);
        contentView.loadDataWithBaseURL(null, content, "text/html", null, null);

        var startIndex = from.indexOf("<");
        var endIndex = from.indexOf(">");

        String replyFrom = from;
        if(!(startIndex == -1 || endIndex == -1)) {
            replyFrom = from.substring(startIndex + 1, endIndex);
        }

        String replyBody = "\r\n\r\n\r\n" + content
                            .replaceAll("\\<.*?\\>", "")
                            .replaceAll("(?m)^", "\t\t> ");

        mailtoReply = "mailto:" + replyFrom + "?subject=" + subject + "&body=" + replyBody;
    }
}