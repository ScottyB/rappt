package io.rappt.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Strings extends FileTemplate {

    // Set values via addStringValue
    public List<StringPair> stringValues = new ArrayList<>();
    public Collection<StringArrayPair> stringArrays = new ArrayList<>();

    public Strings(Project project) {
        super("strings", project.valuesFolder + "strings.xml");
        addStringValue("app_name", project.projectName);
    }

    public void addStringValue(String id, String value) {
        StringPair sp = new StringPair();
        sp.id = id;
        sp.value = value;
        stringValues.add(sp);
    }

    public void addStringValues(String id, String[] values) {
        StringArrayPair sp = new StringArrayPair();
        sp.id = id;
        sp.values = new String[values.length];
        System.arraycopy(values, 0, sp.values, 0, values.length);
        stringArrays.add(sp);
    }

    public class StringPair {
        public String id;
        public String value;
    }

    public class StringArrayPair {
        public String id;
        public String[] values;
    }

}
