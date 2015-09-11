package com.kdoherty.zipchat.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.PrivateRoomAdapter;
import com.kdoherty.zipchat.events.LeaveRoomEvent;
import com.kdoherty.zipchat.events.RequestResponseEvent;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PrivateRoomComparator;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.BusProvider;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.views.DividerItemDecoration;
import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class PrivateRoomsFragment extends Fragment implements Filterable, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = PrivateRoomsFragment.class.getSimpleName();

    private List<PrivateRoom> mPrivateRooms;
    private RecyclerView mPrivateChatsRv;
    private PrivateRoomAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mNoPrivateRoomsTv;

    public PrivateRoomsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_private_rooms, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mNoPrivateRoomsTv = (TextView) view.findViewById(R.id.no_private_chats_tv);
        mPrivateChatsRv = (RecyclerView) view.findViewById(R.id.private_chats_rv);
        mPrivateChatsRv.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.message_list_divider), true, true));
        mPrivateChatsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mPrivateChatsRv.setItemAnimator(new DefaultItemAnimator());
        registerForContextMenu(mPrivateChatsRv);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });

        populateList();
    }

    public void populateList() {
        if (!NetworkManager.checkOnline(getActivity())) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        final long userId = UserManager.getId(getActivity());

        ZipChatApi.INSTANCE.getPrivateRooms(UserManager.getAuthToken(getActivity()), userId, new Callback<List<PrivateRoom>>() {
            @Override
            public void success(List<PrivateRoom> privateRooms, Response response) {
                mSwipeRefreshLayout.setRefreshing(false);
                mPrivateRooms = privateRooms;
                mAdapter = new PrivateRoomAdapter(getActivity(), mPrivateRooms);
                mPrivateChatsRv.setAdapter(mAdapter);

                if (privateRooms.isEmpty()) {
                    displayNoPrivateRoomsNotice();
                } else {
                    hideNoPrivateRoomsNotice();
                    Collections.sort(mPrivateRooms, PrivateRoomComparator.INSTANCE);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                mSwipeRefreshLayout.setRefreshing(false);
                NetworkManager.handleErrorResponse(TAG, "Getting private rooms by userId: " + userId, error, getActivity());
            }
        });
    }

    private void hideNoPrivateRoomsNotice() {
        mNoPrivateRoomsTv.setVisibility(View.GONE);
    }

    private void displayNoPrivateRoomsNotice() {
        mNoPrivateRoomsTv.setVisibility(View.VISIBLE);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onRequestResponse(RequestResponseEvent event) {
        if (event.getResponse() == Request.Status.accepted) {
            populateList();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public Filter getFilter() {
        if (mAdapter != null) {
            return mAdapter.getFilter();
        }
        return null;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onLeftRoom(LeaveRoomEvent event) {
        populateList();
    }

    @Override
    public void onRefresh() {
        populateList();
    }
}
