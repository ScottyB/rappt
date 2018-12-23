package io.rappt.compiler;

import io.rappt.model.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import io.rappt.android.AndroidModel;
import io.rappt.model.*;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class IntermediateTranslator {

    FormatUtils utils = new FormatUtils();


    public IntermediateModel buildScreens(final AppModel appModel, final IntermediateModel interModel) {
        IntermediateModel newInterModel = interModel;
        appModel.screens.forEach(s -> {
            IntermediateModel.Screen screen = new IntermediateModel.Screen();
            newInterModel.intermediateScreens.put(s.id, screen);
            screen.view = transformAppComponent(appModel, interModel, s.view, false);
            screen.view.viewControllerName = utils.formatActivityClass(s.id);
            screen.view.layoutFile = utils.formatLayoutString(s);

            screen.stringTitleId = utils.formatTitleString(s);
            screen.isLandingPage = appModel.landingPage.equals(s.id);
            if (!screen.hasContextVariable)
                screen.hasContextVariable = s.enableHomeBack || !s.view.staticLists.isEmpty();
            screen.doesShowMenu = s.featureIds.contains(appModel.menu.id);
            newInterModel.strings.add(new ImmutablePair<>(screen.stringTitleId, s.label));
            screen.hasInput = !s.view.textInputs.isEmpty();
            if (s.parameter != null) {
                screen.parameter = utils.formatPassedVariable(s.parameter);
            }
        });
        return newInterModel;
    }

    public IntermediateModel buildNotifications(final AppModel appModel, final IntermediateModel interModel) {
        IntermediateModel newInterModel = interModel;
        appModel.screens.forEach(s -> s.accept(p -> {
            IntermediateModel.Notification notification = new IntermediateModel.Notification();
            if (p instanceof Instruction.Notification) {
                Instruction.Notification oldNotification = (Instruction.Notification) p;
                notification.iconDrawable = FilenameUtils.getBaseName(oldNotification.icon);
                notification.functionName = utils.formatFunction(oldNotification.id);
                notification.imageFile = oldNotification.icon;
                Optional.ofNullable(oldNotification.to).ifPresent(to -> {
                    notification.to = utils.formatActivityClass(oldNotification.to);
                });
                newInterModel.intermediateScreens.get(s.id).notifications.add(notification);
                newInterModel.intermediateScreens.get(s.id).sendsNotification = true;
            }
            if (p instanceof Instruction.StaticNotification) {
                Instruction.StaticNotification staticNotification = (Instruction.StaticNotification) p;
                newInterModel.strings.add(new ImmutablePair(utils.formatStringIdContent(staticNotification.id), staticNotification.content));
                newInterModel.strings.add(new ImmutablePair(utils.formatStringIdTitle(staticNotification.id), staticNotification.title));
            }
            if (p instanceof Instruction.DynamicNotification) {
                interModel.intermediateScreens.get(s.id).doesUseDataObject = true;
            }
        }));
        return newInterModel;
    }

    public IntermediateModel buildMainNavigation(final AppModel appModel, final IntermediateModel interModel) {
        IntermediateModel newInterModel = interModel;
        IntermediateModel.Screen tabbarScreen = new IntermediateModel.Screen();
        appModel.navigation.tabs.forEach(t -> {
            IntermediateModel.Navigation.Tabs tab = new IntermediateModel.Navigation.Tabs();
            tab.stringId = utils.formatStringId(t.id);
            newInterModel.strings.add(new ImmutablePair<>(tab.stringId, t.text));
            tab.className = FormatUtils.formatFragmentClass(t.to.toScreenId);
            IntermediateModel.Screen screen = interModel.intermediateScreens.get(t.to.toScreenId);
            if (!tabbarScreen.isLandingPage) tabbarScreen.isLandingPage = screen.isLandingPage;
            if (appModel.navigation.navigationMethod == Navigation.NAVIGATION.TABBAR)
                tabbarScreen.view.viewControllerName = screen.rootActivity = AndroidModel.TABBAR_ACTIVITY;
            if (appModel.navigation.navigationMethod == Navigation.NAVIGATION.DRAWER)
                tabbarScreen.view.viewControllerName = screen.rootActivity = AndroidModel.DRAWER_ACTIVITY;
            screen.isFragment = true;
            screen.isLandingPage = false;
            screen.view.viewControllerName = FormatUtils.formatFragmentClass(t.to.toScreenId);
            newInterModel.navigation.tabs.add(tab);
        });
        if (!appModel.navigation.tabs.isEmpty()) newInterModel.app.navigationScreen = tabbarScreen;
        return newInterModel;
    }

    public IntermediateModel processCallInstructions(final AppModel appModel, final IntermediateModel interModel) {
        IntermediateModel newInterModel = interModel;
        appModel.screens.forEach(s -> {
            s.accept(p -> {
                if (p instanceof Instruction) {
                    if (p instanceof Instruction.Call) {
                        IntermediateModel.Screen screen = newInterModel.intermediateScreens.get(s.id);
                        screen.doesMakeRequest = true;
                        screen.hasContextVariable = true;
                        Instruction.Call call = (Instruction.Call) p;
                        Api appApi = appModel.getApi(call.apiId);
                        newInterModel.intermediateScreens.get(s.id).doesShowList = appApi.resources.stream().filter(r -> r.id.equals(call.resourceId)).anyMatch(r -> r.isList) ||
                                s.view.dynamicLists.stream().anyMatch(l -> l.listFieldPath != null);
                        IntermediateModel.Resource resource = newInterModel.resources.get(call.apiId, call.resourceId);

                        s.view.dynamicLists.forEach(dl -> {
                            IntermediateModel.ListField lF = new IntermediateModel.ListField();
                            if (dl.listFieldPath != null) {
                                lF.listClassName = FormatUtils.formatClassName(dl.listFieldPath.lastPath().fieldName);
                                lF.listClassVariable= utils.formatVariable(dl.listFieldPath.lastPath().fieldName);
                            } else {
                                lF.listClassName = resource.responseClassName;
                                lF.listClassVariable = resource.fieldName;
                            }
                            interModel.resourceListFields.put(call.resourceId, lF);
                        });


                        if (!screen.hasPreferences) screen.hasPreferences = !resource.preferenceFields.isEmpty();
                        if (call.parameter != null) {
                            resource.parameterVariable = utils.formatPassedVariable(call.parameter);
                        }
                        s.view.textInputs.forEach(t -> {
                            if (t.valuePath != null) {
                                IntermediateModel.Field f = new IntermediateModel.Field();
                                f.elementId = utils.formatId(t.id);
                                f.vp = t.valuePath;
                                interModel.intermediateScreens.get(s.id).formFields.add(f);
                            }
                        });
                    }
                    if (p instanceof Instruction.ShowToast) {
                        IntermediateModel.Screen screen = newInterModel.intermediateScreens.get(s.id);
                        screen.hasContextVariable = true;
                        Instruction.ShowToast toast = (Instruction.ShowToast) p;
                        newInterModel.strings.add(new ImmutablePair<>(utils.formatStringId(toast.id), toast.label));
                    }
                }
            });
        });
        return newInterModel;
    }

    public IntermediateModel buildDynamicLists(final AppModel appModel, final IntermediateModel interModel) {
        IntermediateModel newInterModel = interModel;
        appModel.screens.forEach(s -> {
            s.view.dynamicLists.forEach(dl -> {

                IntermediateModel.DynamicList list = new IntermediateModel.DynamicList();
                list.adapterClassName = FormatUtils.formatDataClassName(dl.id) + "Adapter";
                newInterModel.app.hasDynamicList = true;
                dl.listItemLayouts.forEach(listItem -> {
                    View layout = listItem.layout;
                    IntermediateModel.ListItem v = (IntermediateModel.ListItem) transformAppComponent(appModel, interModel, layout, true);
                    v.stringId = utils.formatStringId(layout.id);
                    v.functionName = utils.formatViewFunction(layout.id);
                    v.stateVariable = utils.formatVariable(layout.id);
                    newInterModel.strings.add(new ImmutablePair<>(v.stringId, layout.stateValue));



                    // TODO: Refactor view creation for screen and lists
                    v.viewControllerName = utils.formatDataItemClass(layout.id);
                    v.layoutFile = utils.formatLayoutString(layout.id);
                    //newInterModel =
                    transformAppModel(appModel, interModel, layout, true);

                    list.views.put(layout.id, v);
                    newInterModel.dynamicLists.put(dl.id, list);
                    newInterModel.intermediateScreens.get(s.id).dynamicListVar = dl.id;
                    list.onItemClickEvent = buildEvent(appModel, interModel, dl.onItemClick);

                });
            });
        });
        return newInterModel;
    }


    public IntermediateModel buildModel(final AppModel appModel, final IntermediateModel interModel) {
        IntermediateModel newInterModel = interModel;
        appModel.menu.actions.forEach(a -> buildAction(appModel, newInterModel, a, appModel.menu.id));
        appModel.screens.forEach(s -> {
            IntermediateModel.Screen screen = newInterModel.intermediateScreens.get(s.id);
            s.featureIds.forEach(id -> {
                if (id.equals(appModel.menu.id)) {
                    screen.menuFiles.add(utils.formatLayoutString(appModel.menu.id));
                }
            });

            s.actions.forEach(a -> {
                buildAction(appModel, newInterModel, a, s.id);
                screen.menuFiles.add(utils.formatLayoutString(s));
            });

            s.view.staticLists.forEach(sl -> {
                IntermediateModel.StaticList list = new IntermediateModel.StaticList();
                list.itemVariable = utils.formatVariable(sl.itemName);
                list.collectionVariable = utils.formatVariable(sl.staticContentVariable);
                newInterModel.stringArrays.add(new ImmutablePair<>(list.collectionVariable,
                        sl.staticContent.toArray(new String[sl.staticContent.size()])));
                list.event = buildEvent(appModel, interModel, sl.onItemClick);
                newInterModel.intermediateLists.put(sl.id, list);
            });
            transformAppModel(appModel, interModel, s.view, false);
        });
        //IntermediateModel lastModel = appModel.tracker.transformModel(utils, newInterModel);
        return newInterModel;
    }

    public IntermediateModel processWholeModel(final AppModel appModel, final IntermediateModel interModel) {
        IntermediateModel newInterModel = interModel;
        appModel.screens.forEach(s -> {

            s.accept(p -> {
                if (p instanceof Instruction.Navigate) {
                    Instruction.Navigate navigate = (Instruction.Navigate) p;
                    IntermediateModel.Screen fromScreen = newInterModel.intermediateScreens.get(s.id);
                    IntermediateModel.Screen toScreen = newInterModel.intermediateScreens.get(navigate.toScreenId);
                    fromScreen.doesUseDataObject = navigate.fieldParameter != null;
                    boolean isFragmentToFragment = fromScreen.isFragment && toScreen.isFragment;
                    newInterModel.isFragmentToFragment.put(navigate.parameterId, isFragmentToFragment);
                    if (isFragmentToFragment) toScreen.doesRegisterEvents = true;
                    if (isFragmentToFragment ||
                            (!fromScreen.isFragment && toScreen.isFragment)
                        // TODO: Case needs more work
                        // ||   (fromScreen.isFragment && !toScreen.isFragment)
                            ) {
                        String eventClassName = utils.formatEventDataClass(s.id, navigate.toScreenId);
                        fromScreen.eventClassNames.put(navigate.id, eventClassName);
                        toScreen.eventClassNames.put(navigate.id, eventClassName);
                        fromScreen.sendsEventObjects.put(navigate.id, newInterModel.hasEventObjects = true);
                        if (!newInterModel.eventClasses.putAll(eventClassName, navigate.parameters.stream().map(utils::formatPassedVariable).collect(toList())) &&
                                !newInterModel.eventClasses.put(eventClassName, utils.formatPassedVariable(navigate.parameterId))) {
                            newInterModel.eventClasses.put(eventClassName, "");
                        }
                    } else {
                        fromScreen.sendsEventObjects.put(navigate.id, false);
                    }
                    interModel.navClasses.put(navigate.id, navigate.toScreenId, s.id);
                }
            });

            IntermediateModel.Screen fromScreen = newInterModel.intermediateScreens.get(s.id);
            fromScreen.onLoadEvent = buildEvent(appModel, interModel, s.view.onLoad);
            s.view.map.ifPresent(m -> {
                IntermediateModel.MapView uiField = m.transformComponent(utils);
                fromScreen.view.mapView = uiField;
                interModel.intermediateScreens.get(s.id).view.mapView.event = buildEvent(appModel, interModel, m.onMapClick);
            });
            s.view.webs.forEach(w -> {
                IntermediateModel.WebView webView = new IntermediateModel.WebView();
                webView.doesAuthenticate = !w.apiId.isEmpty();
                if (!w.toId.isEmpty()) {
                    webView.toId = NavigationFlowBuilder.toActivity(interModel, w.toId);
                } else {
                    webView.urlStringId = utils.formatStringId(w.id);
                    newInterModel.strings.add(new ImmutablePair<>(webView.urlStringId, w.url));
                }
                newInterModel.intermediateWebView.put(w.id, webView);
                fromScreen.hasContextVariable = fromScreen.doesMakeRequest = newInterModel.hasInternetAccess = true;
            });
        });
        return newInterModel;
    }

    public IntermediateModel buildApiAndDataModel(final AppModel appModel, final IntermediateModel model) {
        final IntermediateModel newModel = model;
        appModel.allApi.forEach(a -> {
            IntermediateModel.Api api = new IntermediateModel.Api();
            api.className = utils.formatClassName(a);
            api.variableName = utils.formatVariable(a.id);
            api.offlineClassName = "Offline" + utils.formatClassName(a);
            api.setupApiFunction = "setup" + utils.formatClassName(a);
            a.oauth.ifPresent(oauth -> {
                newModel.doesUseScribe = api.hasOAuth = true;
                api.oAuth = oauth;
            });
            newModel.intermediateApis.put(a.id, api);
            if (a.createMockData) newModel.app.provideMockData = a.createMockData;
            a.resources.forEach(r -> {
                IntermediateModel.Resource resource = r.transformComponent(utils, appModel, model);
                api.resources.put(r.id, resource);
                model.resources.put(a.id, r.id, resource);
                if (r.isAuthEndPoint) api.authResource = r;
                api.servicesStatic = a.id.toUpperCase() + "_SERVICE";
            });
        });
        return newModel;
    }

    public IntermediateModel buildSharedPreferences(final AppModel appModel, final IntermediateModel model) {
        IntermediateModel newModel = model;
        Collection<IntermediateModel.SharedPreference> preferences = new ArrayList<>();
        appModel.allApi.forEach(a -> a.resources.forEach(r -> {
            IntermediateModel.Resource resource = model.resources.get(a.id, r.id);
            if (!r.saveValuePaths.isEmpty()) {
                newModel.app.hasPreferences = true;
                newModel.app.preferencesName = utils.formatPrefsName(appModel.projectName);
            }
            r.saveValuePaths.forEach((k, vp) -> {
                IntermediateModel.SharedPreference pref = new IntermediateModel.SharedPreference();
                pref.functionName = utils.formatVariable(k);
                pref.type = utils.formatFieldTypeToJavaType(vp.lastPath());
                preferences.add(pref);
                IntermediateModel.Field field = new IntermediateModel.Field();
                field.vp = vp;
                field.elementId = pref.functionName;
                resource.preferenceFields.add(field);
            });
        }));
        newModel.preferences = preferences;
        return newModel;
    }


    public IntermediateModel buildDataModel(final AppModel appModel, final IntermediateModel model) {
        IntermediateModel newModel = model;
        DataModelBuilder builder = new DataModelBuilder();
        newModel = builder.addFieldsForRequest(appModel, newModel);
        newModel = builder.buildDataModel(appModel, newModel);
        return newModel;
    }

    // TODO: Don't add empty string fields or add message for empty strings
    // TODO: Fix unused resources, some internal check
    public IntermediateModel buildUiFieldStrings(final AppModel appModel, final IntermediateModel model) {
        IntermediateModel newModel = model;
        appModel.screens.forEach(s -> {
            s.view.textInputs.forEach(b -> newModel.strings.add(new ImmutablePair<>(utils.formatStringId(b.id), b.hintText)));
            s.view.buttons.forEach(b -> newModel.strings.add(new ImmutablePair<>(utils.formatStringId(b.id), b.label)));
            s.view.labels.forEach(b -> {
                if (!b.label.isEmpty()) newModel.strings.add(new ImmutablePair<>(utils.formatStringId(b.id), b.label));
            });
            s.view.dynamicLists.forEach(l -> l.listItemLayouts.forEach(i -> {
                i.layout.labels.forEach(lab -> {
                    if (!lab.label.isEmpty())
                        newModel.strings.add(new ImmutablePair<>(utils.formatStringId(lab.id), lab.label));
                });
                i.layout.buttons.forEach(b -> newModel.strings.add(new ImmutablePair<>(utils.formatStringId(b.id), b.label)));
            }));
            s.view.map.ifPresent(p -> p.staticMarkers.forEach(m -> {
                newModel.strings.add(new ImmutablePair<>(utils.formatMarkerTitleStringId(m.id), m.title));
                newModel.strings.add(new ImmutablePair<>(utils.formatMarkerDescriptionStringId(m.id), m.description));
            }));
        });
        return newModel;
    }

    public IntermediateModel.Action buildAction(final AppModel appModel, IntermediateModel model, View.Action viewAction, String id) {
        IntermediateModel.Action action = viewAction.transformComponent(utils, appModel, model, id);
        viewAction.transformModel(utils, model);
        return action;
    }

    private IntermediateModel.Event buildEvent(final AppModel app, IntermediateModel interModel, Event event) {
        IntermediateModel.Event interEvent = event.transformComponent(utils, app, interModel);
        interModel = event.transformModel(utils, interModel);
        return interEvent;
    }

    public IntermediateModel buildMiscellaneous(AppModel appModel, IntermediateModel interModel) {
        IntermediateModel newInterModel = interModel;
        newInterModel.app.hasFragment = interModel.intermediateScreens.values()
                .stream().anyMatch(s -> s.isFragment);
        return newInterModel;
    }

    public IntermediateModel.View transformAppComponent(final AppModel appModel, final IntermediateModel interModel, View oldView, final boolean aListItem) {
        final IntermediateModel.View view = aListItem ? new IntermediateModel.ListItem() : new IntermediateModel.View();
        oldView.labels.forEach(l -> l.valuePath.ifPresent(v -> {
            IntermediateModel.Field f = new IntermediateModel.Field();
            f.vp = v;
            f.elementId = utils.formatViewId(l.id);
            view.pathsToShow.add(f);
        }));
        oldView.images.forEach(l -> {
            if (!l.valuePath.path.isEmpty()) {
                IntermediateModel.Field f = new IntermediateModel.Field();
                f.vp = l.valuePath;
                f.elementId = utils.formatViewId(l.id);
                view.pathsToShow.add(f);
            }
        });
        return view;
    }

    // View
    public IntermediateModel transformAppModel(final AppModel appModel, final IntermediateModel interModel, final View view, final boolean aListItem) {
        final IntermediateModel newInterModel = interModel;

        view.buttons.forEach(b -> {
            IntermediateModel.Button button = b.transformComponent(utils);
            button.shownOnListItem = aListItem;
            button.event = buildEvent(appModel, interModel, b.onTouch);
            newInterModel.intermediateButtons.put(b.id, button);
        });

        view.labels.forEach(l -> {
            IntermediateModel.Label label = l.transformComponent(utils);
            label.shownOnListItem = aListItem;
            newInterModel.intermediateLabels.put(l.id, label);
        });

        view.images.forEach(p -> {
            newInterModel.doesLoadImages = newInterModel.doesLoadImages ? true : !p.valuePath.path.isEmpty();
            newInterModel.images.put(p.id, p.transformComponent(utils));
        });

        view.map.ifPresent(m -> {
            newInterModel.app.hasMap = true;
            newInterModel.hasInternetAccess = true;
        });

        view.textInputs.forEach(t -> {
            IntermediateModel.InputText uiField = new IntermediateModel.InputText();
            uiField.formattedId = utils.formatId(t.id);
            uiField.stringId = utils.formatStringId(t.id);
            uiField.hintTextId = t.hintText;
            if (t.valuePath != null)
                uiField.isPassword = t.valuePath.lastPath().fieldType == ValuePath.FIELD_TYPE.PASSWORD;
            newInterModel.intputText.put(t.id, uiField);
        });


        return newInterModel;
    }
}
