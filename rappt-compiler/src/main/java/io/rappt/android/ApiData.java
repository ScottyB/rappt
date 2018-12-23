package io.rappt.android;

import java.util.ArrayList;
import java.util.List;

public class ApiData extends Template {
    public String variableName;
    public String functionName;
    public List<String> responseData;
    public String responseClass;
    public String localFile;
    public boolean isAuth;
    public boolean isList;


    public ApiData(String variableName, String functionName, boolean isAuth, String responseClass) {
        super("contentApiCall");
        this.variableName = variableName;
        this.functionName = functionName;
        this.isAuth = isAuth;
        this.responseClass = responseClass;

    }

    public ApiData addResponseData(String value) {
        if (responseData == null) {
            responseData = new ArrayList<>();
        }
        responseData.add(value);
        return this;
    }
}
