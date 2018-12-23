package io.rappt.antlr;

import io.rappt.model.*;
import org.antlr.v4.runtime.misc.NotNull;
import io.rappt.AMLBaseVisitor;
import io.rappt.AMLParser;
import io.rappt.model.*;

import java.util.Optional;

public class BehaviourVisitor extends AMLBaseVisitor {

    AppModel appModel;

    public BehaviourVisitor(AppModel appModel) {
        this.appModel = appModel;
    }

    @Override
    public Object visitBehaviours(@NotNull AMLParser.BehavioursContext ctx) {
        for (AMLParser.BehaviourPropertiesContext bpc : ctx.behaviourProperties()) {
            if (bpc.ON_CLICK() != null) {
                PIM pim = appModel.ids.get(bpc.variable().ID().toString());
                if (pim instanceof View.Button) {
                    View.Button button = (View.Button)pim;
                    if (bpc.instructionBlock() != null) {
                        for (AMLParser.InstructionsContext b : bpc.instructionBlock().instructions()) {
                            AMLParser.InstructionBlockContext block = bpc.instructionBlock();
                            retrieveInstruction(b, button.id).ifPresent(i -> {
                                button.onTouch.instructions.add(i);
                                if (block.CLOSE() != null) {
                                    button.onTouch.doesCloseActivity = Boolean.parseBoolean(block.BOOL().getText());
                                }
                            });
                        }
                    }
                }
            }
        }
        return super.visitBehaviours(ctx);
    }

    @Override
    public Object visitScreen(@NotNull AMLParser.ScreenContext ctx) {
        Screen screen = (Screen) appModel.ids.get(ctx.variableDeclaration().ID().getText());
        // TODO: Temp fix for data model
        if (screen.model != null) {
            screen.model.dataSources.forEach(source ->
            {
                Instruction.Call call = new Instruction.Call(source.api.id + source.resource.id, source.api.id, source.resource.id);
                // TODO: Remove onload
                screen.view.onLoad.instructions.add(call);
                screen.view.dynamicLists.forEach(dl -> {
                    dl.apiId = source.api.id;
                    dl.resourceId = source.resource.id;
                });

                // TODO: Change to collection
                if (!source.toBindings.isEmpty()) {
                    source.toBindings.keySet().forEach(c -> {
                            call.parameter = c;
                    });

                }
                screen.view.labels.forEach(l -> {
                        l.valuePath = Optional.ofNullable(source.fromBindings.get(l.id));
                        l.source = source;
                });
                screen.view.images.forEach(i -> {
                    i.valuePath = source.fromBindings.get(i.id);
                    i.source = source;
                });
                screen.view.dynamicLists.forEach(dl -> {
                    Optional.ofNullable(source.fromBindings.get(dl.id)).ifPresent(p -> dl.listFieldPath = p);
                    dl.listItemLayouts.forEach(lil -> lil.layout.labels.forEach(l -> {
                        l.valuePath = Optional.ofNullable(source.fromBindings.get(l.id));
                        l.source = source;
                        dl.listItemLayouts.forEach(il -> {
                            il.layout.images.forEach(i -> {
                                i.valuePath = source.fromBindings.get(i.id);
                                i.source = source;
                            });
                        });
                    }));
                });

            });
        }


        return super.visitScreen(ctx);
    }

    public AppModel getModel() {
        return appModel;
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

    private Instruction.ShowToast retrieveToast(AMLParser.ShowToastContext toastContext) {
        String id = toastContext.variableDeclaration().ID().getText();
        String label = AppModelUtils.formatSTRING(toastContext.STRING());
        Instruction.ShowToast showToast = new Instruction.ShowToast(id, label);
        return showToast;
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
}
