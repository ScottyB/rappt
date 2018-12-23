package io.rappt.antlr;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import io.rappt.AMLBaseVisitor;
import io.rappt.AMLParser;
import io.rappt.model.AppModel;
import io.rappt.model.Resource;
import io.rappt.model.View;

import java.util.regex.Pattern;

public class PIMSemanticVisitor extends AMLBaseVisitor {

    private static final long MAX_CALL_COUNT = 1;
    final public AppModel appModel;

    public PIMSemanticVisitor(final AppModel model) {
        appModel = model;
    }

    private static final String HEX_PATTERN = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    public String currentScreenId;

    @Override
    public Object visitScreen(@NotNull AMLParser.ScreenContext ctx) {
        Token token = ctx.variableDeclaration().ID().getSymbol();
        currentScreenId = ctx.variableDeclaration().ID().getText();
        if (!ctx.screenProperties().stream().anyMatch(s -> s.screenView() != null)) {
            appModel.addError(token, "Screen must have a group");
        }

        return super.visitScreen(ctx);
    }

    @Override
    public Object visitCall(@NotNull AMLParser.CallContext ctx) {
        if (ctx.PASSED() != null) {
            String param = ctx.ID().getText();
            boolean isPresent = appModel.passedParamsToScreen.get(param).stream().filter(s -> s.equals(currentScreenId)).findFirst().isPresent();
            if (!isPresent) {
                appModel.addError(ctx.ID().getSymbol(), "Screen is not passed " + param);
            }
        } else {
            if (appModel.passedParamsToScreen.containsValue(currentScreenId)) {
                appModel.addError(ctx.getStart(), "Screen needs to used passed param ");
            }
        }
        return super.visitCall(ctx);
    }

    @Override
    public Object visitTheme(@NotNull AMLParser.ThemeContext ctx) {
        if (ctx.primary().size() > 1) {
            appModel.addError(ctx.getStart(), "There should only be one primary colour.");
        }
        return super.visitTheme(ctx);
    }

    @Override
    public Object visitPrimary(@NotNull AMLParser.PrimaryContext ctx) {
        Pattern pattern = Pattern.compile(HEX_PATTERN);
        if (!pattern.matcher(appModel.primaryColour).matches()) {
            appModel.addError(ctx.getStart(), appModel.primaryColour + " is not a valid hex number.");
        }
        return super.visitPrimary(ctx);
    }

    @Override
    public Object visitInstructionBlock(@NotNull AMLParser.InstructionBlockContext ctx) {
        long callCount = ctx.instructions().stream().filter(i -> i.call() != null).count();
        if (callCount > MAX_CALL_COUNT) {
            Token token = ctx.instructions().stream().reduce((previous, current) -> current).get().getStart();
            appModel.addError(token, "Only 1 call instruction permitted");
        }
        return super.visitInstructionBlock(ctx);
    }

    @Override
    public Object visitResource(@NotNull AMLParser.ResourceContext r) {
        Resource resource = (Resource) appModel.ids.get(r.variableDeclaration().ID().getText());
        boolean missingEndPoint = true;
        for (AMLParser.ResourcePropertiesContext rp: r.resourceProperties()){
            Token token = rp.getStart();
            if (rp.END_POINT() != null) {
                missingEndPoint = false;
                String endPoint = resource.endPoint;
                if (!endPoint.startsWith("/")) {
                    appModel.addError(token, "Endpoint " + endPoint + " must start with a /");
                }
            }
        }
        if (missingEndPoint) {
            appModel.addError(r.getStart(), "Resource must have an endPoint property");
        }
        return super.visitResource(r);
    }

    @Override
    public Object visitList(@NotNull AMLParser.ListContext ctx) {
        if (!ctx.listProperties().stream().anyMatch(lp -> lp.uiBlock() != null || lp.data() != null)) {
            Token token = ctx.variableDeclaration().ID().getSymbol();
            appModel.addError(token, "List must have a group");
        }
        return super.visitList(ctx);
    }

    @Override
    public Object visitPolyline(@NotNull AMLParser.PolylineContext ctx) {
        AMLParser.MapPropertiesContext mpc = (AMLParser.MapPropertiesContext) ctx.getParent();
        AMLParser.MapContext mc = (AMLParser.MapContext) mpc.getParent();
        View.Map map = (View.Map) appModel.ids.get(mc.variableDeclaration().ID().getText());
        Token token = ctx.variable(0).ID().getSymbol();
        map.polyLines.forEach(p -> {
            if (!map.staticMarkers.stream().anyMatch(m -> m.id.equals(p.markerStartId) || m.id.equals(p.markerEndId))) {
                appModel.addError(token, "Marker IDs in polyline cannot be found");
            }
        });
        return super.visitPolyline(ctx);
    }

}
