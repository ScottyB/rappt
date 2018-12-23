package io.rappt.compiler;

import io.rappt.android.*;
import io.rappt.model.Instruction;
import io.rappt.android.*;
import io.rappt.model.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

// TODO simplify functions in this class
public class InstructionBuilder {

    // DON'T CHANGE THESE: Naming convention
    public static final String REQUEST_FUNCTION = "call";
    public static final String RESPONSE_FUNCTION = "handleResponse";
    public static final String BUILD_FUNCTION = "buildData";
    public static final String LOADING_STARTED_FUNCTION = "loadingStarted";
    public static final String LOADING_FINISHED_FUNCTION = "loadingFinished";
    public static final String HIDE_KEYBOARD_FUNCTION = "hideKeyBoard";


    static public class InstructionBuilderData {
        public AndroidModel model;
        public Event event = new Event();
        public IntermediateModel.Screen screen;
        public String initFunctionName;
        public String annotationTemplate;
        public IntermediateModel interModel;
        public Controller controller;
        public boolean isOptionsItem;
        public IntermediateModel.Event interEvent;
        public boolean isViewController;
    }

    static class LoadAdapter extends Template {

        public String dataClass;
        public String listId;
        public String noDataStringId = AndroidModel.MESSAGE_NO_DATA_ID;

        // TODO: Needs to be field!!!
        public String listField;

        public LoadAdapter(String fieldName, String listId) {
            super("loadAdapter");
            this.listId = listId;
            this.dataClass = fieldName;
        }
    }

    static class DataFromForm extends Template {
        public Field field;

        DataFromForm(Field field) {
            super("buildDataFromForm");
            this.field = field;
        }
    }

    static class PutPreference extends Template {
        public Field field;

        public PutPreference(Field field) {
            super("putPreference");
            this.field = field;
        }
    }

    static class GetPreference extends Template {
        public String fieldId;
        public String preferenceId;

        public GetPreference(String fieldId, String preferenceId) {
            super("getPreference");
            this.fieldId = fieldId;
            this.preferenceId = preferenceId;
        }
    }

    static class RemovePreference extends Template {
        public String preferenceId;

        RemovePreference(String preferenceId) {
            super("removePreference");
            this.preferenceId = preferenceId;
        }
    }

    static class DataRequestFunction extends Template {
        public String api;
        public String dataCallFunction;
        public String loadDataFunction;
        public String responseHandler;
        public String parameter;

        DataRequestFunction() {
            super("buildRequestFunction");
        }
    }

    static class DataFromTag extends Template {
        public String className;
        public String variableName;

        public DataFromTag(String className, String variableName) {
            super("dataFromTag");
            this.className = className;
            this.variableName = variableName;
        }
    }

    public static class FunctionCall extends Template {

        public String functionName;
        public Collection<String> parameters = new ArrayList<>();

        public FunctionCall(String functionName) {
            super("functionCall");
            this.functionName = functionName;
        }
    }

    static class NotificationFromFields extends Template {
        public String functionName;
        public String id;
        public Field title;
        public Field content;

        public NotificationFromFields() {
            super("callNotification");
        }
    }

    static class GetLocation extends Template {
        public GetLocation() {
            super("getLocation");
        }
    }

    private JavaFunction buildRequestFunction(IntermediateModel interModel, Instruction.Call call, boolean doesLoadDataFromGui) {
        JavaFunction javaFunction = new JavaFunction(REQUEST_FUNCTION + call.id)
                .addAnnotation(new Template("annotationsBackground"));
        IntermediateModel.Resource resource = interModel.resources.get(call.apiId, call.resourceId);
        DataRequestFunction data = new DataRequestFunction();
        data.api = interModel.intermediateApis.get(call.apiId).variableName;
        data.dataCallFunction = resource.functionName;
        data.parameter = resource.parameterVariable;
        if (doesLoadDataFromGui) data.loadDataFunction = BUILD_FUNCTION + call.id;
        data.responseHandler = RESPONSE_FUNCTION;
        javaFunction.addContent(data);
        javaFunction.addContent(new FunctionCall(LOADING_FINISHED_FUNCTION));
        return javaFunction;
    }

    private JavaFunction buildObjectFromFormFunction(InstructionBuilderData data, Instruction.Call call) {
        JavaFunction javaFunction = new JavaFunction(BUILD_FUNCTION + call.id);
        IntermediateModel.Resource resource = data.interModel.resources.get(call.apiId, call.resourceId);
        Field field = AndroidTranslatorUtils.buildField(resource.fieldName, resource.responseClassName, data.screen.formFields);
        DataFromForm form = new DataFromForm(field);
        javaFunction.addContent(form).returnType = resource.responseObject;
        return javaFunction;
    }

    // TODO: move formatting to intermediate model
    private JavaFunction buildResponseHandler(InstructionBuilderData data, boolean hasList) {
        JavaFunction javaFunction = new JavaFunction(RESPONSE_FUNCTION)
                .addAnnotation(new Template("annotationsUiThread"));
        Instruction.Call call = data.interEvent.callInstruction;
        IntermediateModel.Call interCall = (IntermediateModel.Call) data.interEvent.allInstructions.get(call.id);
        IntermediateModel.Resource resource = data.interModel.resources.get(call.apiId, call.resourceId);
        javaFunction.addParameter(resource.responseObject, AndroidModel.DATA_VARIABLE);

        if (interCall instanceof IntermediateModel.CallToGET) {
            // TODO: need to attach resource from AppModel to IntermediateModel
            if (resource.returnList) {
                data.controller.imports("java.util.List");
            }

            if (data.screen.doesUseDataObject) {
                data.controller.addField(resource.responseObject, AndroidModel.DATA_VARIABLE);
                javaFunction.addContent(new Template("storeLoadedData"));
            }
            if (hasList) {
                LoadAdapter adapter = new LoadAdapter(resource.fieldName, data.screen.dynamicListVar);
                IntermediateModel.ListField lf = data.interModel.resourceListFields.get(call.resourceId);
                if (lf != null && !lf.listClassName.equals(resource.responseClassName)) {
                    adapter.listField = lf.listClassVariable;
                }
                javaFunction.addContent(adapter);
            } else {
                Field field = AndroidTranslatorUtils.buildField(AndroidModel.DATA_VARIABLE, resource.responseClassName, data.screen.view.pathsToShow);
                javaFunction.addContent(new ObjectTemplate("displayData", field));

                // For loading dynamic map markers
                for (IntermediateModel.MapView.DynamicMapViewMarker dm : data.screen.view.mapView.dynamicMarkers) {
                    UIControls.Map.DynamicMarker marker = new UIControls.Map.DynamicMarker();
                    marker.latitude = AndroidTranslatorUtils.buildField(AndroidModel.DATA_VARIABLE, resource.responseClassName, dm.latitude);
                    marker.title = AndroidTranslatorUtils.buildField(AndroidModel.DATA_VARIABLE, resource.responseClassName, dm.title);
                    marker.longitude = AndroidTranslatorUtils.buildField(AndroidModel.DATA_VARIABLE, resource.responseClassName, dm.longitude);
                    javaFunction.addContent(marker);
                }
            }
        }
        for (IntermediateModel.Field f : resource.preferenceFields) {
            Field field = AndroidTranslatorUtils.buildField(AndroidModel.DATA_VARIABLE, resource.responseClassName, f);
            javaFunction.addContent(new PutPreference(field));
        }
        for (Instruction i : data.interEvent.postCall) {
            javaFunction = addInstruction(javaFunction, data, i, data.interEvent.allInstructions.get(i.id));
        }
        return javaFunction;
    }

    // Currently only a single call can be in an instruction block
    private JavaFunction addInstruction(JavaFunction oldJavaFunction, final InstructionBuilderData data, Instruction i, IntermediateModel.Instruction interInstruction) {
        JavaFunction javaFunction = oldJavaFunction;
        if (i instanceof Instruction.Navigate) {
            IntermediateModel.Navigate instruction = (IntermediateModel.Navigate) interInstruction;
            boolean isFromFragment = data.screen.isFragment && !data.isViewController;
            // todo: hack
            boolean isFragmentToFragment = false;
            if (data.interModel.isFragmentToFragment.containsKey(((Instruction.Navigate) i).parameterId)) {
                isFragmentToFragment = data.interModel.isFragmentToFragment.get(((Instruction.Navigate) i).parameterId);
            }
            IntentModel intent = IntentModel.buildNavigationIntent(data.event.doesCloseActivity, isFromFragment, i, data.interEvent.allInstructions.get(i.id), data.isViewController, isFragmentToFragment);
            if (instruction.doesRequireEventBus) {
                IntentModel.To to = (IntentModel.To) intent.template;
                to.hasEvent = true;
                to.eventClassName = instruction.eventBusClassName;
                data.controller.imports("de.greenrobot.event.EventBus", data.model.project.dataPackage + ".*");
            }
            if (!data.screen.listItemDataClassName.isEmpty() && !data.isOptionsItem && !data.isViewController)
                javaFunction.addParameter(data.screen.listItemDataClassName, AndroidModel.DATA_VARIABLE);
            if (data.isViewController)
                javaFunction.addContent(new DataFromTag(data.screen.listItemDataClassName, AndroidModel.DATA_VARIABLE));
            javaFunction.addContent(intent);
            data.controller.imports(data.model.project.activityPackage + ".*");
            instruction.functionParameters.forEach(p -> javaFunction.addParameter(p.getLeft(), p.getRight()));
        }
        if (i instanceof Instruction.Url) {
            IntentModel intent = IntentModel.buildUrlIntent(data.event.doesCloseActivity, data.screen.isFragment, data.interEvent.allInstructions.get(i.id));
            data.controller.imports("android.content.Intent", "android.net.Uri");
            javaFunction.addContent(intent);
        }
        if (i instanceof Instruction.GetPreference) {
            IntermediateModel.GetPreference get = (IntermediateModel.GetPreference) interInstruction;
            javaFunction.addContent(new GetPreference(get.fieldId, get.prefId));
        }
        if (i instanceof Instruction.RemovePreference) {
            IntermediateModel.RemovePreference remove = (IntermediateModel.RemovePreference) interInstruction;
            javaFunction.addContent(new RemovePreference(remove.prefId));
        }
        if (i instanceof Instruction.ShowToast) {
            data.controller.imports("android.widget.Toast");
            IntermediateModel.ShowToast remove = (IntermediateModel.ShowToast) interInstruction;
            javaFunction.addContent(new SimpleTemplate("showToast", remove.stringId));
        }
        if (i instanceof Instruction.StaticNotification) {
            IntermediateModel.StaticNotification notification = (IntermediateModel.StaticNotification) interInstruction;
            Queue<String> values = new LinkedList<>();
            values.add("0");
            values.add(notification.titleStringRes);
            values.add(notification.contentStringRes);
            FunctionCall functionCall = new FunctionCall(notification.functionName);
            functionCall.parameters = values;
            javaFunction.addContent(functionCall);
            data.controller.addStringRes(notification.contentStringRes, notification.contentStringId);
            data.controller.addStringRes(notification.titleStringRes, notification.titleStringId);
        }
        if (i instanceof Instruction.DynamicNotification) {
            IntermediateModel.DynamicNotification notification = (IntermediateModel.DynamicNotification) interInstruction;
            Field titleField = AndroidTranslatorUtils.buildField(AndroidModel.DATA_VARIABLE, "", notification.titleField);
            Field contentField = AndroidTranslatorUtils.buildField(AndroidModel.DATA_VARIABLE, "", notification.contentField);
            NotificationFromFields notificationFromFields = new NotificationFromFields();
            notificationFromFields.functionName = notification.functionName;
            notificationFromFields.title = titleField;
            notificationFromFields.content = contentField;
            notificationFromFields.id = "0";
            javaFunction.addContent(notificationFromFields);
        }
        if (i instanceof Instruction.CurrentLocation) {
            data.controller.imports("com.google.android.gms.common.ConnectionResult", "android.content.IntentSender", "android.app.Activity", "android.content.Intent", "android.location.Location",
                    "com.google.android.gms.common.GooglePlayServicesClient", "com.google.android.gms.location.LocationClient", "android.os.Bundle", "android.util.Log", "com.google.android.gms.common.ConnectionResult", "android.widget.Toast");
            data.controller.addField("final static int", "CONNECTION_FAILURE_RESOLUTION_REQUEST", "", "9000");
            data.controller.classImplements.add("GooglePlayServicesClient.ConnectionCallbacks");
            data.controller.classImplements.add("GooglePlayServicesClient.OnConnectionFailedListener");
            data.controller.addField("static String", "GOOGLE_PLAY_SERVICE", "", "\"GOOGLE PLAY\"");
            data.controller.addField("LocationClient", "mLocationClient");
            data.controller.addContent(new Template("currentLocation"));
            data.controller.addToOnCreate(new Template("contentContextLocation"));
            javaFunction.addContent(new GetLocation());
        }

        return javaFunction;
    }

    // Currently only a single call can be in an instruction block
    private JavaFunction addCallInstruction(final JavaFunction oldJavaFunction, final InstructionBuilderData data) {
        JavaFunction javaFunction = oldJavaFunction;
        Instruction.Call call = data.interEvent.callInstruction;
        IntermediateModel.Call interCall = (IntermediateModel.Call) data.interEvent.allInstructions.get(call.id);
        javaFunction.addContent(new FunctionCall(REQUEST_FUNCTION + call.id));
        javaFunction.addContent(new FunctionCall(LOADING_STARTED_FUNCTION));

        // TODO: fix check for list
        if (data.screen.doesShowList)
            javaFunction.addContent(new SimpleTemplate("setAdapter", data.screen.dynamicListVar));

        if (interCall instanceof IntermediateModel.CallToGET) {
            boolean hasList = data.screen.doesShowList;
            if (hasList) data.controller.imports("android.view.View");
            data.controller.addContent(buildRequestFunction(data.interModel, call, false));
            data.controller.addContent(buildResponseHandler(data, hasList));
        }
        if (interCall instanceof IntermediateModel.CallToPOST) {
            // TODO: Temporary hack
            if (!data.screen.hasInput) {
                javaFunction.addContent(new FunctionCall(HIDE_KEYBOARD_FUNCTION));
                data.controller.addContent(new Template("hideKeyboard"));
            }
            data.controller.addContent(buildRequestFunction(data.interModel, call, true));
            data.controller.addContent(buildResponseHandler(data, false));
            data.controller.addContent(this.buildObjectFromFormFunction(data, call));
            data.controller.imports("android.view.inputmethod.InputMethodManager");
        }
        return javaFunction;
    }

    public JavaClass processInstructions(InstructionBuilderData data) {
        JavaFunction javaFunction = new JavaFunction(data.initFunctionName);
        if (!data.annotationTemplate.isEmpty()) javaFunction.addAnnotation(new Template(data.annotationTemplate));
        for (Instruction i : data.interEvent.instructions) {
            javaFunction = addInstruction(javaFunction, data, i, data.interEvent.allInstructions.get(i.id));
        }
        if (data.interEvent.callInstruction != null) javaFunction = addCallInstruction(javaFunction, data);
        data.controller.addContent(javaFunction);
        boolean addContext = data.interEvent.instructions.stream()
                .anyMatch(p -> p instanceof Instruction.Navigate || p instanceof Instruction.ShowToast);
        if (addContext && data.isViewController) {
            data.controller.addField("Context", Controller.CONTEXT_VARIABLE, "", "getContext()");
        }
        return data.controller;
    }
}
