package io.rappt.model;


import io.rappt.compiler.FormatUtils;
import io.rappt.compiler.IntermediateModel;

import java.util.ArrayList;
import java.util.List;

public class ValuePath implements PIM {

    // Stores the JSON path to showFields id
    // TODO: JsonPath only has one list at this time
    public List<JsonPath> path = new ArrayList<>();

    public ValuePath(List<JsonPath> path) {
        this.path = path;
    }

    public void addPrefixPath(List<JsonPath>pathToAdd) {
        List<JsonPath> newList = new ArrayList<>(pathToAdd);
        newList.addAll(path);
        path = newList;
    }

    public ValuePath() {
    }

    public ValuePath(FIELD_TYPE type, String name) {
        JsonPath jsonPath = new JsonPath();
        jsonPath.fieldType = type;
        jsonPath.fieldName = name;
        this.path.add(jsonPath);
    }


    public enum FIELD_TYPE {
        OBJECT, LIST, STRING, EMAIL, PHONE, PASSWORD, IMAGE, DATE, LATLONG
    }

    public JsonPath lastPath() {
        return path.isEmpty() ? new JsonPath() : path.get(path.size() - 1);
    }

    public static class JsonPath {
        public FIELD_TYPE fieldType = FIELD_TYPE.STRING;
        public String fieldName = "";

        public JsonPath() {
        }

        public JsonPath(String fieldName, String fieldType) {
            this.fieldType = FIELD_TYPE.valueOf(fieldType);
            this.fieldName = fieldName;
        }

        public IntermediateModel.JsonPath transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
            IntermediateModel.JsonPath jsonPath = new IntermediateModel.JsonPath();
            jsonPath.javaType = utils.formatFieldTypeToJavaType(this);
            jsonPath.isObjectType = jsonPath.initNewObject = !jsonPath.javaType.equals("String"); // TODO: Find a better way to do this
            jsonPath.variableName = utils.formatVariableName(this);
            jsonPath.jsonProperty = this.fieldName;
            return jsonPath;
        }

        // For StringTemplate
        public String getFieldType() {
            return this.fieldType.toString();
        }
    }
}
