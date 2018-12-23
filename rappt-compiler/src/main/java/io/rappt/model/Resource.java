package io.rappt.model;

import io.rappt.compiler.FormatUtils;
import org.apache.commons.lang3.text.WordUtils;
import io.rappt.compiler.IntermediateModel;

import java.util.HashMap;
import java.util.Map;

public class Resource implements PIM {

    public String id;
    public String endPoint = "";

    public String urlParam;
    public HTTP_METHOD HTTPMethod = HTTP_METHOD.GET;
    public boolean isList;

    // Preference name, ValuePath saved
    public Map<String, ValuePath> saveValuePaths = new HashMap<>();

    public boolean isAuthEndPoint = false;

    public ValuePath tokenFieldPath = new ValuePath();
    public ValuePath stateFieldPath = new ValuePath();

    public enum HTTP_METHOD {
        GET, POST, PUT, DELETE
    }

    public String getMethod() {
        return HTTPMethod.toString();
    }

    public Resource(String id, String httpMethod) {
        this.HTTPMethod = HTTP_METHOD.valueOf(httpMethod);
        this.id = id;
    }

    public boolean hasToPrepareRequest() {
        return (HTTPMethod == HTTP_METHOD.POST) || (HTTPMethod == HTTP_METHOD.PUT);
    }

    @Override
    public void accept(PIMVisitor visitor) {
        if (stateFieldPath != null) stateFieldPath.accept(visitor);
        tokenFieldPath.accept(visitor);
        visitor.visit(this);
    }

    public IntermediateModel.Resource transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
        IntermediateModel.Resource resource = new IntermediateModel.Resource();
        resource.functionName = utils.formatVariable(this.id);
        String type = utils.formatResourceURL(this);
        if (this.tokenFieldPath.path.size() > 0) resource.responseValuePaths.add(this.tokenFieldPath);
        if (this.stateFieldPath.path.size() > 0) {
            resource.responseValuePaths.add(this.stateFieldPath);
        }
        resource.responseClassName = type;
        resource.responseObject = this.isList ? "List<" + type + ">" : type;
        resource.returnList = this.isList;
        resource.fieldName = utils.formatVariable(resource.responseClassName);
        resource.mockFileName = WordUtils.capitalize(resource.functionName);

        IntermediateModel.Field f = new IntermediateModel.Field();
        f.vp = this.tokenFieldPath;
        resource.tokenFieldPath = f;

        IntermediateModel.Field field = new IntermediateModel.Field();
        field.vp = this.stateFieldPath;
        resource.stateFieldPath = field;
        return resource;
    }
}
