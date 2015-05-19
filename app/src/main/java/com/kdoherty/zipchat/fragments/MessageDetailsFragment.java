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
import com.kdoherty.zipchat.receivers.GcmBroadcastReceiver;
import com.kdoherty.zipchat.services.GcmIntentService;

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

    //This is the handler that will manager to process the broadcast intent
    private BroadcastReceiver mGcmFavoriteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra(GcmIntentService.Key.EVENT);

            if (GcmIntentService.Event.MESSAGE_FAVORITED.equals(event)) {
                // TODO
            } else {
                // Let GCM intent service deal with it
            }
        }
    };

    public MessageDetailsFragment() {
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
        mFavoriteAdapter = new MessageFavoritorAdapter(getActivity(), mMessageFavoritors);
        mMessageFavorites.setAdapter(mFavoriteAdapter);
    }

    public void displayMessage(Message message) {
        Log.d(TAG, "Displaying message: " + message);
        mMessageFavoritors = message.getFavorites();
        Log.d(TAG, "In displayMessage and messageFavorites are " + mMessageFavoritors);

        if (mFavoriteAdapter != null) {
            Log.d(TAG, "In displayMessage and data set is notified");

            mFavoriteAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGcmFavoriteReceiver, new IntentFilter(GCM_RECEIVER_NAME));
    }

    //Must unregister onPause()
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mGcmFavoriteReceiver);
    }
}
