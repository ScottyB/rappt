package io.rappt.android;

public class StandardOnlineApiConstructor extends Template {

    public String className;

    public String apiName;
    public String apiClass;
    public String authClass;
    public String authToken;
    public Field authTokenPath;
    public String classVariable;

    public String apiKey;
    public String apiValue;

    public StandardOnlineApiConstructor(String className, String apiName, String apiClass) {
        super("standardOnlineApiConstructor");
        this.className = className;
        this.apiName = apiName;
        this.apiClass = apiClass;
    }

    public StandardOnlineApiConstructor addAuth(String authClass, String classVariable, String authToken, Field authTokenPath) {
        this.authClass = authClass;
        this.authToken = authToken;
        this.authTokenPath = authTokenPath;
        this.classVariable = classVariable;
        return this;
    }

    public static class OAuth extends Template {

        public String apiName;
        public String apiVariable;
        public String apiClass;

        public OAuth(String apiName, String apiVariable, String apiClass) {
            super("authenticationOAuth");
            this.apiName = apiName;
            this.apiVariable = apiVariable;
            this.apiClass = apiClass;
        }
    }
}
