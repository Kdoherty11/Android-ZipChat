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
import com.kdoherty.zipchat.events.RequestAcceptedEvent;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;

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
    private Context mContext;
    private final List<Request> mRequests;
    private RequestFilter mFilter = new RequestFilter();
    private List<Request> mFilteredRequests;

    public RequestAdapter(Context context, List<Request> requests) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mRequests = requests;
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

    @Override
    public int getItemCount() {
        return mFilteredRequests.size();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView senderTv;
        TextView timeStamp;
        CircleImageView senderPicture;
        Button acceptButton;
        Button denyButton;

        public RequestViewHolder(View itemView) {
            super(itemView);
            senderTv = (TextView) itemView.findViewById(R.id.chat_request_sender_name);
            timeStamp = (TextView) itemView.findViewById(R.id.chat_request_time_stamp);
            senderPicture = (CircleImageView) itemView.findViewById(R.id.chat_request_sender_picture);
            acceptButton = (Button) itemView.findViewById(R.id.chat_request_accept_button);
            denyButton = (Button) itemView.findViewById(R.id.chat_request_deny_button);
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
                    if (status == Request.Status.accepted) {
                        BusProvider.getInstance().post(new RequestAcceptedEvent());
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    NetworkManager.logErrorResponse(TAG, "Responding to request with id: " + requestId, error);
                }
            });

            deleteChatRequest(position);
        }
    }

    @Override
    public Filter getFilter() {
        return mFilter;
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
