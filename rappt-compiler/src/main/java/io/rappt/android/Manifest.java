package io.rappt.android;


import io.rappt.compiler.IntermediateModel;

import java.util.ArrayList;
import java.util.List;

public class Manifest extends FileTemplate {
    public String packageName = "";
    public String globalApplication;

    public boolean internetPermissions = false;
    public boolean callPermissions = false;
    public String mapKey;
    public boolean locationPermission;

    public List<IntermediateModel.Screen> activities = new ArrayList<>();

    public Manifest(final Project project) {
        super("manifest", project.srcDir + "main/AndroidManifest.xml");
    }


}
