package io.rappt.antlr;

import io.rappt.AMLBaseVisitor;
import io.rappt.AMLParser;
import io.rappt.model.*;
import org.antlr.v4.runtime.misc.NotNull;
import io.rappt.model.*;

// Builds the ID map for the model
public class AMLIdVisitor extends AMLBaseVisitor {

    public AppModel model;

    public AMLIdVisitor(AppModel aModel) {
        this.model = aModel;
    }

    @Override
    public Object visitVariableDeclaration(@NotNull AMLParser.VariableDeclarationContext ctx) {
        String id = ctx.ID().getText();
        if (model.newIds.containsKey(id) && !model.newIds.get(id).equals(Parameter.class.getCanonicalName())) {
            model.addError(ctx.ID().getSymbol(), "Duplicate ID: " + ctx.ID().getText());
        } else {
            Class<? extends PIM> pim = LookupTables.declarationLookup.get(ctx.getParent().getClass());
            assert pim != null : "LookupTable missing declaration for: " + ctx.getParent().getClass().getCanonicalName();
            model.newIds.put(id, pim.getCanonicalName());
        }
        return super.visitVariableDeclaration(ctx);
    }

    // TODO: Below functions may need to be moved to AppModelVisitor
    @Override
    public Object visitScreen(@NotNull AMLParser.ScreenContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        Screen screen = new Screen(id);
        model.ids.put(id, screen);
        return super.visitScreen(ctx);
    }

    @Override
    public Object visitScreenParams(@NotNull AMLParser.ScreenParamsContext ctx) {
        String paramId = ctx.variableDeclaration().ID().getText();
        model.ids.put(paramId, new PimString());
        return super.visitScreenParams(ctx);
    }

    @Override
    public Object visitApi(@NotNull AMLParser.ApiContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        Api api = new Api(id);
        model.ids.put(id, api);
        return super.visitApi(ctx);
    }

    @Override
    public Object visitResource(@NotNull AMLParser.ResourceContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        String method = ctx.HTTP_METHOD().getText();
        Resource resource = new Resource(id, method);
        model.ids.put(id, resource);
        return super.visitResource(ctx);
    }

    @Override
    public Object visitMenu(@NotNull AMLParser.MenuContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        Feature feature = new Feature(id);
        model.ids.put(id, feature);
        return super.visitMenu(ctx);
    }

    @Override
    public Object visitStyle(@NotNull AMLParser.StyleContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        Style style = new Style(id, AppModelUtils.formatSTRING(ctx.STRING()));
        model.ids.put(id, style);
        return super.visitStyle(ctx);
    }

    @Override
    public Object visitButton(@NotNull AMLParser.ButtonContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        View.Button button = new View.Button(id);
        model.ids.put(id, button);
        return super.visitButton(ctx);
    }

    @Override
    public Object visitSource(@NotNull AMLParser.SourceContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        Source source = new Source(id);
        model.ids.put(id, source);
        return super.visitSource(ctx);
    }

    @Override
    public Object visitLabel(@NotNull AMLParser.LabelContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        View.Label label = new View.Label(id);
        model.ids.put(id, label);
        return super.visitLabel(ctx);
    }

    @Override
    public Object visitImage(@NotNull AMLParser.ImageContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        View.Image image = new View.Image(id);
        model.ids.put(id, image);
        return super.visitImage(ctx);
    }

    public Object visitList(@NotNull AMLParser.ListContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        View.DynamicList list = new View.DynamicList(id);
        model.ids.put(id, list);
        return super.visitList(ctx);
    }
}
