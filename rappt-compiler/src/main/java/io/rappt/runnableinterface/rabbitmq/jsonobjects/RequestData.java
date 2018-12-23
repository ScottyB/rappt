package io.rappt.runnableinterface.rabbitmq.jsonobjects;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import io.rappt.model.AppModel;

import java.lang.reflect.Type;

public class RequestData {
    // Contains type adapter for AppModel
    public static transient final Gson REQUEST_DATA_GSON = new GsonBuilder().registerTypeAdapter(AppModel.class, new AppModelAdapter()).create();

    @SerializedName("project-name")
    public String projectName;

    @SerializedName("package")
    public String packageName;

    // optional fields
    private String view;
    public AppModel model;

    public void setView(String view) {
        this.view = view;
    }

    public String getView() {
        if (view == null) {
            return "";
        } else {
            return view;
        }
    }

    // Used to differentiate between a default view string (because it was missing from the JSON) vs a view string that is deliberately empty.
    // Returns true if view field was missing.
    public boolean getViewMissing() {
        return view == null;
    }

    // Optional fields are Operation specific, so are not validated by this method
    public boolean getIsValid() {
        return projectName != null && packageName != null && !projectName.isEmpty() && !packageName.isEmpty();
    }

    public static RequestData fromJson(JsonObject json) throws JsonParseException {
        return REQUEST_DATA_GSON.fromJson(json, RequestData.class);
    }

    // Ask AppModel to de-serialize itself rather than using default GSON adapter
    private static class AppModelAdapter implements JsonDeserializer<AppModel> {
        public AppModel deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context) throws JsonParseException {
            return AppModel.fromJson(elem.getAsJsonObject());
        }
    }
}
