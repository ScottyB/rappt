package io.rappt.android;

public class SimpleTemplate extends Template {
    public String templateValue = "";

    public SimpleTemplate(String templateName, String templateValue) {
        super(templateName);
        this.templateValue = templateValue;
    }
}
