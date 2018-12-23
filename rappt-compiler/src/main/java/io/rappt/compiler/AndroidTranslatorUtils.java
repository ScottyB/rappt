package io.rappt.compiler;

import com.google.common.collect.Iterables;
import io.rappt.android.*;
import io.rappt.android.*;
import io.rappt.model.Resource;
import io.rappt.model.ValuePath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AndroidTranslatorUtils {


    public Controller createController(Project project, IntermediateModel.Screen interScreen) {
        Controller controller;
        if (interScreen.isFragment) {
            controller = Controller.newFragmentInstance(project, interScreen.view.viewControllerName, interScreen.view.layoutFile);
        } else {
            controller = Controller.newActivityInstance(project, interScreen.view.viewControllerName, interScreen.view.layoutFile);
        }
        return controller;
    }

    public JavaFunction addApiFunctionCall(IntermediateModel.Api api, IntermediateModel.Resource resource, Resource r, boolean isOffline) {
        ApiData apiData = new ApiData(api.variableName, resource.functionName, r.isAuthEndPoint, resource.responseObject);
        apiData.isList = r.isList;
        if (r.hasToPrepareRequest()) {
            apiData.addResponseData(resource.fieldName);
        }
        if (isOffline) apiData.localFile = resource.mockFileName;
        JavaFunction javaFunction = new JavaFunction(resource.functionName, resource.responseObject)
                .addContent(apiData).setModifier("public");
        if (r.hasToPrepareRequest()) {
            javaFunction.addParameter(resource.responseObject, resource.fieldName);
        }
        if (r.urlParam != null) {
            javaFunction.addParameter("String", r.urlParam);
            apiData.addResponseData(r.urlParam);
        }
        return javaFunction;
    }

    // TODO: Move types to String Template
    static public String fieldTypeToJavaType(ValuePath.JsonPath value) {
        FormatUtils utils = new FormatUtils();
        String javaType = "", fieldName = utils.formatVariableName(value);
        switch (value.fieldType) {
            case OBJECT:
                javaType = FormatUtils.formatDataClassName(fieldName);
                break;
            case LIST:
                javaType = "List<" + FormatUtils.formatDataClassName(fieldName) + ">";
                break;
            case EMAIL:
            case PHONE:
            case PASSWORD:
                javaType = "String";
                break;
            case DATE:
                javaType = "Date";
                break;
            case LATLONG:
                javaType = "double";
                break;
            default:
                javaType = "String";
        }
        return javaType;
    }

    public SimpleTemplate setupHTTPAnnotation(Resource ep) {
        String templateName = "";
        switch (ep.HTTPMethod) {
            case POST:
                templateName = "annotationPost";
                break;
            default:
                templateName = "annotationGet";
                break;
        }
        return new SimpleTemplate(templateName, ep.endPoint);
    }

    static public Field buildField(String variableName, String objectClass, IntermediateModel.Field field) {
        List<IntermediateModel.Field> vp = new ArrayList<>();
        vp.add(field);
        return buildField(variableName, objectClass, vp);
    }

    // Returns field used to retrieve data from json response
    static public Field buildField(String variableName, String objectClass, Collection<IntermediateModel.Field> fields) {
        Field fPath = new Field();
        fPath.objectClass = objectClass;
        fPath.objectVariableName = variableName;
        FormatUtils utils = new FormatUtils();
        fields.forEach(v -> {
            List<ValuePath.JsonPath> jsonPaths = v.vp.path;
            List<Field.Path> paths = new ArrayList<>();
            int size = jsonPaths.size();
            for (ValuePath.JsonPath p : jsonPaths.subList(0, size)) {
                paths.add(new Field.Path(utils.formatVariableName(p), "", p.fieldType == ValuePath.FIELD_TYPE.LIST));
            }
            Field.FieldPath fp = new Field.FieldPath();
            fp.values = paths;

            if (!jsonPaths.isEmpty()) {
                ValuePath.JsonPath path = Iterables.getLast(jsonPaths);
                fp.isImage = path.fieldType == ValuePath.FIELD_TYPE.IMAGE;
                fp.isString = path.fieldType == ValuePath.FIELD_TYPE.STRING;
            }
            Field.FieldAndElement fieldAndElement = new Field.FieldAndElement();
            fieldAndElement.field = fp;
            fieldAndElement.elementId = v.elementId;
            fPath.fieldAndElementList.add(fieldAndElement);

        });
        return fPath;
    }
}
