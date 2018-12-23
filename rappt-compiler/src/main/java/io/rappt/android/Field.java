package io.rappt.android;


import java.util.ArrayList;
import java.util.List;

// Represents an object path to a value
// Path:  dataVariable.valueName.valueField
public class Field {

    public String objectClass;
    public String objectVariableName;
    public List<FieldAndElement> fieldAndElementList = new ArrayList<>();

    public static class FieldAndElement {
        public FieldPath field;

        // ElementId could be a UI element, a shared preference, parameter to pass to another screen etc.
        public String elementId;
    }

    static public class FieldPath {
        public boolean isString = true;
        public boolean isImage = false;
        public List<Path> values = new ArrayList<>();
    }

    static public class Path {
        public boolean isList;                  // .get(<listIndex>)
        public String listIndex;

        public String valueName;

        public Path(String valueName, String listIndex, boolean list) {
            isList = list;
            this.listIndex = listIndex;
            this.valueName = valueName;
        }
    }


}
