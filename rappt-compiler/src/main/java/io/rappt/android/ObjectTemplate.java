package io.rappt.android;

// A wrapper object to hold a template so that in the String Templates
// a list can contain
public class ObjectTemplate extends Template {

    public Object templateObject;

    public ObjectTemplate(String templateName, Object template) {
        super(templateName);
        this.templateObject = template;
    }
}
