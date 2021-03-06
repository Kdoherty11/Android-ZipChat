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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.RequestAdapter;
import com.kdoherty.zipchat.events.ReceivedRequestEvent;
import com.kdoherty.zipchat.events.RequestResponseEvent;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.BusProvider;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.views.DividerItemDecoration;
import com.squareup.otto.Subscribe;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevindoherty on 1/31/15.
 */
public class RequestsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, Filterable {

    private static final String TAG = RequestsFragment.class.getSimpleName();

    private RecyclerView mChatRequestsRv;
    private RequestAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mNoRequestsTv;

    public RequestsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mNoRequestsTv = (TextView) view.findViewById(R.id.no_chat_requests_tv);
        mChatRequestsRv = (RecyclerView) view.findViewById(R.id.chat_requests_rv);
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
        if (!NetworkManager.checkOnline(getActivity())) {
            return;
        }

        final long userId = UserManager.getId(getActivity());

        ZipChatApi.INSTANCE.getRequests(UserManager.getAuthToken(getActivity()), userId, new Callback<List<Request>>() {

            @Override
            public void success(List<Request> requests, Response response) {
                mSwipeRefreshLayout.setRefreshing(false);
                mAdapter = new RequestAdapter(getActivity(), requests);
                mChatRequestsRv.setAdapter(mAdapter);
                if (requests.isEmpty()) {
                    displayNoRequestsNotice();
                } else {
                    hideNoRequestsNotice();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                mSwipeRefreshLayout.setRefreshing(false);
                NetworkManager.handleErrorResponse(TAG, "Getting chat requests for a user with ID: " + userId, error, getActivity());
            }
        });
    }

    private void hideNoRequestsNotice() {
        mNoRequestsTv.setVisibility(View.GONE);
    }

    private void displayNoRequestsNotice() {
        mNoRequestsTv.setVisibility(View.VISIBLE);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void receivedRequestEvent(ReceivedRequestEvent event) {
        Log.d(TAG, "Received request received event");
        populateList();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onRequestResponse(RequestResponseEvent event) {
        if (mAdapter != null && mAdapter.isEmpty()) {
            displayNoRequestsNotice();
        }
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

    @Override
    public Filter getFilter() {
        if (mAdapter != null) {
            return mAdapter.getFilter();
        }
        return null;
    }
}
