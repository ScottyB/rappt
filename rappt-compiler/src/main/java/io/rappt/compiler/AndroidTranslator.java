package io.rappt.compiler;

import com.google.common.collect.ListMultimap;
import io.rappt.android.*;
import io.rappt.model.*;
import io.rappt.model.Style;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import io.rappt.android.*;
import io.rappt.model.*;
import io.rappt.settings.CompilerConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// All functions in this class take an AppModel object and an AndroidModel object and return an updated AndroidModel
public class AndroidTranslator {

    static final int MIN_SCROLL_LIMIT = 4;
    static final String ADAPTER_VARIABLE = "adapter";
    public static final String APPLICATION_VARIABLE = "app";
    static final String DIALOG_VARIABLE = "dialog";
    static final String LISTITEM_BIND_FUNCTION = "bind";
    static final String PREFERENCES_VARIABLE = "prefs";

    static public final String NO_INTERNET_CONNECTION = "no_internet_connection";
    static public final String WRONG_PASSWORD_EMAIL = "wrong_password_email";
    static public final String RETROFIT_ERROR = "retrofit_error";

    private AndroidTranslatorUtils Utils = new AndroidTranslatorUtils();
    private final CompilerConfig compilerConfig;

    private Map<ScreenPredicate, UpdateScreen> screenFunctionality = new LinkedHashMap<>();
    private Map<ApplyPredicate, ApplyTranslation> functionality = new LinkedHashMap<>();

    public AndroidTranslator(CompilerConfig compilerConfig) {
        this.compilerConfig = compilerConfig;

        // ORDER MATTERS
        screenFunctionality.put((app, interScreen, screen) -> true, addViews);
        screenFunctionality.put((app, interScreen, screen) -> true, screenAddDynamicList);
        screenFunctionality.put((app, interScreen, screen) -> true, addImports);
        screenFunctionality.put((app, interScreen, screen) -> true, addScreenAddons);

        screenFunctionality.put(hasExtras, addExtras);
        screenFunctionality.put(hasActionItems, addActionItems);
        screenFunctionality.put(hasCallInstructions, addCallInstructionAddons);
        screenFunctionality.put(doesSendNotification, addNotification);

        functionality.put((app, interModel) -> true, addStrings);
        functionality.put((app, interModel) -> true, addIntegers);
        functionality.put((app, interModel) -> true, getAddDrawablesClosure());
        functionality.put((app, interModel) -> true, addErrorDialog);
        functionality.put((app, interModel) -> true, addStyles);
        functionality.put((app, interModel) -> true, addColours);

        functionality.put(hasMenuLayouts, addMenuLayouts);
        functionality.put(hasRetrofitInterface, addRetrofitInterface);
        functionality.put(hasOfflineApi, addOfflineApi);
        functionality.put(hasOnlineApi, addOnlineApi);
        functionality.put(hasDynamicList, addDynamicList);
        functionality.put(hasDataModel, addDataModel);
        functionality.put(hasMessage, addMessage);
        functionality.put(hasTabbar, addTabbar);
        functionality.put(hasDrawer, addDrawer);
        functionality.put(hasSharedPreferences, addSharedPreferences);
        functionality.put(hasEventDataModel, addEventDataModel);
        functionality.put(hasUtils, addUtils);

    }

    private ApplyPredicate hasSharedPreferences = (app, interModel) -> interModel.app.hasPreferences;
    private ApplyTranslation addSharedPreferences = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        JavaInterface javaInterface = JavaInterface.newPreferencesInstance(newModel.project, interModel.app.preferencesName);
        interModel.preferences.forEach(p -> {
            // TODO: Add default values: .addAnnotation(Utils.setupHTTPAnnotation(r));
            javaInterface.addFunction(p.functionName, p.type);
        });
        newModel.sharedPreferences = javaInterface;
        return newModel;
    };

    private ApplyPredicate hasMessage = (app, interModel) -> app.allApi.size() > 0 || interModel.intermediateScreens.values().stream().anyMatch(s -> s.doesMakeRequest);
    private ApplyTranslation addMessage = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        newModel.miscellaneous.add(new FileTemplate("message", newModel.project.layoutFolder + "message.xml"));
        newModel.strings.addStringValue(AndroidModel.MESSAGE_NO_DATA_ID, "No data available");
        return newModel;
    };

    // TODO: All screen imports should be added here
    private UpdateScreen addImports = (app, interModel, model, androidScreen, appScreen) -> {
        final AndroidScreen newScreen = androidScreen;
        IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(appScreen.id);
        if (interScreen.doesMakeRequest && appScreen.view.webs.isEmpty()) {
            newScreen.viewController.controller.imports(model.project.dataPackage + ".*");
        }
        if (interScreen.dynamicListVar != null)
            newScreen.viewController.controller.imports(model.project.viewPackage + ".*");
        if (interScreen.isFragment)
            newScreen.viewController.controller.imports(model.project.activityPackage + ".*");
        newScreen.viewController.controller.imports(model.project.projectPackage + ".*");
        newScreen.elements.forEach(e -> e.controller.imports(model.project.projectPackage + ".*",
                model.project.dataPackage + ".*"
        ));
        return newScreen;
    };


    private UpdateScreen screenAddDynamicList = (app, interModel, model, androidScreen, appScreen) -> {
        final AndroidScreen newScreen = androidScreen;
        appScreen.view.dynamicLists.forEach(l -> {
            IntermediateModel.DynamicList dl = interModel.dynamicLists.get(l.id);
            newScreen.viewController.controller.imports(model.project.adapterPackage + ".*");
            l.listItemLayouts.forEach(i -> {
                IntermediateModel.ListItem v = dl.views.get(i.layout.id);
                Controller dataItemView = Controller.newItemViewInstance(model, v.viewControllerName, v.layoutFile, !i.layout.images.isEmpty());
                final DynamicLayout dataItemLayout = new DynamicLayout(model.project, v.layoutFile);
                AndroidScreen.ViewController viewController = new AndroidScreen.ViewController(dataItemView, dataItemLayout);
                viewController.controller.imports(model.project.dataPackage + ".*");

                IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(appScreen.id);

                for (View.Label label : i.layout.labels) {
                    viewController = addLabel(viewController, interModel, model, label);
                }

                for (View.Button button : i.layout.buttons) {
                    viewController = addButton(viewController, interModel, model, button, interScreen);
                }

                for (View.Image image : i.layout.images) {
                    viewController = addImage(viewController, interModel, model, image);
                }

                if (!i.layout.layoutSpecification.isEmpty()) {
                    viewController.view.layout = i.layout.layoutSpecification;
                }

                // TODO: Merge into InstructionBuilder
                final AndroidScreen.ViewController finalViewController = viewController;
                Optional.ofNullable(interModel.resources.get(l.apiId, l.resourceId)).ifPresent(resource -> {
                    JavaFunction bind = new JavaFunction(LISTITEM_BIND_FUNCTION);
                    bind.setModifier("public");

                    IntermediateModel.ListField lf = interModel.resourceListFields.get(l.resourceId);
                    Field field = AndroidTranslatorUtils.buildField(lf.listClassVariable, lf.listClassName, v.pathsToShow);
                    bind.addParameter(lf.listClassName, lf.listClassVariable);

                    bind.addContent(new ObjectTemplate("displayData", field));
                    finalViewController.controller.addContent(bind);
                });
                newScreen.elements.add(finalViewController);
            });
            newScreen.viewController.controller.addField(dl.adapterClassName, ADAPTER_VARIABLE, "annotationInsertBean");
        });
        return newScreen;
    };

    private AndroidScreen.ViewController addLabel(final AndroidScreen.ViewController viewController, final IntermediateModel interModel, final AndroidModel model, View.Label label) {
        final AndroidScreen.ViewController newViewController = viewController;
        IntermediateModel.Label interLabel = interModel.intermediateLabels.get(label.id);
        UIElements.TextView xmlTextView = new UIElements.TextView(interLabel.formattedId, interLabel.stringId);
        if (!label.styleName.isEmpty())
            xmlTextView.styleName = label.styleName;
        UIControls.TextView textView = new UIControls.TextView(interLabel.formattedId, label.label.isEmpty());
        Optional.ofNullable(interModel.isFragmentToFragment.get(label.parameterId)).ifPresent(b -> textView.isFragmentToFragment = b.booleanValue());
        //if (interLabel.shownOnListItem && textView.isFragmentToFragment) System.out.println("WARNING: " )
        interModel.eventClasses.entries().stream()
                .filter(f -> f.getValue().equals(interLabel.value))
                .findFirst()
                .ifPresent(f -> {
                    textView.eventClassName = f.getKey();
                    textView.doesReceiveEvent = true;
                    viewController.controller.imports(model.project.dataPackage + ".*");
                });
        if (interLabel.hasPassedValue) {
            textView.hasValue = true;
            textView.value = interLabel.value;
        }
        newViewController.view.elements.add(xmlTextView);
        newViewController.controller = textView.addToController(newViewController.controller);
        return newViewController;
    }

    private AndroidScreen.ViewController addButton(final AndroidScreen.ViewController viewController, final IntermediateModel interModel, final AndroidModel model, View.Button button, IntermediateModel.Screen interScreen) {
        final AndroidScreen.ViewController newViewController = viewController;
        IntermediateModel.Button interButton = interModel.intermediateButtons.get(button.id);
        UIElements.Button uiButton = new UIElements.Button(interButton.formattedId, interButton.stringId);
        if (!button.styleName.isEmpty())
            uiButton.styleName = button.styleName;
        viewController.view.elements.add(uiButton);
        viewController.controller.addInstructions(interButton.functionName, button.onTouch, interScreen, "annotationClick", interModel, interButton.event, interButton.shownOnListItem, model);
        return newViewController;
    }

    private AndroidScreen.ViewController addImage(final AndroidScreen.ViewController viewController, final IntermediateModel interModel, final AndroidModel model, View.Image image) {
        final AndroidScreen.ViewController newViewController = viewController;
        IntermediateModel.Image newImage = interModel.images.get(image.id);
        UIElements.ImageView img = new UIElements.ImageView(newImage.formattedId);
        if (!image.styleName.isEmpty())
            img.styleName = image.styleName;
        if (!image.valuePath.path.isEmpty()) {
            UIControls.ImageView imageView = new UIControls.ImageView(newImage.formattedId);
            viewController.controller = imageView.addToController(viewController.controller);
        } else {
            img.drawable = newImage.imageVariable.isEmpty() ? null : newImage.imageVariable;
        }
        viewController.view.elements.add(img);
        return newViewController;
    }

    private AndroidScreen.ViewController addTextInput(final AndroidScreen.ViewController viewController, final IntermediateModel interModel, final AndroidModel model, View.TextInput textInput) {
        final AndroidScreen.ViewController newViewController = viewController;
        IntermediateModel.InputText t = interModel.intputText.get(textInput.id);
        UIElements.EditText text = new UIElements.EditText(t.formattedId, t.stringId, t.isPassword);
        if (!textInput.styleName.isEmpty())
            text.styleName = textInput.styleName;
        UIControls.EditText editText = new UIControls.EditText(t.formattedId);
        viewController.controller = editText.addToController(viewController.controller);
        viewController.view.elements.add(text);
        return newViewController;
    }

    private ApplyPredicate hasDynamicList = (app, interModel) -> interModel.app.hasDynamicList;
    private ApplyTranslation addDynamicList = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        newModel.miscellaneous
                .add(new FileTemplate.SimpleFileTemplate(ADAPTER_VARIABLE,
                        androidModel.project.adapterFolder + "DefaultAdapter.java",
                        androidModel.project.adapterPackage));

        // Adapters
        app.screens.forEach(s -> s.view.dynamicLists.forEach(l -> {
            IntermediateModel.DynamicList dl = interModel.dynamicLists.get(l.id);
            Optional.ofNullable(interModel.resources.get(l.apiId, l.resourceId)).ifPresent(resource -> {
                IntermediateModel.ListField lF = interModel.resourceListFields.get(l.resourceId);

                Field field = AndroidTranslatorUtils.buildField(lF.listClassVariable, lF.listClassName, resource.stateFieldPath);
                AdaptarData data = new AdaptarData(lF.listClassName, dl.views.values(), field);
                JavaClass adapter = JavaClass.newAdapterInstance(newModel, dl.adapterClassName, lF.listClassName, data);
                if (interModel.app.hasFragment)
                    adapter.imports(androidModel.project.fragmentPackage + ".*");

                adapter.imports(androidModel.project.projectPackage + ".*",
                        androidModel.project.activityPackage + ".*",
                        androidModel.project.dataPackage + ".*",
                        androidModel.project.viewPackage + ".*");
                if (dl.views.values().size() > 0)
                    adapter.imports("org.androidannotations.annotations.res.StringRes");
                newModel.miscellaneous.add(adapter);
            });
        }));
        return newModel;
    };


    private ApplyPredicate hasMenuLayouts = (app, interModel) -> app.menu != null;
    private ApplyTranslation addMenuLayouts = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        ListMultimap<String, IntermediateModel.Action> values = interModel.intermediateFileMenu;
        for (String s : values.keySet()) {
            MenuLayout menuLayout = new MenuLayout(newModel.project, s);
            menuLayout.actionItems.addAll(values.get(s));
            newModel.menus.add(menuLayout);
        }
        return newModel;
    };

    private ApplyPredicate hasTabbar = (app, interModel) -> app.navigation.navigationMethod == Navigation.NAVIGATION.TABBAR;
    private ApplyTranslation addTabbar = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        JavaClass tabbarActivity = JavaClass.newTabbarInstance(newModel.project, interModel.navigation.tabs, androidModel.properties);
        tabbarActivity.imports(androidModel.project.projectPackage + ".*");
        tabbarActivity.imports(newModel.project.fragmentPackage + ".*");
        newModel.miscellaneous.add(tabbarActivity);
        FileTemplate fileTemplate = new FileTemplate("pager", newModel.project.layoutFolder + "pager.xml");
        newModel.miscellaneous.add(fileTemplate);
        return newModel;
    };

    private ApplyPredicate hasOfflineApi = (app, interModel) -> interModel.app.provideMockData;
    private ApplyTranslation addOfflineApi = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        for (Api api : app.allApi) {
            IntermediateModel.Api interApi = interModel.intermediateApis.get(api.id);
            JavaClass offlineApiClass = new JavaClass(androidModel.project, interApi.offlineClassName);
            offlineApiClass.classImplements.add(interApi.className);
            for (Resource r : api.resources) {
                IntermediateModel.Resource resource = interApi.resources.get(r.id);
                if (r.isList) offlineApiClass.imports("java.util.List");
                offlineApiClass.addContent(Utils.addApiFunctionCall(interApi, resource, r, true));
            }
            offlineApiClass.addContent(new SimpleTemplate("offlineApi", interApi.offlineClassName));
            offlineApiClass.imports(JavaClass.getImports("offlineDataStore", newModel.properties));
            offlineApiClass.imports(androidModel.project.dataPackage + ".*");
            newModel.miscellaneous.add(offlineApiClass);
        }
        newModel.application.addField("static boolean", "IS_OFFLINE", "", "true");
        return newModel;
    };

    private ApplyPredicate hasRetrofitInterface = (app, interModel) -> !app.allApi.isEmpty();
    private ApplyTranslation addRetrofitInterface = (app, interModel, androidModel) -> {
        AndroidModel newModel = androidModel;
        for (Api api : app.allApi) {
            IntermediateModel.Api interApi = interModel.intermediateApis.get(api.id);

            if (api.isParseApp) {
                newModel = buildParseService(androidModel, api, interApi);
            } else {
                newModel = buildRetroFitService(androidModel, api, interApi);
            }


            if (interApi.hasOAuth) {
                newModel.application.addField("static String", "CALLBACK", "", "\"" + interApi.oAuth.callback + "\"");
            }

            if (api.isParseApp) {
                newModel.buildScript.parse = true;
            } else {
                newModel.buildScript.hasRetrofit = true;
            }
        }
        newModel.manifest.internetPermissions = true;

        return newModel;
    };

    public AndroidModel buildParseService(AndroidModel androidModel, Api api, IntermediateModel.Api interApi) {
        final AndroidModel newModel = androidModel;
        JavaClass javaClass = new JavaClass(androidModel.project, interApi.className);
        javaClass.imports("com.parse.*", "android.util.Log");
        javaClass.imports(androidModel.project.dataPackage + ".*");
        for (Resource r : api.resources) {
            IntermediateModel.Resource resource = interApi.resources.get(r.id);

            JavaFunction javaFunction = new JavaFunction(resource.functionName, resource.responseObject).setModifier("public");
            String template = "parseObjectContent";
            if (r.isList) {
                template = "parseListContent";
            }
            javaFunction.addContent(new SimpleTemplate(template, resource.responseClassName));
            javaFunction.throwException("ParseException");

//            if (r.hasToPrepareRequest()) {
//                javaFunction.addParameter(resource.responseObject, resource.fieldName, "annotationBody");
//            }
            if (r.urlParam != null) {
               if (api.isParseApp) {
                   javaFunction.addParameter("String", r.urlParam);
               } else {
                   javaFunction.addParameter("String", r.urlParam, new SimpleTemplate("annotationPath", r.urlParam));
               }
            }
            javaClass.addContent(javaFunction);
            if (r.isList) javaClass.imports("java.util.List");

        }
        newModel.miscellaneous.add(javaClass);
        return  newModel;
    }

    public AndroidModel buildRetroFitService(AndroidModel androidModel, Api api, IntermediateModel.Api interApi) {
        final AndroidModel newModel = androidModel;
        JavaInterface service = JavaInterface.newRetrofitInstance(androidModel.project, interApi.className);
        service.imports(androidModel.project.dataPackage + ".*");
        for (Resource r : api.resources) {
            IntermediateModel.Resource resource = interApi.resources.get(r.id);
            JavaFunction javaFunction = new JavaFunction(resource.functionName, resource.responseObject)
                    .addAnnotation(Utils.setupHTTPAnnotation(r));
            javaFunction.isInterface = true;
            if (r.hasToPrepareRequest()) {
                javaFunction.addParameter(resource.responseObject, resource.fieldName, "annotationBody");
            }
            if (r.urlParam != null) {
                javaFunction.addParameter("String", r.urlParam, new SimpleTemplate("annotationPath", r.urlParam));
            }
            service.addFunction(javaFunction);
            if (r.isList) service.imports("java.util.List");
        }
        if (interApi.hasOAuth) {
            service.imports("android.net.Uri");
        }
        newModel.miscellaneous.add(service);
        return newModel;
    }

    private ApplyPredicate hasDataModel = (app, interModel) -> !app.allApi.isEmpty();
    private ApplyTranslation addDataModel = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        interModel.dataClassFields.keys().elementSet().forEach(s -> {
            JavaClass pojo = new JavaClass(newModel.project, s, newModel.project.dataPackage);
            boolean isParseApp = app.allApi.stream().anyMatch(p -> p.isParseApp);
            if (isParseApp) {
                pojo.addAnnotation(new SimpleTemplate("parseAnnotation", pojo.className));
                pojo.classExtends = "ParseObject";
                pojo.imports("com.parse.ParseObject", "com.parse.ParseClassName");
            }

            List<Map<String, String>> variables = new ArrayList<>();
            interModel.dataClassFields.get(s).stream()
                    .filter(p -> StringUtils.isNotEmpty(p.variableName))
                    .forEach(p -> {
                        JavaField javaField = pojo.addField(p.javaType, p.variableName, p.initNewObject, false);
                        Map<String, String> tempMap = new HashMap<>();
                        tempMap.put("value", p.variableName);
                        //todo: fix this hack after the android semester
                        if (isParseApp && p.variableName.equals("id")) tempMap.put("isId", "true");
                        variables.add(tempMap);
                        if (p.javaType.equals("Date")) pojo.imports("java.util.Date");
                        if (p.javaType.startsWith("List")) {
                            pojo.imports("java.util.List", "java.util.ArrayList");
                            javaField.initNewObject = false;
                            // TODO: Move this to another hack
                            javaField.initValue = "new ArrayList" + StringUtils.removeStart(p.javaType, "List") + "()";
                        }
                        if (!p.variableName.equals(p.jsonProperty)) {
                            javaField.addAnnotation(new SimpleTemplate("annotationsSerializedName", p.jsonProperty));
                            pojo.imports("com.google.gson.annotations.SerializedName");
                        }
                    });
            if (isParseApp) {
                pojo.addContent(new ObjectTemplate("parseObjectLoad", variables));
            }
            newModel.pojos.add(pojo);
        });
        return newModel;
    };

    private ApplyPredicate hasEventDataModel = (app, interModel) -> interModel.hasEventObjects;
    private ApplyTranslation addEventDataModel = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        interModel.eventClasses.keys().forEach(k -> {
            JavaClass javaClass = new JavaClass(newModel.project, k, newModel.project.dataPackage);
            interModel.eventClasses.get(k).stream()
                    .filter(StringUtils::isNotEmpty)
                    .forEach(f -> javaClass.addField("String", f));
            newModel.pojos.add(javaClass);
        });
        newModel.buildScript.hasEvent = true;
        return newModel;
    };

    private ApplyPredicate hasUtils = (app, interModel) -> interModel.doesLoadImages;
    private ApplyTranslation addUtils = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        JavaClass utils = new JavaClass(androidModel.project, "Utils");
        utils.imports("android.content.Context", "android.widget.ImageView", "com.squareup.picasso.Picasso");
        boolean hasMockImages = (interModel.app.provideMockData && interModel.doesLoadImages);
        if (hasMockImages) {
            utils.imports("android.graphics.drawable.Drawable", "java.io.IOException", "java.io.InputStream");
        }
        utils.addContent(new AndroidModel.LoadImage(hasMockImages, interModel.app.applicationClassName));

        newModel.miscellaneous.add(utils);
        return newModel;
    };


    private ApplyPredicate hasOnlineApi = (app, interModel) -> !app.allApi.isEmpty();
    private ApplyTranslation addOnlineApi = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        for (Api api : app.allApi) {
            IntermediateModel.Api interApi = interModel.intermediateApis.get(api.id);
            JavaClass onlineApi = androidModel.application;
            if (!api.isParseApp)
                onlineApi.imports("retrofit.RestAdapter", "retrofit.RequestInterceptor", "retrofit.RestAdapter");
            onlineApi.imports(androidModel.project.dataPackage + ".*");
            IntermediateModel.Api a = interModel.intermediateApis.get(api.id);
            StandardOnlineApiConstructor standardOnlineApiConstructor = new StandardOnlineApiConstructor(
                    interApi.setupApiFunction,
                    a.servicesStatic,
                    a.className
            );
            if (!api.apiParamKey.isEmpty()) {
                standardOnlineApiConstructor.apiKey = api.apiParamKey;
                standardOnlineApiConstructor.apiValue = api.apiParamValue;
            }
//            if (dataStore.doesLoadImages) {
//                onlineApi.imports("android.graphics.drawable.Drawable", "android.widget.ImageView", "android.content.Context",
//                        "com.squareup.picasso.Picasso");
//            }


            if (a.hasOAuth) {
                onlineApi.addField("OAuthService", "oAuthService");
                // TODO: Remove key and secret from strings!!!
                onlineApi.addField("static String", "API_KEY", "", "\"" + a.oAuth.apiKey + "\"");
                onlineApi.addField("static String", "API_SECRET", "", "\"" + a.oAuth.apiSecret + "\"");
                onlineApi.addField("static String", "VERIFIER_PARAMETER", "", "\"oauth_verifier\"");
                onlineApi.addField("static String", "API_LOG", "", "\"TWITTER_LOG\"");
                onlineApi.addField("Token", "requestToken");
                onlineApi.addField("Token", "accessToken");
                onlineApi.imports(JavaClass.getImports("oAuthImports", androidModel.properties));
                onlineApi.classImplements.add("RequestInterceptor");
                onlineApi.addContent(new StandardOnlineApiConstructor.OAuth(interApi.setupApiFunction, a.variableName, a.className));
            } else {
                if (!api.isParseApp) {
                    onlineApi.addContent(standardOnlineApiConstructor);
                } else {
                    onlineApi.imports("com.parse.Parse", "com.parse.ParseACL", "com.parse.ParseObject");
                    List<String> strings = interApi.resources.values()
                            .stream()
                            .map(r -> r.responseClassName)
                            .collect(Collectors.toList());
                    onlineApi.addContent(new Parse(interApi.className, interApi.variableName, strings, api.clientKey, api.appId));
                }
            }

            if (!a.hasOAuth && !api.isParseApp) {
                AndroidModel.ApplicationConstructor constructor = new AndroidModel.ApplicationConstructor();
                constructor.apiVariable = a.variableName;
                constructor.className = newModel.application.className;
                if (newModel.isOffline) constructor.offlineApiClass = a.offlineClassName;
                constructor.setupFunction = a.setupApiFunction;
                newModel.application.addContent(constructor);
            }

            if (!api.isParseApp) onlineApi.addField("String", a.servicesStatic, "", "\"" + api.rootURL + "\"");
            onlineApi.addField(a.className, a.variableName);
            newModel.miscellaneous.add(onlineApi);
        }
        return newModel;
    };

    private ApplyTranslation addErrorDialog = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;

        ErrorDialog errorDialog = new ErrorDialog();
        errorDialog.doesRequireGooglePlayServices = interModel.app.doesRequireGooglePlayServices;
        errorDialog.hasRetrofit = !app.allApi.isEmpty() && !app.allApi.stream().anyMatch(p -> p.isParseApp);
        JavaClass errorDialogClass = new JavaClass(newModel.project, AndroidModel.ERROR_DIALOG, newModel.project.projectPackage)
                .extend("DialogFragment")
                .imports(JavaClass.getImports("errorDialogImports", newModel.properties))
                .addContent(new ObjectTemplate("contentErrorDialog", errorDialog));
        if (errorDialog.hasRetrofit) {
            errorDialogClass.imports("retrofit.RetrofitError");
            newModel.strings.addStringValue(RETROFIT_ERROR, "Server error occured.");
        }
        if (errorDialog.doesRequireGooglePlayServices)
            errorDialogClass.imports("com.google.android.gms.common.ConnectionResult",
                    "com.google.android.gms.common.GooglePlayServicesUtil");
        newModel.strings.addStringValue(NO_INTERNET_CONNECTION, "There is no internet connection.\nPlease check your internet connection.");
        newModel.strings.addStringValue(WRONG_PASSWORD_EMAIL, "Sorry, wrong password or email.");
        newModel.miscellaneous.add(errorDialogClass);
        return newModel;
    };

    private ApplyTranslation addStyles = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        newModel.styles.styles = new HashSet<>(app.styles.stream()
                .map(p -> ((Style) p).styleName)
                .distinct()
                .collect(Collectors.toList()));
        newModel.styles.hasButtons = !interModel.intermediateButtons.isEmpty();
        if (newModel.styles.hasButtons) {
            newModel.drawablesToCopy.add("custom_button.xml");
            newModel.drawablesToCopy.add("custom_button_text.xml");
        }
        return newModel;
    };

    private ApplyPredicate hasDrawer = (app, interModel) -> app.navigation.navigationMethod == Navigation.NAVIGATION.DRAWER;
    private ApplyTranslation addDrawer = (app, interModel, androidModel) -> {
        final AndroidModel newModel = androidModel;
        newModel.strings.addStringValue("drawer_open", "Open navigation drawer");
        newModel.strings.addStringValue("drawer_close", "Close navigation drawer");
        newModel.drawablesToCopy.add("drawer_shadow.9.png");
        newModel.drawablesToCopy.add("ic_drawer.png");
        // newModel.updateLandingPage(AndroidModel.DRAWER_ACTIVITY);

        Navigation navigationModel = app.navigation;

//
//        for (Link link : app.navigation.linkItems) {
//            navigationModel.setClassItem(Utils.formatClassName(link), link.label);
//            if (app.navigation.enableSubHeader) {
//                //TODO   newModel.findLayout(link.to).hasHeader = true;
//            }
//        }
        JavaClass.DrawerData data = new JavaClass.DrawerData();
        data.tabs = interModel.navigation.tabs;
        data.noSubHeader = !app.navigation.enableSubHeader;
        JavaClass javaClass = JavaClass.newDrawerActivity(newModel, data);
        javaClass.imports(androidModel.project.projectPackage + ".*");
        javaClass.imports(newModel.project.fragmentPackage + ".*");

        if (app.navigation.disableSwipe) {
            JavaClass pager = JavaClass.newNoSwipePager(newModel);
            newModel.miscellaneous.add(pager);
            newModel.miscellaneous.add(new FileTemplate.SimpleFileTemplate("drawerLayout", newModel.project.layoutFolder + AndroidModel.DRAWER_LAYOUT + ".xml", "true"));
        } else {
            newModel.miscellaneous.add(new FileTemplate("drawerLayout", newModel.project.layoutFolder + AndroidModel.DRAWER_LAYOUT + ".xml"));
        }
        if (app.navigation.enableSubHeader) {
            newModel.miscellaneous.add(new FileTemplate("headerLayout", newModel.project.layoutFolder + "header.xml"));
        }
        newModel.miscellaneous.add(javaClass);
        newModel.miscellaneous.add(new FileTemplate("drawerListItem", newModel.project.layoutFolder + "drawer_list_item.xml"));
        return newModel;
    };

    private ScreenPredicate doesSendNotification = (app, interScreen, screen) -> interScreen.sendsNotification;
    private UpdateScreen addNotification = (app, interModel, model, androidScreen, appScreen) -> {
        final AndroidScreen newScreen = androidScreen;
        interModel.intermediateScreens.get(appScreen.id).notifications.forEach(notification -> {
            UIControls.Notification notificationUI = new UIControls.Notification();
            notificationUI.icon = notification.iconDrawable;
            notificationUI.functionName = notification.functionName;
            notificationUI.to = notification.to;
            newScreen.viewController.controller = notificationUI.addToController(newScreen.viewController.controller);
        });
        return newScreen;
    };

    private ApplyTranslation getAddDrawablesClosure() {
        // variables to bind to the closure
        String pathToScriptDir = compilerConfig.pathToScriptDir;

        return (app, interModel, androidModel) -> {
            final AndroidModel newModel = androidModel;

            Consumer<String> addImageToCopy = (p) -> {
                Path path = Paths.get(p);
                if (!path.isAbsolute()) {
                    path = Paths.get(pathToScriptDir, p);
                }
                newModel.imagesToCopy.add(path.toAbsolutePath());
            };

            app.screens.forEach(screen -> {
                IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(screen.id);
                if (doesSendNotification.testScreen(app, interScreen, screen)) {
                    interModel.intermediateScreens.get(screen.id).notifications.forEach(notification ->
                            addImageToCopy.accept(notification.imageFile));
                }
                screen.view.images.forEach(p -> addImageToCopy.accept(p.image));
                screen.actions.forEach(a -> {
                    if (!a.icon.isEmpty()) addImageToCopy.accept(a.icon);
                });
            });
            return newModel;
        };
    }

    private ScreenPredicate hasCallInstructions = (app, interScreen, screen) -> interScreen.doesMakeRequest && !app.allApi.isEmpty();
    private UpdateScreen addCallInstructionAddons = (app, interModel, model, androidScreen, appScreen) -> {
        final AndroidScreen newScreen = androidScreen;
        IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(appScreen.id);
        newScreen.viewController.controller.addField(interModel.app.applicationClassName, APPLICATION_VARIABLE, "annotationApp");
        newScreen.viewController.controller.addField("AlertDialog", DIALOG_VARIABLE);

        newScreen.viewController.controller.addContent(new Template("progressDialog"));
        newScreen.viewController.controller.imports("android.app.AlertDialog", "android.app.ProgressDialog", model.project.dataPackage + ".*");

        // TODO: Refactor into controller
        newScreen.viewController.controller.addField("TextView", "message", "annotationFindView");
        newScreen.viewController.controller.imports("android.widget.TextView");
        newScreen.viewController.view.hasMessage = true;
        if (interScreen.doesShowList) newScreen.viewController.controller.imports("java.util.List");
        return newScreen;
    };

    private ScreenPredicate hasActionItems = (app, interScreen, screen) -> !screen.actions.isEmpty() || screen.featureIds.contains(app.menu.id);
    private UpdateScreen addActionItems = (app, interModel, model, androidScreen, appScreen) -> {
        final AndroidScreen newScreen = androidScreen;
        final IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(appScreen.id);
        newScreen.viewController.controller
                .classAnnotations.add(new ObjectTemplate("annotationsOptionsMenu", interScreen.menuFiles));
        Consumer<View.Action> addHandler = (a) -> {
            IntermediateModel.Action action = interModel.intermediateActions.get(a.id);
            newScreen.viewController.controller.addInstructions(action.functionName, a.onTouch, interScreen, "annotationOptionItem", interModel, action.event, model);
        };
        appScreen.actions.forEach(addHandler);
        if (interScreen.doesShowMenu) {
            app.menu.actions.forEach(addHandler);
        }
        return newScreen;
    };

    private ScreenPredicate hasExtras = (app, interScreen, screen) -> !interScreen.isFragment && interScreen.parameter != null;
    private UpdateScreen addExtras = (app, interModel, model, androidScreen, appScreen) -> {
        final AndroidScreen newScreen = androidScreen;
        IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(appScreen.id);
        newScreen.viewController.controller.addField("String", interScreen.parameter)
                .addAnnotation(new Template("annotationExtra"));
        return newScreen;
    };

    private UpdateScreen addScreenAddons = (app, interModel, model, androidScreen, appScreen) -> {
        final AndroidScreen newScreen = androidScreen;
        IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(appScreen.id);
        if (appScreen.enableHomeBack)
            newScreen.viewController.controller.classContent.add(new Template("contentHomeSelected"));
        newScreen.viewController.controller.buildOnCreateFunction(appScreen.enableHomeBack, interScreen.hasContextVariable, interScreen.isFragment);


        newScreen.viewController.controller.addContextVariable(interScreen);
        if (interScreen.hasPreferences) {
            newScreen.viewController.controller.addField(interModel.app.preferencesName + "_", PREFERENCES_VARIABLE)
                    .addAnnotation(new Template("annotationsPrefField"));
            newScreen.viewController.controller.imports(AndroidModel.ANNOTATIONS_PACKAGE + ".sharedpreferences.Pref");
        }

        if (interScreen.doesRegisterEvents) {
            newScreen.viewController.controller.addContent(new Template("contentReceiveEventData"));
        }
        return newScreen;
    };

    private UpdateScreen addViews = (app, interModel, model, androidScreen, appScreen) -> {
        final AndroidScreen newScreen = androidScreen;
        IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(appScreen.id);

        // Process maps
        appScreen.view.map.ifPresent(p -> {
            IntermediateModel.MapView map = interScreen.view.mapView;
            UIElements.Map mapView = new UIElements.Map(map.formattedId);
            newScreen.viewController.view.elements.add(mapView);

            UIControls.Map mapController = new UIControls.Map(map.formattedId);
            mapController.isNotInteractive = p.noInteractions;
            p.staticMarkers.forEach(m -> {
                UIControls.Map.StaticMarker marker = new UIControls.Map.StaticMarker();
                marker.latitude = m.latitude;
                marker.snippet = map.makers.get(m.id).descriptionVariable;
                marker.snippetStringId = map.makers.get(m.id).descriptionStingId;
                marker.title = map.makers.get(m.id).titleVariable;
                marker.titleStringId = map.makers.get(m.id).stringId;
                marker.longitude = m.longitude;

                mapController.staticMarkers.add(marker);
            });
            mapController.hasNoDynamic = p.dynamicMarkers.isEmpty();

            mapController.polyLines = map.polyLines;
            mapController.initialZoom.latitude = map.cameraLat;
            mapController.initialZoom.longitude = map.cameraLong;
            if (!map.onClickFunction.isEmpty()) {
                newScreen.viewController.controller.addInstructions(map.onClickFunction, p.onMapClick, interScreen, "", interModel, map.event, model);
                mapController.handler = map.onClickFunction;
            }
            newScreen.viewController.controller = mapController.addToController(newScreen.viewController.controller);
        });

        for (View.Label label : appScreen.view.labels) {
            newScreen.viewController = addLabel(newScreen.viewController, interModel, model, label);

        }
        for (View.Button button : appScreen.view.buttons) {
            newScreen.viewController = addButton(newScreen.viewController, interModel, model, button, interScreen);
        }

        for (View.Image p : appScreen.view.images) {
            newScreen.viewController = addImage(newScreen.viewController, interModel, model, p);
        }

        for (View.TextInput t : appScreen.view.textInputs) {
            newScreen.viewController = addTextInput(newScreen.viewController, interModel, model, t);
        }

//        interScreen.inputTexts.forEach(t -> {
//            UIElements.EditText text = new UIElements.EditText(t.formattedId, t.stringId, t.isPassword);
//
//            UIControls.EditText editText = new UIControls.EditText(t.formattedId);
//            newScreen.viewController.controller = editText.addToController(newScreen.viewController.controller);
//            newScreen.viewController.view.elements.add(text);
//        });

        // TODO: Process lists together
        appScreen.view.staticLists.forEach(l -> {
            UIElements.ListView uiListView = new UIElements.ListView(l.id);
            uiListView.hasOnItemEvent = l.onItemClick != null;
            newScreen.viewController.view.elements.add(uiListView);
            IntermediateModel.StaticList listView = interModel.intermediateLists.get(l.id);
            newScreen.viewController.controller.addInstructions(l.id, l.onItemClick, interScreen, "annotationItemClick", interModel, listView.event, model);
            UIControls.ListView uiConListView = new UIControls.ListView(l.id, listView.collectionVariable);
            newScreen.viewController.controller = uiConListView.addToController(newScreen.viewController.controller);
        });

        // JUST FOR SCREEN
        appScreen.view.webs.forEach(w -> {
            IntermediateModel.WebView webView = interModel.intermediateWebView.get(w.id);
            UIElements.WebView uiWebView = new UIElements.WebView(w.id);
            UIControls.WebView uiConWebView = new UIControls.WebView(w.id, webView.urlStringId);
            uiConWebView.handlesOAuth = webView.doesAuthenticate;
            if (!w.toId.isEmpty()) {
                uiConWebView.appClass = interModel.app.applicationClassName;
                uiConWebView.appVariable = "app";
                uiConWebView.toClass = webView.toId;
            }
            newScreen.viewController.controller = uiConWebView.addToController(newScreen.viewController.controller);
            newScreen.viewController.view.elements.add(uiWebView);
        });


        appScreen.view.dynamicLists.forEach(l -> {
            UIElements.ListView uiListView = new UIElements.ListView(l.id);
            // TODO: Need to handle list item select styles
            uiListView.hasOnItemEvent = l.onItemClick != null;
            newScreen.viewController.view.elements.add(uiListView);
            IntermediateModel.DynamicList dl = interModel.dynamicLists.get(l.id);
            UIControls.ListView uiConListView = new UIControls.ListView(l.id);
            newScreen.viewController.controller = uiConListView.addToController(newScreen.viewController.controller);
            newScreen.viewController.controller.addInstructions(l.id, l.onItemClick, interScreen, "annotationItemClick", interModel, dl.onItemClickEvent, model);
        });

        if (appScreen.view.numberOfElements() > MIN_SCROLL_LIMIT) {
            newScreen.viewController.view.enableScroll = true;
        }

        if (!appScreen.view.onLoad.instructions.isEmpty()) {
            newScreen.viewController.controller.addInstructions("init", appScreen.view.onLoad, interScreen, "annotationsAfter", interModel, interScreen.onLoadEvent, model);
        }

        if (!appScreen.view.layoutSpecification.isEmpty()) {
            newScreen.viewController.view.layout = appScreen.view.layoutSpecification;
        }
        return newScreen;
    };

    ApplyTranslation addStrings = (app, interModel, model) -> {
        final AndroidModel newModel = model;
        for (Pair<String, String> s : interModel.strings) {
            String newValue = s.getValue().replaceAll("\'", "\\\\'");
            newModel.strings.addStringValue(s.getKey(), newValue);
        }
        interModel.stringArrays.forEach(s -> {
            String[] newStrings = new String[s.getValue().length];
            for (int i = 0; i < newStrings.length; i++) {
                newStrings[i] = s.getValue()[i].replaceAll("\'", "\\\\'");
            }
            newModel.strings.addStringValues(s.getKey(), newStrings);
        });
        return newModel;
    };

    ApplyTranslation addIntegers = (app, interModel, model) -> {
        final AndroidModel newModel = model;
        newModel.integers.integerValues.addAll(interModel.integers.entrySet()
                .stream()
                .map(e -> new ImmutablePair<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));
        return newModel;
    };

    ApplyTranslation addColours = (app, interModel, model) -> {
        final AndroidModel newModel = model;
        newModel.colours.colours.add(new ImmutablePair<>(AndroidModel.PRIMARY, app.primaryColour));
        return newModel;
    };

//    private ScreenPredicate hasHeader = (app, screen) -> app.navigationMethod == AppModel.NAVIGATION.DRAWER && app.navigation.containsId(screen) && app.navigation.enableSubHeader;
//    private UpdateScreen addHeader = (app, model, androidScreen, appScreen) -> {
//        final AndroidScreen newScreen = androidScreen;
//        newScreen.controller
//                .addContent(new SimpleTemplate("contentHeader", Utils.formatHeaderStringId(appScreen.id)))
//                .imports("android.widget.TextView");
//
//        return newScreen;
//    };


    public AndroidModel buildScreens(final AppModel appModel, final IntermediateModel interModel, final AndroidModel androidModel) {
        final AndroidModel newModel = androidModel;
        for (Screen screen : appModel.screens) {
            IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(screen.id);
            DynamicLayout view = new DynamicLayout(newModel.project, interScreen.view.layoutFile);
            Controller controller = Utils.createController(newModel.project, interScreen);
            AndroidScreen androidScreen = new AndroidScreen(view, controller, screen.id);
            newModel.androidScreens.add(androidScreen);
        }
        return newModel;
    }

    public AndroidModel addScreenFunctionality(final AppModel appModel, final IntermediateModel interModel, final AndroidModel androidModel) {
        final AndroidModel newModel = androidModel;
        for (AndroidScreen androidScreen : androidModel.androidScreens) {
            Screen screen = appModel.getScreen(androidScreen.screenId);
            IntermediateModel.Screen interScreen = interModel.intermediateScreens.get(androidScreen.screenId);
            for (ScreenPredicate sp : screenFunctionality.keySet()) {
                if (sp.testScreen(appModel, interScreen, screen)) {
                    UpdateScreen updateScreen = screenFunctionality.get(sp);
                    androidScreen = updateScreen.addFunctionality(appModel, interModel, androidModel, androidScreen, screen);
                }
            }
        }
        return newModel;
    }

    // Initial configuration of the Android Model
    public AndroidModel configureModel(final AppModel appModel, final IntermediateModel interModel, final AndroidModel androidModel) {
        final AndroidModel newModel = androidModel;
        interModel.intermediateScreens.values().stream()
                .filter(s -> !s.isFragment)
                .forEach(newModel.manifest.activities::add);
        newModel.manifest.activities.add(interModel.app.navigationScreen);
        newModel.manifest.locationPermission = interModel.app.doesRequireLocation;

        androidModel.isOffline = interModel.app.provideMockData;
        androidModel.rootProjectFile = new FileTemplate("rootIDEAFile", androidModel.project.projectDir + androidModel.project.projectName + ".iml");
        newModel.manifest.internetPermissions = interModel.hasInternetAccess;
        newModel.buildScript.hasGooglePlay = interModel.app.hasMap;
        if (interModel.app.hasMap) {
            newModel.manifest.mapKey = appModel.mapKey;
        }
        newModel.buildScript.hasScribe = interModel.doesUseScribe;
        newModel.buildScript.hasPicasso = interModel.doesLoadImages;
        interModel.intermediateScreens.values().stream().filter(s -> s.isLandingPage).findFirst().ifPresent(s -> {
            newModel.buildScript.landingActivity = s.view.viewControllerName;
        });
        if (!appModel.acra.email.isEmpty()) {
            newModel.application.addAnnotation(new SimpleTemplate("annotationsAcra", appModel.acra.email));
            newModel.application.imports("org.acra.ReportField", "org.acra.annotation.ReportsCrashes", "org.acra.ACRA");
            newModel.buildScript.hasAcra = true;
            newModel.application.addContent(new Template("acraOnCreate"));
        }
        return newModel;
    }

    public AndroidModel buildFunctionality(final AppModel appModel, final IntermediateModel interModel, final AndroidModel androidModel) {
        AndroidModel newModel = androidModel;
        for (ApplyPredicate predicate : functionality.keySet()) {
            if (predicate.test(appModel, interModel)) {
                newModel = functionality.get(predicate).translation(appModel, interModel, androidModel);
            }
        }
        newModel.isOffline = interModel.app.provideMockData;
        return newModel;
    }
}
