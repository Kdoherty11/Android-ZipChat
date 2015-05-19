package com.kdoherty.zipchat.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.Request;

import java.lang.reflect.Type;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by kdoherty on 12/19/14.
 */
public interface ZipChatApi {

    String ENDPOINT = "http://zipchatapp.herokuapp.com/";

    Gson GSON = new GsonBuilder()
            .registerTypeAdapter(PublicRoom.class, new PublicRoomDeserializer())
            .create();

    RestAdapter ADAPTER = new RestAdapter.Builder()
            .setEndpoint(ENDPOINT)
            .setConverter(new GsonConverter(GSON))
            .build();

    ZipChatApi INSTANCE = ADAPTER.create(ZipChatApi.class);

    class PublicRoomDeserializer implements JsonDeserializer<PublicRoom> {
        public PublicRoomDeserializer() {
        }

        @Override
        public PublicRoom deserialize(JsonElement jsonElement, Type typeOF,
                                      JsonDeserializationContext context) throws JsonParseException {

            JsonObject jsonRoom = jsonElement.getAsJsonObject();

            final long roomId = jsonRoom.get("roomId").getAsLong();
            final String name = jsonRoom.get("name").getAsString();
            final int radius = jsonRoom.get("radius").getAsInt();
            final double latitude = jsonRoom.get("latitude").getAsDouble();
            final double longitude = jsonRoom.get("longitude").getAsDouble();
            final long timeStamp = jsonRoom.get("timeStamp").getAsLong();
            final long lastActivity = jsonRoom.get("lastActivity").getAsLong();

            return new PublicRoom(roomId, name, radius, latitude, longitude, timeStamp, lastActivity) ;
        }
    }

    // *************** Public Rooms ***************

    @FormUrlEncoded
    @POST("/publicRooms")
    void createPublicRoom(@Field("name") String roomName, @Field("radius") int radius,
                          @Field("latitude") double latitude, @Field("longitude") double longitude,
                          Callback<Response> response);

    @GET("/test")
    void getPublicRooms(@Query("lat") double latitude,
                        @Query("lon") double longitude,
                        Callback<List<PublicRoom>> response);

    @FormUrlEncoded
    @POST("/publicRooms/{roomId}/subscriptions")
    void subscribe(@Path("roomId") long roomId, @Field("userId") long userId, Callback<Response> response);

    @DELETE("/publicRooms/{roomId}/subscriptions/{userId}")
    void removeSubscription(@Path("roomId") long roomId, @Path("userId") long userId, Callback<Response> response);

    @GET("/rooms/{roomId}/messages")
    void getRoomMessages(@Path("roomId") long roomId,
                         @Query("limit") int limit,
                         @Query("offset") int offset,
                         Callback<List<Message>> response);

    // *************** Users ***************

    @FormUrlEncoded
    @POST("/users")
    void createUser(@Field("name") String name, @Field("facebookId") String facebookId,
                    @Field("registrationId") String regId, @Field("platform") String platform, Callback<Response> response);

    // *************** Private Rooms ***************

    @GET("/privateRooms")
    void getPrivateRooms(@Query("userId") long userId, Callback<List<PrivateRoom>> response);

    // *************** Requests ***************

    @GET("/requests")
    void getRequests(@Query("userId") long receiverId, Callback<List<Request>> response);

    @FormUrlEncoded
    @POST("/requests")
    void sendChatRequest(@Field("sender") long senderId, @Field("receiver") long receiverId, Callback<Response> response);

    @FormUrlEncoded
    @PUT("/requests/{requestId}")
    void respondToRequest(@Path("requestId") long requestId, @Field("status") String status, Callback<Response> response);

    @GET("/requests/status")
    void getStatus(@Query("senderId") long senderId, @Query("receiverId") long receiverId, Callback<Response> response);

    @PUT("/privateRooms/{roomId}/leave")
    void leaveRoom(@Path("roomId") long roomId, @Query("userId") long userId, Callback<Response> response);

}
