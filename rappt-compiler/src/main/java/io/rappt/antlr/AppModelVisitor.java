package io.rappt.antlr;

import io.rappt.model.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import io.rappt.AMLBaseVisitor;
import io.rappt.AMLParser;
import io.rappt.model.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class AppModelVisitor extends AMLBaseVisitor {

    private AppModel appModel;

    private static final String STYLE_BODY = "TextBody";
    private static final String STYLE_CAPTION = "TextCaption";
    private static final String STYLE_HEADING = "TextHeading";

    public AppModelVisitor(final AppModel model) {
        appModel = model;
    }

    public AppModel getModel() {
        return appModel;
    }

    static public boolean hasProp(ParserRuleContext prc, int tokenNumber) {
        return prc.getToken(tokenNumber, 0) != null;
    }

    @Override
    public Object visitPrimary(@NotNull AMLParser.PrimaryContext ctx) {
        appModel.primaryColour = AppModelUtils.formatSTRING(ctx.STRING());
        return super.visitPrimary(ctx);
    }

    @Override
    public Object visitApp(@NotNull AMLParser.AppContext ctx) {
        List<AMLParser.AppPropertiesContext> properties = ctx.appProperties();
        if (properties != null) {
            for (AMLParser.AppPropertiesContext p : properties) {
                if (hasProp(p, AMLParser.ANDROID_SDK)) {
                    appModel.androidSdk = AppModelUtils.formatSTRING(p.STRING());
                }
                if (p.acra() != null) {
                    appModel.acra.email = AppModelUtils.formatSTRING(p.acra().STRING());
                }
                if (hasProp(p, AMLParser.LANDING_PAGE)) {
                    appModel.landingPage = p.variable().ID().getText();
                }
                if (hasProp(p, AMLParser.MAP_KEY)) {
                    appModel.mapKey = AppModelUtils.formatSTRING(p.STRING());
                }
            }
        }
        return super.visitApp(ctx);
    }


    @Override
    public Object visitMenu(@NotNull AMLParser.MenuContext ctx) {
        appModel.menu = new Feature(ctx.variableDeclaration().ID().getText());
        for (AMLParser.ActionContext a : ctx.action()) {
            appModel.menu.actions.add(retrieveAction(a));
        }
        return super.visitMenu(ctx);
    }

    @Override
    public Object visitNavigation(@NotNull AMLParser.NavigationContext ctx) {
        appModel.navigation = new Navigation(ctx.variableDeclaration().ID().getText());
        appModel.navigation.navigationMethod = Navigation.NAVIGATION.valueOf(ctx.NAV_TYPE().toString().toUpperCase());
        for (AMLParser.NavigationPropertiesContext l : ctx.navigationProperties()) {
            if (l.tab() != null) {
                Navigation.Tab tab = new Navigation.Tab();
                tab.text = AppModelUtils.formatSTRING(l.tab().STRING());
                tab.to = buildTo(l.tab().to(), l.tab().variableDeclaration().ID().toString());
                tab.id = l.tab().variableDeclaration().ID().toString();
                appModel.navigation.tabs.add(tab);
            }
//            if (hasProp(l, AMLParser.DISABLE_SWIPE)) {
//                appModel.navigation.disableSwipe = true;
//            }
//            if (hasProp(l, AMLParser.SUB_HEADER)) {
//                appModel.navigation.enableSubHeader = true;
//            }
        }
        return super.visitNavigation(ctx);
    }


    @Override
    public Object visitSource(@NotNull AMLParser.SourceContext ctx) {
        Source source = (Source) appModel.ids.get(ctx.variableDeclaration().ID().getText());
        ctx.sourceProperties().forEach(s -> {
            if (s.END_POINT() != null) {
                String apiId = s.variable(0).getText();
                String resourceId = s.variable(1).getText();
                source.api = (Api) appModel.ids.get(apiId);
                source.resource = (Resource) appModel.ids.get(resourceId);
            }
        });
        if (ctx.sourceBlock() != null) {
            ctx.sourceBlock().sourceExtras().forEach(se -> {
                String id = se.variable().ID().getText();
                ValuePath path = AppModelUtils.buildField(se.field());
                if (se.BIND_FROM() != null) {
                    source.fromBindings.put(id, path);
                } else if (se.BIND_TO() != null) {
                    source.toBindings.put(id, path);
                }
            });

        }
        return super.visitSource(ctx);
    }

    public View retrieveView(AMLParser.UiBlockContext viewContext) {
        View view = new View(viewContext.variableDeclaration().ID().getText());
        if (viewContext.STRING() != null) {
            view.stateValue = AppModelUtils.formatSTRING(viewContext.STRING());
        }

        for (AMLParser.ViewPropertiesContext v : viewContext.viewProperties()) {

            if (v.map() != null) {
                view.map = Optional.of(retrieveMap(v.map()));
                appModel.ids.put(view.map.get().id, view.map.get());
            }
            if (v.label() != null) {
                View.Label label = retrieveLabel(v.label());
                label.styleName = styleName(v);
                view.labels.add(label);
            }
            if (v.image() != null) {
                View.Image image = retrieveImage(v.image());
                image.styleName = styleName(v);
                view.images.add(image);
            }
            if (v.textInput() != null) {
                View.TextInput textInput = retrieveTextInput(v.textInput());
                textInput.styleName = styleName(v);
                view.textInputs.add(textInput);
            }
            if (v.button() != null) {
                View.Button button = retrieveButton(v.button());
                button.styleName = styleName(v);
                view.buttons.add(button);
            }
            if (v.web() != null) {
                view.webs.add(retrieveWeb(v.web()));
            }
            if (v.list() != null) {
                if (v.list().listProperties().stream().anyMatch(p -> p.data() != null)) {
                    view.staticLists.add(retrieveStaticList(v.list()));
                } else {
                    view.dynamicLists.add(retrieveDynamicList(v.list()));
                }
            }

            if (v.layout() != null) {
                view.layoutSpecification = v.layout().layout_spec().getText().toString();

            }
        }
        return view;
    }

    private View.DynamicList retrieveDynamicList(AMLParser.ListContext listContext) {
        View.DynamicList dynamicList = new View.DynamicList(listContext.variableDeclaration().ID().getText());
        for (AMLParser.ListPropertiesContext l : listContext.listProperties()) {
            dynamicList = (View.DynamicList) buildUICollection(l, dynamicList);
        }
        return dynamicList;
    }

    private UICollection buildUICollection(AMLParser.ListPropertiesContext propertiesContext, final UICollection oldUiCollection) {
        UICollection newUiCollection = oldUiCollection;
        if (propertiesContext.uiBlock() != null) {
            UICollection.ListItem listItem = new UICollection.ListItem();
            listItem.layout = retrieveView(propertiesContext.uiBlock());
            oldUiCollection.listItemLayouts.add(listItem);
        }
        if (propertiesContext.ON_ITEM_CLICK() != null) {
            for (AMLParser.InstructionsContext b : propertiesContext.instructionBlock().instructions()) {
                retrieveInstruction(b, oldUiCollection.id).ifPresent(oldUiCollection.onItemClick.instructions::add);
            }
        }
        return newUiCollection;
    }

    private View.StaticList retrieveStaticList(AMLParser.ListContext listContext) {
        View.StaticList staticList = new View.StaticList(listContext.variableDeclaration().ID().getText());
        for (AMLParser.ListPropertiesContext l : listContext.listProperties()) {
            if (l.data() != null) {
                staticList.staticContentVariable = l.data().variableDeclaration().ID().toString();
                staticList.itemName = l.data().ID().toString();
                staticList.staticContent = l.data().STRING().stream()
                        .map(f -> AppModelUtils.formatSTRING(f))
                        .collect(Collectors.toList());
            }
            staticList = (View.StaticList) buildUICollection(l, staticList);
        }
        return staticList;
    }

    private View.Web retrieveWeb(AMLParser.WebContext webContext) {
        View.Web web = new View.Web(webContext.variableDeclaration().ID().toString());
        if (webContext.STRING() != null) {
            web.url = AppModelUtils.formatSTRING(webContext.STRING());
        } else {
            web.apiId = webContext.ID(0).getText();
            web.toId = webContext.ID(1).getText();
        }
        return web;
    }


    private Instruction.Navigate buildTo(AMLParser.ToContext to, String rootId) {
        String toId = to.variable(0).getText();
        Instruction.Navigate navigate = new Instruction.Navigate(rootId + "_" + toId, toId);
        if (to.variable().size() > 1) {
            navigate.parameterId = to.variable(1).ID().getText();
            navigate.fieldParameter = AppModelUtils.buildField(to.field());
        }
        return navigate;
    }

    private Optional<Instruction> retrieveInstruction(AMLParser.InstructionsContext b, String rootId) {
        Instruction instruction = null;
        if (b.to() != null) {
            instruction = buildTo(b.to(), rootId);
        }
        // TODO: Non unique URL action
        if (b.toUrl() != null) {
            String url = AppModelUtils.formatSTRING(b.toUrl().STRING());
            instruction = new Instruction.Url(rootId + "_url", url);
        }

        if (b.call() != null) {
            String apiId = b.call().variable(0).ID().getText();
            String resourceId = b.call().variable(1).ID().getText();
            Instruction.Call call = new Instruction.Call(apiId + resourceId, apiId, resourceId);
            if (b.call().PASSED() != null)
                call.parameter = b.call().ID().getText();
            instruction = call;
        }

        if (b.getPreference() != null) {
            String prefId = b.getPreference().variable(0).ID().toString();
            String viewId = b.getPreference().variable(1).ID().toString();
            String id = rootId + "_" + prefId;
            instruction = new Instruction.GetPreference(id, viewId, prefId);
        }

        if (b.removePreference() != null) {
            instruction = new Instruction.RemovePreference(b.removePreference().variable().ID().toString());
        }

        if (b.showToast() != null) {
            instruction = retrieveToast(b.showToast());
        }

        if (b.notification() != null) {
            instruction = retrieveNotification(b.notification());
        }

        if (b.currentLocation() != null) {
            instruction = new Instruction.CurrentLocation(b.currentLocation().ID().getText());
        }

        return Optional.ofNullable(instruction);
    }

    private View.Button retrieveButton(AMLParser.ButtonContext buttonContext) {
        View.Button button = (View.Button) appModel.ids.get(buttonContext.variableDeclaration().ID().getText());
        for (AMLParser.LabelPropertiesContext p : buttonContext.labelProperties()) {
            if (p.TEXT() != null) {
                button.label = AppModelUtils.formatSTRING(p.STRING());
            }
        }
        return button;
    }

    private Instruction.ShowToast retrieveToast(AMLParser.ShowToastContext toastContext) {
        String id = toastContext.variableDeclaration().ID().getText();
        String label = AppModelUtils.formatSTRING(toastContext.STRING());
        Instruction.ShowToast showToast = new Instruction.ShowToast(id, label);
        return showToast;
    }

    private View.Label retrieveLabel(AMLParser.LabelContext labelContext) {
        View.Label label = (View.Label) appModel.ids.get(labelContext.variableDeclaration().ID().getText());
        if (labelContext.PASSED() != null) {
            //label.parameterId = id;
            label.isPassed = true;
        } else {
            for (AMLParser.LabelPropertiesContext p : labelContext.labelProperties()) {
                if (p.TEXT() != null) {
                    label.label = AppModelUtils.formatSTRING(p.STRING());
                }
            }
        }
        return label;
    }

    private View.TextInput retrieveTextInput(AMLParser.TextInputContext textInputContext) {
        String id = textInputContext.variableDeclaration().ID().getText();
        View.TextInput textInput = new View.TextInput(id);
        for (AMLParser.TextInputPropertiesContext tp : textInputContext.textInputProperties()) {
            if (tp.HINT() != null) {
                String hintText = AppModelUtils.formatSTRING(tp.STRING());
                textInput.hintText = hintText;
            }
            if (tp.BINDING() != null) {
                textInput.valuePath = AppModelUtils.buildField(tp.field());
            }
        }
        return textInput;
    }

    private View.Image retrieveImage(AMLParser.ImageContext imageContext) {
        String id = imageContext.variableDeclaration().ID().getText();
        View.Image image = new View.Image(id, "");
        for (AMLParser.ImagePropertiesContext ip : imageContext.imageProperties()) {
            if (ip.BINDING() != null) {
                image = new View.Image(id, AppModelUtils.buildField(ip.field()));
            } else if (ip.FILE() != null) {
                image = new View.Image(id, AppModelUtils.formatSTRING(ip.STRING()));
            }
        }
        return image;
    }

    private View.Map addMarkers(AMLParser.MarkerContext mc, final View.Map map) {
        View.Map newMap = map;
        boolean isDynamic = mc.markerProperties().stream().anyMatch(s -> s.field() != null);
        String id = mc.variableDeclaration().ID().getText();
        if (isDynamic) {
            View.Map.DynamicMarker marker = new View.Map.DynamicMarker();
            marker.id = id;
            for (AMLParser.MarkerPropertiesContext mp : mc.markerProperties()) {
                if (mp.LAT() != null) {
                    marker.latitudePath = AppModelUtils.buildField(mp.field());
                } else if (mp.LONG() != null) {
                    marker.longitudePath = AppModelUtils.buildField(mp.field());
                } else if (mp.TITLE() != null) {
                    marker.titlePath = AppModelUtils.buildField(mp.field());
                } else if (mp.TEXT() != null) {
                    marker.descriptionPath = AppModelUtils.buildField(mp.field());
                }
            }
            map.dynamicMarkers.add(marker);
        } else {
            View.Map.StaticMarker marker = new View.Map.StaticMarker();
            marker.id = id;
            for (AMLParser.MarkerPropertiesContext mp : mc.markerProperties()) {
                if (mp.LAT() != null) {
                    marker.latitude = Double.valueOf(mp.NUMBER().getText());
                } else if (mp.LONG() != null) {
                    marker.longitude = Double.valueOf(mp.NUMBER().getText());
                } else if (mp.TITLE() != null) {
                    marker.title = AppModelUtils.formatSTRING(mp.STRING());
                } else if (mp.TEXT() != null) {
                    marker.description = AppModelUtils.formatSTRING(mp.STRING());
                }
            }
            map.staticMarkers.add(marker);
        }
        return newMap;
    }

    private View.Map retrieveMap(AMLParser.MapContext mapContext) {
        String id = mapContext.variableDeclaration().ID().getText();
        View.Map map = new View.Map(id);
        for (AMLParser.MapPropertiesContext p : mapContext.mapProperties()) {
            if (p.marker() != null) {
                map = addMarkers(p.marker(), map);
            }
            if (p.NO_INTERACTIONS() != null) map.noInteractions = true;
            if (p.polyline() != null) {
                View.Map.PolyLine polyLine = new View.Map.PolyLine();
                polyLine.markerStartId = p.polyline().variable(0).ID().getText();
                polyLine.markerEndId = p.polyline().variable(1).ID().getText();
                map.polyLines.add(polyLine);
            }
            if (p.ON_MAP_CLICK() != null) {
                AMLParser.InstructionBlockContext block = p.instructionBlock();
                for (AMLParser.InstructionsContext b : block.instructions()) {
                    Optional<Instruction> instruction = retrieveInstruction(b, map.id);
                    // TODO: tidy up use of optional
                    if (instruction.isPresent()) {
                        map.onMapClick.instructions.add(instruction.get());
                        if (block.CLOSE() != null) {
                            map.onMapClick.doesCloseActivity = Boolean.parseBoolean(block.BOOL().getText());
                        }
                    }
                }
            }
        }
        return map;
    }

    @Override
    public Object visitScreen(@NotNull AMLParser.ScreenContext ctx) {
        Screen screen = (Screen) appModel.ids.get(ctx.variableDeclaration().ID().getText());
        appModel.screens.add(screen);
        if (ctx.screenParams() != null) {
            screen.parameter = ctx.screenParams().variableDeclaration().ID().getText();
        }
        List<AMLParser.ScreenPropertiesContext> properties = ctx.screenProperties();
        if (properties != null) {
            for (AMLParser.ScreenPropertiesContext s : properties) {
                if (s.screenView() != null) {
                    screen.view = retrieveView(s.screenView().uiBlock());
                }
                if (s.screenModel() != null) {
                    screen.model = retrieveModel(s.screenModel());
                }
                if (hasProp(s, AMLParser.BACK)) {
                    screen.enableHomeBack = Boolean.parseBoolean(s.BOOL().getText());
                }
                if (hasProp(s, AMLParser.FEATURES)) {
                    s.idArray().variable().stream().forEach(id -> {
                        screen.featureIds.add(id.ID().getText());
                    });
                }
                if (hasProp(s, AMLParser.PULL_TO_REFRESH)) {
                    screen.pullToRefresh = Boolean.parseBoolean(s.BOOL().getText());
                }
                if (s.action() != null) {
                    screen.actions.add(retrieveAction(s.action()));
                }
                if (hasProp(s, AMLParser.TITLE)) {
                    String label = AppModelUtils.formatSTRING(s.STRING());
                    screen.addLabel(label);
                }
            }
        }
        return super.visitScreen(ctx);
    }

    private Model retrieveModel(AMLParser.ScreenModelContext screenModelContext) {
        Model model = new Model();
        screenModelContext.source().forEach(s -> {
            Source source = (Source) appModel.ids.get(s.variableDeclaration().getText());
            model.dataSources.add(source);
        });
        return model;
    }

    public View.Action retrieveAction(AMLParser.ActionContext actionContext) {
        View.Action action = new View.Action(actionContext.variableDeclaration().ID().getText());
        action.label = AppModelUtils.formatSTRING(actionContext.STRING(0));
        if (actionContext.STRING().size() > 1) {
            action.icon = AppModelUtils.formatSTRING(actionContext.STRING(1));
        }
        if (actionContext.instructionBlock() != null) {
            AMLParser.InstructionBlockContext block = actionContext.instructionBlock();
            for (AMLParser.InstructionsContext b : block.instructions()) {
                retrieveInstruction(b, action.id).ifPresent(i -> {
                    action.onTouch.instructions.add(i);
                    if (block.CLOSE() != null) {
                        action.onTouch.doesCloseActivity = Boolean.parseBoolean(block.BOOL().getText());
                    }
                });
            }
        }
        return action;
    }

    @Override
    public Object visitApi(@NotNull AMLParser.ApiContext ctx) {
        String name = ctx.variableDeclaration().ID().getText();
        Api api = (Api) appModel.ids.get(name);
        appModel.allApi.add(api);
        ctx.apiProperties().forEach(p -> {
            if (p.END_POINTS() != null) {
                for (AMLParser.ResourceContext r : p.resource()) {
                    Resource resource = (Resource) appModel.ids.get(r.variableDeclaration().ID().getText());
                    r.resourceProperties().forEach(rp -> {
                        if (hasProp(rp, AMLParser.END_POINT)) {
                            String endPoint = AppModelUtils.formatSTRING(rp.STRING());
                            if (endPoint.contains("{")) {
                                resource.urlParam = endPoint.replaceAll(".*\\{|\\}.*", "");
                            }
                            resource.endPoint = endPoint;
                        }
                        if (hasProp(rp, AMLParser.RETURNS_LIST)) {
                            resource.isList = Boolean.parseBoolean(rp.BOOL().getText());
                        }
                        if (rp.field() != null) {
                            resource.stateFieldPath = AppModelUtils.buildField(rp.field());
                        }
                        if (rp.PUT() != null) {
                            String prefId = rp.variableDeclaration().ID().toString();
                            resource.saveValuePaths.put(prefId, AppModelUtils.buildField(rp.field()));
                        }
                    });
                    //todo: kill INIT :P
                    if (r.INIT() != null) {
                        resource.isAuthEndPoint = true;
                        resource.tokenFieldPath = AppModelUtils.buildField(r.field());

                    }
                    api.resources.add(resource);
                }
            }
            if (hasProp(p, AMLParser.MOCK_DATA)) {
                api.createMockData = Boolean.parseBoolean(p.BOOL().getText());
            }
            if (hasProp(p, AMLParser.BASE)) {
                String url = AppModelUtils.formatSTRING(p.STRING());
                api.rootURL = url;
            }
            if (p.OAUTH() != null) api.oauth = Optional.of(buildOAuth(p.oauthProperties()));
            if (p.AUTH() != null) {
                api.isParseApp = p.PARSE() != null;
                api.isTokenApp = p.TOKEN() != null;
                p.authProperties().forEach(ap -> {
                    if (ap.API_KEY() != null) {
                        api.apiParamValue = AppModelUtils.formatSTRING(ap.STRING());
                        } else if (ap.TOKEN_PAR() != null) {
                        api.apiParamKey = AppModelUtils.formatSTRING(ap.STRING());
                    }
                });
                p.parseProps().forEach(pp -> {
                    if (pp.APP_ID() != null) api.appId = AppModelUtils.formatSTRING(pp.STRING());
                    if (pp.CLIENT_KEY() != null) api.clientKey = AppModelUtils.formatSTRING(pp.STRING());
                });
            }
        });
        return super.visitApi(ctx);
    }

    @Override
    public Object visitStyle(@NotNull AMLParser.StyleContext ctx) {
        Style style = (Style) appModel.ids.get(ctx.variableDeclaration().ID().getText());
        appModel.styles.add(style);
        return super.visitStyle(ctx);
    }

    public OAuth buildOAuth(List<AMLParser.OauthPropertiesContext> oauthProperties) {
        OAuth oAuth = new OAuth();
        for (AMLParser.OauthPropertiesContext op : oauthProperties) {
            if (hasProp(op, AMLParser.API_KEY)) {
                oAuth.apiKey = AppModelUtils.formatSTRING(op.STRING());
            } else if (hasProp(op, AMLParser.API_SECRET)) {
                oAuth.apiSecret = AppModelUtils.formatSTRING(op.STRING());
            } else if (hasProp(op, AMLParser.API_PROVIDER)) {
                oAuth.apiProvider = AppModelUtils.formatSTRING(op.STRING());
            } else if (hasProp(op, AMLParser.CALLBACK)) {
                oAuth.callback = AppModelUtils.formatSTRING(op.STRING());
            } else if (hasProp(op, AMLParser.API_VERIFIER_PARAMETER)) {
                oAuth.apiVerifierParameter = AppModelUtils.formatSTRING(op.STRING());
            }
        }
        return oAuth;
    }

    public Instruction retrieveNotification(AMLParser.NotificationContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        Instruction.Notification notification;
        if (ctx.field().isEmpty()) {
            Instruction.StaticNotification staticNotification = new Instruction.StaticNotification(id);
            staticNotification.title = AppModelUtils.formatSTRING(ctx.STRING(1));
            staticNotification.content = AppModelUtils.formatSTRING(ctx.STRING(2));
            notification = staticNotification;
        } else {
            Instruction.DynamicNotification dynamicNotification = new Instruction.DynamicNotification(id);
            dynamicNotification.titlePath = AppModelUtils.buildField(ctx.field(0));
            dynamicNotification.contentPath = AppModelUtils.buildField(ctx.field(1));
            notification = dynamicNotification;
        }
        notification.icon = AppModelUtils.formatSTRING(ctx.STRING(0));
        Optional<AMLParser.NotificationBlockContext> blockContext = Optional.ofNullable(ctx.notificationBlock());
        blockContext.ifPresent(b -> notification.to = ctx.notificationBlock().variable().ID().getText());
        return notification;
    }

    String styleName(AMLParser.ViewPropertiesContext viewProperty) {
        String style = "";
        if (viewProperty.styleReference() != null) {
            if (viewProperty.styleReference().BODY() != null) {
                style = STYLE_BODY;
            } else if (viewProperty.styleReference().CAPTION() != null) {
                style = STYLE_CAPTION;
            } else if (viewProperty.styleReference().HEADING() != null) {
                style = STYLE_HEADING;
            } else {
                String id = viewProperty.styleReference().variable().ID().getText();
                style = ((Style) appModel.ids.get(id)).styleName;
            }
        }
        return style;
    }
}
