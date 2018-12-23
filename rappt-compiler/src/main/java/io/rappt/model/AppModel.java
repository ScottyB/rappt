package io.rappt.model;

/**
 * Root node for the Platform Independent Model
 */

import com.google.common.collect.HashMultimap;
import com.google.gson.*;
import io.rappt.antlr.AMLErrorReporter;
import io.rappt.view.InterfaceAdapter;

import java.util.*;

public class AppModel implements PIM, AMLErrorReporter {
    // Use custom serializer so that we can retain type information
    public static transient final Gson APP_MODEL_GSON = new GsonBuilder()
            .registerTypeAdapter(PIM.class, new InterfaceAdapter<PIM>())
            .registerTypeAdapter(Instruction.class, new InterfaceAdapter<Instruction>())
            .create();

    public String projectName;

    // Default main colour for app
    public String primaryColour = "#013499";

    public transient Map<String, PIM> ids = new HashMap<>();


    public HashMultimap<String, String> passedParamsToScreen = HashMultimap.create();


    public transient Map<String, String> newIds = new Hashtable<>();
    public String mapKey;

    public static class ACRA {
        public String email = "";
    }

    public ACRA acra = new ACRA();

    public List<Api> allApi = new ArrayList<>();
    public List<Screen> screens = new ArrayList<>();  // Apps should always have screens!!!
    public Feature menu = new Feature();
    public Navigation navigation = new Navigation();
    public List<Style> styles = new ArrayList<>();

    public String landingPage = "";

    // Platform specific settings
    public String androidSdk;
    public String packageName;

    // No-args constructor for Gson de-serialization
    private AppModel() {
    }

    public AppModel(String projectName, String packageName) {
        this.projectName = projectName;
        this.packageName = packageName;
    }

    public List<Feature> allFeatures() {
        final List<Feature> features = new ArrayList<>();
        features.add(menu);
        features.add(navigation);
        return features;
    }

    @Override
    public void accept(PIMVisitor visitor) {
        visitor.visit(this);
        menu.accept(visitor);
        navigation.accept(visitor);
        screens.forEach(s -> s.accept(visitor));
        allApi.forEach(a -> a.accept(visitor));
        styles.forEach(s -> s.accept(visitor));
    }

    public Screen getScreen(String screenId) throws NoSuchElementException {
        // TODO: build index for efficiency
        return screens.stream().filter(s -> screenId.equals(s.id)).findAny().get();
    }

    public Api getApi(String apiId) throws NoSuchElementException {
        // TODO: build index for efficiency
        return allApi.stream().filter(a -> apiId.equals(a.id)).findAny().get();
    }

    // model -> JSON
    public JsonObject toJson() {
        JsonElement je = APP_MODEL_GSON.toJsonTree(this);
        JsonObject json = je.getAsJsonObject();
        return json;
    }

    // JSON -> model
    public static AppModel fromJson(JsonObject json) throws JsonSyntaxException {
        AppModel appModel = APP_MODEL_GSON.fromJson(json, AppModel.class);
        // TODO: validate appModel
        return appModel;
    }
}
