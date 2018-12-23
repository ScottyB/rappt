package io.rappt.model;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import io.rappt.android.UIControls;
import io.rappt.compiler.FormatUtils;
import io.rappt.compiler.IntermediateModel;

import java.util.*;

public class View extends UIBase {

    public Collection<Button> buttons = new ArrayList<>();
    public Collection<Label> labels = new ArrayList<>();
    public Collection<Image> images = new ArrayList<>();
    public Collection<TextInput> textInputs = new ArrayList<>();
    public Collection<Web> webs = new ArrayList<>();
    public Collection<StaticList> staticLists = new ArrayList<>();
    public Collection<DynamicList> dynamicLists = new ArrayList<>();
    public Optional<Map> map = Optional.empty();
    public String stateValue = "";
    public String layoutSpecification = "";

    public int numberOfElements() {
        return buttons.size() + labels.size() + images.size() + textInputs.size() + webs.size() + staticLists.size();
    }

    // For StringTemplate
    public Map getMap() {
        return map.orElseGet(null);
    }

    public View(String id) {
        super(id);
    }

    // for StringTemplate
    public List<UIBase> getElementsWithBehaviours() {
        List<UIBase> elements = new ArrayList<>();
        accept(p -> {
            if (p instanceof UIBase) {
                UIBase ui = (UIBase) p;
                if (!ui.onTouch.instructions.isEmpty()) {
                    elements.add(ui);
                }
            }
        });
        return elements;
    }

    @Override
    public void accept(PIMVisitor visitor) {
        buttons.forEach(b -> b.accept(visitor));
        webs.forEach(w -> w.accept(visitor));
        staticLists.forEach(l -> l.accept(visitor));
        dynamicLists.forEach(l -> l.accept(visitor));
        labels.forEach(l -> l.accept(visitor));
        images.forEach(p -> p.accept(visitor));
        onLoad.accept(visitor);
        map.ifPresent(m -> m.accept(visitor));
        visitor.visit(this);
    }


    public static class Button extends UIBase {
        public String label = "";
        public String styleName = "";

        public Button(String id) {
            super(id);
        }

        @Override
        public void accept(PIMVisitor visitor) {
            super.accept(visitor);
            visitor.visit(this);

        }

        public IntermediateModel.Button transformComponent(FormatUtils utils) {
            IntermediateModel.Button uiField = new IntermediateModel.Button();
            uiField.formattedId = utils.formatId(this.id);
            uiField.stringId = utils.formatStringId(this.id);
            uiField.functionName = utils.formatId(this.id);
            return uiField;
        }
    }

    public static class Label extends UIBase {
        public String label = "";
        public Source source;
        public Optional<ValuePath> valuePath = Optional.empty();
        public boolean isPassed;
        public String parameterId = "";

        public Label(String id) {
            super(id);
        }

        public String styleName = "";


        @Override
        public void accept(PIMVisitor visitor) {
            valuePath.ifPresent(v -> v.accept(visitor));
            visitor.visit(this);
        }

        public IntermediateModel.Label transformComponent(FormatUtils utils) {
            IntermediateModel.Label uiField = new IntermediateModel.Label();
            if (!this.label.isEmpty()) {
                uiField.formattedId = utils.formatId(this.id);
                uiField.stringId = utils.formatStringId(this.id);
                uiField.variable = utils.formatVariableResource(this.id);
                uiField.value = uiField.variable;
            } else {
                uiField.formattedId = utils.formatViewId(this.id);
                uiField.value = utils.formatPassedVariable(this.id);
                uiField.hasPassedValue = this.isPassed;
            }
            return uiField;
        }

        // for StringTemplate
        public ValuePath getValuePath() {
            return this.valuePath.filter(vp -> !vp.path.isEmpty()).orElseGet(null);
        }
    }

    public static class Image extends UIBase {
        public String image = "";
        public ValuePath valuePath = new ValuePath();
        public Source source;
        public String styleName = "";


        public Image(String id, String image) {
            super(id);
            this.image = image;
        }

        public Image(String id, ValuePath valuePath) {
            super(id);
            this.valuePath = valuePath;
        }

        public Image(String id) {
            super(id);
        }

        @Override
        public void accept(PIMVisitor visitor) {
            if (valuePath != null) {
                valuePath.accept(visitor);
            }
            visitor.visit(this);
        }

        public IntermediateModel.Image transformComponent(FormatUtils utils) {
            IntermediateModel.Image uiField = new IntermediateModel.Image();
            uiField.formattedId = utils.formatViewId(id);
            uiField.imageVariable = FilenameUtils.getBaseName(this.image);
            return uiField;
        }

        // for StringTemplate
        public ValuePath getValuePath() {
            if (this.valuePath.path.isEmpty()) {
                return null;
            }
            return this.valuePath;
        }
    }

    public static class TextInput extends UIBase {
        public String hintText;
        public ValuePath valuePath;

        public String styleName = "";

        public TextInput(String id) {
            super(id);
        }

        // for StringTemplate
        public ValuePath getValuePath() {
            if (this.valuePath.path.isEmpty()) {
                return null;
            }
            return this.valuePath;
        }
    }

    public static class Web extends UIBase {
        public String url = "";
        public String apiId = "";
        public String toId = "";

        public Web(String id) {
            super(id);
        }

        @Override
        public void accept(PIMVisitor visitor) {
            visitor.visit(this);
            super.accept(visitor);
        }
    }

    public static class Action extends UIBase {

        public String icon = "";
        public String label = "";

        public Action(String id) {
            super(id);
        }

        @Override
        public void accept(PIMVisitor visitor) {
            super.accept(visitor);
            visitor.visit(this);
        }

        public IntermediateModel.Action transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model, String id) {
            String menuFile = utils.formatLayoutString(id);
            String functionName = utils.formatId(this.id);
            String drawable = this.icon.contains(".") ? this.icon.split("\\.")[0] : this.icon;
            IntermediateModel.Action action = new IntermediateModel.Action(functionName, menuFile, drawable);
            action.stringId = utils.formatMenuStringId(id, this.id);
            action.event = this.onTouch.transformComponent(utils, appModel, model);
            model.intermediateFileMenu.put(menuFile, action);
            model.intermediateActions.put(this.id, action);
            model.strings.add(new ImmutablePair<>(action.stringId, this.label));
            return action;
        }

        public IntermediateModel transformModel(final FormatUtils utils, final IntermediateModel model) {
            return this.onTouch.transformModel(utils, model);
        }
    }

    public static class StaticList extends UICollection {

        public List<String> staticContent = new ArrayList<>();
        public String itemName;
        public String staticContentVariable;


        public StaticList(String id) {
            super(id);
        }

        @Override
        public void accept(PIMVisitor visitor) {
            super.accept(visitor);
            visitor.visit(this);
        }
    }

    public static class DynamicList extends UICollection {

        public String apiId;
        public String resourceId;

        public ValuePath listFieldPath;

        public DynamicList(String id) {
            super(id);
        }

        @Override
        public void accept(PIMVisitor visitor) {
            super.accept(visitor);
            visitor.visit(this);
        }
    }

    public static class Map extends UIBase {

        public static final double DEFAULT_LAT = -37.702225;
        public static final double DEFAULT_LONG = 144.572020;

        public Event.OnMapClick onMapClick = new Event.OnMapClick();
        public boolean noInteractions = false;
        public List<StaticMarker> staticMarkers = new ArrayList<>();
        public List<DynamicMarker> dynamicMarkers = new ArrayList<>();
        public List<PolyLine> polyLines = new ArrayList<>();

        public static class StaticMarker {
            public String id = "";
            public String title = "";
            public String description = "";
            public Double longitude;
            public Double latitude;
        }

        public static class DynamicMarker {
            public String id;
            public ValuePath titlePath;
            public ValuePath descriptionPath;
            public ValuePath longitudePath;
            public ValuePath latitudePath;
        }

        public static class PolyLine {
            public String markerStartId;
            public String markerEndId;
        }

        public Map(String id) {
            super(id);
        }

        public IntermediateModel.MapView transformComponent(FormatUtils utils) {
            IntermediateModel.MapView uiField = new IntermediateModel.MapView();

            // Setup camera view
            if (!this.dynamicMarkers.isEmpty()) {
                View.Map.DynamicMarker camera = this.dynamicMarkers.get(this.dynamicMarkers.size() - 1);
                uiField.cameraLatPath = camera.latitudePath;
                uiField.cameraLongPath = camera.longitudePath;
            } else if (!this.staticMarkers.isEmpty()) {
                uiField.cameraLat = this.staticMarkers.get(0).latitude;
                uiField.cameraLong = this.staticMarkers.get(0).longitude;
            } else {
                uiField.cameraLat = DEFAULT_LAT;
                uiField.cameraLong = DEFAULT_LONG;
            }

            uiField.formattedId = utils.formatViewId(this.id);
            if (this.onMapClick.instructions.size() > 0)
                uiField.onClickFunction = utils.formatFunction(this.id);
            java.util.Map<String, StaticMarker> tempMarkers = new HashMap<>();

            this.staticMarkers.forEach(m -> {
                tempMarkers.put(m.id, m);
                IntermediateModel.MapView.StaticMapViewMarker marker = new IntermediateModel.MapView.StaticMapViewMarker();
                marker.stringId = utils.formatMarkerTitleStringId(m.id);
                if (!m.description.isEmpty()) {
                    marker.descriptionStingId = utils.formatMarkerDescriptionStringId(m.id);
                    marker.descriptionVariable = utils.formatVariable(m.id) + "Description";
                }
                marker.titleVariable = utils.formatVariable(m.id) + "Title";
                uiField.makers.put(m.id, marker);
            });

            this.dynamicMarkers.forEach(m -> {
                IntermediateModel.MapView.DynamicMapViewMarker dm = new IntermediateModel.MapView.DynamicMapViewMarker();

                IntermediateModel.Field f = new IntermediateModel.Field();
                f.vp = m.latitudePath;
                f.elementId = UIControls.Map.MAP_VARIABLE;
                dm.latitude = f;

                IntermediateModel.Field f1 = new IntermediateModel.Field();
                f1.vp = m.longitudePath;
                f1.elementId = UIControls.Map.MAP_VARIABLE;
                dm.longitude = f1;


                IntermediateModel.Field f2 = new IntermediateModel.Field();
                f2.vp = m.titlePath;
                f2.elementId = UIControls.Map.MAP_VARIABLE;
                dm.title = f2;

                uiField.dynamicMarkers.add(dm);
            });

            this.polyLines.forEach(p -> {
                IntermediateModel.MapView.PolyLine polyLine = new IntermediateModel.MapView.PolyLine();
                polyLine.startLat = tempMarkers.get(p.markerStartId).latitude;
                polyLine.startLong = tempMarkers.get(p.markerStartId).longitude;
                polyLine.endLat = tempMarkers.get(p.markerEndId).latitude;
                polyLine.endLong = tempMarkers.get(p.markerEndId).longitude;
                uiField.polyLines.add(polyLine);
            });
            return uiField;
        }

        @Override
        public void accept(PIMVisitor visitor) {
            onMapClick.accept(visitor);
            super.accept(visitor);
        }
    }

}
