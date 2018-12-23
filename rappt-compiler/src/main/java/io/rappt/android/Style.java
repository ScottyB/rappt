package io.rappt.android;

import java.util.HashSet;
import java.util.Set;

public class Style extends FileTemplate {

    public Set<String> styles = new HashSet<>();
    public boolean hasButtons;

    public Style(Project project) {
        super("styles", project.valuesFolder + "styles.xml");
    }
}


