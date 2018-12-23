package io.rappt.android;

import io.rappt.compiler.IntermediateModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface AddToController {
    public Controller addToController(final Controller controller);
}

public class UIControls {

    static public class WebView extends AndroidView implements AddToController {
        public String urlStringId = "";
        public boolean handlesOAuth;
        public String appVariable = "";
        public String appClass = "";
        public String toClass = "";

        public WebView(String id, String urlStringId) {
            super("webViewController", id);
            this.urlStringId = urlStringId;
        }

        public Controller addToController(final Controller controller) {
            controller.addField("WebView", this.id, "annotationFindView");
            controller.imports("android.webkit.WebView", "android.webkit.WebViewClient");
            controller.addContent(this);
            if (handlesOAuth) controller.imports("android.net.Uri");
            return controller;
        }
    }

    static public class ListView extends AndroidView implements AddToController {
        public String stringArrayId;

        // TODO: Move logic into list builder
        public ListView(String id) {
            super("listViewController", id);
        }

        public ListView(String id, String stringArrayId) {
            this(id);
            this.stringArrayId = stringArrayId;
        }

        public Controller addToController(final Controller controller) {
            controller.addField("ListView", this.id, "annotationFindView");
            if (stringArrayId != null) controller.imports("android.widget.ArrayAdapter", "android.widget.ListView");
            controller.imports("android.widget.ListView");
            controller.addContent(this);
            return controller;
        }

    }

    static public class TextView extends AndroidView implements AddToController {
        public String value = "";
        public String eventClassName = "";
        public boolean doesReceiveEvent;
        public boolean hasValue;
        public boolean isFragmentToFragment;
        public boolean hasLabelText;

        public TextView(String id, boolean labelEmpty) {
            super("textViewController", id);
            this.hasLabelText = !labelEmpty;
        }

        @Override
        public Controller addToController(Controller controller) {
            if (!hasLabelText) {
                controller.addField("TextView", this.id, "annotationFindView");
            }
            controller.imports("android.widget.TextView");
            if (doesReceiveEvent) {
                controller.imports("de.greenrobot.event.EventBus");
            }
            if (isFragmentToFragment) {
                JavaFunction function;
                function = new JavaFunction("onEvent");
                function.addParameter(eventClassName, "event");
                function.modifier = "public";
                function.addContent(this);
                controller.addContent(function);
            }
            if (hasValue && !isFragmentToFragment) {
                JavaFunction function;
                function = new JavaFunction(id + "Load");
                function.addAnnotation(new Template("annotationsAfter"));
                function.addContent(this);
                controller.addContent(function);
            }
            return controller;
        }
    }

    static public class EditText extends AndroidView implements AddToController {

        public EditText(String id) {
            super("editTextController", id);
        }

        @Override
        public Controller addToController(Controller controller) {
            controller.addField("EditText", this.id, "annotationFindView");
            controller.imports("android.widget.EditText");
            controller.addContent(this);
            return controller;
        }
    }

    static public class ImageView extends AndroidView implements AddToController {

        public ImageView(String id) {
            super("imageViewController", id);
        }

        @Override
        public Controller addToController(Controller controller) {
            controller.addField("ImageView", this.id, "annotationFindView");
            controller.imports("android.widget.ImageView");
            controller.addContent(this);
            return controller;
        }
    }

    static public class Map extends AndroidView implements AddToController {

        public boolean isNotInteractive = false;
        public boolean hasNoDynamic = false;
        public Collection<StaticMarker> staticMarkers = new ArrayList<>();
        public List<IntermediateModel.MapView.PolyLine> polyLines = new ArrayList<>();
        public LatLong initialZoom = new LatLong();
        public String handler;

        @STIgnore
        public String MAPS_PACKAGE = "com.google.android.gms.maps";

        @STIgnore
        public static final String MAP_VARIABLE = "googleMap";

        static public class LatLong {
            public double latitude;
            public double longitude;
        }

        static public class StaticMarker extends LatLong {
            public String title;
            public String titleStringId;
            public String snippet;
            public String snippetStringId;
        }

        static public class DynamicMarker extends Template {
            public Field title;
            public Field latitude;
            public Field longitude;

            public DynamicMarker() {
                super("displayMarkers");
            }
        }

        public Map(String id) {
            super("mapController", id);
        }

        @Override
        public Controller addToController(Controller controller) {
            controller.addField("GoogleMap", MAP_VARIABLE);
            if (polyLines.size() > 0)
                controller.imports(MAPS_PACKAGE + ".model.PolylineOptions");
            staticMarkers.forEach(m -> {
                controller.addField("String", m.title, new SimpleTemplate("annotationStringRes", m.titleStringId));
                if (m.snippet != null) {
                    controller.addField("String", m.snippet, new SimpleTemplate("annotationStringRes", m.snippetStringId));
                }
            });
            controller.imports("org.androidannotations.annotations.res.StringRes");
            controller.addField("MapFragment", this.id, "annotationFindFragmentId");
            controller.imports(MAPS_PACKAGE + ".MapFragment",
                    MAPS_PACKAGE + ".model.MarkerOptions",
                    MAPS_PACKAGE + ".GoogleMap",
                    MAPS_PACKAGE + ".CameraUpdateFactory",
                    MAPS_PACKAGE + ".model.LatLng");
            if (isNotInteractive) controller.imports(MAPS_PACKAGE + ".UiSettings", MAPS_PACKAGE + ".model.Marker");

            JavaFunction mapHandler = new JavaFunction("initMap");
            mapHandler.addAnnotation(new Template("annotationsAfter"));
            mapHandler.addContent(this);
            controller.addContent(mapHandler);
            return controller;
        }
    }

    static public class Notification extends AndroidView implements AddToController {

        public String icon;
        public String functionName;

        public String idParam;
        public String titleParam;
        public String contentParam;
        public String to;

        public Notification() {
            super("notificationContent", "");
            this.idParam = "id";
            this.titleParam = "title";
            this.contentParam = "content";
        }

        @Override
        public Controller addToController(final Controller controller) {
            controller.imports("android.app.NotificationManager", "android.support.v4.app.NotificationCompat");
            Optional.ofNullable(to).ifPresent(t -> controller.imports("android.app.PendingIntent",
                    "android.content.Intent",
                    "android.support.v4.app.TaskStackBuilder"));
            JavaFunction function = new JavaFunction(functionName).addContent(this);
            function.addParameter("int", idParam);
            function.addParameter("String", titleParam);
            function.addParameter("String", contentParam);
            controller.addContent(function);
            return controller;
        }
    }
}
