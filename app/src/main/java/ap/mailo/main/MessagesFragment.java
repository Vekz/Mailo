package ap.mailo.main;

import android.accounts.Account;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

    private ArrayList<MessageHeader> messagesList = new ArrayList<>();
    private String mailto;

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
    public static MessagesFragment newInstance(LoggedInUser ACC, String folderName, String mailto) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putParcelable(MainActivity.KEY_Acc, ACC);
        args.putString(MainActivity.KEY_FolderName, folderName);
        args.putString(WriteMessage.MAILTO_STRING, mailto);
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
            mailto = getArguments().getString(WriteMessage.MAILTO_STRING);
        }
        //Get NavigationController vars
        NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        inboxVM = new ViewModelProvider(this, new InboxViewModelFactory(getActivity().getApplication(), folderName, ACC)).get(InboxViewModel.class);
        inboxVM.getAll().observe(this, new Observer<List<MessageHeader>>() {
            @Override
            public void onChanged(List<MessageHeader> messageHeaders) {
                if(messagesList.size() > 0){
                    messagesList.clear();
                }

                if(messageHeaders != null){
                    messagesList.addAll(messageHeaders);
                    adapter.setMessages(messagesList);
                }

                swipeRefreshLayout.setRefreshing(false);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        //Setup swipe refresh
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.messages_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> inboxVM.refreshMessages());

        // Create recycler view adapter
        adapter = new MessageAdapter(messagesList);

        //Setup recyclerView
        recyclerView = view.findViewById(R.id.messages_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        new ItemTouchHelper(itemDeleteCallback).attachToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(visibleItemsChanged);
        recyclerView.setAdapter(adapter);

        TextView titleView = view.findViewById(R.id.FolderTitle);
        titleView.setText(folderName);

        //Setup activity
        FragmentActivity activity = getActivity();

        if(activity != null) {
            FloatingActionButton fab = activity.findViewById(R.id.fab);
            if(fab != null) {
                fab.setImageResource(R.drawable.ic_create_24dp);
                fab.setEnabled(true);
                fab.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(MainActivity.KEY_Acc, ACC);
                    bundle.putString(MainActivity.KEY_FolderName, folderName);
                    navController.navigate(R.id.writeMessage, bundle);
                });
            }
        }

        return swipeRefreshLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inboxVM.refreshMessages();
        swipeRefreshLayout.setRefreshing(true);

        ifMailtoThenRedirectToWriteFragment();
    }

    private class MessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView fromTextView;
        private final TextView subjectTextView;
        private final TextView numberLabelView;
        private long messnr;

        public MessageHolder(LayoutInflater layoutInflater, ViewGroup parent) {
            super(layoutInflater.inflate(R.layout.messages_item, parent, false));
            itemView.setOnClickListener(this);

            fromTextView = itemView.findViewById(R.id.list_item_from);
            subjectTextView = itemView.findViewById(R.id.list_item_Subject);
            numberLabelView = itemView.findViewById(R.id.list_item_Label);
        }

        public void bind(MessageHeader message) {
            try{
                fromTextView.setText(decodeText(message.getFrom()));
            }catch (UnsupportedEncodingException e) {
                fromTextView.setText(message.getFrom());
            }
            String subject = message.getSubject();
            if(subject != null && !subject.isEmpty())
                subjectTextView.setText(message.getSubject());
            else
                subjectTextView.setText(R.string.noTitle);
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
                    bundle.putLong(ReadMessage.ARG_MESS_NR, messnr);
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

        private void setMessages(List<MessageHeader> messages) {
            if(messages != null && !messages.isEmpty()) {
                this.messages = messages;
            }
        }
    }

    private void removeMessage(int position) {
        MessageHeader mess = messagesList.get(position);
        messagesList.remove(mess);
        adapter.notifyItemRemoved(position);

        inboxVM.deleteFromFolderByUIDs(ACC, new long[] {mess.getUID()}, folderName);
    }

    ItemTouchHelper.SimpleCallback itemDeleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i){
            removeMessage(viewHolder.getAdapterPosition());
        }
    };

    RecyclerView.OnScrollListener visibleItemsChanged = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            LinearLayoutManager llm = ((LinearLayoutManager)recyclerView.getLayoutManager());

            if(llm != null) {
                int firstPosition = llm.findFirstVisibleItemPosition();
                int lastPosition = llm.findLastVisibleItemPosition() + 2;

                int count = 1;
                for (int i = firstPosition; i <= lastPosition; i++) {
                    MessageHolder message = ((MessageHolder) recyclerView.findViewHolderForLayoutPosition(i));
                    if (message != null) {
                        message.numberLabelView.setText(Integer.toString(count));
                        count++;
                    }
                }
            }
        }
    };

    private void ifMailtoThenRedirectToWriteFragment() {
        if(mailto != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(MainActivity.KEY_Acc, ACC);
            bundle.putString(MainActivity.KEY_FolderName, folderName);
            bundle.putString(WriteMessage.MAILTO_STRING, mailto);
            navController.navigate(R.id.writeMessage, bundle);
        }
    }
}