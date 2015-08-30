package com.kdoherty.zipchat.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.UserAdapter;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.MyGcmListenerService;
import com.kdoherty.zipchat.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MessageFavoritesFragment extends Fragment {

    private static final String GCM_RECEIVER_NAME = "fragments.MessageFavoritesFragment.GCM_RECEIVER";
    private static final String TAG = MessageFavoritesFragment.class.getSimpleName();
    private static final String ARG_MESSAGE = "fragments.MessageFavoritesFragment.MESSAGE";
    private static final int NUM_COLS = 4;
    private RecyclerView mMessageFavorites;
    private List<User> mMessageFavoritors = new ArrayList<>();
    private UserAdapter mFavoriteAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private BroadcastReceiver mGcmFavoriteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra(MyGcmListenerService.Key.EVENT);

            if (MyGcmListenerService.Event.MESSAGE_FAVORITED.equals(event)) {
                Log.i(TAG, "Received message favorited event!!!: " + intent.getDataString());
                Utils.debugToast(getActivity(), "Received message favorited event!!!: " + intent.getDataString());
            } else {
                // Let GCM intent service deal with it
            }
        }
    };

    public MessageFavoritesFragment() { }

    public static MessageFavoritesFragment newInstance(List<User> messageFavorites) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_MESSAGE, new ArrayList<>(messageFavorites));

        MessageFavoritesFragment fragment = new MessageFavoritesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMessageFavoritors = getArguments().getParcelableArrayList(ARG_MESSAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_details, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mMessageFavorites = (RecyclerView) view.findViewById(R.id.message_favoritor_list);
        mMessageFavorites.setHasFixedSize(true);
        mLayoutManager = new GridLayoutManager(getActivity(), NUM_COLS);
        mMessageFavorites.setLayoutManager(mLayoutManager);
        mFavoriteAdapter = new UserAdapter(getActivity(), R.layout.cell_msg_favoritor, mMessageFavoritors);
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

    private void addFavorite(User favoritor) {
        mFavoriteAdapter.addUser(favoritor);
    }
}
