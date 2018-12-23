package io.rappt.runnableinterface.rabbitmq.jsonobjects;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestObject {
    private static transient final Logger LOGGER = Logger.getLogger(RequestObject.class.getName());
    // Contains type adapter for safely de-serializing RequestData
    public static transient final Gson REQUEST_OBJECT_GSON = new GsonBuilder().registerTypeAdapter(RequestData.class, new SafeRequestDataAdapter()).create();

    public String operation;
    public RequestData data;
    public String id;

    // Validate that fields are present.
    // RequestData is not validated, as the RequestObject is usable even without valid RequestData.
    public boolean getIsValid() {
        return operation != null && id != null && data != null && !operation.isEmpty() && !id.isEmpty();
    }

    public static RequestObject fromJson(JsonObject json) throws JsonParseException {
        return REQUEST_OBJECT_GSON.fromJson(json, RequestObject.class);
    }

    // Catches errors when de-serializing RequestData
    // This is needed because we need to be able to reply to a RequestObject even if it contains invalid Json.
    private static class SafeRequestDataAdapter implements JsonDeserializer<RequestData> {
        public RequestData deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context) throws JsonParseException {
            try {
                return RequestData.fromJson(elem.getAsJsonObject());
            } catch (JsonParseException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return new RequestData();
            }
        }
    }

    // Generate a response object in reply to this request
    public ResponseObject replyTo() {
        return new ResponseObject(this.id, this.operation);
    }
}
