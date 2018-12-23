package io.rappt.android;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Colours extends FileTemplate {

    public List<Pair<String, String>> colours = new ArrayList<>();

    public Colours(Project project) {
        super("colours", project.valuesFolder + "colors.xml");
    }
}
