package com.kdoherty.zipchat.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.MessageFavoritorAdapter;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.MyGcmListenerService;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MessageDetailsFragment extends Fragment {

    private static final String GCM_RECEIVER_NAME = "MessageDetailsFragmentGcmReceiver";

    private static final String TAG = MessageDetailsFragment.class.getSimpleName();
    private RecyclerView mMessageFavorites;
    private List<User> mMessageFavoritors = new ArrayList<>();
    private MessageFavoritorAdapter mFavoriteAdapter;

    private Message mMessage;

    private static final String ARG_MESSAGE = "MessageDetailFragmentMessage";

    //This is the handler that will manager to process the broadcast intent
    private BroadcastReceiver mGcmFavoriteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra(MyGcmListenerService.Key.EVENT);

            if (MyGcmListenerService.Event.MESSAGE_FAVORITED.equals(event)) {
                // TODO
            } else {
                // Let GCM intent service deal with it
            }
        }
    };


    public static MessageDetailsFragment newInstance(Message message) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSAGE, message);

        MessageDetailsFragment fragment = new MessageDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public MessageDetailsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMessage = getArguments().getParcelable(ARG_MESSAGE);
        Log.d(TAG, "Displaying message: " + mMessage);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_details, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mMessageFavorites = (RecyclerView) view.findViewById(R.id.message_favoritor_list);
        mMessageFavorites.setLayoutManager(new LinearLayoutManager(getActivity()));
        Log.d(TAG, "In onViewCreated and messageFavorites are " + mMessageFavoritors);
        mMessageFavoritors = mMessage.getFavorites();
        mFavoriteAdapter = new MessageFavoritorAdapter(getActivity(), mMessageFavoritors);
        mMessageFavorites.setAdapter(mFavoriteAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGcmFavoriteReceiver, new IntentFilter(GCM_RECEIVER_NAME));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mGcmFavoriteReceiver);
    }
}
