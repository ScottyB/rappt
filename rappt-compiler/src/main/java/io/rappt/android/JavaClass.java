package io.rappt.android;

import io.rappt.compiler.AndroidTranslator;
import io.rappt.compiler.InstructionBuilder;
import io.rappt.compiler.IntermediateModel;

import java.util.*;

public class JavaClass extends FileTemplate {

    public String classPackage = "";                         // Dot separated package templateName
    public String className = "";

    // Optional
    public String classExtends;
    public Set<String> classImplements = new HashSet<>(); // Interfaces implemented by class
    public Set<String> classImports = new HashSet<>();    // Dot separated imports
    public List<Template> classAnnotations = new ArrayList<>();
    public Set<JavaField> classFields = new HashSet<>();

    // Must contain a value matching classImplements
    public List<Template> classContent = new ArrayList<>();

    static public class DrawerData {
        public Collection<IntermediateModel.Navigation.Tabs> tabs = new ArrayList<>();
        public boolean noSubHeader;
    }

    public JavaClass(Project project, String className, String classPackage) {
        super("javaClass", project.javaOutputFile(className, classPackage));
        this.className = className;
        this.classPackage = classPackage;
    }

    public JavaClass(Project project, String className) {
        super("javaClass", project.javaOutputFile(className, project.projectPackage));
        this.className = className;
        this.classPackage = project.projectPackage;
    }

    static public String[] getImports(String propertyValue, Properties properties) {
        return properties.getProperty(propertyValue).split(",");
    }

    public JavaField addField(String type, String fieldName) {
        JavaField newJavaField = new JavaField(type, fieldName);
        classFields.add(newJavaField);
        return newJavaField;
    }

    public JavaField addField(String type, String fieldName, String annotation, String initValue) {
        JavaField field = new JavaField(type, fieldName);
        if (!annotation.isEmpty()) field.addAnnotation(new Template(annotation));
        field.initValue = initValue;
        classFields.add(field);
        return field;
    }


    static public JavaClass newTabbarInstance(Project project, Collection<IntermediateModel.Navigation.Tabs> tabs, Properties properties) {
        JavaClass tabbarActivity = new JavaClass(project, AndroidModel.TABBAR_ACTIVITY, project.activityPackage)
                .extend("FragmentActivity")
                .imports(getImports("tabbarImports", properties))
                .addAnnotation(new Template("annotationsEActivity"))
                .addContent(new ObjectTemplate("contentTabbarActivity", tabs));
        return tabbarActivity;
    }

    static public JavaClass newDrawerActivity(AndroidModel model, DrawerData drawerData) {
        JavaClass drawerActivity = new JavaClass(model.project, AndroidModel.DRAWER_ACTIVITY, model.project.activityPackage)
                .imports(getImports("drawerImports", model.properties))
                .extend("FragmentActivity")
                .addContent(new ObjectTemplate("contentDrawerActivity", drawerData))
                .addAnnotation(new SimpleTemplate("annotationsActivity", AndroidModel.DRAWER_LAYOUT));
        return drawerActivity;
    }

    static public JavaClass newApplicationInstance(Project project, String appClass) {
        JavaClass application = new JavaClass(project, appClass)
                .extend("Application")
                .addAnnotation(new Template("annotationApplication"))
                .imports(AndroidModel.ANNOTATIONS_PACKAGE + ".EApplication", "android.app.Application");
        return application;
    }

    static public JavaClass newAdapterInstance(AndroidModel model, String className, String dataClass, AdaptarData data) {
        JavaClass adapterClass = new JavaClass(model.project, className, model.project.adapterPackage)
                .imports(getImports("adapterImports", model.properties))
                .extend("DefaultAdapter<" + dataClass + ">")
                .addAnnotation(new Template("annotationBean"))
                .addContent(data);
        return adapterClass;
    }

    static public JavaClass newNoSwipePager(AndroidModel model) {
        JavaClass noSwipePager = new JavaClass(model.project, "NonSwipeableViewPager")
                .imports(getImports("noSwipedrawerImports", model.properties))
                .extend("ViewPager")
                .addContent(new Template("contentNoSwipePager"));
        return noSwipePager;
    }

    static public JavaClass newServiceInstance(Project project, String className, String appClassName, Template handlerContent) {
        JavaClass service = new JavaClass(project, className, project.servicesPackage)
                .imports("android.app.IntentService",
                        "android.content.Intent",
                        "android.location.Location",
                        "android.util.Log",
                        "org.rappt.tracking.app.*",
                        "org.rappt.tracking.app.model.*",
                        "com.google.android.gms.location.*",
                        "org.androidannotations.annotations.*",
                        "retrofit.*",
                        "java.util.*")
                .extend("IntentService")
                .addAnnotation(new Template("annotationService"))
                .addField(appClassName, AndroidTranslator.APPLICATION_VARIABLE, "annotationApp");
        InstructionBuilder.FunctionCall call = new InstructionBuilder.FunctionCall("super");
        call.parameters.add("\"" + className + "\"");
        JavaFunction constructor = new JavaFunction(className, "")
                .addContent(call);
        service.addContent(constructor);
        service.addContent(handlerContent);
        return service;
    }

    public JavaClass addField(String type, String value, String annotation) {
        JavaField field = new JavaField(type, value)
                .addAnnotation(new Template(annotation));
        this.classFields.add(field);
        return this;
    }

    public JavaClass addField(String type, String value, SimpleTemplate annotation) {
        JavaField field = new JavaField(type, value)
                .addAnnotation(annotation);
        this.classFields.add(field);
        return this;
    }


    public JavaField addField(String type, String fieldName, boolean initNewObject, boolean thisValue) {
        JavaField field = this.addField(type, fieldName);
        field.initNewObject = initNewObject;
        field.param = thisValue;
        this.classFields.add(field);
        return field;
    }

    public JavaClass extend(String classExtends) {
        this.classExtends = classExtends;
        return this;
    }

    public JavaClass imports(String... args) {
        this.classImports.addAll(Arrays.asList(args));
        return this;
    }

    public JavaClass addContent(Template content) {
        this.classContent.add(content);
        return this;
    }

    public JavaClass addAnnotation(Template annotation) {
        this.classAnnotations.add(annotation);
        return this;
    }
}
