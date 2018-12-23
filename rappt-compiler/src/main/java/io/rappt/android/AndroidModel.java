package io.rappt.android;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.rappt.compiler.IntermediateModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import io.rappt.settings.CompilerConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.substringAfter;

public class AndroidModel {
    private static transient final Logger LOGGER = Logger.getLogger(AndroidModel.class.getName());

    public List<FileTemplate> miscellaneous = new ArrayList<>();
    public List<MenuLayout> menus = new ArrayList<>();
    public Set<Path> imagesToCopy = new HashSet<>();
    public List<AndroidScreen> androidScreens = new ArrayList<>();
    public FileTemplate rootProjectFile;
    public List<JavaClass> pojos = new ArrayList<>();
    public Strings strings;
    public Style styles;
    public Integers integers;
    public Colours colours;
    public BuildScript buildScript;
    public LocalProperties localProperties;
    public Manifest manifest;
    public MenuLayout globalMenu;
    public JavaInterface sharedPreferences;
    public JavaClass application;

    public List<String> drawablesToCopy = new ArrayList<>();

    public Project project;
    public boolean isOffline = false;

    // Android specific properties file
    public Properties properties = new Properties();
    final public List<String> icons;

    public static final String TABBAR_ACTIVITY = "TabbarActivity";
    public static final String DRAWER_ACTIVITY = "DrawerActivity";
    public static final String DRAWER_LAYOUT = "drawer_layout";
    public static final String ERROR_DIALOG = "ErrorDialog";
    public static final String ANDROID_PROPERTIES = "/android/android.properties";
    public static final String MESSAGE_NO_DATA_ID = "message_no_data";
    public static final String DATA_VARIABLE = "data";
    public static final String PRIMARY = "primary";

    public static final String ANNOTATIONS_PACKAGE = "org.androidannotations.annotations";


    public AndroidModel(Path directory, String sdk, IntermediateModel model, String projectName, String appPackage) {
        InputStream in = getClass().getResourceAsStream(ANDROID_PROPERTIES);
        try {
            properties.load(in);
            in.close();
        } catch (IOException e) {
            // TODO: Handle correctly
            e.printStackTrace();
        }

        project = new Project(directory, appPackage, sdk, projectName);

        icons = Arrays.asList(properties.getProperty("icons").split(","));
        // Setup model
        Manifest manifest = new Manifest(project);
        manifest.packageName = project.projectPackage;
        this.manifest = manifest;

        buildScript = new BuildScript(project);
        buildScript.hasSupportLibraries = true;                // TODO: Check this is needed

        localProperties = new LocalProperties(project);
        strings = new Strings(project);
        integers = new Integers(project);
        colours = new Colours(project);
        styles = new Style(project);

        // Application class
        String appClass = model.app.applicationClassName;
        JavaClass app = JavaClass.newApplicationInstance(project, appClass);
        manifest.globalApplication = appClass;
        application = app;
        drawablesToCopy.add("ic_launcher.png");
    }

    // TODO: Use reflection to get all File templates
    public Queue<FileTemplate> getTemplates() {
        Queue<FileTemplate> allTemplates = new ArrayDeque<>();
        allTemplates.addAll(miscellaneous);

        allTemplates.addAll(menus);
        androidScreens.forEach(m -> allTemplates.addAll(m.getTemplates()));
        allTemplates.addAll(pojos);
        allTemplates.add(strings);
//        allTemplates.add(integers);
        allTemplates.add(colours);
        allTemplates.add(styles);
        allTemplates.add(buildScript);
        allTemplates.add(localProperties);
        allTemplates.add(manifest);
        if (sharedPreferences != null) allTemplates.add(sharedPreferences);
        allTemplates.add(application);
        if (globalMenu != null) allTemplates.add(globalMenu);
        allTemplates.add(rootProjectFile);
        return allTemplates;
    }

    public void copyDrawables() {
        for (String s : this.drawablesToCopy) {
            copyProjectFiles("templates/drawable/" + s, Paths.get(project.drawableFolder + s));
        }
    }

    public void copyFiles(CompilerConfig config) throws IOException {
        if (config.imagesDisabled) {
            if (!this.imagesToCopy.isEmpty()) {
                LOGGER.info("Images not copied as config disables images");
            }
        } else {
            for (Path path : this.imagesToCopy) {
                Path toPath;
                if (config.absoluteImagePathsDisabled && !path.startsWith(config.pathToScriptDir)) {
                    LOGGER.info("File: " + path.toString() + " not copied as config disables absolute image paths");
                } else if (!Files.exists(path)) {
                    LOGGER.info("File: " + path.toString() + " does not exist and could not be copied");
                } else {
                    toPath = Paths.get(project.drawableFolder + path.getFileName());
                    Files.copy(path, toPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    // TODO: Find way to combine sections for JAR and IDE
    public void copyProjectFiles(String path, Path dest) {
        //final String path = "templates/asIs";

        final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        if (jarFile.isFile()) {
            try (JarFile jar = new JarFile(jarFile)) {
                final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
                while (entries.hasMoreElements()) {
                    final String name = entries.nextElement().getName();
                    if (!name.endsWith("/") && (name.startsWith(path + "/") || name.contains(path))) {
                        URL p = this.getClass().getClassLoader().getResource(name);
                        Path destFile = Paths.get(dest + substringAfter(name, path));
                        FileUtils.copyURLToFile(p, destFile.toFile());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { // Run with IDE
            final URL url = AndroidModel.class.getResource("/" + path);
            if (url != null) {
                try {
                    File file = new File(url.toURI());
                    if (file.isDirectory()) {
                        for (Object f : FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
                            String name = f.toString();
                            Path destFile = Paths.get(dest + substringAfter(name, path));
                            FileUtils.copyFile(new File(name), destFile.toFile());
                        }
                    } else {
                        Path destFile = Paths.get(dest + substringAfter(file.getPath(), path));
                        FileUtils.copyFile(file, destFile.toFile());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static class ApplicationConstructor extends Template {
        public String className;
        public String offlineApiClass;
        public String apiVariable;
        public String setupFunction;
        public Boolean isParseApp;

        public ApplicationConstructor() {
            super("applicationConstructor");
        }
    }

    public static class LoadImage extends Template {
        public boolean hasMockData;
        public String appClass = "";

        public LoadImage(boolean hasMockData, String appClass) {
            super("loadImage");
            this.hasMockData = hasMockData;
            this.appClass = appClass;
        }
    }

    public JsonObject toJson() {
        Gson json = new Gson();
        JsonArray jsonArray = new JsonArray();
        for (FileTemplate t : getTemplates()) {
            jsonArray.add(json.toJsonTree(t));
        }
        JsonObject jsonApp = new JsonObject();
        jsonApp.add("android", jsonArray);
        return jsonApp;
    }


}
