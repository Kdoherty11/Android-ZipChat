package com.kdoherty.zipchat.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.PrivateRoomActivity;
import com.kdoherty.zipchat.adapters.PrivateRoomAdapter;
import com.kdoherty.zipchat.events.RequestAcceptedEvent;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PrivateRoomComparator;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.UserUtils;
import com.kdoherty.zipchat.utils.Utils;
import com.kdoherty.zipchat.views.DividerItemDecoration;
import com.kdoherty.zipchat.views.RecyclerItemClickListener;
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

    public PrivateRoomsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_private_rooms, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mPrivateChatsRv = (RecyclerView) view.findViewById(R.id.my_chats_list);
        mPrivateChatsRv.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.message_list_divider), true, true));
        mPrivateChatsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mPrivateChatsRv.setItemAnimator(new DefaultItemAnimator());


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

        mPrivateChatsRv.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(),
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        PrivateRoom privateRoom = mAdapter.getPrivateChat(position);
                        long roomId = privateRoom.getId();
                        String userName = privateRoom.getOther().getName();
                        String facebookId = privateRoom.getOther().getFacebookId();
                        Intent intent = PrivateRoomActivity.getIntent(getActivity(), roomId, userName, facebookId);
                        startActivity(intent);
                    }
                }));
    }

    public void populateList() {
        if (!Utils.checkOnline(getActivity())) {
            return;
        }

        final long userId = UserUtils.getId(getActivity());

        ZipChatApi.INSTANCE.getPrivateRooms(userId, new Callback<List<PrivateRoom>>() {
            @Override
            public void success(List<PrivateRoom> privateRooms, Response response) {
                mSwipeRefreshLayout.setRefreshing(false);
                mPrivateRooms = privateRooms;
                Collections.sort(mPrivateRooms, PrivateRoomComparator.INSTANCE);
                mAdapter = new PrivateRoomAdapter(getActivity(), mPrivateRooms);
                mPrivateChatsRv.setAdapter(mAdapter);
            }

            @Override
            public void failure(RetrofitError error) {
                mSwipeRefreshLayout.setRefreshing(false);
                Utils.logErrorResponse(TAG, "Getting private rooms by userId: " + userId, error);
            }
        });
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onRequestAccepted(RequestAcceptedEvent event) {
        Log.d(TAG, "Received request accepted event");
        populateList();
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

    @Override
    public void onRefresh() {
        populateList();
    }
}
