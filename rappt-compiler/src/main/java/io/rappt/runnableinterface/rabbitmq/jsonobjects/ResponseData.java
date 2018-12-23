package io.rappt.runnableinterface.rabbitmq.jsonobjects;

import com.google.gson.*;
import io.rappt.model.AppModel;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ResponseData {
    // Contains type adapter for AppModel
    public static transient final Gson RESPONSE_DATA_GSON = new GsonBuilder().registerTypeAdapter(AppModel.class, new AppModelAdapter()).create();

    public ArrayList<String> errors;
    public AppModel model;
    public String view;

    public ResponseData(){
        errors = new ArrayList<>();
    }

    public JsonObject toJson() throws JsonParseException {
        JsonElement je = RESPONSE_DATA_GSON.toJsonTree(this);
        JsonObject json = je.getAsJsonObject();
        return json;
    }

    // Ask AppModel to serialize itself rather than using default GSON adapter
    private static class AppModelAdapter implements JsonSerializer<AppModel> {
        public JsonElement serialize(AppModel src, Type typeOfSrc, JsonSerializationContext context) {
            return src.toJson();
        }
    }
}
