package io.rappt.android;

import io.rappt.compiler.InstructionBuilder;
import io.rappt.compiler.IntermediateModel;
import io.rappt.model.Event;

// A Java class that represents a screen in the mobile app.
// In Android this means the class is either a Fragment or an Activity.
// Primarily this class acts as a way of identifying classes that control a screen. Not
// all activities control a single screen as some contain fragments.
// As we are only dealing with mobile applications we are assuming only one fragment is
// on the screen at one time, for now.
public class Controller extends JavaClass {

    @STIgnore
    public static final String CONTEXT_VARIABLE = "context";

    @STIgnore
    JavaFunction onCreate;

    static public class ItemViewConstructor extends Template {
        public String className = "";
        public boolean hasContext;

        public ItemViewConstructor(String className, boolean hasContext) {
            super("contentDefaultContext");
            this.className = className;
            this.hasContext = hasContext;
        }
    }

    static public class DefaultValue {
        public String isFragment;
    }

    private Controller(Project project, String className, String activityPackage) {
        super(project, className, activityPackage);
    }

    static public Controller newItemViewInstance(AndroidModel model, String className, String layout, boolean hasContext) {
        JavaClass itemViewClass = new Controller(model.project, className, model.project.viewPackage)
                .imports(getImports("itemViewImports", model.properties))
                .extend("LinearLayout")
                .addAnnotation(new SimpleTemplate("annotationGroup", layout))
                .addContent(new ItemViewConstructor(className, hasContext));
        // TODO: Need this context variable?
        if (hasContext) itemViewClass.addField("Context", Controller.CONTEXT_VARIABLE);
        return (Controller) itemViewClass;
    }

    static public Controller newActivityInstance(Project project, String className, String layout) {
        JavaClass activity = new Controller(project, className, project.activityPackage)
                .extend("FragmentActivity")
                .imports("android.support.v4.app.FragmentActivity", "org.androidannotations.annotations.*")
                .addAnnotation(new SimpleTemplate("annotationsActivity", layout));
        Controller screen = ((Controller) activity);
        return screen;
    }

    static public Controller newFragmentInstance(Project project, String className, String layout) {
        JavaClass fragment = new Controller(project, className, project.fragmentPackage)
                .extend("Fragment")
                .imports("android.support.v4.app.Fragment", "org.androidannotations.annotations.*")
                .addAnnotation(new SimpleTemplate("annotationsFragment", layout));
        Controller screen = ((Controller) fragment);
        return screen;
    }

    // TODO: Rework function
    public Controller buildOnCreateFunction(boolean showHome, boolean hasContextVar, boolean isFragment) {
        if (hasContextVar) {
            DefaultValue value = new DefaultValue();
            if (isFragment) value.isFragment = Boolean.toString(isFragment);
            addToOnCreate(new ObjectTemplate("contentContextVariable", value));
        }
        if (showHome)
            addToOnCreate(new Template("contentShowHome"));
        if (showHome || hasContextVar) {
            this.imports("android.os.Bundle");
        }
        return this;
    }

    // Adds content to onCreate, creating the function first if not present
    public void addToOnCreate(Template template) {
        if (onCreate == null) {
            onCreate = new JavaFunction("onCreate")
                    .addAnnotation(new Template("annotationsOverride"))
                    .setModifier("public")
                    .addParameter("Bundle", "savedInstance")
                    .addContent(new SimpleTemplate("contentCreate", "savedInstance"));
            this.classContent.add(0, onCreate);
        }
        onCreate.addContent(template);
    }

    public JavaClass addInstructions(String functionName, Event event, IntermediateModel.Screen screen, String template, IntermediateModel interModel, IntermediateModel.Event interEvent, AndroidModel model) {
        return addInstructions(functionName, event, screen, template, interModel, interEvent, false, model);
    }

    public JavaClass addInstructions(String functionName, Event event, IntermediateModel.Screen screen, String template, IntermediateModel interModel, IntermediateModel.Event interEvent, boolean isView, AndroidModel model) {
        InstructionBuilder builder = new InstructionBuilder();
        InstructionBuilder.InstructionBuilderData data = new InstructionBuilder.InstructionBuilderData();
        data.annotationTemplate = template;
        data.controller = this;
        data.event = event;
        data.interEvent = interEvent;
        data.initFunctionName = functionName;
        data.screen = screen;
        data.interModel = interModel;
        data.model = model;

        // TODO Fix temporary hack. Need to consider creating intermediate event types
        data.isOptionsItem = template.equals("annotationOptionItem");
        data.isViewController = isView;
        builder.processInstructions(data);
        return this;
    }


    public Controller addContextVariable(IntermediateModel.Screen interScreen) {
        if (interScreen.hasContextVariable) {
            this.imports("android.support.v4.app.FragmentActivity");
            this.addField("FragmentActivity", CONTEXT_VARIABLE);
        }
        return this;
    }

    public Controller addStringRes(String variable, String stringId) {
        this.imports("org.androidannotations.annotations.res.StringRes");
        this.addField("String", variable, new SimpleTemplate("annotationStringRes", stringId));
        return this;
    }

}