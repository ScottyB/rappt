package io.rappt.model;

import io.rappt.compiler.FormatUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import io.rappt.compiler.IntermediateModel;
import io.rappt.compiler.NavigationFlowBuilder;

import java.util.ArrayList;
import java.util.List;

public abstract class Instruction implements PIM {

    public String id;

    public Instruction(String id) {
        this.id = id;
    }

    public abstract IntermediateModel.Instruction transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model);

    public IntermediateModel transformModel(final FormatUtils utils, final IntermediateModel model) {
        return model;
    }

    static public class Navigate extends Instruction {

        public String toScreenId = "";
        public String parameterId = "";
        public ValuePath fieldParameter = new ValuePath();
        public List<String> parameters = new ArrayList<>();

        public Navigate(String id, String toScreenId) {
            super(id);
            this.toScreenId = toScreenId;
        }

        @Override
        public void accept(PIMVisitor visitor) {
            fieldParameter.accept(visitor);
            visitor.visit(this);
        }

        @Override
        public IntermediateModel.Navigate transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.Navigate instruction = new IntermediateModel.Navigate();
            instruction.toClassName = NavigationFlowBuilder.toActivity(model, this);
            model.fetchFromScreen(this.id, this.toScreenId).ifPresent(s -> {
                if (s.sendsEventObjects.get(this.id)) {
                    instruction.doesRequireEventBus = true;
                    instruction.eventBusClassName = s.eventClassNames.get(this.id);
                    instruction.fromClassName = s.view.viewControllerName;
                }
            });
            if (this.fieldParameter.path.size() > 0) {
                IntermediateModel.Field f = new IntermediateModel.Field();
                f.elementId = utils.formatPassedVariable(this.parameterId);
                f.vp = this.fieldParameter;
                instruction.parameter = f;
            } else {
                this.parameters.stream().map(utils::formatPassedVariable).forEach(p -> {
                    instruction.functionParameters.add(new ImmutablePair<>("String", p));
                    instruction.formatedParameters.add(p);
                });
            }
            return instruction;
        }

        // for StringTemplate
        public String getParameterId() {
            if (parameterId.isEmpty()) {
                return null;
            }
            return parameterId;
        }

        // for StringTemplate
        public ValuePath getFieldParameter() {
            if (this.fieldParameter.path.isEmpty()) {
                return null;
            }
            return this.fieldParameter;
        }
    }

    static public class Url extends Instruction {
        public Url(String id, String url) {
            super(id);
            this.url = url;
        }

        public String url;

        @Override
        public void accept(PIMVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public IntermediateModel.Url transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.Url instruction = new IntermediateModel.Url();
            instruction.urlStringId = utils.formatStringId(this.id);
            return instruction;
        }

        @Override
        public IntermediateModel transformModel(final FormatUtils utils, final IntermediateModel interModel) {
            final IntermediateModel newModel = interModel;
            newModel.strings.add(new ImmutablePair<>(utils.formatStringId(this.id), this.url));
            return newModel;
        }
    }

    static public class Call extends Instruction {
        public String apiId;
        public String resourceId;

        public String parameter;

        public Call(String id, String apiId, String resourceId) {
            super(id);
            this.apiId = apiId;
            this.resourceId = resourceId;
        }

        @Override
        public void accept(PIMVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public IntermediateModel.Call transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.Call instruction;
            Api api = appModel.getApi(this.apiId);
            Resource.HTTP_METHOD method = api.resources.stream().filter(r -> r.id.equals(this.resourceId)).map(r -> r.HTTPMethod).findFirst().get();
            switch (method) {
                case POST:
                    instruction = new IntermediateModel.CallToPOST();
                    break;
                default: // GET
                    instruction = new IntermediateModel.CallToGET();
                    break;
            }
            return instruction;
        }
    }

    static public class GetPreference extends Instruction {
        public String viewId;
        public String preferenceId;

        public GetPreference(String id, String viewId, String preferenceId) {
            super(id);
            this.viewId = viewId;
            this.preferenceId = preferenceId;
        }

        @Override
        public void accept(PIMVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public IntermediateModel.GetPreference transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.GetPreference get = new IntermediateModel.GetPreference();
            get.fieldId = utils.formatId(this.viewId);
            get.prefId = utils.formatId(this.preferenceId);
            return get;
        }
    }

    static public class RemovePreference extends Instruction {

        public RemovePreference(String id) {
            super(id);
        }

        @Override
        public void accept(PIMVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public IntermediateModel.RemovePreference transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.RemovePreference remove = new IntermediateModel.RemovePreference();
            remove.prefId = this.id;
            return remove;
        }
    }

    public static class ShowToast extends Instruction {
        public String label = "";

        public ShowToast(String id, String label) {
            super(id);
            this.label = label;
        }

        @Override
        public IntermediateModel.ShowToast transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.ShowToast showToast = new IntermediateModel.ShowToast();
            showToast.stringId = utils.formatStringId(this.id);
            return showToast;
        }
    }

    static abstract public class Notification extends Instruction {
        public String to;
        public String icon;

        public Notification(String id) {
            super(id);
        }
    }

    static public class StaticNotification extends Notification {

        public String title;
        public String content;

        public StaticNotification(String id) {
            super(id);
        }

        @Override
        public IntermediateModel.StaticNotification transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.StaticNotification newStaticNotification = new IntermediateModel.StaticNotification();
            newStaticNotification.contentStringId = utils.formatStringIdContent(this.id);
            newStaticNotification.titleStringId = utils.formatStringIdTitle(this.id);
            newStaticNotification.contentStringRes = utils.formatVariableResourceContent(this.id);
            newStaticNotification.titleStringRes = utils.formatVariableResourceTitle(this.id);
            newStaticNotification.functionName = utils.formatFunction(this.id);
            return newStaticNotification;
        }
    }

    static public class DynamicNotification extends Notification {

        public ValuePath titlePath;
        public ValuePath contentPath;


        public DynamicNotification(String id) {
            super(id);
        }

        @Override
        public IntermediateModel.DynamicNotification transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.DynamicNotification newNotification = new IntermediateModel.DynamicNotification();
            newNotification.functionName = utils.formatFunction(this.id);
            IntermediateModel.Field titleField = new IntermediateModel.Field();
            titleField.elementId = utils.formatVariableName(this.titlePath.lastPath());
            titleField.vp = this.titlePath;
            newNotification.titleField = titleField;
            IntermediateModel.Field contentField = new IntermediateModel.Field();
            contentField.elementId = utils.formatVariableName(this.contentPath.lastPath());
            contentField.vp = this.contentPath;
            newNotification.contentField = contentField;
            return newNotification;
        }

    }


    public static class CurrentLocation extends Instruction {

        public CurrentLocation(String id) {
            super(id);
        }

        @Override
        public IntermediateModel.CurrentLocation transformComponent(FormatUtils utils, AppModel appModel, IntermediateModel model) {
            IntermediateModel.CurrentLocation newCurrentLocation = new IntermediateModel.CurrentLocation();
            return newCurrentLocation;
        }

        @Override
        public IntermediateModel transformModel(final FormatUtils utils, final IntermediateModel interModel) {
            final IntermediateModel newModel = interModel;
            newModel.app.doesRequireGooglePlayServices = true;
            newModel.app.doesRequireLocation = true;
            newModel.hasInternetAccess = true;
            newModel.strings.add(new ImmutablePair<>("google_play_connected", "Connected to GooglePlay"));
            newModel.strings.add(new ImmutablePair<>("google_play_disconnected", "Disconnected to GooglePlay"));
            newModel.strings.add(new ImmutablePair<>("google_play_failed", "Connection to GooglePlay failed"));
            newModel.strings.add(new ImmutablePair<>("google_play_issue_resolved", "Issue resolved"));
            newModel.strings.add(new ImmutablePair<>("google_play_client_disconnected", "Client disconnected, no resolution"));
            newModel.strings.add(new ImmutablePair<>("google_play_unknown", "Received an unknown activity request code: "));
            return newModel;
        }

    }

}
