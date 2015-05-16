package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.RequestAcceptedEvent;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.Utils;
import com.kdoherty.zipchat.views.CircleProfilePictureView;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevindoherty on 1/31/15.
 */
public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private static final String TAG = RequestAdapter.class.getSimpleName();

    private final LayoutInflater mInflater;
    private Context mContext;
    private final List<Request> mRequests;

    public RequestAdapter(Context context, List<Request> requests) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mRequests = requests;
    }

    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.cell_request, parent, false);
        return new RequestViewHolder(view);
    }

    public void deleteChatRequest(int position) {
        mRequests.remove(position);
        notifyItemRemoved(position);
    }

    public Request getRequest(int position) {
        return mRequests.get(position);
    }

    @Override
    public void onBindViewHolder(RequestViewHolder holder, final int position) {
        Request request = getRequest(position);
        holder.timeStamp.setText(DateUtils.getRelativeTimeSpanString(
                request.getTimeStamp() * 1000));

        final User sender = request.getSender();
        holder.senderTv.setText(sender.getName());
        holder.senderPicture.setProfileId(sender.getFacebookId());
        holder.acceptButton.setOnClickListener(new ResponseClickListener(Request.Status.accepted, position));
        holder.denyButton.setOnClickListener(new ResponseClickListener(Request.Status.denied, position));
    }

    @Override
    public int getItemCount() {
        return mRequests.size();
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
            final long requestId = getRequest(position).getRequestId();
            ZipChatApi.INSTANCE.respondToRequest(requestId, status.toString(), new Callback<Response>() {
                @Override
                public void success(Response result, Response response) {
                    if (status == Request.Status.accepted) {
                        BusProvider.getInstance().post(new RequestAcceptedEvent());
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Utils.logErrorResponse(TAG, "Responding to request with id: " + requestId, error);
                }
            });

            deleteChatRequest(position);
        }
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView senderTv;
        TextView timeStamp;
        CircleProfilePictureView senderPicture;
        Button acceptButton;
        Button denyButton;

        public RequestViewHolder(View itemView) {
            super(itemView);
            senderTv = (TextView) itemView.findViewById(R.id.chat_request_sender_name);
            timeStamp = (TextView) itemView.findViewById(R.id.chat_request_time_stamp);
            senderPicture = (CircleProfilePictureView) itemView.findViewById(R.id.chat_request_sender_picture);
            acceptButton = (Button) itemView.findViewById(R.id.chat_request_accept_button);
            denyButton = (Button) itemView.findViewById(R.id.chat_request_deny_button);
        }
    }
}
