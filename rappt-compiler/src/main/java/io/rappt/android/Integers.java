package io.rappt.android;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Integers extends FileTemplate {

    public List<Pair<String, Number>> integerValues = new ArrayList<>();

    public Integers(Project project) {
        super("integers", project.valuesFolder + "integers.xml");
    }
}
