package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.UserDetailsActivity;
import com.kdoherty.zipchat.events.RequestResponseEvent;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.BusProvider;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevindoherty on 1/31/15.
 */
public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> implements Filterable {

    private static final String TAG = RequestAdapter.class.getSimpleName();

    private final LayoutInflater mInflater;
    private final List<Request> mRequests;
    private Context mContext;
    private RequestFilter mFilter = new RequestFilter();
    private List<Request> mFilteredRequests;

    public RequestAdapter(Context context, List<Request> requests) {
        mContext = Utils.checkNotNull(context, "Context");
        mRequests = Utils.checkNotNull(requests, "Requests");
        mInflater = LayoutInflater.from(context);
        mFilteredRequests = requests;
    }

    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.cell_request, parent, false);
        return new RequestViewHolder(view);
    }

    public void deleteChatRequest(int position) {
        mFilteredRequests.remove(position);
        notifyItemRemoved(position);
    }

    public Request getRequest(int position) {
        return mFilteredRequests.get(position);
    }

    @Override
    public void onBindViewHolder(RequestViewHolder holder, final int position) {
        final Request request = getRequest(position);
        holder.timeStamp.setText(DateUtils.getRelativeTimeSpanString(
                request.getCreatedAt() * 1000));

        final User sender = request.getSender();
        holder.senderTv.setText(sender.getName());
        FacebookManager.displayProfilePicture(sender.getFacebookId(), holder.senderPicture);
        holder.acceptButton.setOnClickListener(new ResponseClickListener(Request.Status.accepted, position));
        holder.denyButton.setOnClickListener(new ResponseClickListener(Request.Status.denied, position));
        holder.senderPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserDetailsActivity.getIntent(mContext, sender);
                mContext.startActivity(intent);
            }
        });
    }

    public boolean isEmpty() {
        return mFilteredRequests.isEmpty();
    }

    @Override
    public int getItemCount() {
        return mFilteredRequests.size();
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView senderTv;
        TextView timeStamp;
        CircleImageView senderPicture;
        Button acceptButton;
        Button denyButton;

        public RequestViewHolder(View itemView) {
            super(itemView);
            senderTv = (TextView) itemView.findViewById(R.id.chat_request_sender_name_tv);
            timeStamp = (TextView) itemView.findViewById(R.id.chat_request_timestamp_tv);
            senderPicture = (CircleImageView) itemView.findViewById(R.id.chat_request_sender_picture_civ);
            acceptButton = (Button) itemView.findViewById(R.id.chat_request_accept_btn);
            denyButton = (Button) itemView.findViewById(R.id.chat_request_deny_btn);
        }
    }

    private class ResponseClickListener implements View.OnClickListener {

        private Request.Status status;
        private int position;

        ResponseClickListener(Request.Status status, int position) {
            this.status = status;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            if (!NetworkManager.checkOnline(mContext)) {
                return;
            }

            final long requestId = getRequest(position).getRequestId();
            ZipChatApi.INSTANCE.respondToRequest(UserManager.getAuthToken(mContext), requestId, status.toString(), new Callback<Response>() {
                @Override
                public void success(Response result, Response response) {
                    BusProvider.getInstance().post(new RequestResponseEvent(status));
                }

                @Override
                public void failure(RetrofitError error) {
                    NetworkManager.handleErrorResponse(TAG, "Responding to request with id: " + requestId, error, mContext);
                }
            });

            deleteChatRequest(position);
        }
    }

    private class RequestFilter extends Filter {

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            FilterResults filterResults = new FilterResults();
            String filterString = charSequence.toString().trim().toLowerCase();
            if (TextUtils.isEmpty(filterString)) {
                filterResults.count = getItemCount();
                filterResults.values = mRequests;
            } else {
                List<Request> tempList = new ArrayList<>();

                for (Request request : mRequests) {
                    if (request.getSender().getName().toLowerCase().contains(filterString)) {
                        tempList.add(request);
                    }
                }

                filterResults.count = tempList.size();
                filterResults.values = tempList;
            }

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredRequests = (ArrayList<Request>) results.values;
            notifyDataSetChanged();
        }
    }
}
