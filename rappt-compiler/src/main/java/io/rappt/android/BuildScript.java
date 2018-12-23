package io.rappt.android;

public class BuildScript extends FileTemplate {
    public boolean hasSupportLibraries;
    public boolean hasRetrofit;
    public boolean hasGson;
    public boolean hasEvent;
    public boolean hasScribe;
    public boolean hasPicasso;
    public boolean hasDragToRefresh;
    public boolean hasGooglePlay;
    public boolean hasAcra;
	public boolean parse;
    public String resourcePackageName;
    public String landingActivity;

    public BuildScript(final Project project) {
        super("buildScript", project.projectDir + "app/build.gradle");
        resourcePackageName = project.projectPackage;
    }
}
