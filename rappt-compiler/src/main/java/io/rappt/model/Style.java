package io.rappt.model;

public class Style implements PIM {

    public String styleId;
    public String styleName;

    public Style(String id, String styleName) {
        this.styleId = id;
        this.styleName = styleName;
    }
}
