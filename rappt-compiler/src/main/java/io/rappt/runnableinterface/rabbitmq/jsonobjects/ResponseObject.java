package io.rappt.runnableinterface.rabbitmq.jsonobjects;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ResponseObject {
    public static transient final Gson RESPONSE_OBJECT_GSON = new GsonBuilder().registerTypeAdapter(ResponseData.class, new ResponseDataAdapter()).create();

    public String id;
    public ResponseData data;
    public String operation;

    public ResponseObject(String id, String operation) {
        this.id = id;
        this.operation = operation;
        data = new ResponseData();
    }

    public JsonObject toJson() throws JsonParseException {
        JsonElement je = RESPONSE_OBJECT_GSON.toJsonTree(this);
        JsonObject json = je.getAsJsonObject();
        return json;
    }

    // Ask ResponseData to serialize itself rather than using default GSON adapter
    private static class ResponseDataAdapter implements JsonSerializer<ResponseData> {
        public JsonElement serialize(ResponseData src, Type typeOfSrc, JsonSerializationContext context) {
            return src.toJson();
        }
    }
}
