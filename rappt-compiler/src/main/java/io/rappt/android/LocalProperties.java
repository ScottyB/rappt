package io.rappt.android;

public class LocalProperties extends FileTemplate {

    public String directory;

    public LocalProperties(final Project project) {
        super("localProperties", project.projectDir + "local.properties");
        this.directory = project.androidSdk;
    }
}
