package ap.mailo.main;

import android.accounts.Account;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ap.mailo.R;
import ap.mailo.auth.LoggedInUser;
import ap.mailo.store.InboxViewModel;
import ap.mailo.store.InboxViewModelFactory;
import ap.mailo.store.MessageHeader;
import ap.mailo.util.InternetChecker;

import static jakarta.mail.internet.MimeUtility.decodeText;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MessagesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessagesFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();

    private LoggedInUser ACC;
    private String folderName;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private NavController navController;
    private InboxViewModel inboxVM;
    private MessageAdapter adapter;

    public MessagesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param ACC - User details.
     * @param folderName - String, name of the folder to list.
     * @return A new instance of fragment MessagesFragment.
     */
    public static MessagesFragment newInstance(LoggedInUser ACC, String folderName) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putParcelable(MainActivity.KEY_Acc, ACC);
        args.putString(MainActivity.KEY_FolderName, folderName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(getString(R.string.app_name), TAG + "> Message List Create");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ACC = getArguments().getParcelable(MainActivity.KEY_Acc);
            folderName = getArguments().getString(MainActivity.KEY_FolderName);
        }

        // Create recycler view adapter
        adapter = new MessageAdapter(null);

        //Get NavigationController vars
        NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        inboxVM = new ViewModelProvider(this, new InboxViewModelFactory(getActivity().getApplication(), folderName, ACC)).get(InboxViewModel.class);
        inboxVM.getAll().observe(this, messages -> {
            adapter.setMessages(messages);
            swipeRefreshLayout.setRefreshing(false);
        });
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        //Setup swipe refresh
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.messages_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> inboxVM.refreshMessages());

        //Setup recyclerView
        recyclerView = view.findViewById(R.id.messages_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        TextView titleView = view.findViewById(R.id.FolderTitle);
        titleView.setText(folderName);

        return swipeRefreshLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inboxVM.refreshMessages();
        swipeRefreshLayout.setRefreshing(true);
    }

    private class MessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView fromTextView;
        private final TextView subjectTextView;
        private long messnr;

        public MessageHolder(LayoutInflater layoutInflater, ViewGroup parent) {
            super(layoutInflater.inflate(R.layout.messages_item, parent, false));
            itemView.setOnClickListener(this);

            fromTextView = itemView.findViewById(R.id.list_item_from);
            subjectTextView = itemView.findViewById(R.id.list_item_Subject);
        }

        public void bind(MessageHeader message) {
            try{
                fromTextView.setText(decodeText(message.getFrom()));
            }catch (UnsupportedEncodingException e) {
                fromTextView.setText(message.getFrom());
            }
            subjectTextView.setText(message.getSubject());
            messnr = message.getUID();
        }

        @Override
        public void onClick(View view) {
            Log.d(getString(R.string.app_name), TAG + "> Clicked message" + messnr);
            InternetChecker.hasInternetConnection().subscribe(has -> {
                if (has) {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(MainActivity.KEY_Acc, ACC);
                    bundle.putString(MainActivity.KEY_FolderName, folderName);
                    //bundle.putLong(ReadMessage.ARG_MESS_NR, messnr);
                    navController.navigate(R.id.readMessage, bundle);
                }
            });
        }
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageHolder> {
        private List<MessageHeader> messages;

        public MessageAdapter(List<MessageHeader> messages) {
            setMessages(messages);
        }

        @NotNull
        @Override
        public MessageHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new MessageHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
            if(messages != null && !messages.isEmpty()){
                holder.bind(messages.get(position));
            }
        }

        @Override
        public int getItemCount() {
            if(messages != null) {
                return messages.size();
            } else {
                return 0;
            }
        }

        public void setMessages(List<MessageHeader> messages) {
            if(messages != null && !messages.isEmpty()) {
                Collections.reverse(messages);
                this.messages = messages;
                notifyDataSetChanged();
            }
        }
    }

}