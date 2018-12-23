package io.rappt.compiler;

import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.rappt.model.ValuePath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import io.rappt.model.AppModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IntermediateModel {

    public boolean hasInternetAccess = false;

    public ConcurrentHashMap<String, Screen> intermediateScreens = new ConcurrentHashMap<>();
    public Map<String, Button> intermediateButtons = new HashMap<>();
    public Map<String, Label> intermediateLabels = new HashMap<>();
    public Map<String, WebView> intermediateWebView = new HashMap<>();
    public Map<String, Action> intermediateActions = new HashMap<>();
    public Map<String, StaticList> intermediateLists = new HashMap<>();
    public Map<String, DynamicList> dynamicLists = new HashMap<>();
    public Map<String, ListField> resourceListFields = new HashMap<>();
    public Map<String, Image> images = new HashMap<>();
    public Map<String, InputText> intputText = new HashMap<>();

    // Navigation Id, To Screen Id, From Screen Id
    public Table<String, String, String> navClasses = HashBasedTable.create();

    // Class name, class String parameters
    public Multimap<String, String> eventClasses = HashMultimap.create();

    public Collection<SharedPreference> preferences = new ArrayList<>();

    public Multimap<String, JsonPath> dataClassFields = HashMultimap.create();

    public Map<String, Api> intermediateApis = new HashMap<>();

    // Api ID, Resource ID, Resource
    public Table<String, String, Resource> resources = HashBasedTable.create();

    public App app = new App();
    public Navigation navigation = new Navigation();

    // menu file id, action
    public ListMultimap<String, Action> intermediateFileMenu = ArrayListMultimap.create();

    // string id, string value
    public Collection<Pair<String, String>> strings = new ArrayList<>();
    public HashMap<String, Number> integers = new HashMap<>();

    Collection<Pair<String, String[]>> stringArrays = new ArrayList<>();

    public boolean hasEventObjects;

    // Navigation id, is fragment to fragment
    public Map<String, Boolean> isFragmentToFragment = new ConcurrentHashMap<>();
    public boolean doesUseScribe;
    public boolean doesLoadImages;


    public IntermediateModel(String projectName) {
        app.applicationClassName = StringUtils.capitalize(projectName) + "Application";
    }

    public JsonObject toJson() {
        Gson gson = new Gson();
        JsonElement je = gson.toJsonTree(this);
        JsonObject jsonApp = new JsonObject();
        jsonApp.add("intermediate", je);
        return jsonApp;
    }

    static public class Screen {

        public String stringTitleId;

        public boolean isFragment;
        public String rootActivity = "";
        public boolean hasContextVariable;
        public boolean isLandingPage;
        public Collection<String> menuFiles = new HashSet<>();
        public boolean doesShowMenu;
        public boolean doesMakeRequest;
        public boolean hasPreferences;
        public boolean doesShowList;
        public String parameter;
        public String dynamicListVar;
        public String listItemDataClassName = "";
        public Event onLoadEvent;


        public View view = new View();

        public Collection<Field> formFields = new ArrayList<>();
//        public Collection<InputText> inputTexts = new ArrayList<>();


        public boolean sendsNotification;
        public ArrayDeque<Notification> notifications = new ArrayDeque<>();

        // Key: Navigation id, Value: Event Object Class Name
        public Map<String, String> eventClassNames = new Hashtable<>();

        // Key: Navigation id, Value: Does Send Event Objects
        public Map<String, Boolean> sendsEventObjects = new Hashtable<>();
        public boolean doesUseDataObject;
        public boolean doesRegisterEvents;
        public boolean hasInput;
    }

    static public class View {
        public String layoutFile = ""; // TODO: rename
        public String viewControllerName = ""; // TODO: rename
        public Collection<Field> pathsToShow = new ArrayList<>();
        public MapView mapView = new MapView();
    }

    static public class ListField {
        public String listClassName;
        public String listClassVariable;
    }

    static public class ListItem extends View {
        public String stringId;
        public String stateVariable;
        public String functionName;


    }


    static public class WebView {
        public String urlStringId = "";
        public boolean doesAuthenticate;
        public String toId = "";
    }

    static public class Action {
        public String menuFile;
        public String functionName;
        public String stringId;

        public Event event;

        // TODO optimise string names based on icon drawable
        public String iconDrawable;

        public Action(String functionName, String menuFile, String iconFile) {
            this.menuFile = menuFile;
            this.functionName = functionName;
            if (!iconFile.isEmpty())
                this.iconDrawable = iconFile;
        }
    }

    static public class StaticList {
        public String itemVariable;
        public String collectionVariable;
        public Event event;
    }

    static public class DynamicList {
        public String adapterClassName;
        public Map<String, ListItem> views = new HashMap<>();
        public Event onItemClickEvent;
    }

    public interface Instruction {
    }

    static public class Navigate implements Instruction {
        public String toClassName;
        public Field parameter;
        public Collection<Pair<String, String>> functionParameters = new ArrayList<>();

        public boolean doesRequireEventBus;
        public String eventBusClassName;
        public String fromClassName;

        public Collection<String> formatedParameters = new ArrayList<>();
    }

    static public class Event {
        public AppModel appModel;
        public List<io.rappt.model.Instruction> instructions = new ArrayList<>();
        public io.rappt.model.Instruction.Call callInstruction;
        public List<io.rappt.model.Instruction> postCall = new ArrayList<>();
        public Map<String, Instruction> allInstructions = new HashMap<>();

        public Event(AppModel app) {
            appModel = app;
        }
    }

    static public class Url implements Instruction {
        public String urlStringId;
    }

    static public class ShowToast implements Instruction {
        public String stringId;
    }

    static public class GetPreference implements Instruction {
        public String fieldId;
        public String prefId;
    }

    static public class RemovePreference implements Instruction {
        public String prefId;
    }

    static public class Call implements Instruction {
    }

    static public class CallToPOST extends Call {
    }

    static public class CallToGET extends Call {
    }

    static public class Notification {
        public String iconDrawable = "";
        public String functionName = "";
        public String imageFile = "";
        public String to;
    }

    static public class StaticNotification implements Instruction {
        public String titleStringId;
        public String titleStringRes;
        public String contentStringId;
        public String contentStringRes;
        public String functionName;
    }

    static public class DynamicNotification implements Instruction {
        public Field titleField = new Field();
        public Field contentField = new Field();
        public String functionName = "";
    }

    static public class CurrentLocation implements Instruction {
    }


    static public class Api {
        public String className;
        public String variableName;
        public String servicesStatic;
        public io.rappt.model.Resource authResource;
        public Map<String, Resource> resources = new HashMap<>();
        public String offlineClassName;
        public String setupApiFunction;
        public boolean hasOAuth;
        public io.rappt.model.OAuth oAuth;
    }

    static public class Resource {

        // TODO: remove when Resource from IntermediateModel connected to AppModel
        public boolean returnList;

        public String responseObject; // Object including List<> if needed
        public String functionName;
        public String fieldName;
        public String mockFileName;

        public Field tokenFieldPath;
        public Field stateFieldPath;

        public ArrayDeque<ValuePath> requestValuePaths = new ArrayDeque<>();
        public Collection<ValuePath> responseValuePaths = new ArrayList<>();

        public String responseClassName = "";
        public String parameterVariable = "";


        public Collection<Field> preferenceFields = new ArrayList<>();

    }

    static public class App {
        public String applicationClassName;
        public boolean provideMockData;
        public boolean hasDynamicList;
        public boolean hasMap;
        public boolean hasFragment;
        public boolean doesRequireGooglePlayServices;

        public Screen navigationScreen;
        public boolean hasPreferences;
        public String preferencesName;
        public boolean doesRequireLocation;
    }

    static public class Label extends UIField {
        public String value = "";
        public String variable = "";
        public boolean shownOnListItem;
        public boolean hasPassedValue;
    }

    static public class Image extends UIField {
        public String imageVariable;

    }

    static public class MapView extends UIField {

        public String onClickFunction = "";
        public double cameraLat;
        public double cameraLong;

        public ValuePath cameraLatPath;
        public ValuePath cameraLongPath;

        public Map<String, StaticMapViewMarker> makers = new HashMap<>();
        public Collection<DynamicMapViewMarker> dynamicMarkers = new ArrayList<>();

        public List<PolyLine> polyLines = new ArrayList<>();
        public Event event;

        static public class StaticMapViewMarker extends UIField {
            public String descriptionStingId = "";
            public String titleVariable = "";
            public String descriptionVariable;
        }

        static public class DynamicMapViewMarker {
            public Field latitude;
            public Field longitude;
            public Field title;
        }

        static public class PolyLine {
            public double startLat;
            public double startLong;
            public double endLat;
            public double endLong;

        }

    }

    static public class InputText extends UIField {
        public boolean isPassword;
        public String hintTextId;
    }

    static public class Button extends UIField {
        public String functionName;
        public Event event;
        public boolean shownOnListItem = false;
    }

    static public class UIField {
        public String formattedId;
        public String stringId;
    }

    static public class Field {
        public ValuePath vp = new ValuePath();
        public String elementId;
    }

    static public class Navigation {
        public Collection<Tabs> tabs = new ArrayList<>();

        static public class Tabs {
            public String stringId;
            public String className;
        }
    }

    static public class SharedPreference {
        public String functionName;
        public String type;
    }

    // Helper function to look up a screen
    public Optional<Screen> fetchFromScreen(String navId, String toId) {
        String id = Optional.ofNullable(navClasses.get(navId, toId)).orElse("");
        return Optional.ofNullable(intermediateScreens.get(id));
    }

    static public class JsonPath {
        public String variableName;
        public String jsonProperty;
        public String javaType;
        public boolean initNewObject;
        public boolean isObjectType;

        public JsonPath(String variableName, String jsonProperty, String javaType, boolean initNewObject) {
            this.variableName = variableName;
            this.jsonProperty = jsonProperty;
            this.javaType = javaType;
            this.initNewObject = initNewObject;
        }

        public JsonPath() {
        }
    }

    static public class OAuth {
        public String apiScribeProvider = "";
    }

}
