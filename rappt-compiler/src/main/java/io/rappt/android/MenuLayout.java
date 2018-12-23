package io.rappt.android;

import io.rappt.compiler.IntermediateModel;

import java.util.ArrayList;
import java.util.List;

public class MenuLayout extends FileTemplate {

    public List<IntermediateModel.Action> actionItems = new ArrayList<>();

    public MenuLayout(final Project project, String outputFileName) {
        super("menuLayout", project.menuFolder + outputFileName + ".xml");
    }
}
