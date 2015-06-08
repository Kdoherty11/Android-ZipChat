package com.kdoherty.zipchat.services;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kdoherty.zipchat.activities.ZipChatApplication;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.utils.UserUtils;

import java.lang.reflect.Type;
import java.util.List;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by kdoherty on 12/19/14.
 */
public interface ZipChatApi {

    String ENDPOINT = "http://zipchatapp.herokuapp.com/";

    Gson GSON = new GsonBuilder().create();

    RestAdapter ADAPTER = new RestAdapter.Builder()
            .setEndpoint(ENDPOINT)
            .setConverter(new GsonConverter(GSON))
            .setLogLevel(RestAdapter.LogLevel.HEADERS)
            .build();

    ZipChatApi INSTANCE = ADAPTER.create(ZipChatApi.class);

    // *************** Rooms ***************

    @GET("/rooms/{roomId}/messages")
    void getRoomMessages(@Header("X-AUTH-TOKEN") String authToken, @Path("roomId") long roomId,
                         @Query("limit") int limit,
                         @Query("offset") int offset,
                         Callback<List<Message>> response);

    // *************** Public Rooms ***************

    @FormUrlEncoded
    @POST("/publicRooms")
    void createPublicRoom(@Header("X-AUTH-TOKEN") String authToken, @Field("name") String roomName, @Field("radius") int radius,
                          @Field("latitude") double latitude, @Field("longitude") double longitude,
                          Callback<Response> response);

    @GET("/test")
    void getPublicRooms(@Header("X-AUTH-TOKEN") String authToken, @Query("lat") double latitude,
                        @Query("lon") double longitude,
                        Callback<List<PublicRoom>> response);

    @FormUrlEncoded
    @POST("/publicRooms/{roomId}/subscriptions")
    void subscribe(@Header("X-AUTH-TOKEN") String authToken, @Path("roomId") long roomId, @Field("userId") long userId, Callback<Response> response);

    @DELETE("/publicRooms/{roomId}/subscriptions/{userId}")
    void removeSubscription(@Header("X-AUTH-TOKEN") String authToken, @Path("roomId") long roomId, @Path("userId") long userId, Callback<Response> response);

    // *************** Private Rooms ***************

    @PUT("/privateRooms/{roomId}/leave")
    void leaveRoom(@Header("X-AUTH-TOKEN") String authToken, @Path("roomId") long roomId, @Query("userId") long userId, Callback<Response> response);

    // *************** Private Rooms ***************

    @GET("/privateRooms")
    void getPrivateRooms(@Header("X-AUTH-TOKEN") String authToken, @Query("userId") long userId, Callback<List<PrivateRoom>> response);

    // *************** Messages ***************

    @PUT("/messages/{messageId}/favorite")
    void favoriteMessage(@Header("X-AUTH-TOKEN") String authToken, @Path("messageId") long messageId, @Query("userId") long userId, Callback<Response> response);

    @DELETE("/messages/{messageId}/favorite")
    void removeFavorite(@Header("X-AUTH-TOKEN") String authToken, @Path("messageId") long messageId, @Query("userId") long userId, Callback<Response> response);

    @PUT("/messages/{messageId}/favorite")
    void flagMessage(@Header("X-AUTH-TOKEN") String authToken, @Path("messageId") long messageId, @Query("userId") long userId, Callback<Response> response);

    @DELETE("/messages/{messageId}/favorite")
    void removeFlag(@Header("X-AUTH-TOKEN") String authToken, @Path("messageId") long messageId, @Query("userId") long userId, Callback<Response> response);

    // *************** Requests ***************

    @GET("/requests")
    void getRequests(@Header("X-AUTH-TOKEN") String authToken, @Query("userId") long receiverId, Callback<List<Request>> response);

    @FormUrlEncoded
    @POST("/requests")
    void sendChatRequest(@Header("X-AUTH-TOKEN") String authToken, @Field("sender") long senderId, @Field("receiver") long receiverId, @Field("isAnon") boolean isAnon, Callback<Response> response);

    @FormUrlEncoded
    @PUT("/requests/{requestId}")
    void respondToRequest(@Header("X-AUTH-TOKEN") String authToken, @Path("requestId") long requestId, @Field("status") String status, Callback<Response> response);

    @GET("/requests/status")
    void getStatus(@Header("X-AUTH-TOKEN") String authToken, @Query("senderId") long senderId, @Query("receiverId") long receiverId, Callback<Response> response);

    // *************** Users ***************

    @FormUrlEncoded
    @PUT("/users")
    void createUser(@Field("fbAccessToken") String fbAccessToken, @Field("registrationId") String regId,
                    @Field("platform") String platform, Callback<Response> response);

    @GET("/auth")
    void auth(@Query("fbAccessToken") String fbAccessToken, Callback<Response> response);
}
