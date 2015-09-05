package com.kdoherty.zipchat.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.kdoherty.zipchat.models.AbstractRoom;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PublicRoom;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by kdoherty on 9/4/15.
 */
public class GsonProvider {

    private static final String TAG = GsonProvider.class.getSimpleName();

    private static final Gson instance = new GsonBuilder().registerTypeAdapter(AbstractRoom.class, new JsonDeserializer<AbstractRoom>() {
        @Override
        public AbstractRoom deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject =  json.getAsJsonObject();
            JsonPrimitive prim = (JsonPrimitive) jsonObject.get("type");

            Class<?> clazz = null;
            AbstractRoom.RoomType roomType = AbstractRoom.RoomType.valueOf(prim.getAsString());
            switch (roomType) {
                case PUBLIC:
                    clazz = PublicRoom.class;
                    break;
                case PRIVATE:
                    clazz = PrivateRoom.class;
                    break;
            }
            return context.deserialize(jsonObject, clazz);
        }
    }).create();

    private GsonProvider() { }

    public static Gson getInstance() {
        return instance;
    }


}
