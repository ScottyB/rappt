package io.rappt.android;

import java.util.ArrayList;
import java.util.Collection;

public class DynamicLayout extends FileTemplate {

    @STIgnore
    public String layout = "";

    public Collection<AndroidView> elements = new ArrayList<>();
    public boolean enableScroll;
    public String cardLayout;
    public boolean hasHeader;
    public boolean pullToRefresh;
    public Field showFields;
    public boolean hasMessage;

    public DynamicLayout(Project project, String layout) {
        super("dynamicLayout", project.layoutFolder + layout + ".xml");
    }

    public boolean hasLayout() {
        return !layout.isEmpty();
    }
}
