package io.rappt.android;

import java.util.*;

public class JavaInterface extends FileTemplate {
    public String interfacePackage;
    public Set<String> interfaceImports = new HashSet<>();
    public String interfaceName;
    public List<JavaFunction> interfaceFunctions = new ArrayList<>();
    public Template annotation;

    // Must have init value
    public Set<JavaField> classFields = new HashSet<>();

    public List<JavaFunction> getInterfaceFunctions() {
        return interfaceFunctions;
    }

    public void addFunction(String name, String returnType) {
        JavaFunction javaFunction = new JavaFunction(name, returnType);
        javaFunction.isInterface = true;
        interfaceFunctions.add(javaFunction);
    }

    // Returns true if added
    public boolean addFunction(JavaFunction javaFunction) {
        if (javaFunction.isInterface) interfaceFunctions.add(javaFunction);
        return javaFunction.isInterface;
    }

    private Set<JavaField> getClassFields() {
        return classFields;
    }


    public JavaInterface(Project project, String interfaceName) {
        super("javaInterface", project.javaOutputFile(interfaceName, project.projectPackage));
        this.interfaceName = interfaceName;
        this.interfacePackage = project.projectPackage;
    }

    static public JavaInterface newRetrofitInstance(Project project, String interfaceName) {
        JavaInterface newJavaInterface = new JavaInterface(project, interfaceName);
        newJavaInterface.interfaceImports = new HashSet<>();
        newJavaInterface.interfaceImports.add("retrofit.http.*");
        return newJavaInterface;
    }

    static public JavaInterface newPreferencesInstance(Project project, String preferencesName) {
        JavaInterface newJavaInterface = new JavaInterface(project, preferencesName)
                .imports(AndroidModel.ANNOTATIONS_PACKAGE + ".sharedpreferences.SharedPref")
                .addAnnotation(new Template("annotationsPrefs"));
        return newJavaInterface;
    }

    public JavaInterface imports(String... imports) {
        this.interfaceImports.addAll(Arrays.asList(imports));
        return this;
    }

    public JavaInterface addAnnotation(Template annotation) {
        this.annotation = annotation;
        return this;
    }
}
