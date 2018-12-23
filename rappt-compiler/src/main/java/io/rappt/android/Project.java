package io.rappt.android;

import java.io.File;
import java.nio.file.Path;

public class Project {

    // Variables used in templates
    static public final String ACTIVITIES = "activities";


    // Project directory structure
    final public String drawableFolder;
    String resourceFolder = "";

    final public String layoutFolder;
    final public String menuFolder;
    final public String projectDir;
    final public String projectPackage;
    final public String srcDir;
    final public String javaFolder;
    final public String projectName;
    final public String valuesFolder;
    final public String assetsFolder;
    final public String androidSdk;
    final public String dataPackage;
    final public String fragmentPackage;
    final public String adapterPackage;
    final public String activityPackage;
    final public String viewPackage;
    final public String servicesPackage;
    final public String adapterFolder;

    public Project(Path directory, String appPackage, String sdk, String projectName) {

        this.projectName = projectName;
        projectDir = directory + "/" + projectName + "/";
        srcDir = projectDir + "app/src/";
        androidSdk = sdk;
        projectPackage = appPackage + "." + projectName.toLowerCase() + ".app";
        javaFolder = srcDir + "main/java/" + packageToDir(projectPackage);
        dataPackage = projectPackage + ".model";
        activityPackage = projectPackage + "." + ACTIVITIES;
        fragmentPackage = projectPackage + ".fragments";
        adapterPackage = projectPackage + ".adapters";
        viewPackage = projectPackage + ".views";
        servicesPackage = projectPackage + ".services";
        resourceFolder = srcDir + "main/res/";
        assetsFolder = srcDir + "main/assets/";
        layoutFolder = resourceFolder + "layout/";
        menuFolder = resourceFolder + "menu/";
        valuesFolder = resourceFolder + "values/";
        drawableFolder = resourceFolder + "drawable/";
        adapterFolder = javaFolder + "/adapters/";

    }

    // TODO: Tidy this up!!!
    public void buildProjectStructure() {
        new File(this.javaFolder).mkdirs();
        new File(this.layoutFolder).mkdirs();
        new File(this.menuFolder).mkdirs();
        new File(this.valuesFolder).mkdirs();
        new File(this.drawableFolder).mkdirs();
        new File(this.assetsFolder).mkdirs();
        new File(this.javaFolder + "/model").mkdirs();
        new File(this.javaFolder + "/" + ACTIVITIES).mkdirs();
        new File(this.javaFolder + "/services").mkdirs();
        new File(this.javaFolder + "/fragments").mkdirs();
        new File(this.javaFolder + "/views").mkdirs();
        new File(this.adapterFolder).mkdirs();
        new File(srcDir + "androidTest/java/" + packageToDir(projectPackage)).mkdirs();
    }

    public String packageToDir(String manifestPackage) {
        return manifestPackage.replace(".", "/") + "/";
    }

    public String javaOutputFile(String className, String packageName) {
        return srcDir + "main/java/" + packageToDir(packageName) + className + ".java";
    }

}
