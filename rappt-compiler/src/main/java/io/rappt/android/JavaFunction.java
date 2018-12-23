package io.rappt.android;

import java.util.ArrayList;
import java.util.List;

// Object to be passed into a function
public class JavaFunction extends Template {
    public String functionName;
    public List<Template> functionContent = new ArrayList<>();
    public List<Template> functionAnnotations = new ArrayList<>();
    public List<Parameters> parameters = new ArrayList<>();
    public String functionThrows;
    public String returnType = "void";
    public String modifier = "";
    public boolean isInterface;

    public JavaFunction(String functionName) {
        super("javaFunction");
        this.functionName = functionName;
    }

    public JavaFunction(String functionName, String returnType) {
        this(functionName);
        this.returnType = returnType;
    }

    public JavaFunction addParameter(String type, String name) {
        parameters.add(new Parameters(type, name));
        return this;
    }

    public JavaFunction addParameter(String type, String name, String template) {
        if (template.isEmpty())
            parameters.add(new Parameters(type, name));
        else
            parameters.add(new Parameters(type, name, new Template(template)));
        return this;
    }

    public JavaFunction addParameter(String type, String name, Template template) {
        parameters.add(new Parameters(type, name, template));
        return this;
    }

    public JavaFunction addContent(Template content) {
        functionContent.add(content);
        return this;
    }

    public JavaFunction addAnnotation(Template simpleTemplate) {
        functionAnnotations.add(simpleTemplate);
        return this;
    }

    public JavaFunction throwException(String e) {
        this.functionThrows = e;
        return this;
    }

//    static public JavaFunction onEventFunction(String datatype, String to, boolean isFragment, Field field, String id, Link.LINK_TYPE type) {
//        List<IntentModel.Extra> extras = new ArrayList<>();
//        extras.add(new IntentModel.Extra(id, field));
//        IntentModel intentModel = new IntentModel(to, isFragment, extras)
//                .setIntentDestination(type, field);
//        JavaFunction javaFunction = new JavaFunction("onEvent")
//                .addParameter(datatype, field.dataVariable)
//                .addContent(intentModel)
//                .setModifier("public");
//        return javaFunction;
//    }

    public JavaFunction setModifier(String modifier) {
        this.modifier = modifier;
        return this;
    }

    public class Parameters {
        public Template annotation;
        public String type;
        public String name;

        public Parameters(String type, String name) {
            this.type = type;
            this.name = name;
        }

        public Parameters(String type, String name, Template annotation) {
            this(type, name);
            this.annotation = annotation;
        }
    }
}
