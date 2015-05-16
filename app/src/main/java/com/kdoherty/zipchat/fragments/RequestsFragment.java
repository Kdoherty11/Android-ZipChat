package com.kdoherty.zipchat.fragments;

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

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.RequestAdapter;
import com.kdoherty.zipchat.events.ReceivedRequestEvent;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.UserUtils;
import com.kdoherty.zipchat.utils.Utils;
import com.kdoherty.zipchat.views.DividerItemDecoration;
import com.squareup.otto.Subscribe;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevindoherty on 1/31/15.
 */
public class RequestsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = RequestsFragment.class.getSimpleName();

    private RecyclerView mChatRequestsRv;
    private RequestAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public RequestsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mChatRequestsRv = (RecyclerView) view.findViewById(R.id.chat_requests);
        mChatRequestsRv.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.message_list_divider), true, true));
        mChatRequestsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mChatRequestsRv.setItemAnimator(new DefaultItemAnimator());

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

        final long userId = UserUtils.getId(getActivity());

        ZipChatApi.INSTANCE.getRequests(userId, new Callback<List<Request>>() {

            @Override
            public void success(List<Request> requests, Response response) {
                mSwipeRefreshLayout.setRefreshing(false);
                mAdapter = new RequestAdapter(getActivity(), requests);
                mChatRequestsRv.setAdapter(mAdapter);
            }

            @Override
            public void failure(RetrofitError error) {
                mSwipeRefreshLayout.setRefreshing(false);
                Utils.logErrorResponse(TAG, "Getting chat requests for a user with ID: " + userId, error);
            }
        });
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void receivedRequestEvent(ReceivedRequestEvent event) {
        Log.d(TAG, "Received request received event");
        populateList();
    }

    @Override
    public void onRefresh() {
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
}
