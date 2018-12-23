package io.rappt.android;

import java.util.List;

public class Parse extends Template {
    public String className;
    public String variableName;
    public List<String> classes;
    public String clientKey;
    public String appId;

    public Parse(String className, String variableName, List<String> classes, String clientKey, String appId) {
        super("parseConstructor");
        this.className = className;
        this.variableName = variableName;
        this.classes = classes;
        this.clientKey = clientKey;
        this.appId = appId;
    }
}
