package com.kdoherty.zipchat.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.UserAdapter;
import com.kdoherty.zipchat.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MessageFavoritesFragment extends Fragment {

    private static final String TAG = MessageFavoritesFragment.class.getSimpleName();
    private static final String ARG_MESSAGE_FAVORITES = "fragments.MessageFavoritesFragment.arg.MESSAGE_FAVORITES";
    private static final int NUM_COLS = 4;
    private List<User> mMessageFavoritors = new ArrayList<>();
    private UserAdapter mFavoriteAdapter;

    public MessageFavoritesFragment() {
    }

    public static MessageFavoritesFragment newInstance(List<User> favorites) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_MESSAGE_FAVORITES, new ArrayList<>(favorites));

        MessageFavoritesFragment fragment = new MessageFavoritesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMessageFavoritors = getArguments().getParcelableArrayList(ARG_MESSAGE_FAVORITES);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_favorites, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        RecyclerView messageFavoritesRv = (RecyclerView) view.findViewById(R.id.message_favorites_rv);
        messageFavoritesRv.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getActivity(), NUM_COLS);
        messageFavoritesRv.setLayoutManager(layoutManager);
        mFavoriteAdapter = new UserAdapter(getActivity(), R.layout.cell_msg_favoritor, mMessageFavoritors);
        messageFavoritesRv.setAdapter(mFavoriteAdapter);
    }

    public void addFavorite(User user) {
        mFavoriteAdapter.addUser(user);
    }

    public void removeFavorite(User user) {
        mFavoriteAdapter.removeByUserId(user.getUserId());
    }
}