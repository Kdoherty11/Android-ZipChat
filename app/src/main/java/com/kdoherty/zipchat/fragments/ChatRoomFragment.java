package com.kdoherty.zipchat.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.MessageAdapter;
import com.kdoherty.zipchat.events.IsSubscribedEvent;
import com.kdoherty.zipchat.events.MemberJoinEvent;
import com.kdoherty.zipchat.events.MemberLeaveEvent;
import com.kdoherty.zipchat.events.ReceivedRoomMembersEvent;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ChatService;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.UserUtils;
import com.kdoherty.zipchat.utils.Utils;
import com.kdoherty.zipchat.views.DividerItemDecoration;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevindoherty on 2/2/15.
 */
public class ChatRoomFragment extends Fragment implements AsyncHttpClient.WebSocketConnectCallback, View.OnClickListener, WebSocket.StringCallback, MessageAdapter.MessageFavoriteListener {

    private static final String TAG = ChatRoomFragment.class.getSimpleName();

    private static final String HEARTBEAT_MESSAGE = "Beat";

    private static final int MESSAGE_LIMIT = 20;
    private static final int ITEM_VIEW_CACHE_SIZE = 20;

    public static final String ARG_ROOM_ID = "ChatRoomFragmentRoomIdArg";

    private MessageAdapter mMessageAdapter;
    private RecyclerView mMessagesRv;
    private EditText mMessageBoxEt;
    private long mRoomId;

    private Queue<String> mMessageQueue = new ArrayDeque<>();
    private WebSocket mWebSocket;

    private int mMessageOffset = 0;
    private long mSelfId;

    public static ChatRoomFragment newInstance(long roomId) {
        Bundle args = new Bundle();
        args.putLong(ARG_ROOM_ID, roomId);

        ChatRoomFragment fragment = new ChatRoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public ChatRoomFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelfId = UserUtils.getId(getActivity());
        mRoomId = getArguments().getLong(ARG_ROOM_ID);

        new ChatService(mSelfId, mRoomId, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_chat_room, container, false);

        mMessagesRv = (RecyclerView) rootView.findViewById(R.id.chat_room_activity_messages);
        mMessageBoxEt = (EditText) rootView.findViewById(R.id.chat_room_activity_message_box);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMessagesRv.setItemViewCacheSize(ITEM_VIEW_CACHE_SIZE);
        mMessagesRv.setItemAnimator(new DefaultItemAnimator());
        mMessagesRv.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.message_list_divider), true, true));
        mMessagesRv.setLayoutManager(new LinearLayoutManager(getActivity()));

        populateMessageList();

        mMessageBoxEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        ImageView mSendIv = (ImageView) view.findViewById(R.id.chat_room_activity_send_button);
        mSendIv.setOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (socketIsAvailable()) {
            Log.i(TAG, "Closing socket!");
            mWebSocket.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MessageAdapter.AnimateFirstDisplayListener.clearImages();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.chat_room_activity_send_button:
                sendMessage();
                break;
        }
    }

    private void populateMessageList(List<Message> messageList) {
        mMessageAdapter = new MessageAdapter(getActivity(), messageList, this);
        mMessagesRv.setAdapter(mMessageAdapter);
        mMessagesRv.scrollToPosition(mMessageAdapter.getItemCount() - 1);
    }

    private void populateMessageList() {
        if (!Utils.checkOnline(getActivity())) {
            return;
        }
        ZipChatApi.INSTANCE.getRoomMessages(mRoomId, MESSAGE_LIMIT, mMessageOffset, new Callback<List<Message>>() {
            @Override
            public void success(List<Message> messages, Response response) {
                populateMessageList(messages);
            }

            @Override
            public void failure(RetrofitError error) {
                Utils.logErrorResponse(TAG, "Getting chat messages", error);
            }
        });
    }

    private void sendMessage() {
        String messageContent = mMessageBoxEt.getText().toString().trim();
        if (!messageContent.isEmpty()) {
            sendMessage(messageContent);
            Utils.hideKeyboard(getActivity(), mMessageBoxEt);
            mMessageBoxEt.setText("");
        }
    }

    private void favoriteMessage(final User user, final long messageId) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessageAdapter.favoriteMessage(user, messageId, mSelfId);
            }
        });
    }

    private void removeFavorite(final User user, final long messageId) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessageAdapter.removeFavorite(user, messageId, mSelfId);
            }
        });
    }

    private void addMessage(final Message message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessageAdapter.addMessage(message);
                mMessagesRv.scrollToPosition(mMessageAdapter.getItemCount() - 1);
            }
        });
    }

    @Override
    public void sendFavoriteEvent(long messageId, boolean isFavorite) {
        Log.i(TAG, "Sending a favorite event to message " + messageId + " is favorite: " + isFavorite);
        if (!socketIsAvailable()) {
            Toast.makeText(getActivity(), "sendFavoriteEvent called when the socket is null or closed", Toast.LENGTH_LONG).show();
            return;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("event", "FavoriteNotification");
            json.put("messageId", messageId);
            String action = isFavorite ? "add" : "remove";
            json.put("action", action);
        } catch (JSONException e) {
            Log.e(TAG, "Problem creating the favorite event JSON: " + e.getMessage());
            return;
        }

        mWebSocket.send(json.toString());
    }

    private void sendQueuedMessages() {
        while (!mMessageQueue.isEmpty()) {
            Log.d(TAG, "Sending message from mMessageQueue");
            sendMessage(mMessageQueue.poll());
        }
    }

    private void sendMessage(String message) {
        if (!socketIsAvailable()) {
            mMessageQueue.add(message);
            return;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("event", "talk");
            json.put("message", message);
        } catch (JSONException e) {
            Log.e(TAG, "Problem creating the chat message JSON: " + e.getMessage());
            return;
        }

        mWebSocket.send(json.toString());
    }

    @Override
    public void onCompleted(Exception exception, WebSocket webSocket) {
        if (exception != null) {
            Log.e(TAG, "Problem connecting to the web socket: " + exception.getMessage());
            return;
        }
        mWebSocket = webSocket;
        mWebSocket.setStringCallback(this);
        sendQueuedMessages();
    }

    private boolean socketIsAvailable() {
        return mWebSocket != null && mWebSocket.isOpen();
    }

    @Override
    public void onStringAvailable(final String s) {
        try {
            Gson gson = new Gson();
            JSONObject stringJson = new JSONObject(s);

            String event = stringJson.getString("event");
            final String message = stringJson.getString("message");
            User user;

            switch (event) {
                case "talk":
                    if (!HEARTBEAT_MESSAGE.equals(message)) {
                        addMessage(gson.fromJson(message, Message.class));
                    }
                    break;
                case "join":
                    Log.d(TAG, "Received room members join from socket");
                    if (stringJson.has("user")) {
                        user = gson.fromJson(stringJson.getString("user"), User.class);
                        if (mSelfId != user.getUserId()) {
                            BusProvider.getInstance().post(new MemberJoinEvent(user));
                        }
                    }
                    break;
                case "quit":
                    Log.d(TAG, "Received room members quit from socket");
                    if (stringJson.has("user")) {
                        user = gson.fromJson(stringJson.getString("user"), User.class);
                        if (mSelfId != user.getUserId()) {
                            BusProvider.getInstance().post(new MemberLeaveEvent(user));
                        }
                    }
                    break;
                case "joinSuccess":
                    Log.d(TAG, "Received join success event from socket");
                    JSONObject messageJson = new JSONObject(message);
                    User[] users = gson.fromJson(messageJson.getString("roomMembers"), User[].class);
                    if (messageJson.has("isSubscribed")) {
                        BusProvider.getInstance().post(new IsSubscribedEvent(messageJson.getBoolean("isSubscribed")));
                    }
                    BusProvider.getInstance().post(new ReceivedRoomMembersEvent(users));
                    break;
                case "favorite":
                    Log.d(TAG, "favorite event from socket");
                    user = gson.fromJson(stringJson.getString("user"), User.class);
                    if (mSelfId != user.getUserId()) {
                        favoriteMessage(user, Long.parseLong(message));
                    }
                    break;
                case "removeFavorite":
                    Log.d(TAG, "Received removeFavorite event from socket");
                    user = gson.fromJson(stringJson.getString("user"), User.class);
                    if (mSelfId != user.getUserId()) {
                        removeFavorite(user, Long.parseLong(message));
                    }
                    break;
                case "error":
                    Log.e(TAG, "Error: " + message);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Error: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                default:
                    Log.i(TAG, "DEFAULT RECEIVED: " + s);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Problem parsing socket received JSON: " + s);
        }
    }
}
