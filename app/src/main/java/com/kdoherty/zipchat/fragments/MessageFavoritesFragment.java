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
    private static final String ARG_MESSAGE = "fragments.MessageFavoritesFragment.arg.MESSAGE";
    private static final int NUM_COLS = 4;
    private RecyclerView mMessageFavorites;
    private List<User> mMessageFavoritors = new ArrayList<>();
    private UserAdapter mFavoriteAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public MessageFavoritesFragment() {
    }

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

    public void addFavorite(User user) {
        mFavoriteAdapter.addUser(user);
    }

    public void removeFavorite(User user) {
        mFavoriteAdapter.removeByUserId(user.getUserId());
    }
}
